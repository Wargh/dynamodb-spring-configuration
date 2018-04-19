# README #

### What is this repository for? ###

This repository is used to automatically load application properties from a DynamoDB "configuration" table.

### How do I use dynamodb-spring-configuration ###

Simply include dynamodb-spring-configuration as a project dependency. 
On application startup Spring will automatically load entries from the configuration DynamoDB table in the following order:

* default
* active spring profiles, in order

The properties loaded by dynamodb-spring-configuration will override any specified in application yml or properties files included in the application but will be overridden by system properties.

It is possible to disable dynamodb-spring-configuration by specifying -Ddynamodb.configuration.enabled=false.

### How does it work ###

dynamodb-spring-configuration is implemented as a standard Spring EnvironmentPostProcessor. It is registered for loading in META-INF/spring.factories as normal.
