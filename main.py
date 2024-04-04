import discord
from bot_logic import *
# La variable intents almacena los privilegios del bot
intents = discord.Intents.default()
# Activar el privilegio de lectura de mensajes
intents.message_content = True
# Crear un bot en la variable cliente y transferirle los privilegios
client = discord.Client(intents=intents)

@client.event
async def on_ready():
    print(f'Hemos iniciado sesión como {client.user}')

@client.event
async def on_message(message):
    if message.author == client.user:
        return
    if message.content.startswith('ItsTuki'):
        await message.channel.send("Puedes Seguirme en Twitch: https://www.twitch.tv/theitstuki ")
    elif message.content.startswith('$bye'):
        await message.channel.send("\\U0001f642")
    elif message.content.startswith('!gen'):
        await message.channel.send('La clave generada es :')
        await message.channel.send(gen_pass(10))
    elif message.content.startswith('!moneda'):
        await message.channel.send(flip_coin())
        
    else:
        await message.channel.send(message.content)

client.run("Token Aqui we")
