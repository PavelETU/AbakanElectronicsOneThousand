//
// Created by Pavel Suvit on 28/09/2022.
//

#ifndef AMPLIFIER_AMPLIFIERSYSTEM_H
#define AMPLIFIER_AMPLIFIERSYSTEM_H

#include <unistd.h>
#include <sys/types.h>
#include <android/log.h>

#ifndef MODULE_NAME
#define MODULE_NAME  "AE-USB"
#endif

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, MODULE_NAME, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, MODULE_NAME, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, MODULE_NAME, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,MODULE_NAME, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,MODULE_NAME, __VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,MODULE_NAME, __VA_ARGS__)

#include "oboe/Oboe.h"

class AmplifierSystem : public oboe::AudioStreamCallback {

public:
    AmplifierSystem() {}

    void setInputStream(std::shared_ptr<oboe::AudioStream> stream) {
        inputStream = stream;
    }

    void setOutputStream(std::shared_ptr<oboe::AudioStream> stream) {
        outputStream = stream;
    }

    virtual void start();

    virtual void stop();

    oboe::DataCallbackResult onAudioReady(
            oboe::AudioStream *audioStream,
            void *audioData,
            int numFrames) override;

private:
    static constexpr int32_t kNumCallbacksToDrain   = 20;
    static constexpr int32_t kNumCallbacksToDiscard = 30;

    // let input fill back up, usually 0 or 1
    int32_t              mNumInputBurstsCushion = 1;

    // We want to reach a state where the input buffer is empty and
    // the output buffer is full.
    // These are used in order.
    // Drain several callback so that input is empty.
    int32_t              mCountCallbacksToDrain = kNumCallbacksToDrain;
    // Let the input fill back up slightly so we don't run dry.
    int32_t              mCountInputBurstsCushion = mNumInputBurstsCushion;
    // Discard some callbacks so the input and output reach equilibrium.
    int32_t              mCountCallbacksToDiscard = kNumCallbacksToDiscard;

    std::shared_ptr<oboe::AudioStream> inputStream;
    std::shared_ptr<oboe::AudioStream> outputStream;

    int32_t              mBufferSize = 0;
    std::unique_ptr<float[]> mInputBuffer;

    oboe::DataCallbackResult onBothStreamsReady(
            std::shared_ptr<oboe::AudioStream> inputStream,
            const void *inputData,
            int   numInputFrames,
            std::shared_ptr<oboe::AudioStream> outputStream,
            void *outputData,
            int   numOutputFrames
    );
};


#endif //AMPLIFIER_AMPLIFIERSYSTEM_H
