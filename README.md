# Android Sensors Demo

A sample Android app that demonstrates how device sensors can drive real-time UI and media interactions. Video playback is controlled through motion and location input using ExoPlayer.

Built as a technical demo showcasing Android sensor APIs (2022).

## Features

| Sensor | Control |
|--------|---------|
| **Accelerometer** | Shake the device to pause or resume playback |
| **Rotation vector** | Tilt forward/back to lower or raise volume; tilt left/right to seek backward/forward |
| **GPS (Fused Location)** | Walk more than 10 meters to restart the video from the beginning |

The app plays a bundled sample video from `res/raw/` so playback works offline without relying on external URLs.

## Architecture

```
MainActivity
├── MotionService      — rotation vector + accelerometer (SensorManager)
├── LocationService    — fused location updates (Google Play Services)
└── ExoPlayer          — video playback (StyledPlayerView)
```

- **`MainActivity`** — wires sensor callbacks to ExoPlayer commands (play, pause, seek, volume).
- **`MotionService`** — listens for rotation and shake events, maps orientation to pitch/roll.
- **`LocationService`** — requests location updates and notifies when the user moves beyond a threshold.
- **`PermissionsUtils`** — handles runtime location permission requests.

## Requirements

- Android 5.0+ (API 21)
- A physical device recommended (sensors and GPS are limited on emulators)
- Location permission granted at runtime for GPS-based controls

## Getting started

1. Open the project in Android Studio.
2. Sync Gradle and run on a device.
3. Grant location permission when prompted.
4. Wait ~4 seconds after launch for sensor listeners to start, then try shaking or tilting the phone.

## Tech stack

- Kotlin
- ExoPlayer 2
- Google Play Services Location
- AndroidX (AppCompat, Lifecycle, Coroutines)

## License

MIT — see [LICENSE](LICENSE).
