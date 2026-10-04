package com.example.chiptune.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import com.example.chiptune.ChiptuneSynthesizer
import com.example.chiptune.MainActivity
import com.example.chiptune.R
import com.example.chiptune.SynthType
import com.example.chiptune.TetrisSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class TetrisAudioService : Service() {

    private var synth: ChiptuneSynthesizer? = null
    private var mediaSession: MediaSession? = null
    private var positionUpdateJob: Job? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    private val currentLoopPositionSeconds: Double
        get() {
            val totalSec = synth?.getCurrentPositionSeconds() ?: 0.0
            return totalSec % TetrisSong.LOOP_DURATION_SECONDS
        }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopAudioAndService()
            ACTION_PLAY -> resumePlayback()
            ACTION_PAUSE -> pausePlayback()
            ACTION_TOGGLE_PLAY_PAUSE -> {
                if (_isPlaying.value) {
                    pausePlayback()
                } else {
                    resumePlayback()
                }
            }
            ACTION_SEEK_TO -> {
                val seekSeconds = intent.getDoubleExtra(EXTRA_SEEK_SECONDS, 0.0)
                seekToPosition(seekSeconds)
            }
            ACTION_START -> startForegroundServiceWithAudio()
            else -> {
                if (synth == null) {
                    startForegroundServiceWithAudio()
                }
            }
        }
        return START_STICKY
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager = am

        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        hasAudioFocus = false
                        pausePlayback()
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        hasAudioFocus = true
                        resumePlayback()
                    }
                }
            }
            .build()
        audioFocusRequest = focusRequest
        val res = am.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        hasAudioFocus = res
        return res
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return
        val am = audioManager ?: return
        audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
        hasAudioFocus = false
    }

    private fun ensureSynthesizerInitialized(): ChiptuneSynthesizer {
        var s = synth
        if (s == null) {
            s = ChiptuneSynthesizer()
            s.start(
                synthType = SynthType.Square,
                leadSequence = TetrisSong.tetrisMelody,
                bassSequence = TetrisSong.tetrisBass,
            )
            synth = s
        }
        setupMediaSession()
        _isRunning.value = true
        return s
    }

    private fun startForegroundServiceWithAudio() {
        requestAudioFocus()
        val s = ensureSynthesizerInitialized()
        s.resume()

        _isPlaying.value = true

        startPositionUpdater()
        updatePlaybackState()

        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun setupMediaSession() {
        if (mediaSession != null) return

        mediaSession = MediaSession(this, "TetrisMediaSession").apply {
            setMetadata(
                MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, "Tetris Theme")
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, "ChipTune Synthesizer")
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, "8-Bit Game Chiptunes")
                    .putLong(
                        MediaMetadata.METADATA_KEY_DURATION,
                        (TetrisSong.LOOP_DURATION_SECONDS * 1000).toLong(),
                    )
                    .build(),
            )

            setCallback(
                object : MediaSession.Callback() {
                    override fun onPlay() {
                        resumePlayback()
                    }

                    override fun onPause() {
                        pausePlayback()
                    }

                    override fun onSeekTo(pos: Long) {
                        seekToPosition(pos / 1000.0)
                    }

                    override fun onStop() {
                        stopAudioAndService()
                    }
                },
            )

            isActive = true
        }

        updatePlaybackState()
    }

    private fun updatePlaybackState() {
        val session = mediaSession ?: return
        val currentPos = currentLoopPositionSeconds * 1000
        val isPlayingBool = synth?.isPlaying == true

        val state = if (isPlayingBool) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED

        val stateBuilder = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_SEEK_TO or
                        PlaybackState.ACTION_STOP,
            )
            .setState(state, currentPos.toLong(), if (isPlayingBool) 1.0f else 0.0f)

        session.setPlaybackState(stateBuilder.build())
    }

    private fun startPositionUpdater() {
        positionUpdateJob?.cancel()
        positionUpdateJob = serviceScope.launch {
            while (_isRunning.value) {
                _currentPositionSeconds.value = currentLoopPositionSeconds
                updatePlaybackState()
                delay(200.milliseconds)
            }
        }
    }

    private fun pausePlayback() {
        synth?.pause()
        _isPlaying.value = false
        abandonAudioFocus()
        updatePlaybackState()
        updateNotification()
    }

    private fun resumePlayback() {
        requestAudioFocus()
        val s = ensureSynthesizerInitialized()
        s.resume()
        _isPlaying.value = true

        startPositionUpdater()
        updatePlaybackState()

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun seekToPosition(seconds: Double) {
        val s = ensureSynthesizerInitialized()
        s.seekSeconds(seconds)
        _currentPositionSeconds.value = currentLoopPositionSeconds
        updatePlaybackState()
        updateNotification()
    }

    private fun updateNotification() {
        if (_isRunning.value) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, buildNotification())
        }
    }

    private fun stopAudioAndService() {
        abandonAudioFocus()
        positionUpdateJob?.cancel()
        positionUpdateJob = null
        synth?.stop()
        synth = null

        mediaSession?.apply {
            isActive = false
            release()
        }
        mediaSession = null

        _isRunning.value = false
        _isPlaying.value = false
        _currentPositionSeconds.value = 0.0

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopAudioAndService()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Chiptune Background Playback",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Foreground notification for background audio player"
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val playPauseIntent = Intent(this, TetrisAudioService::class.java).apply {
            action = ACTION_TOGGLE_PLAY_PAUSE
        }
        val playPausePendingIntent = PendingIntent.getService(
            this, 11, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = Intent(this, TetrisAudioService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 13, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val playPauseRes = if (_isPlaying.value) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        val playPauseTitle = if (_isPlaying.value) "Pause" else "Play"

        val mediaStyle = Notification.MediaStyle()
            .setMediaSession(mediaSession?.sessionToken)
            .setShowActionsInCompactView(0, 1)

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Tetris Theme (Chiptune)")
            .setContentText(if (_isPlaying.value) "Playing in background" else "Paused")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(_isPlaying.value)
            .setStyle(mediaStyle)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, playPauseRes),
                    playPauseTitle,
                    playPausePendingIntent,
                ).build(),
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
                    "Stop",
                    stopPendingIntent,
                ).build(),
            )
            .build()
    }

    companion object {
        const val CHANNEL_ID = "chiptune_media_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.chiptune.action.START"
        const val ACTION_STOP = "com.example.chiptune.action.STOP"
        const val ACTION_PLAY = "com.example.chiptune.action.PLAY"
        const val ACTION_PAUSE = "com.example.chiptune.action.PAUSE"
        const val ACTION_TOGGLE_PLAY_PAUSE = "com.example.chiptune.action.TOGGLE_PLAY_PAUSE"
        const val ACTION_SEEK_TO = "com.example.chiptune.action.SEEK_TO"

        const val EXTRA_SEEK_SECONDS = "extra_seek_seconds"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _isPlaying = MutableStateFlow(false)
        val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

        private val _currentPositionSeconds = MutableStateFlow(0.0)
        val currentPositionSeconds: StateFlow<Double> = _currentPositionSeconds.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, TetrisAudioService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun play(context: Context) {
            val intent = Intent(context, TetrisAudioService::class.java).apply {
                action = ACTION_PLAY
            }
            context.startService(intent)
        }

        fun pause(context: Context) {
            val intent = Intent(context, TetrisAudioService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun seekTo(context: Context, seconds: Double) {
            val intent = Intent(context, TetrisAudioService::class.java).apply {
                action = ACTION_SEEK_TO
                putExtra(EXTRA_SEEK_SECONDS, seconds)
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TetrisAudioService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
