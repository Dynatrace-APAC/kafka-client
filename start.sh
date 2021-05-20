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

# Validates if a network trace is created, and if not, creates it.
validate_network

# Add Jaeger to the trace network
validate_jaeger

# Start zookeeper
docker run -d --rm --network="trace" \
  -p 2181:2181 \
  --name zookeeper \
  --env ZOOKEEPER_CLIENT_PORT=2181 \
  --env KAFKA_OPTS="-Dzookeeper.4lw.commands.whitelist=*" \
  confluentinc/cp-zookeeper:5.5.1

validate_zookeeper

# Start kafka
docker run -d --rm --network="trace" \
  -p 9092:9092 \
  --name kafka \
  --env KAFKA_BROKER_ID="1" \
  --env KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092 \
  --env KAFKA_ZOOKEEPER_CONNECT="zookeeper:2181" \
  --env KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR="1" \
  --env KAFKA_DELETE_TOPIC_ENABLE='true' \
  confluentinc/cp-kafka:5.5.1

validate_kafka

# Build and run the Producer container
docker build -t mykafka .
docker run -d --rm --network="trace" --name kafkaproducer mykafka com.dynatrace.kafka.KafkaProducerExample

# Build and run the Consumer container
docker run -d --rm --network="trace" --name kafkaconsumer mykafka com.dynatrace.kafka.KafkaConsumerExample

#Cleaning builder image
docker rmi "$(docker images -q -f 'label=autodelete=true')"