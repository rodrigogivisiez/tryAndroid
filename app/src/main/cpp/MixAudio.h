#ifndef Header_MixAudio
#define Header_MixAudio

#include <math.h>
#include <pthread.h>
#include <SuperpoweredAdvancedAudioPlayer.h>
#include <AndroidIO/SuperpoweredAndroidAudioIO.h>
#include <jni.h>
#include <stdio.h>

#define HEADROOM_DECIBEL 3.0f
static const float headroom = powf(10.0f, -HEADROOM_DECIBEL * 0.025f);

class MixAudio {
public:

	const char *audioAid, *audioBid;

	MixAudio(unsigned int samplerate, unsigned int buffersize, const char *path1, const char *path2);
	~MixAudio();

	bool process(short int *output, unsigned int numberOfSamples);
	void onPlayPause(bool play);
	void onVolume(float volumeA, float volumeB, int delta);
	jdoubleArray getPlayerPosition(int playerId);

    float crossValue;
private:
    SuperpoweredAndroidAudioIO *audioSystem;
    SuperpoweredAdvancedAudioPlayer *playerA, *playerB;
    float *stereoBuffer;
    unsigned char activeFx;
    float volA, volB;
    FILE *fd;
};

#endif
