package com.aleksa.overplayinterviewtest.locationevents

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*

class LocationService(var activity: Activity, listener: LocationListener) {

    interface LocationListener {
        fun onLocationUpdate(long: Double, lat: Double)
    }

    private var fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(activity)
    private var locationRequest: LocationRequest
    private var locationCallback: LocationCallback
    var minDistance = 5f // 10 meters
    var currentLong: Double = 0.0
    var currentLat: Double = 0.0
    private var mListener: LocationListener? = null


    init {
        mListener = listener
        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 10000
            smallestDisplacement = minDistance
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    var newLocationLong = 0.0
                    var newLocationLat = 0.0
                    //Calculating current location.
                    currentLong = locationResult.lastLocation.longitude
                    currentLat = locationResult.lastLocation.latitude
                    for (location in locationResult.locations){
                        newLocationLong = location.longitude
                        newLocationLat = location.latitude
                    }
                    mListener?.onLocationUpdate(newLocationLong, newLocationLat)
                }
            }
        }
    }

    fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    // remove location callback
    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}