# -*- coding: utf-8 -*-
"""nasayoquese.ipynb

Automatically generated by Colab.

Original file is located at
    https://colab.research.google.com/drive/1UYsLHftaC_RwIhg_noGiIXfCj_brMzdo
"""

from PIL import Image
import numpy as np
import soundfile as sf

# Leyenda de variables
# R = mediana del valor de R del código de color    (Ritmo)
# G = mediana del valor de G del código de color    (Frecuencia)
# B = mediana del valor de B del código de color    (Volumen)
# R_silencio = duración de notas y silencios
# dur_notas = duración de cada nota y silencios en segundos
# tiempo_total = tiempo en segundos de la canción



def obtener_rgb_por_fila(imagen_path):
    # Abrir la imagen
    imagen = Image.open(imagen_path)

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
        mediana_b = int(np.median(valores_b)) % 6

        # Almacenar las medianas
        R.append(mediana_r)
        G.append(mediana_g)
        B.append(mediana_b)

    # Devolver las medianas
    return R, G, B

def duracion(R):  # R es un vector de ritmos

    dur_notas = [2**(1- elemento) for elemento in R]
    tiempo_total = np.sum(dur_notas)  # tiempo en segundos
    return  dur_notas, tiempo_total

def puntos_asignados(nota):  # función que te dice cuántas veces varía cada nota
    dur_notas = [4 * (elemento) for elemento in nota]
    return dur_notas

def volumen(B,R):  # Modulación de volumen usando el valor de B
    V = [20 + b for  b in B]
    V_norm = [v / 26 for v in V]
    for elemento, indice in zip(R, range(len(R))):
        if elemento >= 5:
            V_norm[indice] = 0
    return V_norm

# Función para generar la señal sinusoidal
def generate_wave(frequency, duration, sample_rate, v):
    t = np.linspace(0, duration, int(sample_rate * duration), endpoint=False)
    return np.sin(2 * np.pi * frequency * t)

# Identificamos la matriz G con las frecuencias
def frecuencia(G, frecuencias):
    nota = [frecuencias[modulo] for modulo in G]
    return nota



if __name__ == "__main__":
    # Parámetros
    sample_rate = 44100  # Frecuencia de muestreo
    frecuencias = [130.813, 146.832, 164.814, 195.998, 220, 261.626, 293.663, 329.628,
               391.995, 440, 523.251, 587.33, 659.255, 783.991, 880]


    # Ruta a la imagen
    imagen_path = '/Users/javierrodriguez/Documents/Python/nasa nos que/nebula.jpg'
    sound_path = '/Users/javierrodriguez/Documents/Python/nasa nos que/output.wav'

    # Obtener las medianas de los valores RGB por fila
    R, G, B = obtener_rgb_por_fila(imagen_path)
    dur_notas, _ = duracion(R)  # dur_notas es el tiempo en segundos que dura cada nota
    frecuencias_G = frecuencia(G, frecuencias)
    volumen_B = volumen(B,R)  # Obtener el vector de volumen normalizado

    # Crear la señal
    audio_signal = np.array([])  # Inicializamos la señal de audio como un array vacío

    # Generar la señal de audio con modulación de volumen
    for freq, dur, vol in zip(frecuencias_G, dur_notas, volumen_B):
        wave = generate_wave(freq, dur, sample_rate, vol)
        audio_signal = np.concatenate((audio_signal, wave))

    # Normalización de la señal
    audio_signal = audio_signal / np.max(np.abs(audio_signal))

    # Guardar el archivo de audio
    sf.write(sound_path, audio_signal, sample_rate)
    print(f"Archivo de audio generado y guardado en {sound_path}")