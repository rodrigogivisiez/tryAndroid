package com.tullyapp.tully.Interface;

import com.tullyapp.tully.FirebaseDataModels.AudioFile;

public interface AudioFileEvents {
    void audioFileUploaded(AudioFile audioFile);
    void audioFileUploadFailed(AudioFile audioFile);
}
