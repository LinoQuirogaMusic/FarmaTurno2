package com.linoquirogamusic.farmaciasdeturnochile.utils

import com.google.gson.JsonArray
import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    @GET("getLocalesTurnos.php")
    fun obtenerDatos(): Call<JsonArray>
}
