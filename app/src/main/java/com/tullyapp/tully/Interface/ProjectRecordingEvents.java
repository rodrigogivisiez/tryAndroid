package com.tullyapp.tully.Interface;

import com.tullyapp.tully.FirebaseDataModels.Recording;

public interface ProjectRecordingEvents {
    void projectRecordingUploaded(Recording recording);
    void projectRecordingUploadFailed(Recording recording);
}
