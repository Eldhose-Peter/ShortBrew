package com.epproject.ShortBrew.service;

import com.epproject.ShortBrew.config.RabbitConfig;
import com.epproject.ShortBrew.model.ClickEventPayload;
import com.epproject.ShortBrew.security.RequestIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
     * Publishes a click event to RabbitMQ, propagating MDC trace/request correlation context.
     * Exceptions are caught and logged so that publishing failures do not crash the user's redirect request.
     */
    public void publishClickEvent(Long urlId, String shortCode, String referrer, String userAgent, String ipHash) {
        String requestId = MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY);
        String traceId = MDC.get(RequestIdFilter.MDC_TRACE_ID_KEY);
        String spanId = MDC.get(RequestIdFilter.MDC_SPAN_ID_KEY);

        try {
            ClickEventPayload payload = new ClickEventPayload(
                urlId,
                shortCode,
                referrer,
                userAgent,
                ipHash,
                Instant.now().toString(),
                0,
                requestId,
                traceId,
                spanId
            );

            rabbitTemplate.convertAndSend(
                RabbitConfig.CLICK_EVENTS_EXCHANGE,
                RabbitConfig.CLICK_EVENTS_ROUTING_KEY,
                payload,
                message -> {
                    message.getMessageProperties().setDeliveryMode(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT);
                    if (requestId != null) {
                        message.getMessageProperties().setHeader("x-request-id", requestId);
                    }
                    if (traceId != null) {
                        message.getMessageProperties().setHeader("x-trace-id", traceId);
                    }
                    if (spanId != null) {
                        message.getMessageProperties().setHeader("x-span-id", spanId);
                    }
                    return message;
                }
            );
            logger.debug("Successfully published click event to RabbitMQ for URL ID: {} with request_id: {}", urlId, requestId);
        } catch (Exception e) {
            logger.error("Failed to publish click event to RabbitMQ for URL ID: {}, shortCode: {}", urlId, shortCode, e);
        }
    }
}
