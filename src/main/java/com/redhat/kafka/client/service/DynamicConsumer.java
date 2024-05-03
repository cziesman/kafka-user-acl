package com.redhat.kafka.client.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class DynamicConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicConsumer.class);

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd - HH:mm:ss.SSS z");

    private final List<Message> messages = new ArrayList<>();

    @Value("${kafka.bootstrap-server}")
    private String bootstrapServer;

    @Value("${kafka.truststore.location}")
    private String truststoreLocation;

    @Value("${kafka.truststore.password}")
    private String truststorePassword;

    @Value("${kafka.truststore.type}")
    private String truststoreType;

    public void newContainer(String topic, String username, String password) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory = kafkaListenerContainerFactory(username, password);
        ConcurrentMessageListenerContainer<String, String> container = factory.createContainer(topic);

        container.setupMessageListener((MessageListener<String, String>) record -> {

            LOG.info(" \nKey: " + record.key() + "\n" +
                    "Value: " + record.value() + "\n" +
                    "Topic: " + record.topic() + "\n" +
                    "Partition: " + record.partition() + "\n" +
                    "Offset:" + record.offset() + "\n" +
                    "Timestamp: " + timeAsString(record.timestamp()));

            saveMessage(record);
        });

        container.start();
    }

    public List<Message> getMessages() {

        return Collections.unmodifiableList(messages);
    }

    protected ConsumerFactory<String, String> consumerFactory(String username, String password) {

        return new DefaultKafkaConsumerFactory<>(getConsumerProperties(username, password));
    }

    protected ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(String username, String password) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory(username, password));
        factory.getContainerProperties().setGroupId(UUID.randomUUID().toString());
        factory.getContainerProperties().setSyncCommits(true);
        factory.setConcurrency(1);

        return factory;
    }

    protected Map<String, Object> getConsumerProperties(String username, String password) {

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        props.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-512");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, String.format("org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";", username, password));

        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststoreLocation);
        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword);
        props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, truststoreType);

        return props;
    }

    protected void saveMessage(ConsumerRecord<String, String> record) {

        Message incoming = new Message(timeAsString(record.timestamp()), record.value(), record.topic());

        messages.add(incoming);
    }

    protected String timeAsString(long timestamp) {

        return formatter.format(ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp / 1000L, 0), ZoneOffset.UTC));
    }

}
