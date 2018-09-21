#include "MixAudio.h"
#include <SuperpoweredSimple.h>
#include <SuperpoweredCPU.h>
#include <jni.h>
#include <stdio.h>
#include <SuperpoweredRecorder.h>
#include <stdlib.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_AndroidConfiguration.h>
#include <android/log.h>

#define log_print __android_log_print

static SuperpoweredAdvancedAudioPlayer *mPlayerA = NULL;
static SuperpoweredAdvancedAudioPlayer *mPlayerB = NULL;
static unsigned int mSamplerate;
float minBpm, maxBpm;
double playerAEOF = 0;
double playerBEOF = 0;

// This is called by player A upon successful load.
static void playerEventCallbackA (
	void *clientData,   // &playerA
	SuperpoweredAdvancedAudioPlayerEvent event,
	void * __unused value
) {
    if (event == SuperpoweredAdvancedAudioPlayerEvent_LoadSuccess) {
        // The pointer to the player is passed to the event callback via the custom clientData pointer.
    	SuperpoweredAdvancedAudioPlayer *playerA = mPlayerA = *((SuperpoweredAdvancedAudioPlayer **)clientData);
        //log_print(ANDROID_LOG_ERROR, "MixAudio", "TEMPO %f : %f", minBpm, maxBpm);
        //playerA->setBpm(126.0f);
        //playerA->setFirstBeatMs(353);
        playerA->setPosition(playerA->firstBeatMs, false, false);
        playerAEOF = 0;
    };

    if (event == SuperpoweredAdvancedAudioPlayerEvent_LoadError){
        SuperpoweredAdvancedAudioPlayer *playerA = mPlayerA = *((SuperpoweredAdvancedAudioPlayer **)clientData);
    }

    if (event == SuperpoweredAdvancedAudioPlayerEvent_EOF){
        SuperpoweredAdvancedAudioPlayer *playerA = *((SuperpoweredAdvancedAudioPlayer **)clientData);
        playerAEOF = 1;
        playerA->pause();
    }
}

// This is called by player B upon successful load.
static void playerEventCallbackB(
	void *clientData,   // &playerB
	SuperpoweredAdvancedAudioPlayerEvent event,
	void * __unused value
) {
    if (event == SuperpoweredAdvancedAudioPlayerEvent_LoadSuccess) {
        // The pointer to the player is passed to the event callback via the custom clientData pointer.
    	SuperpoweredAdvancedAudioPlayer *playerB = mPlayerB = *((SuperpoweredAdvancedAudioPlayer **)clientData);
        //playerB->setBpm(123.0f);
        //playerB->setFirstBeatMs(40);
        playerB->setPosition(playerB->firstBeatMs, false, false);
        playerBEOF = 0;
    };

    if (event == SuperpoweredAdvancedAudioPlayerEvent_EOF){
        SuperpoweredAdvancedAudioPlayer *playerB = *((SuperpoweredAdvancedAudioPlayer **)clientData);
        playerBEOF = 1;
        playerB->pause();
    }
}

// Audio callback function. Called by the audio engine.
static bool audioProcessing (
	void *clientdata,		    // A custom pointer your callback receives.
	short int *audioIO,		    // 16-bit stereo interleaved audio input and/or output.
	int numFrames,			    // The number of frames received and/or requested.
	int __unused samplerate	    // The current sample rate in Hz.
) {
	return ((MixAudio *)clientdata)->process(audioIO, (unsigned int)numFrames);
}

// Crossfader example - Initialize players and audio engine
MixAudio::MixAudio (
		unsigned int samplerate,    // sampling rate
		unsigned int buffersize,    // buffer size
        const char *path1,           // path to Audio 1
        const char *path2           // path to Audio 2
) : activeFx(0), crossValue(0.0f), volB(0.0f), volA(1.0f * headroom)
{
    mSamplerate = samplerate;
    // Allocate aligned memory for floating point buffer.
    stereoBuffer = (float *)memalign(16, buffersize * sizeof(float) * 2);
    //fd = createWAV("/data/user/0/com.tullyapp.tully/files/copytotully/out.wav",mSamplerate,2);

        // Initialize players and open audio files.
    playerA = new SuperpoweredAdvancedAudioPlayer(&playerA, playerEventCallbackA, samplerate, 0);
    playerA->open(path1);
    playerB = new SuperpoweredAdvancedAudioPlayer(&playerB, playerEventCallbackB, samplerate, 0);
    playerB->open(path2);

    crossValue = 0.5;
    volA = 1;
    volB = 1;

    playerA->syncMode = playerB->syncMode = SuperpoweredAdvancedAudioPlayerSyncMode_TempoAndBeat;

    // Initialize audio engine and pass callback function.
    audioSystem = new SuperpoweredAndroidAudioIO (
			samplerate,                     // sampling rate
			buffersize,                     // buffer size
			false,                          // enableInput
			true,                           // enableOutput
			audioProcessing,                // audio callback function
			this,                           // clientData
			-1,                             // inputStreamType (-1 = default)
			SL_ANDROID_STREAM_MEDIA,        // outputStreamType (-1 = default)
			buffersize * 2                  // latency (frames)
	);
}

// Destructor. Free resources.
MixAudio::~MixAudio() {
    //closeWAV(fd);
    delete audioSystem;
    delete playerA;
    delete playerB;
    free(stereoBuffer);
}

// onPlayPause - Toggle playback state of players.
void MixAudio::onPlayPause(bool play) {
    if (!play) {
        playerA->pause();
        playerB->pause();
    } else {
        bool masterIsA = (crossValue <= 0.5f);
        playerA->play(!masterIsA);
        playerB->play(masterIsA);
        playerAEOF = 0;
        playerBEOF = 0;
    };
    SuperpoweredCPU::setSustainedPerformanceMode(play); // <-- Important to prevent audio dropouts.
}

void MixAudio::onVolume(float volumeA, float volumeB, int delta){
    crossValue = float(delta) * 0.01f;
    //log_print(ANDROID_LOG_ERROR, "Delta", "%f", crossValue);
    //volA = cosf(float(M_PI_2) * volumeA) * headroom;
    //volB = cosf(float(M_PI_2) * volumeB) * headroom;
    volA = volumeA;
    volB = volumeB;
}

// Main process function where audio is generated.
bool MixAudio::process (
        short int *output,         // buffer to receive output samples
        unsigned int numFrames     // number of frames requested
) {
    bool masterIsA = (crossValue <= 0.5f);
    double masterBpm = masterIsA ? playerA->currentBpm : playerB->currentBpm;
    // When playerB needs it, playerA has already stepped this value, so save it now.
    double msElapsedSinceLastBeatA = playerA->msElapsedSinceLastBeat;
    // Request audio from player A.
    bool silence = !playerA->process (
            stereoBuffer,  // 32-bit interleaved stereo output buffer.
            false,         // bufferAdd - true: add to buffer / false: overwrite buffer
            numFrames,     // The number of frames to provide.
            volA,          // volume - 0.0f is silence, 1.0f is "original volume"
            masterBpm,     // BPM value to sync with.
            playerB->msElapsedSinceLastBeat // ms elapsed since the last beat on the other track.
    );

    // Request audio from player B.
    if (playerB->process(
            stereoBuffer,  // 32-bit interleaved stereo output buffer.
            !silence,      // bufferAdd - true: add to buffer / false: overwrite buffer
            numFrames,     // The number of frames to provide.
            volB,          // volume - 0.0f is silence, 1.0f is "original volume"
            masterBpm,     // BPM value to sync with.
            msElapsedSinceLastBeatA   // ms elapsed since the last beat on the other track.
    )) silence = false;

    // The stereoBuffer is ready now, let's write the finished audio into the requested buffers.
    if (!silence) {
        SuperpoweredFloatToShortInt(stereoBuffer, output, numFrames);
        /*if (!fd){
            log_print(ANDROID_LOG_ERROR, "MixAudio", "File Exist");
        }
        else{
            fwrite(output, 1, numFrames * 4, fd);
        }*/
    }
    return !silence;
}


static MixAudio *example = NULL;


// MixAudio - Create the DJ app and initialize the players.
extern "C" JNIEXPORT void
Java_com_tullyapp_tully_Multitrack_MultiTrackProject_MixAudio (
		JNIEnv *env,
		jobject __unused obj,
		jint samplerate,        // sampling rate
		jint buffersize,        // buffer size
		jstring path1,        // path to Audio1
        jstring path2        // path to Audio2
) {
    const char *audio1Path = env->GetStringUTFChars(path1, JNI_FALSE);
    const char *audio2Path = env->GetStringUTFChars(path2, JNI_FALSE);
    example = new MixAudio((unsigned int)samplerate, (unsigned int)buffersize, audio1Path, audio2Path);
    env->ReleaseStringUTFChars(path1, audio1Path);
    env->ReleaseStringUTFChars(path2, audio2Path);
}

// onPlayPause - Toggle playback state of player.
extern "C" JNIEXPORT void
Java_com_tullyapp_tully_Multitrack_MultiTrackProject_onPlayPause (
        JNIEnv * __unused env,
        jobject __unused obj,
        jboolean play
) {
	example->onPlayPause(play);
}

// onPlayPausePlayer - Toggle playback state of individual player.
extern "C" JNIEXPORT void
Java_com_tullyapp_tully_Multitrack_MultiTrackProject_onPlayPausePlayer (
        JNIEnv * __unused env,
        jobject __unused obj,
        jboolean play,
        jint playerIndex
) {

    bool masterIsA = (example->crossValue <= 0.5f);

    if (playerIndex == 0){
        if (!play){
            mPlayerA->pause();
        }
        else{
            playerAEOF = 0;
            mPlayerA->play(!masterIsA);
        }
    }
    if (playerIndex == 1){
        if (!play){
            mPlayerB->pause();
        }
        else{
            playerBEOF = 0;
            mPlayerB->play(masterIsA);
        }
    }

    SuperpoweredCPU::setSustainedPerformanceMode(play); // <-- Important to prevent audio dropouts.
}

// onVolume - Handle onVolume events.
extern "C" JNIEXPORT void
Java_com_tullyapp_tully_Multitrack_MultiTrackProject_onVolume (
        JNIEnv * __unused env,
        jobject __unused obj,
        jfloat volumeA,
        jfloat volumeB,
        jint delta
) {
    example->onVolume(volumeA,volumeB,delta);
}

// getPlayerPosition - get the position of the requested player
extern "C" JNIEXPORT jdoubleArray JNICALL
Java_com_tullyapp_tully_Multitrack_MultiTrackProject_getPlayerPosition (
        JNIEnv * __unused env,
        jobject __unused obj,
        jint value
) {
    jdoubleArray arr = env->NewDoubleArray(8);
    jdouble fill[8];
    switch(value){
        case 0:
            if (mPlayerA!=NULL){
                fill[0] = mPlayerA->positionMs;
                fill[1] = mPlayerA->durationMs;
                fill[2] = mPlayerA->playing;
                fill[3] = playerAEOF;
            }

            if (mPlayerB!=NULL){
                fill[4] = mPlayerB->positionMs;
                fill[5] = mPlayerB->durationMs;
                fill[6] = mPlayerB->playing;
                fill[7] = playerBEOF;
            }
            env->SetDoubleArrayRegion(arr, 0, 8, fill);
            return arr;

        case 1:
            if (mPlayerA!=NULL){
                fill[0] = mPlayerA->positionMs;
                fill[1] = mPlayerA->durationMs;
                fill[2] = mPlayerA->playing;
                fill[3] = playerAEOF;
                // move from the temp structure to the java structure
                env->SetDoubleArrayRegion(arr, 0, 4, fill);
                return arr;
            }
            break;

        case 2:
            if (mPlayerB!=NULL){
                fill[0] = mPlayerB->positionMs;
                fill[1] = mPlayerB->durationMs;
                fill[2] = mPlayerB->playing;
                fill[3] = playerBEOF;
                // move from the temp structure to the java structure
                env->SetDoubleArrayRegion(arr, 0, 4, fill);
                return arr;
            }
            break;
        default:
            return NULL;
    }
}


// onClearPlayers - Handle clearing players
extern "C" JNIEXPORT void
Java_com_tullyapp_tully_Multitrack_MultiTrackProject_onClearPlayers (
        JNIEnv * __unused env,
        jobject __unused obj
) {
    if (mPlayerA!=NULL){
        mPlayerA->pause();
    }
    if (mPlayerB!=NULL){
        mPlayerB->pause();
    }
    if (example!=NULL){
        example->~MixAudio();
        //free(example);
    }
}