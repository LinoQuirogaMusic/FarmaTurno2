package com.linoquirogamusic.farmaciasdeturnochile.models

data class PharmacyDetails(
    val id: String,
    val nombre: String,
    val direccion: String,
    val region: String,
    val comuna: String,
    val telefono: String,
    val horarioSemana: String,
    val horarioDia: String,
    val horarioTurno: String
)