#
# Build stage
#
FROM maven:3.6.0-jdk-11-slim AS build
LABEL autodelete="true"
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn clean package -f /home/app/pom.xml

#
# Package stage
#
FROM openjdk:11-jre-slim
LABEL kafkaclient="latest"
COPY --from=build /home/app/target/lib /usr/local/lib/
COPY --from=build /home/app/target/kafkaclient-1.0-SNAPSHOT.jar /usr/local/kafkaclient.jar
ENV JAEGER_AGENT_HOST=jaeger JAEGER_AGENT_PORT=6831
ENTRYPOINT ["java", "-cp", "/usr/local/kafkaclient.jar"]
