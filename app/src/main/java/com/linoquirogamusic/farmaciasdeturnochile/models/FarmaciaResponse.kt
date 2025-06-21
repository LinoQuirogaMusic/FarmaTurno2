package com.linoquirogamusic.farmaciasdeturnochile.models

import com.google.gson.annotations.SerializedName

data class FarmaciaResponse(
    @SerializedName("local_id")
    val id: String,
    
    @SerializedName("local_nombre")
    val nombre: String,
    
    @SerializedName("local_direccion")
    val direccion: String,
    
    @SerializedName("local_telefono")
    val telefono: String,
    
    @SerializedName("local_lat")
    val latitud: String,
    
    @SerializedName("local_lng")
    val longitud: String,
    
    @SerializedName("local_comuna")
    val comuna: String,
    
    @SerializedName("local_region")
    val region: String,
    
    @SerializedName("funcionamiento_hora_apertura")
    val horaApertura: String,
    
    @SerializedName("funcionamiento_hora_cierre")
    val horaCierre: String,
    
    @SerializedName("funcionamiento_dia")
    val dia: String
)
