@echo off

set JAVAVER="0.0"

for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
	set JAVAVER=%%g
)

set JAVAVER=%JAVAVER:"=%

for /f "delims=. tokens=1-3" %%v in ("%JAVAVER%") do (
	set MINOR=%%v
)

if %MINOR% geq 10 (
	for /f %%i in ('where javaw') do (
		set java=%%i 
		goto :break
	)
) else (
	set /p java=<%~dp0jvm.txt
)

:break
if exist "%java%" (
	"%java%" ^
		--module-path %~dp0libs ^
		--add-modules javafx.base,javafx.controls,javafx.graphics,javafx.fxml,javafx.web ^
		-Djava.library.path=%~dp0libs ^
		-jar %~dp0Gameroom.jar
) else (
	msg "%username%" No Java 10+ found. Please set path to javaw.exe in %~dp0jvm.txt
)