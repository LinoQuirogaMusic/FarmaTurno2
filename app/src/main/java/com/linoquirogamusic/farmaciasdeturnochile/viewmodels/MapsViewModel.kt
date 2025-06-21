package com.linoquirogamusic.farmaciasdeturnochile.viewmodels

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.linoquirogamusic.farmaciasdeturnochile.models.Farmacia
import com.linoquirogamusic.farmaciasdeturnochile.repositories.FarmaciaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapsViewModel(private val repository: FarmaciaRepository): ViewModel() {

    val isSheetOpen = mutableStateOf(false)
    val data = mutableStateOf("")
    val farmaciasCercanas = mutableStateOf<List<Farmacia>>(emptyList())
    val actualUserLocation = mutableStateOf(LatLng(0.0, 0.0))
    val actualUserLocality = mutableStateOf("")
    val isLoaded = mutableStateOf(false)
    val isDataExtracted = mutableStateOf(false)
    val isMarkersPrinted = mutableStateOf(false)
    val mapReady = mutableStateOf(false)
    val radio = mutableIntStateOf(12000)
    val selectedPharmacy = mutableStateOf<Farmacia?>(null)
    val navigateToMap = mutableStateOf(false)
    val userAction = mutableStateOf(false)
    val selectedPageIndex = mutableIntStateOf(0)
    val allPermissionGranted = mutableStateOf(false)

    init {
        loadFarmacias()
    }

    // Cambia a public para que pueda llamarse desde SplashScreen
    fun loadFarmacias() {
        viewModelScope.launch {
            try {
                println("Iniciando carga de farmacias...")
                val datos = repository.loadData()
                println("Datos recibidos: ${datos.take(200)}...") // Mostrar primeros 200 caracteres
                
                if (datos.isNotEmpty()) {
                    data.value = datos
                    isLoaded.value = true
                    navigateToMap.value = true
                    println("Datos cargados exitosamente. Tamaño: ${datos.length} caracteres")
                } else {
                    println("La respuesta está vacía")
                    isLoaded.value = false
                    navigateToMap.value = false
                }
            } catch (e: Exception) {
                println("Error en loadFarmacias: ${e.message}")
                e.printStackTrace()
                isLoaded.value = false
                // Propagar el error para que pueda ser manejado por el UI si es necesario
                throw e
            }
        }
    }

    fun extractData() {
        viewModelScope.launch {
            repository.extractData(mapsVm = this@MapsViewModel)
            isDataExtracted.value = true
        }
    }

    fun onMapReady() {
        mapReady.value = true
        println("Mapa cargado completamente.")
    }

    fun selectPharmacy(pharmacy: Farmacia) {
        //println("Farmacia ${pharmacy.nombre} seleccionada")
        selectedPharmacy.value = pharmacy


    }

    fun newLocation(userLocation: LatLng) {
        actualUserLocation.value = userLocation
        if(actualUserLocation.value != LatLng(0.0,0.0) && !isDataExtracted.value){
            extractData()
        }else{
            println("Location no valida (newlocation)")
        }
        println("New Location: $userLocation")
    }

    fun onLocationClick() {
        userAction.value = true
        //println("locATION click")
        val mapsIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo:${selectedPharmacy.value?.lat},${selectedPharmacy.value?.long}?z=16&q=${selectedPharmacy.value?.lat},${selectedPharmacy.value?.long}(${selectedPharmacy.value?.nombre})")
        )
        mapsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        repository.launchIntent(mapsIntent)
    }

    fun onCallClick() {
        userAction.value = true
        //println("callclick")
        val callIntent =
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${selectedPharmacy.value?.telefono}"))
        callIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        repository.launchIntent(callIntent)
    }

    fun onPermissionGranted() {
        allPermissionGranted.value = true
    }
}
