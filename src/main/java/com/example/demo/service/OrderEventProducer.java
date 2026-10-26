package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.event.OrderCreatedEvent;

@Service
public class OrderEventProducer {

    private static final Logger log =
            LoggerFactory.getLogger(OrderEventProducer.class);

    private static final String TOPIC = "order-created";

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public OrderEventProducer(
            KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreatedEvent(OrderCreatedEvent event) {

        kafkaTemplate.send(TOPIC, event.getOrderId().toString(), event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error(
                                "Failed to publish order-created event for order ID: {}",
                                event.getOrderId(),
                                exception);
                    } else {
                        log.info(
                                "Published order-created event for order ID: {} to topic: {}",
                                event.getOrderId(),
                                result.getRecordMetadata().topic());
                    }
                });
    }
}
