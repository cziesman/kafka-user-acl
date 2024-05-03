package com.redhat.kafka.client.service;

public class Message {

    private final String timestamp;

    private final String text;

    private final String topic;

    public Message(String timestamp, String text, String topic) {

        this.timestamp = timestamp;
        this.text = text;
        this.topic = topic;
    }

    public String getTimestamp() {

        return timestamp;
    }

    public String getText() {

        return text;
    }

    public String getTopic() {

        return topic;
    }

}
