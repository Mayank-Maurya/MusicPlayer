package com.example.musicplayer.Player

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.example.musicplayer.Player.callbacks.MusicPlaybackPreparer
import com.example.musicplayer.Player.callbacks.MusicPlayerEventListener
import com.example.musicplayer.Player.callbacks.MusicPlayerNotificationListener
import com.example.musicplayer.others.CONSTANTS.MUSIC_ROOT_ID
import com.example.musicplayer.others.CONSTANTS.NETWORK_ERROR
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
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
    private lateinit var musicPlayerEventListener : MusicPlayerEventListener

    var isForeGroundService = false

    private var isPlayerInitialized = false
    private var currentPlayingSong: MediaMetadataCompat? = null


    companion object{
        var curSongDuration = 0L
            private set
    }

    override fun onCreate() {
        super.onCreate()

        serviceScope.launch {
            firebaseMusicSource.fetchMediaData()

        }
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

            curSongDuration = player.duration

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
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator())
        mediaSessionConnector.setPlayer(player)

        musicPlayerEventListener = MusicPlayerEventListener(this)
        player.addListener(musicPlayerEventListener)
        musicNotificationManager.showNotification(player)


    }

    private inner class MusicQueueNavigator : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return firebaseMusicSource.songs[windowIndex].description
        }
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

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        player.stop()
    }
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        player.removeListener(musicPlayerEventListener)
        player.release()
    }




    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {

        return BrowserRoot(MUSIC_ROOT_ID,null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        when(parentId)
        {
            MUSIC_ROOT_ID -> {
                val resultSent = firebaseMusicSource.whenReady { isInitialized ->

                    if(isInitialized)
                    {
                        result.sendResult(firebaseMusicSource.asMediaItems())
                        if(!isPlayerInitialized && firebaseMusicSource.songs.isNotEmpty())
                        {
                            preparePlayer(firebaseMusicSource.songs,firebaseMusicSource.songs[0],false)
                            isPlayerInitialized = true
                        }
                    }else{
                        mediaSession.sendSessionEvent(NETWORK_ERROR,null)
                        result.sendResult(null)
                    }

                }
                if(!resultSent)
                {
                    result.detach()
                }
            }
        }
    }

}