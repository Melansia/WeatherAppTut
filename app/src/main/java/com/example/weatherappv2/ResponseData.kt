package com.example.weatherappv2

data class ResponseData(
    val cord: Cord,
    val weather: List<Weather>,
    val main: MainDetails,
    val visibility: Int,
    val wind: Wind,
    val sys: Sys,
    val timezone: Int,
    val id: Int,
    val name: String,
    val cod: Int

)

data class Cord(
    val lon: Double,
    val lat: Double
)

data class Weather(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)


data class MainDetails(
    val temp: Double,
    val feels_like: Double,
    val temp_min: Double,
    val temp_max: Double,
    val pressure: Double,
    val humidity: Double,
)

data class Wind(
    val speed: Double,
    val deg: Double
)

data class Sys(
    val type: Int,
    val id: Long,
    val country: String,
    val sunrise: Long,
    val sunset: Long

)
