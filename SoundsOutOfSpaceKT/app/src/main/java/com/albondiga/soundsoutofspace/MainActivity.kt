package com.albondiga.soundsoutofspace

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
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApp()
        }
    }
}

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


@OptIn(ExperimentalMaterial3Api::class)
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