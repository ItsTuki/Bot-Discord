Primera versión de **TukiBot**: JMusicBot (jagrosh) modernizado y unido con mi bot original en Python.

## Novedades
- **Voz funcionando otra vez**: migrado de JDA 4 a JDA 6.7 con el cifrado DAVE, obligatorio en Discord desde marzo de 2026.
- **Comandos con barra** con nombres en español (`/reproducir`, `/cola`, `/saltar`...) e inglés.
- **Sugerencias de YouTube** mientras escribes en `/play`.
- **Panel de reproducción** con botones: pausar, saltar, detener, repetir, mezclar, volumen, cola y letra.
- **`/search`** con menú de hasta 10 resultados (YouTube, YouTube Music, SoundCloud).
- **Estado del canal de voz** con la canción actual.
- **Clic derecho › Apps › Reproducir en voz** para poner en la cola el enlace de un mensaje.
- Letras desde LRCLIB, OAuth opcional para YouTube y protección si la voz no logra conectar.
- Del bot original: `/gen`, `/moneda`, `/twitch` y las respuestas `ItsTuki`, `$bye`, `!gen`, `!moneda`.

## Cómo usarlo
1. Instala **Java 25** de 64 bits: https://adoptium.net/temurin/releases/?version=25
2. Descarga `TukiBot.jar` (o el `.zip` con los scripts de inicio).
3. Ejecuta `iniciar.bat` (Windows), `./iniciar.sh` (Linux/macOS) o `java -jar TukiBot.jar`.
4. La primera vez te pide el token y crea `config.txt`.

Más detalles en el [README](https://github.com/ItsTuki/Bot-Discord#readme). Términos: [docs/terminos.md](https://github.com/ItsTuki/Bot-Discord/blob/main/docs/terminos.md) · Privacidad: [docs/privacidad.md](https://github.com/ItsTuki/Bot-Discord/blob/main/docs/privacidad.md)
