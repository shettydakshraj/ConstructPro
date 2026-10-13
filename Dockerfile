# Multi-stage build for lightweight, secure, and reliable Java backend execution
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

# Copy backend source code and dependencies
COPY backend/ /app/backend/

# Ensure PostgreSQL JDBC driver is present (reuse existing jar or fetch if missing)
RUN if [ ! -f /app/backend/postgresql-42.7.3.jar ]; then \
      apt-get update && apt-get install -y --no-install-recommends wget ca-certificates && \
      wget -q https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.3/postgresql-42.7.3.jar -O /app/backend/postgresql-42.7.3.jar && \
      rm -rf /var/lib/apt/lists/*; \
    fi

# Compile Main.java with the PostgreSQL JDBC driver
RUN javac -cp "/app/backend/postgresql-42.7.3.jar" /app/backend/Main.java

# Runtime container using lightweight JRE
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy compiled classes and the PostgreSQL driver from builder
COPY --from=builder /app/backend /app/backend

# Default port (Render overrides PORT via environment variable at runtime)
ENV PORT=8081

EXPOSE 8081

# Run the Java application on Render (listening on dynamic PORT)
CMD ["sh", "-c", "java -cp backend:backend/postgresql-42.7.3.jar Main"]
