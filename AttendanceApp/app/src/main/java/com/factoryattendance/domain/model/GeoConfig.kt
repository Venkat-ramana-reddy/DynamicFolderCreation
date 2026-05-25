package com.factoryattendance.domain.model

data class GeoConfig(
    val id: String = "default",
    val factoryLat: Double = 0.0,
    val factoryLng: Double = 0.0,
    val radiusMeters: Int = 200
) {
    val isSet: Boolean get() = factoryLat != 0.0 || factoryLng != 0.0
}
