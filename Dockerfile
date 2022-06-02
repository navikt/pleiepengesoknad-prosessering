FROM gcr.io/distroless/java17

COPY build/libs/*.jar app.jar

CMD ["app.jar"]
