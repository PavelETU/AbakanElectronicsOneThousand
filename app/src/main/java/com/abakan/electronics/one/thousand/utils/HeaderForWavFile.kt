package com.abakan.electronics.one.thousand.utils

import com.abakan.electronics.one.thousand.SAMPLE_RATE

object HeaderForWavFile {
    fun getHeaderForWavFile(amountOfBytesRecorded: Int): ByteArray {
        val chunkID = "RIFF".toByteArray()
        val amountOfBytesForChunkSize = 36 + amountOfBytesRecorded
        val chunkSize = ByteArray(4)
        chunkSize[0] = (amountOfBytesForChunkSize shr 0).toByte()
        chunkSize[1] = (amountOfBytesForChunkSize shr 8).toByte()
        chunkSize[2] = (amountOfBytesForChunkSize shr 16).toByte()
        chunkSize[3] = (amountOfBytesForChunkSize shr 24).toByte()
        val format = "WAVE".toByteArray()
        val subchunk1Id = "fmt ".toByteArray()
        val subchunk1Size = ByteArray(4)
        subchunk1Size[0] = 16
        subchunk1Size[1] = 0
        subchunk1Size[2] = 0
        subchunk1Size[3] = 0
        val audioFormat = ByteArray(2)
        audioFormat[0] = 1
        audioFormat[1] = 0
        val numChannel = ByteArray(2)
        numChannel[0] = 2
        numChannel[1] = 0
        val sampleRate = ByteArray(4)
        sampleRate[0] = ( (SAMPLE_RATE) shr 0).toByte()
        sampleRate[1] = ( (SAMPLE_RATE) shr 8).toByte()
        sampleRate[2] = ( (SAMPLE_RATE) shr 16).toByte()
        sampleRate[3] = ( (SAMPLE_RATE) shr 24).toByte()
        val byteRate = ByteArray(4)
        byteRate[0] = ( (SAMPLE_RATE*4) shr 0).toByte()
        byteRate[1] = ( (SAMPLE_RATE*4) shr 8).toByte()
        byteRate[2] = ( (SAMPLE_RATE*4) shr 16).toByte()
        byteRate[3] = ( (SAMPLE_RATE*4) shr 24).toByte()
        val blockAlign = ByteArray(2)
        blockAlign[0] = 4
        blockAlign[1] = 0
        val bitsPerSample = ByteArray(2)
        bitsPerSample[0] = 16
        bitsPerSample[1] = 0
        val subChunk2Id = "data".toByteArray()
        val subChunk2Size = ByteArray(4)
        subChunk2Size[0] = (amountOfBytesRecorded shr 0).toByte()
        subChunk2Size[1] = (amountOfBytesRecorded shr 8).toByte()
        subChunk2Size[2] = (amountOfBytesRecorded shr 16).toByte()
        subChunk2Size[3] = (amountOfBytesRecorded shr 24).toByte()
        return chunkID + chunkSize + format + subchunk1Id + subchunk1Size +
                audioFormat + numChannel + sampleRate + byteRate + blockAlign + bitsPerSample +
                subChunk2Id + subChunk2Size
    }
}