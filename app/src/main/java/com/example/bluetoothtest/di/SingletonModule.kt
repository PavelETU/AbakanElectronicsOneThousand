package com.example.bluetoothtest.di

import com.example.bluetoothtest.AudioTrackProvider
import com.example.bluetoothtest.AudioTrackProviderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SingletonModule {
    @Singleton
    @Binds
    abstract fun bindAudioTrackProvider(audioTrackProvider: AudioTrackProviderImpl): AudioTrackProvider
}