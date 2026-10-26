package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.demo.event.OrderCreatedEvent;

import jakarta.annotation.PostConstruct;

@Service
public class OrderEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(OrderEventConsumer.class);

    @PostConstruct
    public void checkConsumerBean() {
        log.info("OrderEventConsumer bean initialized successfully");
    }

    @KafkaListener(
            topics = "order-created",
            groupId = "ecommerce-group"
    )
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {

        log.info("Received OrderCreatedEvent from Kafka");
        log.info("Order ID: {}", event.getOrderId());
        log.info("User ID: {}", event.getUserId());
        log.info("Total Amount: {}", event.getTotalAmount());
        log.info("Order Status: {}", event.getStatus());
        log.info("Created At: {}", event.getCreatedAt());
    }
}
