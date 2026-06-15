package com.aleksa.sensorsdemo.ui

import android.location.Location
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aleksa.sensorsdemo.R
import com.aleksa.sensorsdemo.locationevents.LocationService
import com.aleksa.sensorsdemo.motionevents.MotionService
import com.aleksa.sensorsdemo.utils.PermissionsUtils
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), Player.Listener, MotionService.MotionListener,
    LocationService.LocationListener {

    private lateinit var motionService: MotionService
    private lateinit var locationService: LocationService
    private lateinit var playerView: StyledPlayerView
    private lateinit var player: ExoPlayer
    private val locationA = Location("point A")
    private var currentPitch = 0
    private var currentRoll = 0
    private val minDistance = 10 // meters

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
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        player.setMediaSource(buildMediaSource())
        player.prepare()
        lifecycleScope.launch {
            delay(4000L)
            startMotionServiceListening()
            startLocationServiceListening()
            player.play()
        }
    }

    override fun onLocationUpdate(long: Double, lat: Double) {
        locationA.longitude = locationService.currentLong
        locationA.latitude = locationService.currentLat
        val locationB = Location("point B")
        locationB.longitude = long
        locationB.latitude = lat
        if (player.isPlaying && locationB.distanceTo(locationA) > minDistance) {
            player.seekTo(0)
            player.playWhenReady = true
            locationService.currentLong = long
            locationService.currentLat = lat
        }
    }

    override fun onShakeChange() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    override fun onRotationChange(pitch: Int, roll: Int) {
        val pitchOffSet = 30
        val rollOffSet = 40
        if (pitch > currentPitch + pitchOffSet) {
            player.volume = .2f
        } else if (pitch < currentPitch - pitchOffSet) {
            player.volume = 1f
        }
        if (roll > currentRoll + rollOffSet) {
            player.seekTo(player.currentPosition - 500)
        } else if (roll < currentRoll - rollOffSet) {
            player.seekTo(player.currentPosition + 500)
        }
    }

    private fun startMotionServiceListening() {
        motionService.startListening(this)
        currentRoll = motionService.currentRoll
        currentPitch = motionService.currentPitch
    }

    private fun startLocationServiceListening() {
        locationService.startLocationUpdates()
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= 24) {
            initPlayer()
        }
        motionService = MotionService(this)
        locationService = LocationService(this, this)
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
        motionService.stopListening()
        locationService.stopLocationUpdates()
        if (Util.SDK_INT >= 24) {
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        player.release()
    }

    private fun buildMediaSource(): MediaSource {
        val videoUri = Uri.parse("android.resource://$packageName/${R.raw.sample_video}")
        val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(this)
        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoUri))
    }
}
