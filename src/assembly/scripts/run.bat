@echo off
setlocal
set "DIR=%~dp0"
if "%DIR:~-1%"=="\" set "DIR=%DIR:~0,-1%"

java --module-path "%DIR%\lib" ^
     --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.swing ^
     -cp "%DIR%\app.jar" ^
     cz.bliksoft.javautils.fx.test.FxTests

endlocal
