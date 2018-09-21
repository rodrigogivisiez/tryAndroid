#include "Analyze.h"
#include <jni.h>
#include <stdio.h>
#include <android/log.h>
#include <stdlib.h>
#include <SuperpoweredDecoder.h>
#include <SuperpoweredSimple.h>
#include <SuperpoweredAnalyzer.h>

#define log_print

static Analyze *analyzeObject = NULL;

Analyze::Analyze() {

}

bool Analyze::analyzeAudio(const char *path) {
    // Open the input file.
    superpoweredDecoder = new SuperpoweredDecoder();
    const char *openError = superpoweredDecoder->open(path, false, 0, 0);
    if (openError) {
        //printf("Open error:", openError);
        log_print(ANDROID_LOG_ERROR, "Open error :","%s",openError);
        delete superpoweredDecoder;
        return false;
    }
    else{
        analyzeObject->percent = 0;
        // Create the analyzer.
        superpoweredAnalyzer = new SuperpoweredOfflineAnalyzer(superpoweredDecoder->samplerate, 0, superpoweredDecoder->durationSeconds);

        // Create a buffer for the 16-bit integer samples coming from the decoder.
        short int *intBuffer = (short int *)malloc(superpoweredDecoder->samplesPerFrame * 2 * sizeof(short int) + 32768);
        // Create a buffer for the 32-bit floating point samples required by the effect.
        float *floatBuffer = (float *)malloc(superpoweredDecoder->samplesPerFrame * 2 * sizeof(float) + 32768);

        // Processing.

        while (true) {
            // Decode one frame. samplesDecoded will be overwritten with the actual decoded number of samples.
            unsigned int samplesDecoded = superpoweredDecoder->samplesPerFrame;
            if (superpoweredDecoder->decode(intBuffer, &samplesDecoded) == SUPERPOWEREDDECODER_ERROR) break;
            if (samplesDecoded < 1) break;

            // Convert the decoded PCM samples from 16-bit integer to 32-bit floating point.
            SuperpoweredShortIntToFloat(intBuffer, floatBuffer, samplesDecoded);

            // Submit samples to the analyzer.
            superpoweredAnalyzer->process(floatBuffer, samplesDecoded);

            // Update the progress indicator.
            int p = int(((double)superpoweredDecoder->samplePosition / (double) superpoweredDecoder->durationSamples) * 100.0);
            if (analyzeObject->percent != p) {
                analyzeObject->percent = p;
                //printf("\r%i%%", progress);
                log_print(ANDROID_LOG_ERROR, "Progress :","%d",p);
                //fflush(stdout);
            }
        };

        // Get the result.
        unsigned char *averageWaveform = NULL, *lowWaveform = NULL, *midWaveform = NULL, *highWaveform = NULL, *peakWaveform = NULL, *notes = NULL;
        int waveformSize, overviewSize, keyIndex;
        char *overviewWaveform = NULL;
        float loudpartsAverageDecibel, peakDecibel, bpm, averageDecibel, beatgridStartMs = 0;
        superpoweredAnalyzer->getresults(&averageWaveform, &peakWaveform, &lowWaveform, &midWaveform, &highWaveform, &notes, &waveformSize, &overviewWaveform, &overviewSize, &averageDecibel, &loudpartsAverageDecibel, &peakDecibel, &bpm, &beatgridStartMs, &keyIndex);

        analyzeObject->bpm = bpm;
        analyzeObject->key = musicalChordNames[keyIndex];

        //log_print(ANDROID_LOG_ERROR,"ANALYZE RESULT","\rBpm is %f, Key is %s", bpm, analyzeObject->key);

        // Cleanup.
        delete superpoweredDecoder;
        delete superpoweredAnalyzer;
        free(intBuffer);
        free(floatBuffer);

        // Do something with the result.
        // printf("\rBpm is %f, average loudness is %f db, peak volume is %f db.\n", bpm, loudpartsAverageDecibel, peakDecibel);
        //log_print(ANDROID_LOG_ERROR,"ANALYZE RESULT","\rBpm is %f, average loudness is %f db, peak volume is %f db.\n beatgridStartMs is %f.\n keyIndex is %i", bpm, loudpartsAverageDecibel, peakDecibel, beatgridStartMs, keyIndex);

        // Done with the result, free memory.
        if (averageWaveform) free(averageWaveform);
        if (lowWaveform) free(lowWaveform);
        if (midWaveform) free(midWaveform);
        if (highWaveform) free(highWaveform);
        if (peakWaveform) free(peakWaveform);
        if (notes) free(notes);
        if (overviewWaveform) free(overviewWaveform);

        return true;
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tullyapp_tully_Services_AudioAnalyzeService_analyzeAudio(JNIEnv *env, jobject instance, jstring path_) {
    const char *path = env->GetStringUTFChars(path_, 0);
    bool boo = analyzeObject->analyzeAudio(path);
    env->ReleaseStringUTFChars(path_, path);
    return static_cast<jboolean>(boo);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_tullyapp_tully_Services_AudioAnalyzeService_getPercent(JNIEnv *env, jobject instance) {
    return analyzeObject->percent;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tullyapp_tully_Services_AudioAnalyzeService_init(JNIEnv *env, jobject instance) {
    analyzeObject = new Analyze();
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tullyapp_tully_Services_AudioAnalyzeService_getKey(JNIEnv *env, jobject instance) {
    return env->NewStringUTF(analyzeObject->key);
}

extern "C"
JNIEXPORT jfloat JNICALL
Java_com_tullyapp_tully_Services_AudioAnalyzeService_getBpm(JNIEnv *env, jobject instance) {
    return analyzeObject->bpm;
}