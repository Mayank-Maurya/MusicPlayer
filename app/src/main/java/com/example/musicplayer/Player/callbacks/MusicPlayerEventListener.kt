package com.example.musicplayer.Player.callbacks

import android.widget.Toast
import com.example.musicplayer.Player.MusicService
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player

class MusicPlayerEventListener(
    private val musicService: MusicService
) : Player.Listener {


    private var playWhenReady = false
    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        if(playbackState == Player.STATE_READY && !playWhenReady)
        {
            musicService.stopForeground(false)
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        super.onPlayWhenReadyChanged(playWhenReady, reason)
        this.playWhenReady = playWhenReady
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)

        Toast.makeText(musicService,"An unknown Error Occured",Toast.LENGTH_SHORT).show()
    }
}