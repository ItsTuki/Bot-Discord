@echo off
title TukiBot
REM Inicia TukiBot. Necesita Java 25: https://adoptium.net/temurin/releases/?version=25
cd /d "%~dp0"
REM si quedo una actualizacion descargada, se instala antes de arrancar
if exist TukiBot.jar.new move /y TukiBot.jar.new TukiBot.jar >nul
java -Dnogui=true -jar TukiBot.jar
pause
