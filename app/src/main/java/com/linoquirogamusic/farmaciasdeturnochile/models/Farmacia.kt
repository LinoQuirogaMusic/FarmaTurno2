package com.linoquirogamusic.farmaciasdeturnochile.models

data class Farmacia(
    var id: String = "",
    var nombre: String = "",
    var direccion: String = "",
    var lat: Double = 0.0,
    var long: Double = 0.0,
    var comuna: String = "",
    var hApertura: String = "",
    var hCierre: String = "",
    var telefono: String = "",
    var patrocinada: Boolean = false
)