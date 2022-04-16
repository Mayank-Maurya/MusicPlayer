package com.example.musicplayer.ui.fragment

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.R
import com.example.musicplayer.adapters.SongAdapter
import com.example.musicplayer.others.Status
import com.example.musicplayer.ui.viewModels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_home.*
import java.lang.Error
import java.lang.Exception
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {


    lateinit var  mainViewModel: MainViewModel

    @Inject
    lateinit var  songAdapter: SongAdapter


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
            setuprecyclerView()
            subscribeToObservers()

            songAdapter.setOnItemClickListener {
                mainViewModel.playOrToggleSong(it)
            }
        }catch (e: Exception){

            Log.i(TAG, " "+e)


        }

    }

    private fun setuprecyclerView() = rvAllSongs.apply {
        adapter = songAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }



    private fun subscribeToObservers()
    {
        mainViewModel.mediaItems.observe(viewLifecycleOwner){ result ->
            when(result.status)
            {
                Status.SUCESS->{
                    allSongsProgressBar.isVisible = false

                    result.data?.let { songs ->
                        songAdapter.songs = songs
                    }
                }
                Status.ERROR-> Unit
                Status.LOADING -> allSongsProgressBar.isVisible = true
            }
        }
    }


}