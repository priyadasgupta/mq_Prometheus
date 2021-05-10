FROM maven:3.6.3-openjdk-8 as MAVEN_BUILD
COPY pom.xml /build/
COPY src /build/src/
WORKDIR /build/
RUN mvn clean package -DskipTests

FROM openjdk:8-jdk-alpine
COPY clientkey.jks clientkey.jks
COPY --from=MAVEN_BUILD /build/target/mq_prometheus_poc*.jar mq_prometheus.jar
ENTRYPOINT ["java","-jar","mq_prometheus.jar"]
