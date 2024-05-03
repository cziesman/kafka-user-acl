package com.redhat.kafka.client.service;

import java.util.HashMap;
import java.util.Map;

import com.redhat.kafka.client.controller.KafkaRestException;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.stereotype.Component;

@Component
public class DynamicProducer {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicProducer.class);

    private final Map<String, KafkaTemplate<String, String>> templateMap = new HashMap<>();

    @Value("${kafka.bootstrap-server}")
    private String bootstrapServer;

    @Value("${kafka.truststore.location}")
    private String truststoreLocation;

    @Value("${kafka.truststore.password}")
    private String truststorePassword;

    @Value("${kafka.truststore.type}")
    private String truststoreType;

    public KafkaTemplate<String, String> newTemplate(String username, String password) {

        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory(username, password));
        templateMap.put(username, template);

        return template;
    }

    public void send(String topic, String message, String username) {

        if (templateMap.containsKey(username)) {
            KafkaTemplate<String, String> template = templateMap.get(username);
            try {
                template.send(topic, message);
            } catch (KafkaException t) {
                LOG.error(t.getCause().getMessage(), t);
                throw new KafkaRestException(t.getCause().getMessage(), 500);
            } catch (Throwable t) {
                LOG.error(t.getMessage(), t);
                throw new KafkaRestException(t.getMessage(), 500);
            }
        } else {
            throw new KafkaRestException("No RestTemplate found for " + username, 500);
        }
    }

    protected ProducerFactory<String, String> producerFactory(String username, String password) {

        return new DefaultKafkaProducerFactory<>(getProducerProperties(username, password));
    }

    protected Map<String, Object> getProducerProperties(String username, String password) {

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        props.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-512");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, String.format("org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";", username, password));

        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststoreLocation);
        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword);
        props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, truststoreType);

        return props;
    }

}
