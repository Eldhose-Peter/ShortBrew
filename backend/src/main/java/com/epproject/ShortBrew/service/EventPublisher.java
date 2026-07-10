package com.epproject.ShortBrew.service;

import com.epproject.ShortBrew.config.RabbitConfig;
import com.epproject.ShortBrew.model.ClickEventPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(EventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publishes a click event to RabbitMQ.
     * Exceptions are caught and logged so that publishing failures do not crash the user's redirect request.
     */
    public void publishClickEvent(Long urlId, String shortCode, String referrer, String userAgent, String ipHash) {
        try {
            ClickEventPayload payload = new ClickEventPayload(
                urlId,
                shortCode,
                referrer,
                userAgent,
                ipHash,
                Instant.now().toString(),
                0
            );

            rabbitTemplate.convertAndSend(
                RabbitConfig.CLICK_EVENTS_EXCHANGE,
                RabbitConfig.CLICK_EVENTS_ROUTING_KEY,
                payload,
                message -> {
                    message.getMessageProperties().setDeliveryMode(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT);
                    return message;
                }
            );
            logger.debug("Successfully published click event to RabbitMQ for URL ID: {}", urlId);
        } catch (Exception e) {
            logger.error("Failed to publish click event to RabbitMQ for URL ID: {}, shortCode: {}", urlId, shortCode, e);
        }
    }
}
