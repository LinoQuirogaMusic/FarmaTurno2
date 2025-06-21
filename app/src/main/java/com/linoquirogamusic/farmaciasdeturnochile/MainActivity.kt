package com.linoquirogamusic.farmaciasdeturnochile

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.linoquirogamusic.farmaciasdeturnochile.repositories.FarmaciaRepository
import com.linoquirogamusic.farmaciasdeturnochile.screens.ads.BannerAd
import com.linoquirogamusic.farmaciasdeturnochile.screens.MapsScreen
import com.linoquirogamusic.farmaciasdeturnochile.ui.theme.FarmaciasDeTurnoChileTheme
import com.linoquirogamusic.farmaciasdeturnochile.utils.SetupNavGraph
import com.linoquirogamusic.farmaciasdeturnochile.viewmodels.MapsViewModel

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationRequired: Boolean = false
    private var adSeen = mutableStateOf(false)

    //ADS
    private val bannerId = R.string.banner
    private val interstitialAdId = R.string.resumeInter

    val pharmacyZoom = 15f
    val areaZoom = 11f
    val radius = 12000.0

    override fun onResume() {
        super.onResume()
        if(locationRequired) {
            showInterstitialAd()
        }
        if(adSeen.value){
            loadInterstitialAd(getString(interstitialAdId)){

            }
        }
    }

    override fun onPause() {
        super.onPause()
    }

    private var mInterstitialAd:InterstitialAd?= null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) { initializationStatus ->
            // Puedes manejar el estado de inicialización aquí
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapsVm = MapsViewModel(FarmaciaRepository(applicationContext))
        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST){
        }

        setContent{
            val navController = rememberNavController()
            // Efecto para actualizar la posición de la cámara cuando cambia la ubicación del usuario
            LaunchedEffect(mapsVm.isLoaded.value && mapsVm.allPermissionGranted.value) {
                    if(mapsVm.isLoaded.value && !mapsVm.isDataExtracted.value && mapsVm.actualUserLocation.value == LatLng(0.0,0.0)){
                        fusedLocationClient.lastLocation.addOnSuccessListener { it: Location? ->
                            if(it != null){
                                val userLatLng = LatLng(it.latitude, it.longitude)
                                mapsVm.newLocation(userLatLng)
                                println(userLatLng)
                            }
                        }
                    }
            }

            SetupNavGraph(navController = navController,
                applicationContext,
                mapsVm, movetoMap = {
                if(mapsVm.isDataExtracted.value){
                    FarmaTurno(mapsVm = mapsVm, pharmacyZoom, areaZoom)
                }
                                    },
                onPermissionGranted = {
                locationRequired = true

                mapsVm.onPermissionGranted() }
            )
        }

    }

    @SuppressLint("MissingPermission")
    @Composable
    fun FarmaTurno(mapsVm: MapsViewModel, pharmacyZoom: Float, areaZoom:Float) {


        FarmaciasDeTurnoChileTheme {

            MapsScreen(
                vm = mapsVm,
                onMapLoaded = {
                    mapsVm.onMapReady()
                },
                pharmacyZoom = pharmacyZoom,
                areaZoom = areaZoom,
                radius = radius
            )
            BannerAd(adUnitId = getString(bannerId))
            var adStatus by remember {
                mutableStateOf(false)
            }
            if (!adStatus){
                loadInterstitialAd(getString(interstitialAdId)){
                    adStatus = it
                    adSeen.value = false
                }
            }
        }
    }


    private fun loadInterstitialAd(adId: String, adStatus: (Boolean)->Unit){
        var adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this,adId, adRequest, object : InterstitialAdLoadCallback(){
            override fun onAdFailedToLoad(error: LoadAdError) {
                super.onAdFailedToLoad(error)
                mInterstitialAd = null
                Log.i("AD_TAG", "onAdFailedToLoad: ${error.message}")
                adStatus.invoke(false)
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                super.onAdLoaded(interstitialAd)
                mInterstitialAd = interstitialAd
                Log.i("AD_TAG", "onAdLoaded:")
                adStatus.invoke(true)
            }
        })


    }

    private fun showInterstitialAd(){

        mInterstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    Log.i("AD_TAG", "onAdDismissedFullScreenContent:")
                    mInterstitialAd = null

                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    Log.i("AD_TAG", "onAdImpression:")
                    adSeen.value = true
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    Log.i("AD_TAG", "onAdClickewd:")
                }
            }
            ad.show(this)
        } ?: kotlin.run {
            //Toast.makeText(this, "Ad is null", Toast.LENGTH_SHORT).show()
        }
    }
}