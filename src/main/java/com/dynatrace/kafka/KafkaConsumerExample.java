package com.dynatrace.kafka;

import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.contrib.kafka.TracingKafkaConsumer;
import io.opentracing.contrib.kafka.TracingKafkaUtils;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class KafkaConsumerExample {
    public static final int INTERVAL = 10000;
    private final static String TOPIC_NAME = ProducerInit.TOPIC_NAME;

    public static void main(String[] args) {
        //try (Tracer tracer = MyTracer.init("KafkaConsumerTrace")) {
            Consumer<String, String> consumer = KafkaConsumerExample.createConsumer();
            //TracingKafkaConsumer<String, String> tracingConsumer = new TracingKafkaConsumer<>(consumer, tracer);

            //tracingConsumer.subscribe(Collections.singletonList(TOPIC_NAME));
            consumer.subscribe(Collections.singletonList(TOPIC_NAME));
            //try {
                String receivedText = null;
                while (true) {
                    //ConsumerRecords<String, String> records = tracingConsumer.poll(Duration.ofMillis(100));
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                    for (ConsumerRecord<String, String> record : records) {
                        receivedText = record.value();
                        if (receivedText != null) {
                            //SpanContext spanContext = TracingKafkaUtils.extractSpanContext(record.headers(), tracer);
                            processRecord(record);
                        }
                    }
                    try {
                        Thread.sleep(INTERVAL);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            /*} finally {
                tracingConsumer.close();
            }*/
        /*} catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    private static void processRecord(ConsumerRecord<String, String> record) {
        System.out.println(
                String.format(
                        "Message received ==> topic = %s, partition = %s, offset = %s, key = %s, value = %s",
                        record.topic(), record.partition(), record.offset(), record.key(), record.value()
                )
        );
    }

    private static Consumer<String, String> createConsumer() {
        String kafkaService = System.getenv("KAFKA_SERVICE");
        if (kafkaService == null) {
            kafkaService = "kafka:9092";
        }

        Properties kafkaProps = new Properties();
        kafkaProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaService);
        kafkaProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test_consumer_group");
        kafkaProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        kafkaProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return new KafkaConsumer<String, String>(kafkaProps);
    }
}
