## Novedades de la 1.3.0

- 📻 **Autoplay / modo radio** (`/autoplay`): cuando se acaba la cola, el bot sigue con canciones parecidas a la última (usa el "mix" de YouTube y evita repetir lo que ya sonó). También hay un botón 📻 en el panel.
- 🎛️ **Filtros de audio** (`/filter`): bass boost, nightcore, vaporwave, 8D, karaoke, suave, agudos y vibrato. El filtro activo se ve en el panel.
- 📂 **Playlists personales** (`/myplaylist`): guarda lo que está sonando y la cola, agrega canciones, y ponlas en cualquier servidor donde esté el bot. Hasta 20 playlists de 500 canciones por persona.
- 🟣 **Avisos de directos de Twitch**: con `/config twitch` cada servidor elige el canal (y un rol opcional a mencionar) donde se avisa cuando empieza el directo. Necesita un Client ID y Secret gratuitos de Twitch en `config.txt`. `/twitch` ahora muestra si está en vivo.
- ⚡ **Precarga**: la siguiente canción de la cola se descarga mientras suena la actual, así el cambio de canción es casi instantáneo.
- 🔔 **Aviso al dueño por MD** cuando YouTube bloquea la música (por ejemplo, cookies vencidas), como mucho una vez cada 6 horas.
- ⬆️ **Actualización automática**: el bot revisa los releases cada 6 horas, descarga la versión nueva y la instala al reiniciar. Se desactiva con `autoupdate = false`.
- 🦖 **Egg de Pterodactyl** (`egg-tukibot.json`, adjunto abajo): instala el bot con un clic y el token se pone en la pestaña *Startup* (variable `BOT_TOKEN`), sin tocar `config.txt`.
- El token, el dueño y las credenciales de Twitch ahora también se pueden dar con variables de entorno (`BOT_TOKEN`, `OWNER_ID`, `TWITCH_CLIENT_ID`, `TWITCH_CLIENT_SECRET`).

Los `config.txt` anteriores siguen sirviendo tal cual; las opciones nuevas usan valores por defecto.

---
Requiere **Java 25** de 64 bits: https://adoptium.net/temurin/releases/?version=25 · Más detalles en el [README](https://github.com/ItsTuki/Bot-Discord#readme).
