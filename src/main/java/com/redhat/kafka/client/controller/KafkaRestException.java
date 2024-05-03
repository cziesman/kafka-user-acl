package com.redhat.kafka.client.controller;

public class KafkaRestException extends RuntimeException {

    private final int statusCode;

    public KafkaRestException(String message, int statusCode) {

        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {

        return statusCode;
    }

}
