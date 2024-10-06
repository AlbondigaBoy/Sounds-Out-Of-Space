from PIL import Image
import numpy as np
import soundfile as sf
import random
from io import BytesIO
import base64


# Leyenda de variables
# R = mediana del valor de R del código de color    (Ritmo)
# G = mediana del valor de G del código de color    (Frecuencia)    
# B = mediana del valor de B del código de color    (Volumen)

# Obtengo RGB
def obtener_rgb_por_fila(b64):
    imagen_data = base64.b64decode(b64)
    imagen = Image.open(BytesIO(imagen_data))

    # Convertir la imagen a modo RGB
    imagen = imagen.convert('RGB')

    # Obtener las dimensiones de la imagen
    ancho, alto = imagen.size

    # Inicializar listas para las medianas de cada fila
    R = []
    G = []
    B = []

    # Recorrer cada fila (y) de la imagen
    for y in range(alto):
        valores_r = []
        valores_g = []
        valores_b = []

        # Recorrer cada columna (x) de la fila
        for x in range(ancho):
            # Obtener el valor RGB del píxel en la coordenada (x, y)
            rgb = imagen.getpixel((x, y))

            # Almacenar los valores R, G, B para el cálculo de la mediana
            valores_r.append(rgb[0])
            valores_g.append(rgb[1])
            valores_b.append(rgb[2])

        # Calcular la mediana de los colores RGB y aplicar módulos
        mediana_r = int(np.median(valores_r)) % 5
        mediana_g = int(np.median(valores_g)) % 15
        mediana_b = int(np.median(valores_b)) % 11

        # Almacenar las medianas
        R.append(mediana_r)
        G.append(mediana_g)
        B.append(mediana_b)

    # Devolver las medianas
    return R, G, B


def duracion(R):  # R es un vector de ritmos
    dur_notas = [2 ** (-elemento) for elemento in R]
    tiempo_total = np.sum(dur_notas)  # tiempo en segundos
    return dur_notas, tiempo_total


# Función para generar la señal sinusoidal
def generate_wave(frequency, duration, sample_rate, vi, vj, fase=0):
    t = np.linspace(0, duration, int(sample_rate * duration), endpoint=True)
    aux = np.ones(len(t))
    alfa = vi * aux + (vj - vi) * t
    wave = np.sin(2 * np.pi * frequency * t + fase) * alfa
    fasew = 2 * np.pi * frequency * duration + fase
    return wave, fasew


def volumen(B):
    B = np.array(B)
    aux = np.ones(len(B))
    v = 0.2 * aux + 0.08 * B
    return v


# Identificamos la matriz G con las frecuencias
def frecuencia(G, frecuencias):
    nota = [frecuencias[modulo] for modulo in G]
    return nota


def imagen_audio(entrada):
    sample_rate = 44100  # Frecuencia de muestreo
    frecuencias = [261.626, 293.663, 329.628, 391.995, 440, 523.251, 587.33, 659.255,
                   783.991, 880, 1046.5, 1174.66, 1318.55, 1567.98, 195.998, 220]

    # Obtener RGB
    R, G, B = obtener_rgb_por_fila(entrada)

    # Aumentar el valor de B para que sea más perceptible en volumen y eliminar los valores 0
    # B = [random.randint(1, 10) if b == 0 else b for b in B]  # Asignar un valor aleatorio si b es 0
    # B = [b / np.max(B) * 10 for b in B]
    # Obtener duraciones
    dur_notas, _ = duracion(R)

    # Obtener frecuencias
    frecuencias_G = frecuencia(G, frecuencias)
    v = volumen(B)

    # Generar la señal
    audio_signal = np.array([])

    # Iniciar con la primera onda
    wave, fase = generate_wave(frecuencias_G[0], dur_notas[0], sample_rate, v[0], v[1])

    # Concatenar las ondas, comenzando desde el segundo elemento
    for idx, (freq, dur) in enumerate(zip(frecuencias_G[1:], dur_notas[1:])):
        audio_signal = np.concatenate((audio_signal, wave))
        wave, fase = generate_wave(freq, dur, sample_rate, v[idx], v[(idx + 1)], fase)

    # Concatenar la última onda
    audio_signal = np.concatenate((audio_signal, wave))

    # Normalización de la señal
    audio_signal = audio_signal / np.max(np.abs(audio_signal))

    # Guardar el archivo de audio
    return sf.write('audio.wav', audio_signal, sample_rate)


if __name__ == "_main_":
    audio = imagen_audio(input())
