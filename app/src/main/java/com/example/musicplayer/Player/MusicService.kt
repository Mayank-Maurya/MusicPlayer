package com.example.musicplayer.Player

import android.app.PendingIntent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.example.musicplayer.Player.callbacks.MusicPlaybackPreparer
import com.example.musicplayer.Player.callbacks.MusicPlayerEventListener
import com.example.musicplayer.Player.callbacks.MusicPlayerNotificationListener
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import javax.inject.Inject

private const val SERVICE_TAG="MusicService"
@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {


    @Inject
    lateinit var datasouce : DefaultDataSourceFactory

    @Inject
    lateinit var player: ExoPlayer

    @Inject
    lateinit var firebaseMusicSource: FirebaseMusicSource

    private val serviceJob= Job()
    private val serviceScope= CoroutineScope(Dispatchers.Main + serviceJob)


    private lateinit var musicNotificationManager: MusicNotificationManager
    private lateinit var mediaSession : MediaSessionCompat
    private lateinit var mediaSessionConnector : MediaSessionConnector

    var isForeGroundService = false

    private var currentPlayingSong: MediaMetadataCompat? = null

    override fun onCreate() {
        super.onCreate()

        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this,0,it,0)
        }

        mediaSession= MediaSessionCompat(this, SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive=true
        }

        sessionToken = mediaSession.sessionToken

        musicNotificationManager = MusicNotificationManager(
            this,
            mediaSession.sessionToken,
            MusicPlayerNotificationListener(this)
        ){



        }

        val musicplaybackPreparer = MusicPlaybackPreparer (firebaseMusicSource){
            currentPlayingSong = it
            preparePlayer(
                firebaseMusicSource.songs,
                it,
                true
            )
        }

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(musicplaybackPreparer)
        mediaSessionConnector.setPlayer(player)

        player.addListener(MusicPlayerEventListener(this))
        musicNotificationManager.showNotification(player)


    }

    private fun preparePlayer(
        songs: List<MediaMetadataCompat>,
        itemToPlay: MediaMetadataCompat?,
        playNow: Boolean
    ){
        var curSongIndex = if(currentPlayingSong == null) 0 else songs.indexOf(itemToPlay)
        player.prepare(firebaseMusicSource.asMediaSource(datasouce))
        player.seekTo(curSongIndex,0L)
        player.playWhenReady = playNow

    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }




    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        TODO("Not yet implemented")
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        TODO("Not yet implemented")
    }

}