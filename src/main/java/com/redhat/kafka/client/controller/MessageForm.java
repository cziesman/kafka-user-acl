package com.redhat.kafka.client.controller;

public class MessageForm {

    private String topic;

    private String username;

    private String message;

    public String getTopic() {

        return topic;
    }

    public void setTopic(String topic) {

        this.topic = topic;
    }

    public String getUsername() {

        return username;
    }

    public void setUsername(String username) {

        this.username = username;
    }

    public String getMessage() {

        return message;
    }

    public void setMessage(String message) {

        this.message = message;
    }

    @Override
    public String toString() {

        return "MessageForm{" +
                "topic='" + topic + '\'' +
                ", username='" + username + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

}
