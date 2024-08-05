FROM eclipse-mosquitto:2.0.18
#FROM eclipse-mosquitto:1.4.12
#FROM eclipse-mosquitto:1.4.4

RUN mkdir -p /mosquitto/data/
COPY ./Docker/mosquitto.conf /mosquitto/config/mosquitto.conf
VOLUME /mosquitto/data/
