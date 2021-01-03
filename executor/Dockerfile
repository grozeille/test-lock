FROM openjdk:14
RUN groupadd spring && adduser spring -g spring
USER spring:spring
ARG JAR_FILE=target/executor.jar
COPY ${JAR_FILE} executor.jar
ENTRYPOINT ["java","-jar","/executor.jar"]