FROM eclipse-temurin:21-jre-alpine

ENV APPLICATION_PORT=3001

RUN mkdir /app
RUN mkdir /app/data
COPY ./target/klukka-jar-with-dependencies.jar /app/klukka.jar

WORKDIR /app
CMD ["java", "-jar", "klukka.jar"]