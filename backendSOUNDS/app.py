from flask import Flask, request, jsonify, send_file
from functions import imagen_audio
import os

app = Flask(__name__)

@app.route('/algoritmo', methods=['POST'])
def hello_world():
    # Obtener la imagen codificada en Base64 del formulario
    base64_image = request.form.get('image')
    #print(base64_image)

    if base64_image:
        try:
            # Convertir la imagen a sonido
            audio_path = 'audio.wav'  # Asegúrate de usar un nombre único para cada petición si es necesario
            imagen_audio(base64_image)

            # Verifica si el archivo de audio fue creado
            if os.path.exists(audio_path):
                # Enviar el archivo de audio .wav como respuesta
                return send_file(audio_path, mimetype='audio.wav')
            else:
                return jsonify({"error": "No se pudo generar el archivo de audio"}), 500

        except Exception as e:
            return jsonify({"error": str(e)}), 500
    else:
        return jsonify({"error": "No se recibieron datos"}), 400

if __name__ == '__main__':
    app.run(debug=True)  # Activa el modo de depuración para ver errores en el servidor
