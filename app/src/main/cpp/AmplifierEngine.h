//
// Created by Pavel Suvit on 27/09/2022.
//

#ifndef AMPLIFIER_AMPLIFIERENGINE_H
#define AMPLIFIER_AMPLIFIERENGINE_H

#include <jni.h>
#include <string>
#include <oboe/Oboe.h>
#include "AmplifierSystem.h"

class AmplifierEngine : public oboe::AudioStreamDataCallback {

public:
    AmplifierEngine() {}

    void setAEDeviceId(int32_t receiverId);

    bool startStreaming();

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *oboeStream,
                                          void *audioData, int32_t numFrames) override;

    void stopStreaming();

    int getInputStreamFramesPerBurst();

    int getOutputStreamFramesPerBurst();

    double getOutLatency();

    double getInLatency();

    int getNumOfFrames();

private:
    AmplifierSystem amplifierSystem;
    int32_t recordingDeviceId = oboe::kUnspecified;
    std::shared_ptr<oboe::AudioStream> inStream;
    std::shared_ptr<oboe::AudioStream> outStream;
    int32_t lastNumOfFrames;
    int32_t systemSampleRate = oboe::kUnspecified;

    oboe::Result prepareStreams();

    oboe::AudioStreamBuilder *setupOutStreamParameters(oboe::AudioStreamBuilder *builder);

    oboe::AudioStreamBuilder *setupCommonStreamParameters(oboe::AudioStreamBuilder *builder);

    oboe::AudioStreamBuilder *setupInStreamParameters(oboe::AudioStreamBuilder *builder, int32_t sampleRate);

    void closeStream(std::shared_ptr<oboe::AudioStream> stream);
};

#endif //AMPLIFIER_AMPLIFIERENGINE_H
