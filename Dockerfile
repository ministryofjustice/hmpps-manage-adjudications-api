FROM eclipse-temurin:21.0.10_7-jdk-jammy AS builder

WORKDIR /app

COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle/ gradle/
RUN ./gradlew build || return 0

ARG BUILD_NUMBER
ENV BUILD_NUMBER=${BUILD_NUMBER:-1_0_0}

COPY . .
RUN ./gradlew clean assemble -Dorg.gradle.daemon=false

# Grab AWS RDS Root cert
RUN apt-get update && apt-get install -y curl
RUN curl https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem  > root.crt

FROM eclipse-temurin:21.0.10_7-jre-jammy
LABEL maintainer="HMPPS Digital Studio <info@digital.justice.gov.uk>"

ARG BUILD_NUMBER
ENV BUILD_NUMBER=${BUILD_NUMBER:-1_0_0}

RUN apt-get update && apt-get upgrade -y && apt-get clean && rm -rf /var/lib/apt/lists/*

ENV TZ="Europe/London"
RUN ln -snf "/usr/share/zoneinfo/$TZ" /etc/localtime && echo "$TZ" > /etc/timezone

RUN groupadd --gid 2000 appgroup && \
    useradd --uid 2000 --gid 2000 --system --create-home appuser

# Install AWS RDS Root cert into Java truststore
RUN mkdir -p /home/appuser/.postgresql
COPY --from=builder --chown=appuser:appgroup /app/root.crt /home/appuser/.postgresql/root.crt

WORKDIR /app
COPY --from=builder --chown=appuser:appgroup /app/build/libs/hmpps-manage-adjudications-api*.jar /app/app.jar
COPY --from=builder --chown=appuser:appgroup /app/build/libs/applicationinsights-agent*.jar /app/agent.jar
COPY --from=builder --chown=appuser:appgroup /app/applicationinsights.json /app
COPY --from=builder --chown=appuser:appgroup /app/applicationinsights.dev.json /app

USER 2000

ENTRYPOINT ["java", "-XX:+AlwaysActAsServerClassMachine", "-javaagent:/app/agent.jar", "-jar", "/app/app.jar"]
