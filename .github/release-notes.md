## cookies.txt automático

Cuando YouTube marca tu IP ("Sign in to confirm you're not a bot"), la solución es darle a yt-dlp una sesión de YouTube. Ahora basta con dejar un archivo **`cookies.txt`** junto a `TukiBot.jar`: el bot lo detecta solo, sin tocar `config.txt`. Al iniciar, la consola muestra qué cookies está usando.

Cómo obtener el `cookies.txt` (con Chrome en Windows; yt-dlp no puede leer sus cookies directamente):
1. Abre una ventana de incógnito e inicia sesión en YouTube con una **cuenta de Google secundaria**.
2. Con la extensión **Get cookies.txt LOCALLY** (permítela en incógnito), entra a `youtube.com/robots.txt` y exporta las cookies.
3. Cierra la ventana de incógnito y guarda el archivo como `cookies.txt` junto al bot.

---
Requiere **Java 25** de 64 bits: https://adoptium.net/temurin/releases/?version=25 · Más detalles en el [README](https://github.com/ItsTuki/Bot-Discord#readme).
