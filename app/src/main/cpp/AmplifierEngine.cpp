//
// Created by Pavel Suvit on 27/09/2022.
//

#include "AmplifierEngine.h"

void AmplifierEngine::setAEDeviceId(int32_t receiverId) {
    recordingDeviceId = receiverId;
}

oboe::DataCallbackResult
AmplifierEngine::onAudioReady(oboe::AudioStream *oboeStream, void *audioData, int32_t numFrames) {
    lastNumOfFrames = numFrames;
    return amplifierSystem.onAudioReady(oboeStream, audioData, numFrames);
}

bool AmplifierEngine::startStreaming() {
    bool noErrors = true;
    noErrors = prepareStreams() == oboe::Result::OK;
    if (noErrors) {
        amplifierSystem.start();
    }
    return noErrors;
}

oboe::Result AmplifierEngine::prepareStreams() {
    oboe::AudioStreamBuilder inBuilder, outBuilder;
    setupOutStreamParameters(&outBuilder);
    oboe::Result result = outBuilder.openStream(outStream);
    if (result != oboe::Result::OK) {
        return result;
    }
    systemSampleRate = outStream->getSampleRate();
    setupInStreamParameters(&inBuilder, systemSampleRate);
    result = inBuilder.openStream(inStream);
    if (result != oboe::Result::OK) {
        closeStream(outStream);
        return result;
    }
    amplifierSystem.setInputStream(inStream);
    amplifierSystem.setOutputStream(outStream);
    return result;
}

oboe::AudioStreamBuilder *
AmplifierEngine::setupOutStreamParameters(oboe::AudioStreamBuilder *builder) {
    builder->setDataCallback(this)
            ->setDataCallback(this)
            ->setDirection(oboe::Direction::Output)
            ->setChannelCount(oboe::ChannelCount::Stereo);
    return setupCommonStreamParameters(builder);
}

oboe::AudioStreamBuilder *
AmplifierEngine::setupInStreamParameters(oboe::AudioStreamBuilder *builder, int32_t sampleRate) {
    builder
            ->setDeviceId(recordingDeviceId)
            ->setDirection(oboe::Direction::Input)
            ->setSampleRate(sampleRate)
            ->setChannelCount(oboe::ChannelCount::Stereo);
    return setupCommonStreamParameters(builder);
}

oboe::AudioStreamBuilder *
AmplifierEngine::setupCommonStreamParameters(oboe::AudioStreamBuilder *builder) {
    builder->setFormat(oboe::AudioFormat::Float)
            ->setFormatConversionAllowed(true)
            ->setSharingMode(oboe::SharingMode::Exclusive)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency);
    return builder;
}

void AmplifierEngine::closeStream(std::shared_ptr<oboe::AudioStream> stream) {
    stream->stop();
    stream->close();
    stream.reset();
}

void AmplifierEngine::stopStreaming() {
    amplifierSystem.stop();
    closeStream(outStream);
    amplifierSystem.setOutputStream(nullptr);

    closeStream(inStream);
    amplifierSystem.setInputStream(nullptr);
}

int AmplifierEngine::getInputStreamFramesPerBurst() {
    return inStream->getFramesPerBurst();
}

int AmplifierEngine::getOutputStreamFramesPerBurst() {
    return outStream->getFramesPerBurst();
}

double AmplifierEngine::getInLatency() {
    const oboe::ResultWithValue<double> &resultWithValue = inStream->calculateLatencyMillis();
    if (resultWithValue != oboe::Result::OK) {
        return -1;
    }
    return resultWithValue.value();
}

double AmplifierEngine::getOutLatency() {
    const oboe::ResultWithValue<double> &resultWithValue = outStream->calculateLatencyMillis();
    if (resultWithValue != oboe::Result::OK) {
        return -1;
    }
    return resultWithValue.value();
}

int AmplifierEngine::getNumOfFrames() {
    return lastNumOfFrames;
}
