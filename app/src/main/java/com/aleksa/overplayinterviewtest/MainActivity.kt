package com.aleksa.overplayinterviewtest


import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.aleksa.overplayinterviewtest.locationevents.LocationService
import com.aleksa.overplayinterviewtest.motionevents.MotionService
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(), Player.Listener, MotionService.MotionListener,
    LocationService.LocationListener {

    private lateinit var mMotionService: MotionService
    private lateinit var mLocationService: LocationService
    private lateinit var playerView: StyledPlayerView
    var currentPitch = 0
    var currentRoll = 0
    var minDistance = 5 // 5 meters

    private val videoURL =
        "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!checkPermission()) {
            requestPermission()
        }
        playerView = findViewById(R.id.playerView)
    }

    private fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val result1 = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            1
        )
    }

    /*private fun getLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 5000
            smallestDisplacement = 10f
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                //Calculating current location.
                val locationA = Location("point A")
                locationA.latitude = locationResult.lastLocation.latitude
                locationA.longitude = locationResult.lastLocation.longitude
                for (location in locationResult.locations) {
                    val locationB = Location("point B")
                    locationA.latitude = location.latitude
                    locationA.longitude = location.longitude
                    if (mPlayer.isPlaying && locationB.distanceTo(locationA) > minDistance) {
                        mPlayer.seekTo(0)
                        mPlayer.playWhenReady = true
                        locationA.latitude = location.latitude
                        locationA.longitude = location.longitude
                    }
                    Log.d("PATKA", location.longitude.toString())

                }
            }
        }
    }*/

   /* private fun startLocationUpdates() {
        if (checkPermission()) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper() *//* Looper *//*
            )
        }
    }*/

    // stop location updates
    /*private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }*/

    private fun initPlayer() {
        // Create a player instance.
        mPlayer = ExoPlayer.Builder(this).build()
        // Bind the player to the view.
        playerView.player = mPlayer
        // Play player after 4 sec.
        lifecycleScope.launch {
            delay(4000L)
            startMotionServiceListening()
            startLocationServiceListening()
            mPlayer.play()
        }
        // Set the media source to be played.
        mPlayer.setMediaSource(buildMediaSource())
        // Prepare the player.
        mPlayer.prepare()
    }

    override fun onLocationUpdate(long: Double, lat: Double) {
        val locationA = Location("point A")
        locationA.longitude = mLocationService.currentLong
        locationA.latitude = mLocationService.currentLat
        val locationB = Location("point B")
        locationB.longitude = long
        locationB.latitude = lat
        Log.d("PATKA1",mLocationService.currentLat.toString())
        Log.d("PATKA2",lat.toString())
        // Comparing start location with new location.
        if (mPlayer.isPlaying && locationB.distanceTo(locationA) > minDistance) {
            mPlayer.seekTo(0)
            mPlayer.playWhenReady = true
            locationA.longitude = long
            locationA.latitude = lat
        }
    }

    override fun onRotationChange(pitch: Int, roll: Int) {
        val pitchOffSet = 30
        val rollOffSet = 40
        // Calculating if new pitch value is bigger or smaller from startPitch plus/minus offSet.
        if (pitch > currentPitch + pitchOffSet) {
            mPlayer.volume = .2f
        } else if (pitch < currentPitch - pitchOffSet) {
            mPlayer.volume = 1f
        }
        // Calculating if new roll value is bigger or smaller from startPitch plus/minus offSet.
        if (roll > currentRoll + rollOffSet) {
            mPlayer.seekTo(mPlayer.currentPosition - 500)
        } else if (roll < currentRoll - rollOffSet) {
            mPlayer.seekTo(mPlayer.currentPosition + 500)
        }
    }

    private fun startMotionServiceListening() {
        mMotionService.startListening(this)
        currentRoll = mMotionService.currentRoll
        currentPitch = mMotionService.currentPitch
    }

    private fun startLocationServiceListening() {
        mLocationService.startLocationUpdates()
    }

    override fun onStart() {
        super.onStart()
        mMotionService = MotionService(this)
        mLocationService = LocationService(this, this)
        if (Util.SDK_INT >= 24) {
            initPlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT < 24) {
            initPlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT < 24) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        mMotionService.stopListening()
        mLocationService.stopLocationUpdates()
        if (Util.SDK_INT >= 24) {
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        //release player when done
        mPlayer.release()
    }

    //creating mediaSource
    private fun buildMediaSource(): MediaSource {
        // Create a data source factory.
        val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
        // Create a progressive media source pointing to a stream uri.
        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoURL))
    }

    companion object {
        lateinit var mPlayer: ExoPlayer
    }

}
