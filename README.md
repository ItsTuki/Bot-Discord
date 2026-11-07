# Bot-Discord

Bot de Discord muy básico escrito en Python con `discord.py`. Fue mi primer bot: responde a mensajes de texto con respuestas fijas y genera contraseñas aleatorias.

## Qué responde

| Mensaje que empieza con | Respuesta |
|---|---|
| `ItsTuki` | Comparte el enlace a mi canal de Twitch. |
| `$bye` | Responde con un emoji 🙂. |
| `!gen` | Genera una contraseña aleatoria de 10 caracteres (letras, números y símbolos). |
| `!moneda` | Lanza una moneda (cara o cruz). |
| Cualquier otro texto | Lo repite tal cual (modo eco). |

> **Nota:** `!moneda` usa la función `flip_coin()`, que no está en `bot_logic.py` de este repo (sí está en [test](https://github.com/ItsTuki/test)). Hay que copiarla para que el comando funcione.

## Requisitos

- Python 3.8 o superior
- `discord.py`

```bash
pip install discord.py
```

## Cómo usarlo

1. Crea un bot en el [Portal de desarrolladores de Discord](https://discord.com/developers/applications) y activa el intent **Message Content**.
2. Reemplaza `"Token Aqui we"` en `main.py` por el token de tu bot (no lo subas a GitHub).
3. Ejecuta:

```bash
python main.py
```

## Estructura

```
main.py       # eventos del bot y respuestas
bot_logic.py  # generador de contraseñas
```
