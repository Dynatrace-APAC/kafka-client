package com.dynatrace.kafka;

import io.opentracing.Tracer;

public class KafkaProducerExample {
    public static final int INTERVAL = 60000;

    public static void main(String[] args) {
        //try (Tracer initializedTracer = MyTracer.init("KafkaProducerTrace")) {
        try {
            while (true) {
                new ProducerInit().init();
                Thread.sleep(INTERVAL);
            }
        } catch (InterruptedException e) {
                e.printStackTrace();
        }
    }
}
