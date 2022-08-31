package com.example.weatherappv2.models

import java.io.Serializable

data class WeatherResponse(
    val cord: Coord,
    val weather: List<Weather>,
    val base: String,
    val main: Main,
    val visibility: Int,
    val wind: Wind,
    val sys: Sys,
    val clouds: Clouds,
    val timezone: Int,
    val id: Int,
    val name: String,
    val cod: Int
) : Serializable