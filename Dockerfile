FROM amazoncorretto:17 AS builder
WORKDIR /app

RUN (yum install -y findutils && yum clean all) || (microdnf install -y findutils && microdnf clean all)

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

FROM amazoncorretto:17
WORKDIR /app

COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
