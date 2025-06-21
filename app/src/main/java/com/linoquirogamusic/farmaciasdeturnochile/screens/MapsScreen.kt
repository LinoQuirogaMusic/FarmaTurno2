package com.linoquirogamusic.farmaciasdeturnochile.screens

import android.annotation.SuppressLint
import android.graphics.Camera
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.linoquirogamusic.farmaciasdeturnochile.R
import com.linoquirogamusic.farmaciasdeturnochile.models.Farmacia
import com.linoquirogamusic.farmaciasdeturnochile.ui.theme.BackgroundColor
import com.linoquirogamusic.farmaciasdeturnochile.ui.theme.CircleColor
import com.linoquirogamusic.farmaciasdeturnochile.ui.theme.TextPrimary
import com.linoquirogamusic.farmaciasdeturnochile.ui.theme.TextSecondary
import com.linoquirogamusic.farmaciasdeturnochile.ui.theme.Titulos
import com.linoquirogamusic.farmaciasdeturnochile.viewmodels.MapsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsScreen(
    vm: MapsViewModel,
    onMapLoaded: () -> Unit,
    pharmacyZoom: Float,
    areaZoom: Float,
    radius: Double
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(vm.actualUserLocation.value.latitude, vm.actualUserLocation.value.longitude), areaZoom
        )
    }

    val bottomSheetState = rememberModalBottomSheetState()
    var isSheetOpen = vm.isSheetOpen.value
    val scope = rememberCoroutineScope()
    val permissionsGranted = vm.allPermissionGranted.value

    // Detectar cambios en el estado de la hoja inferior
    LaunchedEffect(bottomSheetState.hasExpandedState) {
        if(bottomSheetState.hasExpandedState){
            print("Expanded")
        }else{
            println("Contraido")
        }
    }

    if(isSheetOpen){
        ModalBottomSheet(
            onDismissRequest = {
                vm.isSheetOpen.value = false
                scope.launch { moveCameratoUser(vm, cameraPositionState, areaZoom)
                    vm.isSheetOpen.value = false}
            },
            sheetState = bottomSheetState,
            shape = RoundedCornerShape(24.dp),
            containerColor = BackgroundColor,
            modifier = Modifier.fillMaxHeight(0.32f)
        )
        {
            if (vm.farmaciasCercanas.value.isNotEmpty()) {
                PharmacyDetailsPager(
                    farmacias = vm.farmaciasCercanas.value,
                    onLocationClick = { vm.onLocationClick() },
                    onCallClick = { vm.onCallClick() },
                    onPageChanged = { pageIndex ->
                        if (pageIndex in vm.farmaciasCercanas.value.indices) {
                            vm.selectPharmacy(vm.farmaciasCercanas.value[pageIndex])
                        }
                    }, vm, cameraPositionState, pharmacyZoom = pharmacyZoom, areaZoom = areaZoom
                )
            } else {
                CircularProgressIndicator() // Mostrar un indicador de carga si no hay farmacias
            }
        }
    }



    // Muestra el contenido de la pantalla solo si los permisos han sido otorgados
    if (permissionsGranted) {
        Column(modifier = Modifier.fillMaxSize()) {
            Mapa(cameraPositionState, vm, onMapLoaded, pharmacyZoom, areaZoom, radius)
        }
    } else {
        // Puedes mostrar un indicador de carga o un mensaje mientras se esperan los permisos
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}





@Composable
fun Mapa(
        cameraPositionState: CameraPositionState,
        vm: MapsViewModel,
        onMapLoaded: () -> Unit,
        pharmacyZoom: Float,
        areaZoom: Float,
        radius: Double)
{



    val markers = remember { mutableStateListOf<MarkerState>() }
    val farmaciasCercanas = vm.farmaciasCercanas.value

    // Actualizar los marcadores solo cuando farmaciasCercanas cambie
    LaunchedEffect(farmaciasCercanas) {
                    if(markers.size != farmaciasCercanas.size || markers.isEmpty()){
                        markers.clear()
                        vm.farmaciasCercanas.value.forEach { pharmacy ->
                            markers.add(MarkerState(LatLng(pharmacy.lat, pharmacy.long)))
                            //println("Marker ${pharmacy.nombre} añadido")
                        }
                        vm.isMarkersPrinted.value = true // Indicar que los marcadores han sido impresos
                    }
            }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = true, minZoomPreference = areaZoom, maxZoomPreference = pharmacyZoom),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false, tiltGesturesEnabled = false),
        onMapLoaded = { onMapLoaded()}
    ) {
        markers.forEachIndexed { index, markerState ->
            Marker(
                state = markerState,
                title = vm.farmaciasCercanas.value[index].nombre,
                icon = BitmapDescriptorFactory.fromResource(R.drawable.farm_marker),
                onClick = {
                    vm.isSheetOpen.value = true
                    val pharmacy = farmaciasCercanas[index]
                    vm.selectPharmacy(pharmacy)
                    vm.selectedPageIndex.value = index
                    println("${vm.selectedPharmacy.value?.nombre} seleccionada")
                    CoroutineScope(Dispatchers.Main).launch{
                        moveCamera(vm, cameraPositionState,pharmacyZoom = pharmacyZoom, areaZoom = areaZoom)
                    }
                    true
                }
            )
            //println("Marker ${vm.farmaciasCercanas.value[index].nombre} pintada")
        }
        // Indicar que los marcadores han sido impresos
            vm.isMarkersPrinted.value = true

        Circle(center = vm.actualUserLocation.value, radius = radius, fillColor = CircleColor, strokeWidth = 0f )
    }
}




suspend fun moveCamera(vm: MapsViewModel, cameraPositionState: CameraPositionState, pharmacyZoom: Float, areaZoom: Float) {
    val selectedPharmacy = vm.selectedPharmacy.value
        withContext(Dispatchers.Main){
            if (selectedPharmacy!= null) {
                cameraPositionState.animate(update = CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(
                    LatLng(selectedPharmacy.lat, selectedPharmacy.long),pharmacyZoom)), 500)
            } else {
                cameraPositionState.animate(update = CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(
                    LatLng(vm.actualUserLocation.value.latitude,vm.actualUserLocation.value.longitude),areaZoom)), 500)
            }
        }
}

suspend fun moveCameratoUser(vm: MapsViewModel, cameraPositionState: CameraPositionState, areaZoom: Float) {
    withContext(Dispatchers.Main){
            cameraPositionState.animate(update = CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(
                LatLng(vm.actualUserLocation.value.latitude,vm.actualUserLocation.value.longitude),areaZoom)), 500)
        }
}

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalPagerApi::class)
@Composable
fun PharmacyDetailsPager(
    farmacias: List<Farmacia>,
    onLocationClick: () -> Unit,
    onCallClick: () -> Unit,
    onPageChanged: (Int) -> Unit,
    vm: MapsViewModel,
    cameraPositionState: CameraPositionState,
    areaZoom: Float,
    pharmacyZoom: Float
) {
    if (farmacias.isEmpty()) return
    
    val initialPage = Int.MAX_VALUE / 2 // Punto medio para tener espacio para navegar en ambas direcciones
    val pagerState = rememberPagerState(initialPage = initialPage + (vm.selectedPageIndex.value % farmacias.size))
    val currentPage = pagerState.currentPage
    val scope = rememberCoroutineScope()
    val pageCount = Int.MAX_VALUE // Número muy grande para simular infinito

    // Calcular el índice real de la farmacia basado en la posición actual del pager
    val currentPharmacyIndex = currentPage % farmacias.size
    val currentPharmacy = farmacias[currentPharmacyIndex]

    HorizontalPager(
        count = pageCount,
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.Top
    ) { pageIndex ->
        // Calcular el índice real de la farmacia para esta página
        val realIndex = pageIndex % farmacias.size
        val pharmacy = farmacias[realIndex]
        
        PharmacyDetailsSheet(
            pharmacy = pharmacy,
            onLocationClick = onLocationClick,
            onCallClick = onCallClick
        )
    }

    // Actualizar la farmacia seleccionada cuando cambia la página
    LaunchedEffect(currentPage) {
        // Solo actualizar si hay farmacias disponibles
        if (farmacias.isNotEmpty()) {
            val realIndex = currentPage % farmacias.size
            onPageChanged(realIndex)
            vm.selectedPharmacy.value = farmacias[realIndex]
            vm.selectedPageIndex.value = realIndex
            println("Cambio de página a la posición $realIndex")
            
            // Mover la cámara a la farmacia seleccionada
            scope.launch { 
                moveCamera(vm, cameraPositionState, pharmacyZoom, areaZoom)
            }
        }
    }
}



@Composable
fun PharmacyDetailsSheet(pharmacy: Farmacia,
                         onLocationClick: () -> Unit,
                         onCallClick: () ->Unit){
    Column(
        modifier = Modifier
            .padding(8.dp, bottom = 16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = pharmacy.nombre.substringAfter("FARMACIA EXTERNA"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Titulos, // Color de texto relacionado con la salud
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Text(
            text = pharmacy.comuna,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Apertura: ${pharmacy.hApertura}",
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Cierre: ${pharmacy.hCierre}",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF004D40)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { onCallClick() }) {
                Icon(Icons.Default.Call, contentDescription = "Llamar", tint = Color.Red, modifier = Modifier.size(64.dp))
            }
            IconButton(onClick = { onLocationClick()}) {
                Icon(Icons.Default.LocationOn, contentDescription = "Navegar", tint = Color.Red, modifier = Modifier.size(64.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}