package com.epproject.ShortBrew.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String CLICK_EVENTS_EXCHANGE = "click_events";
    public static final String CLICK_EVENTS_QUEUE = "click_events.process";
    public static final String CLICK_EVENTS_ROUTING_KEY = "click.created";
    public static final String CLICK_EVENTS_DLQ = "click_events.process.dlq";

    @Bean
    public Queue clickEventsQueue() {
        return new Queue(CLICK_EVENTS_QUEUE, true); // Durable queue
    }

    @Bean
    public TopicExchange clickEventsExchange() {
        return new TopicExchange(CLICK_EVENTS_EXCHANGE);
    }

    @Bean
    public Binding clickEventsBinding(Queue clickEventsQueue, TopicExchange clickEventsExchange) {
        return BindingBuilder.bind(clickEventsQueue).to(clickEventsExchange).with(CLICK_EVENTS_ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
