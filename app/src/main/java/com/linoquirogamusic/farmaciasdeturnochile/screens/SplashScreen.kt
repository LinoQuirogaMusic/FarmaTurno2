package com.linoquirogamusic.farmaciasdeturnochile.screens

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.linoquirogamusic.farmaciasdeturnochile.R
import com.linoquirogamusic.farmaciasdeturnochile.ui.theme.TextPrimary
import com.linoquirogamusic.farmaciasdeturnochile.viewmodels.MapsViewModel
import kotlinx.coroutines.delay

val permissions = arrayOf(
    android.Manifest.permission.ACCESS_FINE_LOCATION,
    android.Manifest.permission.ACCESS_COARSE_LOCATION,
    android.Manifest.permission.INTERNET
)

@Composable
fun SplashScreenContent(
    mapsVm: MapsViewModel,
    context: Context,
    onDataExtracted: () -> Unit,
    onPermissionGranted: () -> Unit
) {
    // Controla si los permisos han sido otorgados
    var permissionsGranted by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    
    val launcherMultiplePermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val areGranted = permissionsMap.values.all { it }
        permissionsGranted = areGranted
        if (areGranted) {
            onPermissionGranted()
            // Iniciar carga de datos cuando se otorgan los permisos
            mapsVm.loadFarmacias()
        } else {
            Toast.makeText(context, "Se requieren los permisos para continuar", Toast.LENGTH_LONG).show()
        }
    }

    // Verificar permisos al inicio
    LaunchedEffect(Unit) {
        if (permissions.all { 
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED 
        }) {
            permissionsGranted = true
            onPermissionGranted()
            // Iniciar carga de datos si ya hay permisos
            mapsVm.loadFarmacias()
        } else {
            launcherMultiplePermissions.launch(permissions)
        }
    }

    // Observar cambios en el estado de carga
    LaunchedEffect(mapsVm.navigateToMap.value, mapsVm.isDataExtracted.value) {
        // Actualizar progreso de carga
        when {
            !mapsVm.isLoaded.value -> {
                // Mientras carga
                while (loadingProgress < 0.8f) {
                    delay(50)
                    loadingProgress += 0.01f
                }
            }
            mapsVm.isDataExtracted.value -> {
                // Cuando los datos están listos
                while (loadingProgress < 1f) {
                    delay(30)
                    loadingProgress += 0.02f
                }
                // Navegar cuando todo esté listo
                onDataExtracted()
            }
            mapsVm.isLoaded.value && !mapsVm.isDataExtracted.value -> {
                // Extraer datos si no se han extraído
                mapsVm.extractData()
            }
        }
    }


    // Mostrar UI de carga
    SplashScreenUI(loadingProgress, mapsVm)
}

@Composable
private fun SplashScreenUI(progress: Float, vm: MapsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Imagen del logo
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo FarmaTurno",
            modifier = Modifier
                .height(150.dp)
                .fillMaxWidth(),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Texto de estado
        Text(
            text = if (vm.isDataExtracted.value) "Listo"
                  else if (vm.isLoaded.value) "Procesando datos..."
                  else "Cargando farmacias de turno...",
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Barra de progreso
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun SplashScreenContent(progress: Float, vm: MapsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Imagen del logo
        Image(
            painter = painterResource(id = R.drawable.logo), // Reemplaza con el ID correcto
            contentDescription = "Pharmacy Logo",
            modifier = Modifier
                .height(150.dp)
                .fillMaxWidth(),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Texto
        Text(
            text = if(vm.isLoaded.value) stringResource(id = R.string.title_splash)
            else stringResource(id = R.string.title_downloading),
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Barra de progreso
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}