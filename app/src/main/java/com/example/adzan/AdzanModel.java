package com.example.adzan;

import java.util.List;

public class AdzanModel {
    private String nama;
    private String audio;
    private String icon;
    private List<String> arabic;
    private List<String> latin;
    private List<String> indo;
    private List<int[]> timestamps;

    public String getNama() {
        return nama;
    }

    public String getAudioUrl() {
        return audio;
    }

    public int getImageResId() {
        return App.getContext().getResources().getIdentifier(icon, "drawable", App.getContext().getPackageName());
    }

    public List<String> getArabicSegments() {
        return arabic;
    }

    public List<String> getLatinSegments() {
        return latin;
    }

    public List<String> getIndoSegments() {
        return indo;
    }

    public List<int[]> getTimeStamps() {
        return timestamps;
    }
}
