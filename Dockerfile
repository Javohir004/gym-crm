# ---------- 1-bosqich: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Dependency'larni alohida qatlamda yuklab olamiz (keyingi build'lar tezroq bo'ladi)
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- 2-bosqich: run ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# root bo'lmagan foydalanuvchi bilan ishga tushiramiz
RUN groupadd -r app && useradd -r -g app app \
    && mkdir -p /app/logs && chown -R app:app /app

COPY --from=build --chown=app:app /app/target/gym-crm-app-*.jar app.jar

USER app
EXPOSE 8081
# DB parol/login bu yerda YO'Q. Ular container ishga tushganda environment orqali beriladi.
ENTRYPOINT ["java", "-jar", "app.jar"]