## Mejoras para YouTube

- **Mensajes de error claros:** si YouTube no entrega el audio de un video, el bot muestra en Discord lo que respondió YouTube (por ejemplo "Sign in to confirm you're not a bot") en vez de un error genérico.
- **Cookies del navegador:** nueva opción `youtube.cookiesfrombrowser` (`firefox`, `chrome`, `edge`, `brave`...) para que yt-dlp use tu sesión de YouTube cuando algún video lo exige. También sigue existiendo `youtube.cookies` para un archivo `cookies.txt`.
- **Panel:** ya no dice "No hay música sonando" mientras el bot todavía se está conectando al canal de voz.

### Actualizar
Reemplaza `TukiBot.jar` y reinicia.

---
Requiere **Java 25** de 64 bits: https://adoptium.net/temurin/releases/?version=25 · Más detalles en el [README](https://github.com/ItsTuki/Bot-Discord#readme).
