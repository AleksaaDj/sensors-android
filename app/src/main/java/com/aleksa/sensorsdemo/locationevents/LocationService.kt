package com.aleksa.sensorsdemo.locationevents

import android.app.Activity
import android.os.Looper
import com.aleksa.sensorsdemo.utils.PermissionsUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

class LocationService(activity: Activity, listener: LocationListener) {

    interface LocationListener {
        fun onLocationUpdate(long: Double, lat: Double)
    }

    private var fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(activity)
    private val locationRequest: LocationRequest
    private val locationCallback: LocationCallback
    private val minDistanceRefresh = 10f // meters
    var currentLong: Double = 0.0
    var currentLat: Double = 0.0
    private var locationListener: LocationListener? = null
    private val permissionUtils = PermissionsUtils(activity)

    init {
        locationListener = listener
        locationRequest = LocationRequest.create().apply {
            interval = 8000
            fastestInterval = 8000
            smallestDisplacement = minDistanceRefresh
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                var newLocationLong = 0.0
                var newLocationLat = 0.0
                for (location in locationResult.locations) {
                    newLocationLong = location.longitude
                    newLocationLat = location.latitude
                }
                locationListener?.onLocationUpdate(newLocationLong, newLocationLat)
            }
        }
    }

    fun startLocationUpdates() {
        if (!permissionUtils.checkPermission()) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener {
                currentLat = it.latitude
                currentLong = it.longitude
            }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
