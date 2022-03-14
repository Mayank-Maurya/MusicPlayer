package com.example.musicplayer.data.entities.remote

import com.example.musicplayer.data.entities.Song
import com.example.musicplayer.others.CONSTANTS.SONG_COLLECTION
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class MusicDatabase {

    private  val firestore=FirebaseFirestore.getInstance()

    private  val songCollection=firestore.collection(SONG_COLLECTION)

    suspend fun getAllSongs() : List<Song>{
        return try {
            songCollection.get().await().toObjects(Song::class.java)
        }catch (e: Exception){
            emptyList()
        }
    }

}