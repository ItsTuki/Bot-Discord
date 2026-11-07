#!/usr/bin/env sh
# Inicia TukiBot. Necesita Java 25: https://adoptium.net/temurin/releases/?version=25
cd "$(dirname "$0")"
# si quedó una actualización descargada, se instala antes de arrancar
[ -f TukiBot.jar.new ] && mv -f TukiBot.jar.new TukiBot.jar
exec java -Dnogui=true -jar TukiBot.jar "$@"
