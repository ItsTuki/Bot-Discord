## Arreglo: YouTube no reproducía

YouTube cambió su reproductor y la librería ya no lograba descifrar las firmas de los videos (`must find sig function`), así que las canciones se cargaban pero no sonaban.

- Las firmas ahora se descifran con el servidor público **yt-cipher** (`https://cipher.kikkia.dev/`), el que recomiendan los autores de youtube-source. Funciona sin tocar nada: un `remotecipher` vacío en `config.txt` usa ese servidor. Puedes poner la URL de uno propio o `none` para volver al descifrado local.
- Nuevo orden de los métodos de YouTube (MWEB y WEB primero); con OAuth, los clientes de TV van después de los web, como recomiendan los autores.

### Actualizar
Reemplaza `TukiBot.jar` por el de esta release y reinicia el bot. Tu `config.txt` sigue sirviendo.

### Si aún falla
Activa `oauth = true` dentro de `youtube { ... }` en `config.txt`, reinicia y sigue las instrucciones de la consola con una cuenta de Google secundaria.

---
Requiere **Java 25** de 64 bits: https://adoptium.net/temurin/releases/?version=25 · Más detalles en el [README](https://github.com/ItsTuki/Bot-Discord#readme).
