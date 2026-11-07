## YouTube con yt-dlp

YouTube agregó otra protección (los audios respondían `403` y los clientes de TV pedían "Sign in to confirm you're not a bot"), que la librería de Java aún no resuelve. Esta versión reproduce YouTube con **yt-dlp**, la herramienta que mejor sigue los cambios de YouTube.

- La primera vez que inicias el bot, descarga solo **yt-dlp** y **Deno** (el runtime de JavaScript que yt-dlp necesita) en la carpeta `bin` y revisa cada día si hay una versión nueva de yt-dlp. Si ya los tienes instalados, usa los tuyos.
- Si yt-dlp falla, el bot vuelve a intentar con el método interno (youtube-source + yt-cipher).
- Opciones nuevas en `youtube { ... }`: `ytdlp` (activar/desactivar), `ytdlppath` (tu propio yt-dlp) y `cookies` (archivo cookies.txt de una cuenta secundaria, por si YouTube sigue pidiendo iniciar sesión).
- La miniatura, el título y los controles del panel funcionan igual.

### Actualizar
Reemplaza `TukiBot.jar` y reinicia. La primera vez verás en la consola "Descargando yt-dlp…" y "Descargando Deno…" (unos 60 MB en total) y luego "yt-dlp listo".

---
Requiere **Java 25** de 64 bits: https://adoptium.net/temurin/releases/?version=25 · Más detalles en el [README](https://github.com/ItsTuki/Bot-Discord#readme).
