package com.albondiga.soundsoutofspace

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.jsoup.Jsoup
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

fun getHubbleImage(date: String, callback: (String?) -> Unit) {
    val url = "https://imagine.gsfc.nasa.gov/hst_bday/$date"

    Thread {
        try {
            val doc = Jsoup.connect(url).get()
            val imgElement = doc.select("meta[property=og:image]").first()
            val imageUrl = imgElement?.attr("content")

            if (imageUrl != null) {
                Log.d("HubbleImage", "URL de la imagen original: $imageUrl")

                val year = imageUrl.split("/")[5].split("-")[2]
                val imageName = imageUrl.substringAfter(year).substringBeforeLast(".")
                val extension = imageUrl.substringAfterLast(".")

                val validExtensions = listOf("jpg", "jpeg", "png")
                if (extension in validExtensions) {
                    val finalImageUrl =
                        "https://imagine.gsfc.nasa.gov/hst_bday/images/${date}-$year$imageName.$extension"
                    Log.d("HubbleImage", "URL final de la imagen: $finalImageUrl")

                    // Primero mostrar la imagen en la interfaz de usuario
                    callback(finalImageUrl)

                    // Una vez mostrada, descargarla y codificarla en Base64 para enviarla al servidor
                    downloadImageAndEncodeBase64(finalImageUrl) { base64Image ->
                        if (base64Image != null) {
                            sendImageToServer(base64Image) { audioUrl ->
                                if (audioUrl != null) {
                                    Log.d("AudioResponse", "URL de audio recibida: $audioUrl")
                                } else {
                                    Log.e("AudioResponse", "No se recibió audio del servidor.")
                                }
                            }
                        } else {
                            Log.e("Base64Image", "Error al codificar la imagen en Base64.")
                        }
                    }
                } else {
                    Log.e("HubbleImage", "Extensión de imagen no válida: $extension")
                    callback(null)
                }
            } else {
                Log.e("HubbleImage", "No se encontró la imagen en el HTML.")
                callback(null)
            }
        } catch (e: Exception) {
            Log.e("HubbleImage", "Error al obtener la imagen: ${e.message}")
            callback(null)
        }
    }.start()
}

@OptIn(ExperimentalEncodingApi::class)
fun downloadImageAndEncodeBase64(imageUrl: String, callback: (String?) -> Unit) {
    Thread {
        try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()

            val inputStream: InputStream = connection.inputStream
            val byteArrayOutputStream = ByteArrayOutputStream()

            // Leer la imagen en bytes
            val buffer = ByteArray(1024)
            var len: Int
            while (inputStream.read(buffer).also { len = it } != -1) {
                byteArrayOutputStream.write(buffer, 0, len)
            }

            // Convertir a array de bytes
            val imageBytes = byteArrayOutputStream.toByteArray()

            // Codificar en Base64
            val encodedImage = Base64.encode(imageBytes, 0, imageBytes.size)

            callback(encodedImage)
        } catch (e: Exception) {
            e.printStackTrace()
            callback(null)
        }
    }.start()
}

fun sendImageToServer(base64Image: String, callback: (String?) -> Unit) {
    val client = OkHttpClient()
    val requestBody = FormBody.Builder()
        .add("image", base64Image)
        .build()

    val request = Request.Builder()
        .url("http://10.0.2.2:5000/algoritmo")
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("ServerError", "Error al comunicarse con el servidor: ${e.message}")
            callback(null)
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                val audioUrl =
                    response.body?.string() // Asegúrate de que tu servidor devuelva la URL del audio
                callback(audioUrl)
            } else {
                Log.e("ServerError", "Respuesta no exitosa del servidor: ${response.code}")
                callback(null)
            }
        }
    })
}

fun playAudio(file: File) {
    val mediaPlayer = MediaPlayer()

    try {
        mediaPlayer.setDataSource(file.absolutePath)
        mediaPlayer.prepare()
        mediaPlayer.start()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}


fun downloadAudio(context: Context, url: String, onFileDownloaded: (File?) -> Unit) {
    val client = OkHttpClient()

    // Crea la solicitud POST
    val request = Request.Builder()
        .url(url)
        .post(
            RequestBody.create(
                null,
                byteArrayOf()
            )
        )
        .build()

    try {
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            // Maneja la respuesta y guarda el archivo
            val file =
                File(context.cacheDir, "audio.wav")
            file.outputStream().use { output ->
                output.write(response.body?.bytes())
            }
            onFileDownloaded(file)
        } else {
            throw IOException("Error al descargar el audio: $response")
        }
    } catch (e: IOException) {
        e.printStackTrace()
        onFileDownloaded(null)
    }
}