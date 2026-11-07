# TukiBot

Bot de música para Discord de **ItsTuki**. Es una versión modernizada de [JMusicBot](https://github.com/jagrosh/MusicBot) (abandonado desde 2024), unida con las funciones de mi primer bot en Python.

JMusicBot ya no podía entrar a canales de voz: desde el 1 de marzo de 2026 Discord exige el cifrado de extremo a extremo **DAVE** en todas las llamadas, y el bot seguía en JDA 4. TukiBot lo migra a **JDA 6** con DAVE y lo adapta a las funciones actuales de Discord.

## Qué trae

**Funciones nuevas de Discord**
- Comandos con barra (`/play`, `/queue`, `/skip`...) con descripciones y opciones en español. Con `/play` escribes directo el nombre o pegas el enlace: el bot reconoce solo cuál es.
- **Enlaces de Spotify**: canciones, álbumes y playlists. Cada canción se busca en YouTube (eligiendo la versión con la duración más parecida) cuando le toca sonar, así las playlists se agregan al instante.
- **Autocompletado**: `/play` sugiere canciones de YouTube mientras escribes; `/remove`, `/skipto` y `/movetrack` muestran los títulos de la cola.
- **Panel de reproducción** con botones (pausar, saltar, detener, repetir, mezclar, volumen, cola, letra) que se actualiza solo y aparece cada vez que empieza una canción.
- **Menú de selección** en `/search` con hasta 10 resultados (YouTube, YouTube Music o SoundCloud).
- **Estado del canal de voz** con la canción que está sonando.
- **Menú contextual**: clic derecho en un mensaje con un enlace › Apps › **Reproducir en voz**.
- **`/playfile`** para reproducir archivos de audio adjuntos.
- Paneles con Components V2, botones de enlace, respuestas privadas para errores y contraseñas.

**De JMusicBot** (se mantiene todo): cola justa o lineal, rol de DJ, votación para saltar, repetir cola o canción, playlists guardadas, playlist automática, canal de texto/voz exclusivo, buscar, adelantar, mover canciones, salir solo si nadie escucha.

**De mi bot original**: `/gen` (contraseña aleatoria), `/moneda`, `/twitch`, y las respuestas de texto `ItsTuki`, `$bye`, `!gen` y `!moneda`.

## Comandos

| Comando | Para qué sirve |
|---|---|
| `/play` | Reproduce una canción o playlist: nombre o enlace (YouTube, Spotify, SoundCloud, Twitch...) |
| `/playfile` | Reproduce un archivo de audio adjunto |
| `/search` | Busca y elige de una lista |
| `/nowplaying` | Panel de la canción actual con botones |
| `/queue` | Cola de reproducción con páginas |
| `/skip` | Vota para saltar (si la pediste tú, se salta al instante) |
| `/remove` | Quita una canción tuya de la cola (0 = todas las tuyas) |
| `/seek` | Adelanta o retrocede (`1:30`, `+30`, `-10`, `2m15s`) |
| `/shuffle` | Mezcla tus canciones |
| `/lyrics` | Letra de la canción (desde LRCLIB) |
| `/playlist` | Reproduce, lista y (dueño) crea/edita playlists |
| `/playnext` · `/pause` · `/resume` · `/forceskip` · `/skipto` · `/movetrack` · `/forceremove` · `/repeat` · `/volume` · `/stop` | Comandos de **DJ** |
| `/config` | (Admin) rol de DJ, canales, % para saltar, tipo de cola, playlist automática |
| `/settings` | Muestra la configuración del servidor |
| `/owner` | (Dueño) actividad, estado, nombre, avatar, debug, apagar |
| `/help` · `/about` · `/ping` · `/gen` · `/moneda` · `/twitch` | Generales |

## Cómo usarlo

### 1. Requisitos
- **Java 25** de 64 bits (lo exige DAVE): [Temurin 25](https://adoptium.net/temurin/releases/?version=25).
- Windows x64, Linux x64/ARM64 o macOS.
- Un equipo que permita conexiones **UDP salientes** (la voz de Discord usa UDP). Tu PC de casa o un VPS normal sirven.

### 2. Preparar el bot en Discord
1. En el [Developer Portal](https://discord.com/developers/applications) abre tu aplicación › **Bot** › **Reset Token** y copia el token.
2. Si quieres las respuestas de texto (`ItsTuki`, `!gen`...), activa **MESSAGE CONTENT INTENT** en esa misma página. Si no, pon `textresponses = false` en el config.
3. Invita al bot con el enlace que aparece en la consola al iniciarlo (ya trae los permisos necesarios).

### 3. Descargar o compilar
- **Descargar**: baja `TukiBot.jar` (o el `.zip` con los scripts) desde la última [release](https://github.com/ItsTuki/Bot-Discord/releases/latest).
- **Compilar tú mismo** (necesitas Maven):
  ```bash
  mvn package
  # el bot queda en target/TukiBot.jar
  ```

### 4. Iniciar
Pon `TukiBot.jar` en una carpeta y ejecuta `iniciar.bat` (Windows) o `./iniciar.sh` (Linux/macOS), o directamente:
```bash
java -jar TukiBot.jar
```
La primera vez te pide el token y crea `config.txt`. Ahí puedes cambiar todo lo demás (comentado en español). Para generar el archivo sin iniciar: `java -jar TukiBot.jar generate-config`.

> `config.txt` guarda tu token: **nunca lo subas a GitHub**. El `.gitignore` ya lo excluye.

Consejo para probar: pon el ID de tu servidor en `devguild` y los comandos aparecen ahí al instante.

## YouTube

YouTube cambia sus protecciones contra bots casi cada semana, así que TukiBot usa varias capas:

1. **yt-dlp (por defecto):** el audio de YouTube se obtiene con [yt-dlp](https://github.com/yt-dlp/yt-dlp), que se actualiza constantemente. La primera vez que inicias el bot descarga solo `yt-dlp` y [Deno](https://deno.com) (el runtime de JavaScript que yt-dlp necesita para YouTube) en la carpeta `bin`, y mantiene yt-dlp al día. Si ya los tienes instalados, usa los tuyos.
2. **Respaldo interno:** si yt-dlp falla, se intenta con youtube-source, que descifra las firmas con el servidor público yt-cipher (`youtube.remotecipher`; vacío = servidor público).
3. **Si algún video responde "Sign in to confirm you're not a bot":** pon en `config.txt` `youtube { cookiesfrombrowser = "firefox" }` (o `chrome`, `edge`, `brave`; Chrome y Edge deben estar cerrados) para que yt-dlp use tu sesión de YouTube de ese navegador, o exporta un `cookies.txt` y déjalo junto a `TukiBot.jar` (se detecta solo; también puedes poner otra ruta en `youtube.cookies`). Mejor si es una **cuenta de Google secundaria**. En Windows, yt-dlp no puede leer las cookies de Chrome directamente, así que con Chrome usa el método del `cookies.txt`:
   1. Abre una ventana de incógnito e inicia sesión en YouTube con la cuenta secundaria.
   2. Con la extensión **Get cookies.txt LOCALLY** (permítela en incógnito), entra a `youtube.com/robots.txt` y exporta las cookies.
   3. Cierra la ventana de incógnito (así esas cookies no cambian) y guarda el archivo como `cookies.txt` junto al bot.

Las búsquedas y sugerencias de YouTube funcionan sin iniciar sesión. Para desactivar yt-dlp: `youtube { ytdlp = false }`.

## Estructura

```
src/main/java/com/itstuki/tukibot/
  TukiBot.java          inicio y conexión a Discord (DAVE, intents, comandos)
  Bot.java, BotConfig.java, Listener.java
  audio/                reproductor, cola, panel automático, fuentes de audio
  commands/             sistema de comandos slash y comandos por categoría
  interactions/         botones, menú contextual y respuestas de texto
  music/                carga de canciones, acciones compartidas, autocompletado
  ui/Panels.java        paneles Components V2
  ytdlp/                reproducción de YouTube con yt-dlp
  spotify/              enlaces de Spotify (se reproducen desde YouTube)
  fun/BotLogic.java     gen_pass y flip_coin del bot original
  lyrics/               cliente de LRCLIB
  queue/, playlist/, settings/, utils/, gui/   (heredados de JMusicBot)
```

## Términos y privacidad

- [Términos del Servicio](docs/terminos.md)
- [Política de Privacidad](docs/privacidad.md)

## Créditos y licencia

Basado en [JMusicBot](https://github.com/jagrosh/MusicBot) de John Grosh (jagrosh) y sus contribuidores, bajo la [Licencia Apache 2.0](LICENSE). Los cambios están descritos en [NOTICE](NOTICE).

Usa [JDA](https://github.com/discord-jda/JDA), [yt-dlp](https://github.com/yt-dlp/yt-dlp), [JDAVE](https://github.com/MinnDevelopment/jdave), [Lavaplayer](https://github.com/lavalink-devs/lavaplayer), [youtube-source](https://github.com/lavalink-devs/youtube-source) y [LRCLIB](https://lrclib.net).

La primera versión de este repo (el bot en Python) sigue disponible en el historial de commits.
