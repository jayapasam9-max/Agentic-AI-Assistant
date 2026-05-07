# ---- build stage ----
# Use the JDK image for compilation; we'll throw it away after the jar is built.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Maven wrapper + pom first, so Docker can cache the dependency download
# layer and skip re-downloading on every code change.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

# Now copy sources and build. Tests are skipped here — CI/local should run them.
COPY src src
RUN ./mvnw -B -ntp -DskipTests package

# ---- runtime stage ----
# JRE-only image is ~80 MB smaller than the JDK image.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar

# Render free tier gives us 512 MB RAM. Without these flags the JVM uses its
# default container ergonomics (25% of RAM = ~128 MB heap), which is far too
# small for Spring Boot + Hibernate + LangChain4j. 70% leaves ~150 MB for
# metaspace, native buffers, and threads. ExitOnOutOfMemoryError makes Render
# restart us on OOM instead of running degraded.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
