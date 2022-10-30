package com.abakan.electronics.one.thousand

class AmpLibrary {
    companion object {
        init {
            System.loadLibrary("amplifier")
        }
    }
    external fun getNativeOutput(): String
    external fun setDefaultParams(sampleRate: Int, framesPerBuffer: Int)
    external fun startStreamingFrom(deviceID: Int): Boolean
    external fun stopStreaming()
    external fun getInputStreamFramesPerBurst(): Int
    external fun getOutputStreamFramesPerBurst(): Int
    external fun getInputLatency(): Double
    external fun getOutputLatency(): Double
    external fun getNumberOfFrames(): Int
}