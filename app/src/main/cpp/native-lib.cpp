#include <jni.h>
#include <string>
#include <oboe/Oboe.h>
#include "AmplifierEngine.h"

static AmplifierEngine *engine = nullptr;

extern "C"
JNIEXPORT jstring JNICALL
Java_com_abakan_electronics_one_thousand_AmpLibrary_getNativeOutput(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF("Things are good in C++ world!");
}
extern "C"
JNIEXPORT void JNICALL
Java_com_abakan_electronics_one_thousand_AmpLibrary_setDefaultParams(JNIEnv *env, jobject thiz,
                                                                  jint sample_rate,
                                                                  jint frames_per_buffer) {
    oboe::DefaultStreamValues::SampleRate = (int32_t) sample_rate;
    oboe::DefaultStreamValues::FramesPerBurst = (int32_t) frames_per_buffer;
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_abakan_electronics_one_thousand_AmpLibrary_startStreamingFrom(JNIEnv *env, jobject thiz,
                                                                    jint device_id) {
    if (engine == nullptr) {
        engine = new AmplifierEngine();
    }
    engine->setAEDeviceId((int32_t) device_id);
    return engine->startStreaming() ? JNI_TRUE : JNI_FALSE;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_abakan_electronics_one_thousand_AmpLibrary_stopStreaming(JNIEnv *env, jobject thiz) {
    engine->stopStreaming();
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_abakan_electronics_one_thousand_AmpLibrary_getInputStreamFramesPerBurst(JNIEnv *env,
                                                                              jobject thiz) {
    return engine->getInputStreamFramesPerBurst();
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_abakan_electronics_one_thousand_AmpLibrary_getOutputStreamFramesPerBurst(JNIEnv *env,
                                                                               jobject thiz) {
    return engine->getOutputStreamFramesPerBurst();
}
extern "C"
JNIEXPORT jdouble JNICALL
Java_com_abakan_electronics_one_thousand_AmpLibrary_getInputLatency(JNIEnv *env, jobject thiz) {
    return engine->getInLatency();
}
extern "C"
JNIEXPORT jdouble JNICALL
Java_com_abakan_electronics_one_thousand_AmpLibrary_getOutputLatency(JNIEnv *env, jobject thiz) {
    return engine->getOutLatency();
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_abakan_electronics_one_thousand_AmpLibrary_getNumberOfFrames(JNIEnv *env, jobject thiz) {
    return engine->getNumOfFrames();
}