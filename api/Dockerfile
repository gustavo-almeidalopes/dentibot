# syntax=docker/dockerfile:1
#
# Imagem da API (ST-37). Duas etapas: a de build tem JDK e Maven e fica para
# trás; a final tem só o JRE, a aplicação em camadas e um usuário sem root.
#
# Base pinada por digest, não por tag: "25-jre-alpine" aponta para outra imagem
# amanhã, e o build de hoje não se reproduz. O Dependabot (ecossistema docker)
# sobe o digest por PR, com o changelog na frente de quem aprova.
#
#   docker build -t dentibot-api api/
#   docker run --read-only --tmpfs /tmp -p 8080:8080 --env-file .env dentibot-api

FROM eclipse-temurin:25-jdk-alpine@sha256:3fd2d245c4e0eba615fe366a71b8bd25f5db7104f53e4026b24bf508b880bd2a AS build
WORKDIR /src
# Dependências numa camada própria: mudar código não baixa o Maven Central de novo.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sh mvnw -B -q dependency:go-offline
COPY src src
# Os testes rodam no CI (mvnw verify), com Docker para o Testcontainers; aqui não
# há Docker dentro do build, e rodá-los só falharia.
RUN sh mvnw -B -q -DskipTests package \
 && java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination /camadas

FROM eclipse-temurin:25-jre-alpine@sha256:3c0a9084927a221ccd1d007fcaf614465672c0af37aaa834c5184483afe56d61
RUN addgroup -S dentibot && adduser -S -G dentibot -H -s /sbin/nologin dentibot
WORKDIR /app
# Da que muda menos para a que muda mais: um deploy de código troca só a última.
COPY --from=build /camadas/dependencies/ ./
COPY --from=build /camadas/spring-boot-loader/ ./
COPY --from=build /camadas/snapshot-dependencies/ ./
COPY --from=build /camadas/application/ ./
USER dentibot
EXPOSE 8080
# liveness e não health: health inclui o banco, e banco lento não é motivo para
# o Docker declarar o contêiner doente e reiniciá-lo.
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/liveness | grep -q UP || exit 1
# MaxRAMPercentage: a JVM respeita o limite do contêiner em vez da RAM do host.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djava.io.tmpdir=/tmp", \
            "org.springframework.boot.loader.launch.JarLauncher"]
