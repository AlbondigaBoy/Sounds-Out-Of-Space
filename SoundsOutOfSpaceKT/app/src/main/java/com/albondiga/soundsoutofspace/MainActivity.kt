package com.albondiga.soundsoutofspace

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApp() {
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var openDatePicker by remember { mutableStateOf(false) }
    var hubbleImageUrl by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Espacio antes de la imagen
            Spacer(modifier = Modifier.height(40.dp)) // Puedes ajustar este valor

            // Mostrar la imagen si está disponible
            hubbleImageUrl?.let { imageUrl ->
                Image(
                    painter = rememberImagePainter(imageUrl),
                    contentDescription = "Imagen del Hubble",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp) // Espacio horizontal mínimo de 16.dp a ambos lados
                        .wrapContentHeight() // Ajusta la altura automáticamente
                        .aspectRatio(16f / 9f) // Cambia esto a la proporción deseada (ej. 16:9)
                        .clickable {
                            // Aquí descargas el audio y lo reproduces
                            CoroutineScope(Dispatchers.IO).launch {
                                downloadAudio(context, "http://10.0.2.2:5000/algoritmo") { file ->
                                    CoroutineScope(Dispatchers.IO).launch {
                                        withContext(Dispatchers.Main) {
                                            file?.let {
                                                playAudio(it)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                )
            } ?: run {
                Text(
                    text = "",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // Espacio flexible para empujar el botón hacia arriba
            Spacer(modifier = Modifier.weight(1f))

            // Espacio adicional antes del botón
            Spacer(modifier = Modifier.height(20.dp)) // Ajusta este valor también

            // Botón para abrir el selector de fecha
            Button(
                onClick = { openDatePicker = true },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = "Seleccionar fecha")
            }

            Spacer(modifier = Modifier.height(100.dp))

            // Mostrar el diálogo del selector de fecha si está abierto
            if (openDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { openDatePicker = false },
                    onDateSelected = { date ->
                        selectedDate = date
                        openDatePicker = false
                        val calendar = Calendar.getInstance().apply { time = date }
                        val month =
                            calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH)
                                ?.toLowerCase()
                        val day = calendar.get(Calendar.DAY_OF_MONTH)
                        val formattedDate = "$month-$day"

                        getHubbleImage(formattedDate) { imageUrl ->
                            if (imageUrl != null) {
                                hubbleImageUrl = imageUrl
                            } else {
                                Log.e(
                                    "HubbleImage",
                                    "No se pudo obtener la imagen para la fecha: $formattedDate"
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}


private fun getHubbleImage(date: String, callback: (String?) -> Unit) {
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


@OptIn(ExperimentalMaterial3Api::class, ExperimentalEncodingApi::class)
@Composable
fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (Date) -> Unit
) {
    val datePickerState = rememberDatePickerState()

    // Año fijo
    val fixedYear = Calendar.getInstance().get(Calendar.YEAR)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = {
                    // Crear un objeto Date con el año fijo y la fecha seleccionada
                    val selectedDate = Calendar.getInstance().apply {
                        set(Calendar.YEAR, fixedYear) // Establecer el año fijo
                        timeInMillis =
                            datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    }.time
                    onDateSelected(selectedDate)
                }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            Button(onClick = onDismissRequest) {
                Text("Cancelar")
            }
        },
        text = {
            Box(
                modifier = Modifier.size(400.dp, 490.dp)
            ) {
                // Configuración del DatePicker
                DatePicker(
                    state = datePickerState,
                    headline = { Text("Selecciona una fecha") }

                )
            }
        }
    )

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

private fun sendImageToServer(base64Image: String, callback: (String?) -> Unit) {
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
        //Log.d("a", file.absolutePath)
        mediaPlayer.setDataSource(file.absolutePath)
        mediaPlayer.prepare()
        mediaPlayer.start()
    } catch (e: IOException) {
        //Log.d("a", file.absolutePath)
        e.printStackTrace()
    }
}


fun downloadAudio(context: Context, url: String, onFileDownloaded: (File?) -> Unit) {
    val client = OkHttpClient()

    // Crea la solicitud POST
    val request = Request.Builder()
        .url(url)
        .post(RequestBody.create(null, byteArrayOf())) // Puedes enviar datos en el cuerpo si es necesario
        .build()

    try {
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            // Maneja la respuesta y guarda el archivo
            val file = File(context.cacheDir, "audio.wav") // Cambia la extensión según sea necesario
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
