package com.tullyapp.tully.Models;

import java.io.Serializable;

public class AudioMixConfiguration implements Serializable{
    private String audioOnePath, audioTwoPath, outputPath, audioOneTime, audioTwoTime;

    private final static String OFFSET = "-ss";
    private final static String IN = "-i";
    private final static String FILTER_COMPLEX = "-filter_complex";
    private final static String AMERGE = "amerge";
    private final static String AC = "-ac";
    private final static String CHANNEL_2 = "2";
    private final static String STEREO_OUTPUT = "-c:a";
    private final static String LIBMP3LAME = "libmp3lame";
    private final static String QUALITY_PARAM = "-q:a";
    private final static String CHANNEL_4 = "4";

    public AudioMixConfiguration(String audioOnePath, String audioTwoPath, String outputPath, String audioOneTime, String audioTwoTime) {
        this.audioOnePath = audioOnePath;
        this.audioTwoPath = audioTwoPath;
        this.outputPath = outputPath;
        this.audioOneTime = audioOneTime;
        this.audioTwoTime = audioTwoTime;
    }

    public String[] generateCommand(){
        String[] command = {
            OFFSET,
            getAudioOneTime(),
            IN,
            getAudioOnePath(),
            OFFSET,
            getAudioTwoTime(),
            IN,
            getAudioTwoPath(),
            FILTER_COMPLEX,
            AMERGE,
            AC,
            CHANNEL_2,
            STEREO_OUTPUT,
            LIBMP3LAME,
            QUALITY_PARAM,
            CHANNEL_4,
            getOutputPath()
        };
        return command;
    }

    public String getAudioOnePath() {
        return audioOnePath;
    }

    public String getAudioTwoPath() {
        return audioTwoPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public String getAudioOneTime() {
        return audioOneTime;
    }

    public String getAudioTwoTime() {
        return audioTwoTime;
    }
}