package com.example.di

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.repository.ReelRepositoryImpl
import com.example.domain.downloader.ReelDownloader
import com.example.domain.repository.ReelRepository
import com.example.domain.resolver.DefaultReelResolver
import com.example.domain.resolver.ReelResolver

class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    val repository: ReelRepository by lazy {
        ReelRepositoryImpl(database.reelDao())
    }

    val resolver: ReelResolver by lazy {
        DefaultReelResolver()
    }

    val downloader: ReelDownloader by lazy {
        ReelDownloader(context, repository)
    }

    val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(context)
    }
}
