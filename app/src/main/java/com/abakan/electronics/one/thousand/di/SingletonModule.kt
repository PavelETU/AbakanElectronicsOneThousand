package com.abakan.electronics.one.thousand.di

import com.abakan.electronics.one.thousand.AudioTrackProvider
import com.abakan.electronics.one.thousand.AudioTrackProviderImpl
import com.abakan.electronics.one.thousand.utils.CooleyTurkeyFFT
import com.abakan.electronics.one.thousand.utils.FFTHelper
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

    @Singleton
    @Binds
    abstract fun bindFFTHelper(cooleyTurkeyFFT: CooleyTurkeyFFT): FFTHelper
}