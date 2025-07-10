# Base image with Java and Leiningen
FROM clojure:lein-2.10.0

# Working directory
WORKDIR /app

# Copy project files
COPY project.clj .
COPY src/core.clj src/core.clj
COPY resources/asyncapi.yaml resources/asyncapi.yaml

# Install dependencies
RUN lein deps

# Expose port for WebSocket
EXPOSE 8080

# Run the application
CMD ["lein", "run"]
