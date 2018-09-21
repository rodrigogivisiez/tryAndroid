package com.tullyapp.tully.Interface;

import com.tullyapp.tully.FirebaseDataModels.Recording;

public interface RecordingEvents {
    void recordingUploaded(Recording recording);
    void recordingUploadFailed(Recording recording);
}
