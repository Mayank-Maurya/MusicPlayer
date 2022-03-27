package com.example.musicplayer.ui.viewModels

import android.media.MediaMetadata.METADATA_KEY_MEDIA_ID
import android.media.browse.MediaBrowser
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.musicplayer.Player.MusicServiceConnector
import com.example.musicplayer.Player.isPlayEnabled
import com.example.musicplayer.Player.isPlaying
import com.example.musicplayer.Player.isPrepared
import com.example.musicplayer.data.entities.Song
import com.example.musicplayer.others.CONSTANTS.MUSIC_ROOT_ID
import com.example.musicplayer.others.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel  @Inject constructor(
    private val musicServiceConnector: MusicServiceConnector
) :ViewModel(){

    private val _mediaItems = MutableLiveData<Resource<List<Song>>>()
    val mediaItems: LiveData<Resource<List<Song>>> = _mediaItems

    val isConnected = musicServiceConnector.isConnected
    val networkError = musicServiceConnector.NetworkError
    val curPlayingSong = musicServiceConnector.curPlayingSong
    val playbackState = musicServiceConnector.playbackState

    init {
        _mediaItems.postValue(Resource.loading(null))
        musicServiceConnector.subscribe(MUSIC_ROOT_ID,object : MediaBrowserCompat.SubscriptionCallback() {

            override fun onChildrenLoaded(
                parentId: String,
                children: MutableList<MediaBrowserCompat.MediaItem>
            ) {
                super.onChildrenLoaded(parentId, children)

                val items = children.map {
                    Song(
                        it.mediaId!!,
                        it.description.title.toString(),
                        it.description.subtitle.toString(),
                        it.description.mediaUri.toString(),
                        it.description.iconUri.toString()
                    )
                }
                _mediaItems.postValue(Resource.success(items))
            }
        })

    }

    fun skipToNextSong(){
        musicServiceConnector.transportControls.skipToNext()
    }

    fun skipToPreviousSong(){
        musicServiceConnector.transportControls.skipToPrevious()
    }

    fun seekToSong(pos: Long){
        musicServiceConnector.transportControls.seekTo(pos)
    }


    fun playOrToggleSong(mediaItem: Song, toggle: Boolean= false){
        val isPrepared = playbackState.value?.isPrepared?: false
        if(isPrepared && mediaItem.songId ==
            curPlayingSong?.value?.getString(METADATA_KEY_MEDIA_ID)){
            playbackState.value?.let { playbackState ->
                when{
                    playbackState.isPlaying -> if (toggle) musicServiceConnector.transportControls.pause()
                    playbackState.isPlayEnabled -> musicServiceConnector.transportControls.play()
                    else -> Unit
                }
            }
        }else {
            musicServiceConnector.transportControls.playFromMediaId(mediaItem.songId, null)
        }
    }

    override fun onCleared() {
        super.onCleared()
        musicServiceConnector.unsubscribe(MUSIC_ROOT_ID,object : MediaBrowserCompat.SubscriptionCallback(){})
    }















































}