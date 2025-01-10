@echo off
REM start-server.bat
java -jar chat-server.jar

REM start-client.bat
@echo off
set JAVAFX_PATH=C:\Program Files\Java\javafx-sdk-23.0.1\lib
java --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.fxml -jar chat-client.jar