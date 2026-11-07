#!/usr/bin/env sh
# Inicia TukiBot. Necesita Java 25: https://adoptium.net/temurin/releases/?version=25
cd "$(dirname "$0")"
exec java -Dnogui=true -jar TukiBot.jar "$@"
