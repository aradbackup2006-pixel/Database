package com.example.database;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.database.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayerActivity extends AppCompatActivity {

    public static final String EXTRA_START_MUSIC_ID = "extra_start_music_id";

    private MediaPlayer mediaPlayer;
    private SeekBar seekBar;
    private TextView tvElapsed, tvDuration, tvTitle;
    private ImageButton btnPlayPause, btnNext, btnPrev;

    private com.example.database.AppDatabase db;
    private ExecutorService exec = Executors.newSingleThreadExecutor();

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;

    private List<com.example.database.Music> playlist = new ArrayList<>();
    private int currentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        db = com.example.database.AppDatabase.getInstance(this);

        seekBar = findViewById(R.id.seekBar);
        tvElapsed = findViewById(R.id.tvElapsed);
        tvDuration = findViewById(R.id.tvDuration);
        tvTitle = findViewById(R.id.tvTitle);

        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);

        long startId = getIntent().getLongExtra(EXTRA_START_MUSIC_ID, -1);

        loadPlaylistAndStart(startId);

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnNext.setOnClickListener(v -> playNext());
        btnPrev.setOnClickListener(v -> playPrev());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean userTouch = false;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    int newPos = progress;
                    tvElapsed.setText(formatTime(newPos));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                userTouch = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(seekBar.getProgress());
                }
                userTouch = false;
            }
        });
    }

    private void loadPlaylistAndStart(long startId) {
        exec.execute(() -> {
            List<com.example.database.Music> list = db.musicDao().getAllOrdered();
            playlist.clear();
            if (list != null) playlist.addAll(list);


            if (startId != -1) {
                for (int i = 0; i < playlist.size(); i++) {
                    if (playlist.get(i).id == startId) {
                        currentIndex = i;
                        break;
                    }
                }
            }

            runOnUiThread(() -> {
                if (!playlist.isEmpty()) {
                    prepareAndPlay(currentIndex);
                } else {
                    tvTitle.setText("لیست خالی است");
                }
            });
        });
    }

    private void prepareAndPlay(int index) {
        if (index < 0 || index >= playlist.size()) return;
        String url = playlist.get(index).url;
        tvTitle.setText(url);

        releasePlayer();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            mediaPlayer.setOnPreparedListener(mp -> {
                seekBar.setMax(mp.getDuration());
                tvDuration.setText(formatTime(mp.getDuration()));
                mp.start();
                startUpdater();
            });
            mediaPlayer.setOnCompletionListener(mp -> {

                playNext();
            });
        } catch (IOException e) {
            e.printStackTrace();
            tvTitle.setText("خطا در پخش");
        }
    }

    private void startUpdater() {
        stopUpdater();
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int pos = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(pos);
                    tvElapsed.setText(formatTime(pos));
                } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {

                    int pos = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(pos);
                    tvElapsed.setText(formatTime(pos));
                }
                handler.postDelayed(this, 500);
            }
        };
        handler.post(updateRunnable);
    }

    private void stopUpdater() {
        if (updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
            updateRunnable = null;
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        } else {
            mediaPlayer.start();
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
        }
    }

    private void playNext() {
        if (playlist.isEmpty()) return;
        currentIndex++;
        if (currentIndex >= playlist.size()) currentIndex = 0;
        prepareAndPlay(currentIndex);
    }

    private void playPrev() {
        if (playlist.isEmpty()) return;
        currentIndex--;
        if (currentIndex < 0) currentIndex = playlist.size() - 1;
        prepareAndPlay(currentIndex);
    }

    private String formatTime(int ms) {
        int totalSeconds = ms / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void releasePlayer() {
        stopUpdater();
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            } catch (IllegalStateException ignored){}
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
        exec.shutdown();
    }
}