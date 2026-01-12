package poc.globo.globostreaming.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.payment.exchange}")
    private String paymentExchange;

    @Value("${rabbitmq.payment.queue.processing}")
    private String paymentProcessingQueue;

    @Value("${rabbitmq.payment.queue.result}")
    private String paymentResultQueue;

    @Value("${rabbitmq.payment.queue.dlq}")
    private String paymentDlq;

    @Value("${rabbitmq.payment.routing-key.processing}")
    private String paymentProcessingRoutingKey;

    @Value("${rabbitmq.payment.routing-key.result}")
    private String paymentResultRoutingKey;

    @Value("${rabbitmq.payment.routing-key.dlq}")
    private String paymentDlqRoutingKey;


    @Bean
    public TopicExchange paymentExchange() {
        return ExchangeBuilder
                .topicExchange(paymentExchange)
                .durable(true)
                .build();
    }


    @Bean
    public Queue paymentProcessingQueue() {
        return QueueBuilder
                .durable(paymentProcessingQueue)
                .withArgument("x-dead-letter-exchange", paymentExchange)
                .withArgument("x-dead-letter-routing-key", paymentDlqRoutingKey)
                .build();
    }


    @Bean
    public Queue paymentResultQueue() {
        return QueueBuilder
                .durable(paymentResultQueue)
                .build();
    }


    @Bean
    public Queue paymentDeadLetterQueue() {
        return QueueBuilder
                .durable(paymentDlq)
                .build();
    }


    @Bean
    public Binding paymentProcessingBinding(Queue paymentProcessingQueue, TopicExchange paymentExchange) {
        return BindingBuilder
                .bind(paymentProcessingQueue)
                .to(paymentExchange)
                .with(paymentProcessingRoutingKey);
    }


    @Bean
    public Binding paymentResultBinding(Queue paymentResultQueue, TopicExchange paymentExchange) {
        return BindingBuilder
                .bind(paymentResultQueue)
                .to(paymentExchange)
                .with(paymentResultRoutingKey);
    }


    @Bean
    public Binding paymentDlqBinding(Queue paymentDeadLetterQueue, TopicExchange paymentExchange) {
        return BindingBuilder
                .bind(paymentDeadLetterQueue)
                .to(paymentExchange)
                .with(paymentDlqRoutingKey);
    }


    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }


    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }

    public String getPaymentExchange() {
        return paymentExchange;
    }

    public String getPaymentProcessingQueue() {
        return paymentProcessingQueue;
    }

    public String getPaymentResultQueue() {
        return paymentResultQueue;
    }

    public String getPaymentDlq() {
        return paymentDlq;
    }

    public String getPaymentProcessingRoutingKey() {
        return paymentProcessingRoutingKey;
    }

    public String getPaymentResultRoutingKey() {
        return paymentResultRoutingKey;
    }

    public String getPaymentDlqRoutingKey() {
        return paymentDlqRoutingKey;
    }
}
