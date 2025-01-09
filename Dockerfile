FROM eclipse-temurin:23-jre

WORKDIR /app
COPY target/chat-client.jar .
COPY javafx /opt/javafx

RUN apt-get update && apt-get install -y xvfb

CMD ["xvfb-run", "-a", "java", "--module-path", "/opt/javafx/lib/", "--add-modules", "javafx.controls,javafx.fxml", "-jar", "chat-client.jar"]
