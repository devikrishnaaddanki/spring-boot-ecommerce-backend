package com.example.demo.event;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.demo.event.OrderCreatedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.demo.repository.OrderRepository;
import com.example.demo.service.OrderEventProducer;

@Component
public class OrderCreatedApplicationEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(OrderCreatedApplicationEventListener.class);

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;

    public OrderCreatedApplicationEventListener(
            OrderRepository orderRepository,
            OrderEventProducer orderEventProducer) {

        this.orderRepository = orderRepository;
        this.orderEventProducer = orderEventProducer;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedApplicationEvent event) {

        log.info("Database transaction committed for order ID: {}",
                event.orderId());

        orderRepository.findById(event.orderId())
                .ifPresentOrElse(order -> {

                    OrderCreatedEvent kafkaEvent = new OrderCreatedEvent(
                            order.getId(),
                            order.getUser().getId(),
                            order.getTotalAmount(),
                            order.getStatus().name(),
                            order.getCreatedAt()
                    );

                    orderEventProducer.publishOrderCreatedEvent(kafkaEvent);

                }, () -> log.error(
                        "Order not found after transaction commit. Order ID: {}",
                        event.orderId()));
    }
}
