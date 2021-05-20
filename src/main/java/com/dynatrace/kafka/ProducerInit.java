package com.dynatrace.kafka;

import io.opentracing.Tracer;
import io.opentracing.contrib.kafka.TracingKafkaProducer;
import io.opentracing.util.GlobalTracer;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerInit {
	public static final Logger logger = LoggerFactory.getLogger(ProducerInit.class.getName());
	public static final String TOPIC_NAME = "test-topic";

	public void init() {
		//Tracer tracer = GlobalTracer.get();
		
		Producer<String, String> producer = createProducer();
		//TracingKafkaProducer<String, String> tracingProducer = new TracingKafkaProducer<>(producer, tracer);

		String text = "notrace";

		try {
			ProducerRecord<String, String> recordToSend = new ProducerRecord<>(TOPIC_NAME, null, text);
			//tracingProducer.send(recordToSend, this::sendRecord);
			producer.send(recordToSend, this::sendRecord);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			//tracingProducer.flush();
			//tracingProducer.close();
			producer.flush();
			producer.close();
		}
	}

	private void sendRecord(RecordMetadata recordMetadata, Exception e) {
		if (e == null) {
			logger.info("Message Sent. topic={}, partition={}, offset={}", recordMetadata.topic(),
					recordMetadata.partition(), recordMetadata.offset());
		} else {
			logger.error("Error while sending message. ", e);
		}
	}

	private static Producer<String, String> createProducer() {
		String kafkaService = System.getenv("KAFKA_SERVICE");
		if (kafkaService == null) {
			kafkaService = "kafka:9092";
		}
		
		Properties kafkaProps = new Properties();
		kafkaProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaService);
		kafkaProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		kafkaProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		return new KafkaProducer<String, String>(kafkaProps);
	}
}
