FROM openjdk:14
RUN groupadd spring && adduser spring -g spring
USER spring:spring
ARG JAR_FILE=target/gateway.jar
COPY ${JAR_FILE} gateway.jar
ENTRYPOINT ["java","-jar","/gateway.jar"]