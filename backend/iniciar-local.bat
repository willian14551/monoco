@echo off
setlocal
cd /d "%~dp0"
if not defined JAVA_HOME if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
echo Modo H2: http://localhost:8080/api/. Para Docker e MySQL, use iniciar-backend.bat na raiz.
call gradlew.bat --console=plain bootRun --args="--spring.profiles.active=local"
pause
