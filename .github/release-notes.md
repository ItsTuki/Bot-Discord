## Spotify y `/play` más simple

- **Enlaces de Spotify:** `/play` acepta canciones, álbumes y playlists de Spotify (también los enlaces con `?si=...` o `/intl-es/`). No necesita claves de API: el bot lee los datos públicos de Spotify y busca cada canción en YouTube cuando le toca sonar, eligiendo la versión con la duración más parecida. Las playlists se agregan al instante y el panel muestra la portada de Spotify.
- **`/play` directo:** ahora escribes `/play` y de inmediato el nombre o el enlace; ya no hay que elegir la opción "busqueda". El bot reconoce solo si es un enlace (YouTube, Spotify, SoundCloud...) o una búsqueda.
- **Comandos en inglés para todos:** `/play`, `/skip`, `/queue`, etc. funcionan igual sin importar el idioma de tu Discord (las descripciones siguen en español).
- **`/playfile`:** nuevo comando para reproducir un archivo de audio adjunto (antes era una opción de `/play`).

### Actualizar
Reemplaza `TukiBot.jar` y reinicia. Discord puede tardar unos minutos en mostrar los comandos con los nombres nuevos; si no aparecen, reinicia Discord (Ctrl+R).

---
Requiere **Java 25** de 64 bits: https://adoptium.net/temurin/releases/?version=25 · Más detalles en el [README](https://github.com/ItsTuki/Bot-Discord#readme).
