## Kafka SCRAM-SHA-512 Authentication and ACL Demo

This project provides a very simple demonstration of a Kafka broker that uses SCRAM-SHA-512 for client authentication along with ACLs for Kafka topics, and a Java Kafka client that connects to a Kafka broker.

It depends on the *spring-kafka* library to provide the plumbing needed to connect to the Kafka broker and to send and receive messages.

It also uses the *openshift-maven-plugin* to simplify deployment of the demo application to an Openshift project.

### Assumptions

The demo assumes that the _AMQ Streams_ operator is used to manage Kafka, and that the developer can login to Openshift with access to Kafka resources.

The demo application is deployed into its own project. In this case we use *kafka-client* for the project name. Kafka is deployed in the *kafka* project.

The demo application uses a topic named *my-topic*. This topic should be created in Kafka using the _AMQ Streams_ operator.


### Kafka Configuration

In order to allow access to Kafka from outside Openshift, a route needs to be defined. To create the necessary route, update the *listeners* section of the YAML file for the cluster with the following snippet. Since SCRAM-SHA-512 authentication will be used, the listener for PORT 9094 needs to include the authentication details. In this case, the cluster is named *my-cluster*.

      - name: plain
        port: 9092
        tls: false
        type: internal
      - authentication:
          type: scram-sha-512
        name: tls
        port: 9093
        tls: true
        type: internal
      - authentication:
          type: scram-sha-512
        name: external
        port: 9094
        tls: true
        type: route

The name for the listener on port 9094 is arbitrary. Here we use *external* to indicate that the listener is for clients that are external to Openshift.

Since Access Control Lists (ACLs) will be used to control access to the Kafka topics, the authorization scheme needs to be defined for the Kafka cluster. Note that `simple` is the only available choice since an external authorization provider is not being used.

    authorization:
      type: simple

Two Kafka topics called *my-topic-0* and *my-topic-1* need to be created with ten partitions. The YAML should appear similar to the following.

#### my-topic-0

    apiVersion: kafka.strimzi.io/v1beta2
    kind: KafkaTopic
    metadata:
      labels:
        strimzi.io/cluster: my-cluster
      name: my-topic
      namespace: kafka
    spec:
      config: {}
      partitions: 10
      replicas: 3

#### my-topic-1

    kind: KafkaTopic
    apiVersion: kafka.strimzi.io/v1beta2
    metadata:
      name: my-topic-1
      labels:
        strimzi.io/cluster: my-cluster
      namespace: kafka
    spec:
      partitions: 10
      replicas: 3
      config:
        retention.ms: 604800000
        segment.bytes: 1073741824

The next step is to create two Kafka Users using the _AMQ Streams_ operator. Create `kafka-user-0` and `kafka-user-1` in the `kafka-cluster`. The authentication type must be `scram-sha-512`. The YAML should appear similar to the following. Note that *my-user-0* has access to *my-topic-0* and *my-user-1* has access to *my-topic-1*. Note also that group `Read` access has been defined for consumers.

#### my-user-0

    kind: KafkaUser
    apiVersion: kafka.strimzi.io/v1beta2
    metadata:
      name: my-user-0
      labels:
        strimzi.io/cluster: my-cluster
      namespace: kafka
    spec:
      authentication:
        type: scram-sha-512
      authorization:
        type: simple
    acls:
      - host: '*'
        operations:
          - Read
          - Describe
          - Write
          - Create
        resource:
          name: my-topic-0
          patternType: literal
          type: topic
      - host: '*'
        operations:
          - Read
        resource:
          name: '*'
          patternType: literal
          type: group
    type: simple

#### my-user-1

    kind: KafkaUser
    apiVersion: kafka.strimzi.io/v1beta2
    metadata:
      name: my-user-1
      labels:
        strimzi.io/cluster: my-cluster
      namespace: kafka
    spec:
      authentication:
        type: scram-sha-512
      authorization:
        type: simple
        acls:
          - host: '*'
            operations:
              - Read
              - Describe
              - Write
              - Create
            resource:
              name: my-topic-1
              patternType: literal
              type: topic
          - host: '*'
            operations:
              - Read
            resource:
              name: '*'
              patternType: literal
              type: group
        type: simple

### User Credentials

Since SCRAM-SHA-512 authentication uses a user name and password, the credentials need to be retrieved from Openshift. When the _AMQ Streams_ operator creates the user *my-user-0*, it also creates a Secret named *my-user-0* that contains the password. 

Use the following command to extract the user password.

    oc get secret my-user-0 -n kafka -o jsonpath='{.data.password}' | base64 -d

The password will need to be configured in the client application, and will be used to authenticate against the Kafka broker. Repeat this configuration for user *my-user-1*.


### Truststore

The cluster will also have a certificate defined in a Secret. This demo assumes that the default self-signed certificate is used. In this case, the secret is named _my-cluster-cluster-ca-cert_.

Use the following commands to extract the cluster truststore and associated password.

    oc get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.p12}' | base64 -d > truststore.p12
    
    oc get secret my-cluster-cluster-ca-cert -n kafka -o jsonpath='{.data.ca\.password}' | base64 -d

When running the demo on a local machine, the truststore file needs to be accessible. For this demo, it is placed in the `src/main/resources/certs` directory and the path is configured for Kafka in `application.yaml`. The password that was extracted is also configured in `application.yaml`. Note that in a production environment, the password would be stored in a Secret or retrieved from a secure application like Vault.

In order to deploy the client application on Openshift, the truststore must be available via a Secret. Luckily, the openshift-maven-plugin makes this easy. Use the following commands to convert the truststore file into a secret that can be configured in a template for use by the openshift-maven-plugin:

    oc create secret generic dontcare --from-file=./src/main/resources/certs/truststore.p12 -o yaml --dry-run=client

Use the YAML output from the commands to create the definition for a Secret named *kafka-client-secret*, which can be found in the file `src/main/resources/secret.yaml`.

Create the secret:

    oc apply -f src/main/resources/secret.yaml -n kafka-client

### Client Application

The client application is built using Maven, Spring Boot, and the *spring-kafka* library. It consists of a `DynamicConsumer` and a `DynamicProducer`. By default, *spring-kafka* only supports a single producer, a consumer, and a single set of user credentials in a Spring Boot application. As a result, the `@KafkaListener` annotation and the default Spring Boot configuration via YAML cannot be used. The *application.yaml* file for this demo contains the configuration necessary to create producers and consumers manually:

    kafka:
      bootstrap-server: my-cluster-kafka-bootstrap-kafka.apps.cluster-4npj5.dynamic.redhatworkshops.io:443
      topic0:
        name: my-topic-0
      topic1:
        name: my-topic-1
      truststore:
        location: src/main/resources/certs/truststore.p12
        password: MouP8C5Ws3zr
        type: PKCS12
      user0:
        password: BHCUgbbnitSTAp0oqujQjlCgyuvsd0lY
        username: my-user-0
      user1:
        password: dN6mp9QHA3yFysL4EyB1SHwCoynuetHi
        username: my-user-1

The `Initializer` class uses these properties in conjunction with the `DynamicConsumer` class and the `DynamicProducer` class to construct two consumers that use different topics and credentials and two producers that use different credentials. This allows for a demonstration of how ACLs can be used to control access to Kafka topics.

#### DynamicConsumer

The `DynamicConsumer` uses the `ConcurrentKafkaListenerContainerFactory` class and the `ConcurrentMessageListenerContainer`, provided by *spring-kafka*, to implement a listener on an individual topic with individual credentials.

    container.setupMessageListener((MessageListener<String, String>) record -> {

        LOG.info(" \nKey: " + record.key() + "\n" +
                "Value: " + record.value() + "\n" +
                "Topic: " + record.topic() + "\n" +
                "Partition: " + record.partition() + "\n" +
                "Offset:" + record.offset() + "\n" +
                "Timestamp: " + timeAsString(record.timestamp()));

        saveMessage(record);
    });

The container that is created listens on the specified topic, logs the details of each incoming message, and saves the message to an in-memory data store.

#### DynamicProducer

The `DynamicProducer` uses the `KafkaTemplate` class and the `DefaultKafkaProducerFactory` class, provided by *spring-kafka*, to send messages to a topic.

    KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory(username, password));

    try {
        template.send(topic, message);
    } catch (KafkaException t) {
        LOG.error(t.getCause().getMessage(), t);
        throw new KafkaRestException(t.getCause().getMessage(), 500);
    } catch (Throwable t) {
        LOG.error(t.getMessage(), t);
        throw new KafkaRestException(t.getMessage(), 500);
    }

#### Web User Interface

##### Sending Messages

The application also provides a simple web interface for sending messages. The interfaces supports the selection of valid and invalid combinations of Kafka users and topics.

When running locally, the web interface can be viewed at:

    http://localhost:9090/demo/send

When deployed to Openshift, the web interface can be viewed using a URL similar to the following:

    http://client-kafka-client.apps.cluster-jccqj.dynamic.redhatworkshops.io/demo/send

The actual hostname can be retrieved from the Openshift route for the client application.

##### Viewing received Messages

The application provides a very simple web interface that displays all messages received on the topics *my-topic-0* and *my-topic-1*. The interface is built using the Thymeleaf library and uses Ajax to update the display dynamically.

When running locally, the web interface can be viewed at:

    http://localhost:9090/msg/list

When deployed to Openshift, the web interface can be viewed using a URL similar to the following:

    http://client-kafka-client.apps.cluster-jccqj.dynamic.redhatworkshops.io/msg/list

The actual hostname can be retrieved from the Openshift route for the client application.

### Local Deployment

The `application.yaml` file needs to be updated with a few values in order for the application to run successfully.

Set the values for the Kafka bootstrap server.

    kafka-server: my-cluster-kafka-bootstrap-kafka.apps.cluster-jccqj.dynamic.redhatworkshops.io:443

Set the passwords for the new truststore.

        truststore:
          location: src/main/resources/certs/truststore.p12
          password: ToRmZ8qgDC1H
          type: PKCS12

The demo client application is deployed using the following command:

    mvn clean spring-boot:run

This command will compile the code, build a JAR file, and run the JAR file.

Once the application is running, it can be tested using a command similar to the following:

    curl http://localhost:9090/msg/send?message=hello

If the message is sent, the response should be `Sent [hello]`.

### Openshift Deployment

The openshift-maven-plugin uses the file `src/main/jkube/deploymentconfig.yaml` to extract the data from the *kafka-client-secret* and to mount the truststore file at a filesystem location where they are accessible by the demo client application.

The demo client application is deployed using the following command:

    mvn clean oc:build oc:deploy -Popenshift

This command will run an S2I build on Openshift to compile the code, build a JAR file, create an image, push the image to Openshift, and deploy the image.

Once the application is running, it can be tested using a command similar to the following:

    curl http://scram-sha-demo-kafka-client.apps.cluster-jccqj.dynamic.redhatworkshops.io/msg/send?message=hello

The actual hostname can be retrieved from the Openshift route for the client application.

If the message is sent, the response should be `Sent [hello]`.

