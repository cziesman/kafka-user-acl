package com.redhat.kafka.client.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/*
 * Provides a quick and dirty way to create two consumers and two producers.
 */
@Component
public class Initializer {

    @Value("${kafka.topic0.name}")
    private String topic0Name;

    @Value("${kafka.topic1.name}")
    private String topic1Name;

    @Value("${kafka.user0.username}")
    private String user0Name;

    @Value("${kafka.user0.password}")
    private String user0Password;

    @Value("${kafka.user1.username}")
    private String user1Name;

    @Value("${kafka.user1.password}")
    private String user1Password;

    @Autowired
    private DynamicConsumer dynamicConsumer;

    @Autowired
    private DynamicProducer dynamicProducer;

    @PostConstruct
    void setup() {

        dynamicConsumer.newContainer(topic0Name, user0Name, user0Password);

        dynamicConsumer.newContainer(topic1Name, user1Name, user1Password);

        dynamicProducer.newTemplate(user0Name, user0Password);

        dynamicProducer.newTemplate(user1Name, user1Password);
    }

}
