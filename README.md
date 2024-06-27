# netty-mqtt-websocket-client
A simple MQTT Websocket client written in Java using Netty.

# Netty Based Websocket MQTT Client

## Overview
This is a very simple example of a MQTT client which support connecting to a broker using Websockets.
SSL connections are currently unsupported.
It is simply provided as a starting point for more complex implementations as I couldn't find amy other working examples.

## Building/Requirements
The code is intended to be simply built in Intellij. I haven't used Maven or Gradle.
The code assumes the following files are within a 'libs' folder.
Not all maybe required, but it will ensure the code builds.

````
bcpkix-jdk15on-156.jar
bcprov-jdk15on-156.jar
commons-pool-1.6.jar
guava-31.1-jre.jar
log4j-1.2-api-2.17.0.jar
log4j-1.2.17.jar
log4j-core-2.17.0.jar
netty-all-4.1.100.Final.jar
netty-buffer-4.1.100.Final.jar
netty-codec-4.1.100.Final.jar
netty-codec-dns-4.1.100.Final.jar
netty-codec-haproxy-4.1.100.Final.jar
netty-codec-http-4.1.100.Final.jar
netty-codec-mqtt-4.1.100.Final.jar
netty-codec-socks-4.1.100.Final.jar
netty-common-4.1.100.Final.jar
netty-handler-4.1.100.Final.jar
netty-handler-proxy-4.1.100.Final.jar
netty-resolver-4.1.100.Final.jar
netty-resolver-dns-4.1.100.Final.jar
netty-transport-4.1.100.Final.jar
netty-transport-classes-epoll-4.1.100.Final.jar
netty-transport-classes-kqueue-4.1.100.Final.jar
netty-transport-native-unix-common-4.1.100.Final.jar
slf4j-api-1.7.25.jar
slf4j-simple-1.7.25.jar
````

## Configuration
By default the code connects to the public HiveMQ broker using
[ws://broker.hivemq.com:8000/mqtt]()

as documented [here](hivemq.com/mqtt/public-mqtt-broker/) 


Other brokers use port 8083 as the default unsecured port and
8084 for secured connections.

The public EMQX broker configuration is documented [here](https://www.emqx.com/en/mqtt/public-mqtt5-broker)

Mosquitto usually defaults to port 9001 for Websocket connections.

## Limitations
The code has not been tested for any sort of MQTTv5 compliance.
It is intended as a starting point for a more complete Netty based MQTT Client implementation such as
[this](https://github.com/jeffreykog/netty-mqtt) 

Far more implementation and error handling is required.


## Known Issues

At least in my testing Mosquitto version 2.0.18 (on Windows platform) does not support
subscriptions with QOS 0 (AT_MOST_ONCE ) over Websockets from this code. It closes the connection without logging anything.
Other MQTT clients can subscribe to Mosquitto using QOS0.

However, the same subscription code works on other brokers and QOS1 works as expected on Mosquitto





