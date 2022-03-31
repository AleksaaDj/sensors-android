package com.aleksa.overplayinterviewtest

/**
 * Created by Aleksa Djordjevic on March 31st 2022
 * Copyright (c) 2022 . All rights reserved.
 */

import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aleksa.overplayinterviewtest.locationevents.LocationService
import com.aleksa.overplayinterviewtest.motionevents.MotionService
import com.aleksa.overplayinterviewtest.utils.PermissionsUtils
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
    lateinit var mPlayer: ExoPlayer
    private val locationA = Location("point A")
    private var currentPitch = 0
    private var currentRoll = 0
    private var minDistance = 10 // 10 meters

    private val videoURL =
        "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val permissionsUtils = PermissionsUtils(this)
        if (!permissionsUtils.checkPermission()) {
            permissionsUtils.requestPermission()
        }
        playerView = findViewById(R.id.playerView)
    }

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
        // Creating Location A object with initial user location.
        locationA.longitude = mLocationService.currentLong
        locationA.latitude = mLocationService.currentLat
        // Creating Location B object with new user location.
        val locationB = Location("point B")
        locationB.longitude = long
        locationB.latitude = lat
        // Comparing initial location with new location and restart video if user new location is bigger than new location plus minDistance
        if (mPlayer.isPlaying && locationB.distanceTo(locationA) > minDistance) {
            mPlayer.seekTo(0)
            mPlayer.playWhenReady = true
            // Updating user current location.
            mLocationService.currentLong = long
            mLocationService.currentLat = lat
        }
    }

    override fun onShakeChange() {
        if (mPlayer.isPlaying) {
            mPlayer.pause()
        } else {
            mPlayer.play()
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
        if (Util.SDK_INT >= 24) {
            initPlayer()
        }
        mMotionService = MotionService(this)
        mLocationService = LocationService(this, this)
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
}
