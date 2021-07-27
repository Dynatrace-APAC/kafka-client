# Kafka observability with Dynatrace
<!-- ------------------------ -->
## Introduction

Apache Kafka is an open-source, distributed publish-subscribe message bus designed to be fast, scalable, and durable. Dynatrace has out-of-the-box support for Kafka process monitoring. [Read blog post](https://www.dynatrace.com/news/blog/introducing-kafka-process-monitoring)

In addition, leveraging on the power of Dynatrace's PurePath technology, we can further provide tracing capabilities within your application code.

This hands on tutorial guides you on the following:
- Deploying Zookeeper and Kafka cluster as docker containers
- Build and run Kafka Producer and Consumer micro-services as docker containers
- Configuring Dynatrace to gain tracing capabilities across the Producer and Consumer micro-services

## Pre-requisites
- VM/Cloud instance
  - Memory: 8GB
  - Disk space: at least 16 GB available 
- OS: Ubuntu 20 and above
- Docker engine for Ubuntu

### What You’ll Learn 
- Configure Dynatrace to gain purepath visibility in a Kafka Consumer micro-service
- Configure Dynatrace on a Kafka Producer in order to trace transactions from Producer to Consumer 

<!-- ------------------------ -->
## Preparing the environment

### Install Docker engine
If you have not installed Docker engine for Ubuntu, please follow the instructions [here](https://docs.docker.com/engine/install/ubuntu/)

## Deploy OneAgent
If you have not deployed the OneAgent, please do so before running the app.

<!-- ------------------------ -->
## Running the app
- Ensure that docker is installed
- Clone the repo from `https://github.com/Dynatrace-APAC/kafka-client.git`
  ```bash
  $ docker ps
  CONTAINER ID   IMAGE     COMMAND   CREATED   STATUS    PORTS     NAMES
  $
  $ git clone https://github.com/Dynatrace-APAC/kafka-client.git  
  ```
- Execute `start.sh` script
  ```bash
  $ cd kafka-client
  $ ./start.sh  
  ```

> The `start.sh` script will do the following in this sequence
>
> 1. Provision and start confluent **Zookeeper** as a container from public docker repository
>
> 2. Provision and start confluent **kafka cluster** as a **single node** container from public docker repository
>
> 3. Build the **Java** code from the `src` directory, into a container
>
> 4. Run the Kafka **Producer** code as one micro-service and,
>
> 5. Kafka **Consumer** code as another micro-service

**IGNORE**
***Formating not correct here***
Positive
: The `start.sh` script will do the following in this sequence
:
: 1. Provision and start confluent **Zookeeper** as a container from public docker repository
:
: 2. Provision and start confluent **kafka cluster** as a **single node** container from public docker repository
:
: 3. Build the **Java** code from the `src` directory, into a container
:
: 4. Run the Kafka **Producer** code as one micro-service and,
:
: 5. Kafka **Consumer** code as another micro-service
***-end-**

<!-- ------------------------ -->
## Configuring Dynatrace to show Kafka Consumer Service
Reference documentation: [Queue messaging custom services](https://www.dynatrace.com/support/help/how-to-use-dynatrace/transactions-and-services/custom-services/define-messaging-services/)

### a. Define a Custom Java Messaging Service
- Go to Settings > Server-side service monitoring > **Custom service detection**
- Select the Java tab
- Click the **Define messaging service** button
- Give the service a meaningful name
- Select **Apache Kafka**
- Click Find entry point and select the process group that contains your entry point. In this case, it is the **com.dynatrace.kafka.KafkaConsumerExample** process
- Find the class you want to instrument. In this case it is **com.dynatrace.kafka.KafkaConsumerExample**
- Select the required class and click Continue
- Select the method you want to instrument, in this case, you can only select **processRecord** and click Finish
- Remember to **SAVE**

> Apache Kafka is different from other messaging services. Its interfaces are designed to process messages in bulk, yet the purpose of Dynatrace is to follow single transactions. Dynatrace can only do this if you define a method that is responsible for processing a single Kafka message in your code and use this as the entry point of your Kafka messaging service. For Java, you must select a method that has a Kafka **org.apache.kafka.clients.consumer.ConsumerRecord** as an **argument**, not the class itself. The full package path is required!

**IGNORE**
***Formating not correct here***
Negative
: Apache Kafka is different from other messaging services. Its interfaces are designed to process messages in bulk, yet the purpose of Dynatrace is to follow single transactions. Dynatrace can only do this if you define a method that is responsible for processing a single Kafka message in your code and use this as the entry point of your Kafka messaging service. For Java, you must select a method that has a Kafka **org.apache.kafka.clients.consumer.ConsumerRecord** as an **argument**, not the class itself. The full package path is required!
***-end-***

### b. Explore the purepaths
- Go to Transactions & Service
- Wait for a few minutes for your custom messaging service to appear as a service
- Select it and investigate the purepaths
- You will notice that a **Apache Kafka Queue Listener** service is created automatically
- You can investigate the purepaths from the Kafka listener as well

<!-- ------------------------ -->
## Kafka Producer
When trying to trace the transactions from Producer to Consumer, it is important to take note of the following:
- Identify the class and method that would be triggering the **Apache Kafka Queue Listener**
- In most cases, it would be the **.send** method. An example is in the code snippet below
  ```java
  try {
     ProducerRecord<String, String> recordToSend = new ProducerRecord<>(TOPIC_NAME, null, text);
     producer.send(recordToSend, this::sendRecord);
  } catch (Exception e) {
     e.printStackTrace();
  } finally {
     producer.flush();
     producer.close();
  }
  ```

- Check if Dynatrace is instrumenting the java methods of the Producer, if not, chances are that you have to define a **custom service**
- If it is, but the Producer purepath is not linked to the consumer the only thing you can do is to use the OneAgent SDK to link it up
  > Do note that if you define a custom service for the Producer, it will create an entry point (i.e. start a new PurePath), thus creating another service. If your entry point is a method that is not linked up with the rest of the other business logic in the Producer code, you will ***not*** be able to have a complete **Service Flow**.

**IGNORE**
***Formating not correct here***
Negative
: Do note that if you define a custom service for the Producer, it will create an entry point (i.e. start a new PurePath), thus creating another service. If your entry point is a method that is not linked up with the rest of the other business logic in the Producer code, you will ***not*** be able to have a complete **Service Flow**.
***-end-***

### a. Define a Custom Java Service
For our sample application, the entry point is in the **com.dynatrace.kafka.ProducerInit** Class and the **public void init** method. Reference documentation: [Define custom services](https://www.dynatrace.com/support/help/how-to-use-dynatrace/transactions-and-services/custom-services/)

```java
public void init() {
    Producer<String, String> producer = createProducer();
    TracingKafkaProducer<>(producer, tracer);
    String text = "notrace";
    try {
        ProducerRecord<String, String> recordToSend = new ProducerRecord<>(TOPIC_NAME, null, text);
        producer.send(recordToSend, this::sendRecord);
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        producer.flush();
        producer.close();
    }
}
```

Why choose **public void init** instead of other methods in the code? The **public void init** method is considered an entry point to the code. In order for PurePaths to make sense, is a best practice to **choose an entry point**. In some other codes, it could the the **main** method for example. Do not choose any methods that go into loops.

### b. Explore the PurePaths
- Go to Transactions & Service
- Wait for a few minutes for your custom service to appear as a service
- Select it and investigate the purepaths
- You will notice that the **linkage** between the Producer and Consumer happens on the **Apache Kafka Queue Listener** tier
- That is how Dynatrace is able to generate a complete **Service Flow** for this sample app
- You can investigate the purepaths and meta-data from the Kafka listener as well
