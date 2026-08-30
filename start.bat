@echo off
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%
echo Starting CropSense...
echo Open browser at: http://localhost:8080
echo Press Ctrl+C to stop.
mvnw.cmd spring-boot:run
