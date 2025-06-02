package com.example.adzan;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.gauravk.audiovisualizer.visualizer.BlastVisualizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 100;

    private RecyclerView recyclerView;
    private TextView textArabic, textLatin, textIndo;
    private ImageButton btnPlayPause;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private Handler handler;
    private Runnable runnable;
    private AdzanModel currentAdzan;
    private AdView adView;
    private BlastVisualizer blastVisualizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerViewAdzan);
        textArabic = findViewById(R.id.textArabic);
        textLatin = findViewById(R.id.textLatin);
        textIndo = findViewById(R.id.textIndo);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        blastVisualizer = findViewById(R.id.blast);

        // Minta permission RECORD_AUDIO saat runtime jika belum diberikan
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }

        MobileAds.initialize(this, initializationStatus -> {});
        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        List<AdzanModel> adzanList = loadAdzanDataFromAssets();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        AdzanAdapter adapter = new AdzanAdapter(this, adzanList, this::playAdzan);
        recyclerView.setAdapter(adapter);

        btnPlayPause.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                if (isPlaying) {
                    mediaPlayer.pause();
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                    stopUpdatingText();
                } else {
                    mediaPlayer.start();
                    btnPlayPause.setImageResource(R.drawable.ic_pause);
                    startUpdatingText();
                }
                isPlaying = !isPlaying;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Record audio permission granted");
            } else {
                Log.e("MainActivity", "Record audio permission denied");
                Toast.makeText(this, "Permission RECORD_AUDIO diperlukan untuk visualizer suara", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void playAdzan(AdzanModel item) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        currentAdzan = item;

        int resId = getResources().getIdentifier(item.getAudioUrl(), "raw", getPackageName());

        try {
            mediaPlayer = new MediaPlayer();

            AssetFileDescriptor afd = getResources().openRawResourceFd(resId);
            if (afd == null) {
                Log.e("MainActivity", "Audio resource not found: " + item.getAudioUrl());
                return;
            }

            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();

            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                mediaPlayer.start();
                btnPlayPause.setImageResource(R.drawable.ic_pause);
                isPlaying = true;

                stopUpdatingText();
                startUpdatingText();

                int sessionId = mediaPlayer.getAudioSessionId();
                Log.d("MainActivity", "Audio Session ID: " + sessionId);

                if (sessionId > 0 && blastVisualizer != null) {
                    try {
                        blastVisualizer.release();
                        blastVisualizer.setAudioSessionId(sessionId);
                    } catch (Exception e) {
                        Log.e("MainActivity", "Failed to init visualizer: " + e.getMessage());
                    }
                } else {
                    Log.e("MainActivity", "Invalid audio session ID for visualizer.");
                }
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                btnPlayPause.setImageResource(R.drawable.ic_play);
                isPlaying = false;
                stopUpdatingText();

                if (blastVisualizer != null) {
                    blastVisualizer.release();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startUpdatingText() {
        if (handler == null) {
            handler = new Handler();
        }
        runnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && currentAdzan != null && mediaPlayer.isPlaying()) {
                    int currentPos = mediaPlayer.getCurrentPosition();
                    updateDisplayedText(currentPos);
                    handler.postDelayed(this, 500);
                }
            }
        };
        handler.post(runnable);
    }

    private void stopUpdatingText() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    private void updateDisplayedText(int currentPos) {
        List<int[]> times = currentAdzan.getTimeStamps();
        List<String> arabics = currentAdzan.getArabicSegments();
        List<String> latins = currentAdzan.getLatinSegments();
        List<String> indos = currentAdzan.getIndoSegments();

        if (times.size() == arabics.size() && arabics.size() == latins.size() && latins.size() == indos.size()) {
            for (int i = 0; i < times.size(); i++) {
                int start = times.get(i)[0];
                int end = times.get(i)[1];

                if (currentPos >= start && currentPos <= end) {
                    textArabic.setText(arabics.get(i));
                    textLatin.setText(latins.get(i));
                    textIndo.setText(indos.get(i));
                    break;
                }
            }
        } else {
            Log.e("MainActivity", "Jumlah teks dan timestamp tidak sama!");
        }
    }

    private List<AdzanModel> loadAdzanDataFromAssets() {
        try {
            AssetManager assetManager = getAssets();
            InputStream inputStream = assetManager.open("adzan_data.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            reader.close();

            Gson gson = new Gson();
            Type listType = new TypeToken<List<AdzanModel>>() {}.getType();
            return gson.fromJson(jsonBuilder.toString(), listType);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        stopUpdatingText();
        if (blastVisualizer != null) {
            blastVisualizer.release();
        }
        super.onDestroy();
    }
}
