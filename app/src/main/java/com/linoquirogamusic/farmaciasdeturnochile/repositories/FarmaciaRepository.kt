package com.linoquirogamusic.farmaciasdeturnochile.repositories

import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.linoquirogamusic.farmaciasdeturnochile.models.Farmacia
import com.linoquirogamusic.farmaciasdeturnochile.viewmodels.MapsViewModel
import com.linoquirogamusic.farmaciasdeturnochile.utils.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class FarmaciaRepository(private val context: Context) {

    val gdc = Geocoder(context, Locale.getDefault())


    private val retrofit: Retrofit by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val gson = GsonBuilder()
            .setLenient()
            .create()

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .header("Accept", "application/json")
                    .header("Accept-Language", "es-CL,es;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("Referer", "https://midas.minsal.cl/farmacia_v2/WS/getLocalesTurnos.php")
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .sslSocketFactory(getSSLSocketFactory(), getTrustManager())
            .hostnameVerifier { _, _ -> true }
            .build()

        Retrofit.Builder()
            .baseUrl("https://midas.minsal.cl/farmacia_v2/WS/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }
    
    private val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    fun launchIntent(intent: Intent){
        context.startActivity(intent)
    }

    fun getSSLSocketFactory(): SSLSocketFactory {
        // Crea un TrustManager que confía en todos los certificados
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
        })

        // Inicializa un SSLContext con nuestro TrustManager
        val sslContext = SSLContext.getInstance("TLSv1.2")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        // Retorna el SSLSocketFactory de nuestro SSLContext
        return sslContext.socketFactory
    }

    fun getTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
        }
    }

    suspend fun loadData(): String = withContext(Dispatchers.IO) {
        try {
            Log.d("FarmaciaRepository", "Iniciando descarga de datos...")
            
            val response = suspendCancellableCoroutine<String> { continuation ->
                apiService.obtenerDatos().enqueue(object : Callback<JsonArray> {
                    override fun onResponse(call: Call<JsonArray>, response: Response<JsonArray>) {
                        if (response.isSuccessful) {
                            val jsonArray = response.body()
                            if (jsonArray != null) {
                                Log.d("FarmaciaRepository", "Datos recibidos: ${jsonArray.size()} farmacias")
                                continuation.resume(jsonArray.toString(), null)
                            } else {
                                Log.w("FarmaciaRepository", "La respuesta está vacía")
                                continuation.resume("[]", null)
                            }
                        } else {
                            val errorMsg = "Error HTTP: ${response.code()} - ${response.message()}"
                            Log.e("FarmaciaRepository", errorMsg)
                            continuation.resume("[]", null)
                        }
                    }

                    override fun onFailure(call: Call<JsonArray>, t: Throwable) {
                        Log.e("FarmaciaRepository", "Error en la llamada: ${t.message}", t)
                        continuation.resume("[]", null)
                    }
                })
            }
            
            return@withContext response
        } catch (e: Exception) {
            Log.e("FarmaciaRepository", "Error en loadData: ${e.message}", e)
            "[]"
        }
    }
    
    private fun cleanResponse(dirtyString: String) = dirtyString
        .trim()
        .replace("\uFEFF", "")
        .replace(Regex("[^\u0009\u000A\u000D\u0020-\u007E\u00A0-\u00FF]"), "")



    private fun distanceInMeter(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double
    ): Float {
        var results = FloatArray(1)
        Location.distanceBetween(startLat, startLon, endLat, endLon, results)
        return results[0]
    }


    suspend fun extractData(mapsVm: MapsViewModel) {
        val userLocation = mapsVm.actualUserLocation.value
        return withContext(Dispatchers.IO) {
            try {
                val jsonArray = JSONArray(mapsVm.data.value)
                var lat: Double
                var long: Double

                // Verificar si la ubicación del usuario está disponible
                println("Coordenadas del usuario: Latitud = ${userLocation.latitude}, Longitud = ${userLocation.longitude}")

                // Obtener la localidad del usuario
                val addresses = gdc.getFromLocation(userLocation.latitude, userLocation.longitude, 4)
                if (addresses.isNullOrEmpty()) {
                    println("No se pudo obtener la localidad del usuario.")
                    return@withContext
                }
                mapsVm.actualUserLocality.value = addresses[0].locality

                val farmaciasArray: ArrayList<Farmacia> = arrayListOf()
                val farmaciasCercanas: ArrayList<Farmacia> = arrayListOf()

                for (i in 0 until jsonArray.length()) {
                    val local = jsonArray.getJSONObject(i)
                    val id = local.getString("local_id")
                    val nombre = String(local.getString("local_nombre").toByteArray(Charsets.UTF_8))
                    val direccion = local.getString("local_direccion").substringBefore("LOCAL").substringBefore(",")
                    val comuna = local.getString("comuna_nombre")

                    val latLongValid = local.getString("local_lat").isNotBlank() &&
                            local.getString("local_lng").isNotBlank() &&
                            local.getString("local_lng").matches("[+-]?\\d*(\\.\\d+)?".toRegex())

                    if (!latLongValid) {
                        val coords = gdc.getFromLocationName("farmacia $direccion $comuna", 1)
                        if (!coords.isNullOrEmpty()) {
                            lat = coords[0].latitude
                            long = coords[0].longitude
                        } else {
                            val coordsFromName = gdc.getFromLocationName("$direccion $comuna", 1)
                            if (!coordsFromName.isNullOrEmpty()) {
                                lat = coordsFromName[0].latitude
                                long = coordsFromName[0].longitude
                            } else {
                                println("No se encontraron coordenadas para $nombre.")
                                continue
                            }
                        }
                    } else {
                        lat = local.getString("local_lat").toDoubleOrNull() ?: continue
                        long = local.getString("local_lng").toDoubleOrNull() ?: continue
                    }

                    val hApertura = local.getString("funcionamiento_hora_apertura")
                    val hCierre = local.getString("funcionamiento_hora_cierre")
                    val telefono = local.getString("local_telefono")
                    val farmacia = farmaciaAdapter(
                        id, nombre, direccion, lat, long, comuna, hApertura, hCierre, telefono
                    )
                    farmaciasArray.add(farmacia)

                    val dist = distanceInMeter(
                        farmacia.lat, farmacia.long, userLocation.latitude, userLocation.longitude
                    )
                    if (dist <= mapsVm.radio.value ||
                        farmacia.comuna == addresses[0].locality
                    ) {
                        farmaciasCercanas.add(farmacia)
                    }
                }

                mapsVm.farmaciasCercanas.value = farmaciasCercanas
                mapsVm.isDataExtracted.value = true
            } catch (e: Exception) {
                println("Error al extraer datos: ${e.message}")
            }
        }
    }




    fun farmaciaAdapter(
        id: String,
        nombre: String,
        direccion: String,
        lat: Double,
        long: Double,
        comuna: String,
        hApertura: String,
        hCierre: String,
        telefono: String
    ): Farmacia {
        val farmaciaOut = Farmacia()
        farmaciaOut.id = id
        farmaciaOut.nombre = nombre
        farmaciaOut.lat = lat
        farmaciaOut.long = long
        farmaciaOut.direccion = direccion
        farmaciaOut.comuna = comuna
        farmaciaOut.hApertura = hApertura
        farmaciaOut.hCierre = hCierre
        farmaciaOut.telefono = telefono
        return farmaciaOut
    }


}
