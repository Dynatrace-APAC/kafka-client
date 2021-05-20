#!/bin/bash

JAEGER_NAME="jaeger"

validate_network() {
  networkController=false;

  for networkName in $(docker network ls --format '{{.Name}}')
  do
    if [ "$networkName" = "trace" ]
    then
      networkController=true;
    fi
  done

  if [ "$networkController" = false ]
  then
    docker network create trace
  fi
}

validate_jaeger() {
  if [ "$(docker ps --filter name="$JAEGER_NAME" --format '{{.Names}}')" = "$JAEGER_NAME" ]
  then
    docker network connect trace jaeger
  fi
}

validate_zookeeper() {
  zookeeperMode=""
  while [ "$zookeeperMode" = "" ]
  do
    zookeeperMode=$(docker run --network=trace --rm confluentinc/cp-zookeeper:5.5.1 bash -c "echo stat | nc zookeeper 2181 | grep Mode")
  done
}

validate_kafka() {
  kafkaStart=""
  while [ "$kafkaStart" = "" ]
  do
    kafkaStart=$(docker logs kafka | grep started)
  done
}

# Start zookeeper
validate_zookeeper

# Start kafka
validate_kafka

# clean up old image first to be safe
docker stop kafkaproducer kafkaconsumer
docker rmi "$(docker images -q -f 'label=kafkaclient')"

# Build 
docker build -t mykafka .

# run the Producer container
docker run -d --rm --network="trace" --name kafkaproducer mykafka com.dynatrace.kafka.KafkaProducerExample

# run the Consumer container
docker run -d --rm --network="trace" --name kafkaconsumer mykafka com.dynatrace.kafka.KafkaConsumerExample

#Cleaning builder image
docker rmi "$(docker images -q -f 'label=autodelete=true')"
