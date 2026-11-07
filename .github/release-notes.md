## Arreglo: 403 al reproducir YouTube con yt-dlp

En la 1.1.0, yt-dlp obtenía el enlace del audio pero YouTube rechazaba la descarga hecha por el bot (`403`), porque cada enlace queda atado a los encabezados y tokens que usó yt-dlp.

- Ahora **yt-dlp también descarga el audio** a un archivo temporal en `bin/cache` y el bot lo va reproduciendo mientras baja (empieza a sonar en menos de un segundo después de que yt-dlp obtiene el enlace).
- Se puede adelantar con `/seek` dentro de lo ya descargado (yt-dlp baja mucho más rápido que el tiempo real).
- Los archivos temporales se borran al terminar cada canción y al iniciar el bot.
- Si yt-dlp falla, se intenta con el método interno.

### Actualizar
Reemplaza `TukiBot.jar` y reinicia.

---
Requiere **Java 25** de 64 bits: https://adoptium.net/temurin/releases/?version=25 · Más detalles en el [README](https://github.com/ItsTuki/Bot-Discord#readme).
