//
// Created by Pavel Suvit on 28/09/2022.
//

#include "AmplifierSystem.h"

void AmplifierSystem::start() {
    mCountCallbacksToDrain = kNumCallbacksToDrain;
    mCountInputBurstsCushion = mNumInputBurstsCushion;
    mCountCallbacksToDiscard = kNumCallbacksToDiscard;

    // Determine maximum size that could possibly be called.
    int32_t bufferSize = outputStream->getBufferCapacityInFrames()
                         * outputStream->getChannelCount();
    if (bufferSize > mBufferSize) {
        mInputBuffer = std::make_unique<float[]>(bufferSize);
        mBufferSize = bufferSize;
    }
    oboe::Result result = inputStream->requestStart();
    if (result != oboe::Result::OK) {
        return;
    }
    outputStream->requestStart();
}

void AmplifierSystem::stop() {
    if (outputStream) {
        outputStream->requestStop();
    }
    if (inputStream) {
        inputStream->requestStop();
    }
}

oboe::DataCallbackResult AmplifierSystem::onAudioReady(
        oboe::AudioStream *audioStream,
        void *audioData,
        int numFrames) {
    oboe::DataCallbackResult callbackResult = oboe::DataCallbackResult::Continue;
    int32_t actualFramesRead = 0;

    // Silence the output.
    int32_t numBytes = numFrames * audioStream->getBytesPerFrame();
    memset(audioData, 0 /* value */, numBytes);

    if (mCountCallbacksToDrain > 0) {
        // Drain the input.
        int32_t totalFramesRead = 0;
        do {
            oboe::ResultWithValue<int32_t> result = inputStream->read(mInputBuffer.get(),
                                                                      numFrames,
                                                                      0 /* timeout */);
            if (!result) {
                // Ignore errors because input stream may not be started yet.
                break;
            }
            actualFramesRead = result.value();
            totalFramesRead += actualFramesRead;
        } while (actualFramesRead > 0);
        // Only counts if we actually got some data.
        if (totalFramesRead > 0) {
            mCountCallbacksToDrain--;
        }

    } else if (mCountInputBurstsCushion > 0) {
        // Let the input fill up a bit so we are not so close to the write pointer.
        mCountInputBurstsCushion--;

    } else if (mCountCallbacksToDiscard > 0) {
        // Ignore. Allow the input to reach to equilibrium with the output.
        oboe::ResultWithValue<int32_t> result = inputStream->read(mInputBuffer.get(),
                                                                  numFrames,
                                                                  0 /* timeout */);
        if (!result) {
            callbackResult = oboe::DataCallbackResult::Stop;
        }
        mCountCallbacksToDiscard--;

    } else {
        // Read data into input buffer.
        oboe::ResultWithValue<int32_t> result = inputStream->read(mInputBuffer.get(),
                                                                  numFrames,
                                                                  0 /* timeout */);
        if (!result) {
            callbackResult = oboe::DataCallbackResult::Stop;
        } else {
            int32_t framesRead = result.value();

            callbackResult = onBothStreamsReady(
                    inputStream, mInputBuffer.get(), framesRead,
                    outputStream, audioData, numFrames
            );
        }
    }

    if (callbackResult == oboe::DataCallbackResult::Stop) {
        inputStream->requestStop();
    }

    return callbackResult;
}

oboe::DataCallbackResult
AmplifierSystem::onBothStreamsReady(std::shared_ptr<oboe::AudioStream> inputStream,
                                    const void *inputData, int numInputFrames,
                                    std::shared_ptr<oboe::AudioStream> outputStream,
                                    void *outputData, int numOutputFrames) {
    const float *inputFloats = static_cast<const float *>(inputData);
    float *outputFloats = static_cast<float *>(outputData);

    // It also assumes the channel count for each stream is the same.
    int32_t samplesPerFrame = outputStream->getChannelCount();
    int32_t numInputSamples = numInputFrames * samplesPerFrame;
    int32_t numOutputSamples = numOutputFrames * samplesPerFrame;

    // It is possible that there may be fewer input than output samples.
    int32_t samplesToProcess = std::min(numInputSamples, numOutputSamples);
    for (int32_t i = 0; i < samplesToProcess; i++) {
        outputFloats[i] = inputFloats[i] * 1.5;
    }

    // If there are fewer input samples then clear the rest of the buffer.
    int32_t samplesLeft = numOutputSamples - numInputSamples;
    for (int32_t i = 0; i < samplesLeft; i++) {
        *outputFloats++ = 0.0; // silence
    }

    return oboe::DataCallbackResult::Continue;
}
