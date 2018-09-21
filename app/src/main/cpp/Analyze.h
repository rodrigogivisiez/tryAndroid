#ifndef Header_Analyze
#define Header_Analyze

#include <math.h>
#include <pthread.h>
#include <jni.h>
#include <SuperpoweredDecoder.h>
#include <SuperpoweredAnalyzer.h>

class Analyze {
public:
	Analyze();
	int percent;
	float bpm;
	const char *key;
	~Analyze();

	bool analyzeAudio(const char *path);

    SuperpoweredDecoder *superpoweredDecoder;
private:
    SuperpoweredOfflineAnalyzer *superpoweredAnalyzer;
	float *stereoBuffer;
};

#endif
