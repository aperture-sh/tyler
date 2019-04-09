FROM openjdk:8-jdk-alpine AS build

ADD . /build
WORKDIR /build

RUN ./gradlew shadowJar

FROM openjdk:8-jre-alpine
MAINTAINER Florian Zouhar <zouhar@marauder.io>

ENV APPLICATION_USER ktor
RUN adduser -D -g '' $APPLICATION_USER

RUN mkdir /app
RUN mkdir /storage
RUN chown -R $APPLICATION_USER /app
RUN chown -R $APPLICATION_USER /storage

USER $APPLICATION_USER

EXPOSE 23513/tcp

COPY --from=build /build/build/libs/tyler-fat.jar /app/tyler-fat.jar
COPY --from=build /build/scripts/start.sh /app/start.sh
WORKDIR /app

ENTRYPOINT ./start.sh
