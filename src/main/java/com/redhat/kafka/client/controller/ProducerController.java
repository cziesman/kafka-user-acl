package com.redhat.kafka.client.controller;

import java.util.Collections;

import com.redhat.kafka.client.service.DynamicProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProducerController {

    private static final String SEND_ERROR_MESSAGE = "send-error-message";

    private static final Logger LOG = LoggerFactory.getLogger(ProducerController.class);

    @Autowired
    private DynamicProducer dynamicProducer;

    @GetMapping(value = "/send")
    public String send(@RequestParam String message, @RequestParam String username, @RequestParam String topic) {

        LOG.info(message);

        dynamicProducer.send(topic, message, username);

        return "Sent [" + message.trim() + "]\n";
    }

    @ExceptionHandler(KafkaRestException.class)
    public ResponseEntity<Object> handleException(KafkaRestException ex) {

        LOG.error(ex.getMessage(), ex);
        final String error = ex.getMessage();

        return new ResponseEntity<>(error, headers(error), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    protected HttpHeaders headers(String error) {

        Assert.notNull(error, "error cannot be null");
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(SEND_ERROR_MESSAGE, Collections.singletonList(error));

        return headers;
    }

}
