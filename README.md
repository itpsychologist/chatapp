To run via Windows you need to:
1. Install Javafx in the directory C:\Program Files\Java\javafx-sdk-23.0.1\lib
2. Run start.bat

Using a Docker container:
1. Run docker run -d -p 8080:8080 --name chat-server chat-server-image server part
2. Run the client part java --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.fxml -jar chat-client.jar
