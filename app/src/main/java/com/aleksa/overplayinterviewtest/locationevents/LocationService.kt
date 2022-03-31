package com.aleksa.overplayinterviewtest.locationevents

import android.app.Activity
import android.os.Looper
import com.aleksa.overplayinterviewtest.utils.PermissionsUtils
import com.google.android.gms.location.*

class LocationService(activity: Activity, listener: LocationListener) {

    interface LocationListener {
        fun onLocationUpdate(long: Double, lat: Double)
    }

    private var fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(activity)
    private var locationRequest: LocationRequest
    private var locationCallback: LocationCallback
    var minDistanceRefresh = 10f // 10 meters
    var currentLong: Double = 0.0
    var currentLat: Double = 0.0
    private var mListener: LocationListener? = null
    var permissionUtils = PermissionsUtils(activity)

    init {
        mListener = listener
        locationRequest = LocationRequest.create().apply {
            interval = 8000
            fastestInterval = 8000
            smallestDisplacement = minDistanceRefresh
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    var newLocationLong = 0.0
                    var newLocationLat = 0.0
                    for (location in locationResult.locations) {
                        newLocationLong = location.longitude
                        newLocationLat = location.latitude
                    }
                    mListener?.onLocationUpdate(newLocationLong, newLocationLat)
                }
            }
        }
    }

    fun startLocationUpdates() {
        if (permissionUtils.checkPermission()) {
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
        } else {
            return
        }
    }

    // remove location callback
    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}