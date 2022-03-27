package com.example.musicplayer.others

import android.content.res.Resources
import java.security.KeyStore

data class Resource<out T>(val status: Status, val data:T?, val message:String? ) {

    companion object{
        fun <T> success(data: T) = Resource(Status.SUCESS, data, null)

        fun <T> error(message: String?, data: T?) = Resource(Status.ERROR, data, message)

        fun <T> loading(data: T?) = Resource(Status.LOADING, data, null)
    }

}

enum class Status{
    SUCESS,
    ERROR,
    LOADING
}