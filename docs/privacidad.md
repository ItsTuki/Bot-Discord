# Política de Privacidad de TukiBot

*Vigente desde el 7 de noviembre de 2026.*

Esta política explica qué datos usa **TukiBot** (ID de aplicación `1093304936885923910`), el bot de Discord operado por **ItsTuki**, para qué los usa y cómo puedes pedir que se borren. TukiBot recopila lo mínimo necesario para funcionar.

## 1. Datos que se guardan de forma permanente
**Configuración de cada servidor**, que los administradores definen con `/config` y otros comandos:
- ID del servidor;
- IDs del canal de texto y de voz elegidos para la música, del rol de DJ y, si se activan los avisos de Twitch, del canal y rol para esos avisos;
- volumen, modo de repetición, autoplay, tipo de cola, porcentaje de votos para saltar y nombre de la playlist automática.

**Playlists personales**, solo si tú las creas con `/myplaylist`: tu ID de usuario de Discord, el nombre que les pusiste y el título y enlace de cada canción. Puedes borrarlas cuando quieras con `/myplaylist delete`.

No se guardan mensajes, nombres de usuario ni un historial de lo que escuchas.

## 2. Datos que se usan solo mientras el bot funciona
Estos datos viven en memoria y se borran al terminar la cola, al expirar o al reiniciar el bot:
- **Cola de reproducción**: el ID, nombre visible y avatar de quien pidió cada canción, junto con el enlace o la búsqueda que usó, para mostrar "pedida por" y permitir quitar o saltar sus propias canciones.
- **Votos para saltar**: los IDs de quienes votaron por la canción actual.
- **Búsquedas**: los textos escritos en `/play`, `/search` y similares, guardados hasta 5 minutos para acelerar las sugerencias.
- **Botones y menús**: datos temporales de búsquedas y playlists pendientes, por hasta 2 minutos.
- **Estado de voz**: Discord informa al bot quién está en los canales de voz, para saber si alguien escucha y validar los comandos.

## 3. Contenido de mensajes
Si las respuestas de texto están activadas, TukiBot lee los mensajes de los canales donde está solo para detectar si empiezan con `ItsTuki`, `$bye`, `!gen` o `!moneda` y responder. Los mensajes **no se guardan, no se registran y no se comparten**. El menú contextual "Reproducir en voz" solo lee el mensaje que eliges, para obtener su enlace o archivo de audio.

## 4. Servicios de terceros
Para cumplir lo que pides, TukiBot envía datos a:
- **Discord**, que opera la plataforma (ver su [política de privacidad](https://discord.com/privacy));
- **plataformas de audio** (YouTube, SoundCloud, Bandcamp, Vimeo, Twitch y otras) cuando buscas o reproduces algo: se envía la búsqueda o el enlace, nunca tus datos de Discord;
- **LRCLIB** (lrclib.net), cuando pides una letra: se envía el título y el artista de la canción;
- **Twitch**, si el operador activa los avisos de directos: se consulta si un canal público está en vivo, sin enviar datos de usuarios;
- **GitHub**, para buscar actualizaciones del propio bot, sin enviar datos de usuarios.

TukiBot no vende ni comparte datos con nadie más, no muestra publicidad y no hace seguimiento.

## 5. Registros
La consola del bot registra eventos técnicos (inicio, errores, identificadores de canciones que fallan) para corregir problemas. No registra el contenido de los mensajes.

## 6. Conservación y borrado
- La configuración de un servidor se conserva mientras el bot esté en él. Los administradores pueden cambiarla en cualquier momento con `/config`.
- Tus playlists personales se conservan hasta que las borres con `/myplaylist delete`.
- Para pedir que se borre la configuración de un servidor o cualquier dato relacionado contigo, abre un *issue* en [github.com/ItsTuki/Bot-Discord/issues](https://github.com/ItsTuki/Bot-Discord/issues) o escribe a **itstuki** en Discord. Las solicitudes se atienden en un plazo de 30 días.
- Los datos temporales se borran solos según lo indicado en la sección 2.

## 7. Seguridad
Los datos se guardan en el equipo donde se ejecuta el bot, con acceso solo de ItsTuki. Las conexiones con Discord y las plataformas de audio usan cifrado, y el audio de voz usa el cifrado de extremo a extremo DAVE de Discord.

## 8. Menores de edad
TukiBot se rige por la edad mínima que exige Discord en cada país y no recopila a sabiendas datos adicionales de menores.

## 9. Copias del bot
El código es abierto. Esta política solo cubre la instancia operada por ItsTuki; quien hospede su propia copia es responsable de sus datos.

## 10. Cambios
Esta política puede actualizarse. La versión vigente siempre está en este archivo y la fecha de arriba indica el último cambio.

## 11. Contacto
[github.com/ItsTuki/Bot-Discord/issues](https://github.com/ItsTuki/Bot-Discord/issues) · **itstuki** en Discord.
