package com.example.database;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;



import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LauncherActivity extends AppCompatActivity {
    private com.example.database.AppDatabase db;
    private ExecutorService exec = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = com.example.database.AppDatabase.getInstance(this);
        showInputDialog();
    }

    private void showInputDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint("https://.../song.mp3");

        new AlertDialog.Builder(this)
                .setTitle("لینک موزیک")
                .setMessage("لطفاً آدرس موزیک (URL) را وارد کنید:")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    if (url.isEmpty()) {
                        Toast.makeText(this, "لینک خالی است", Toast.LENGTH_SHORT).show();
                        showInputDialog();
                        return;
                    }

                    exec.execute(() -> {
                        com.example.database.Music music = new com.example.database.Music(url, "");
                        long id = db.musicDao().insert(music);

                        runOnUiThread(() -> {
                            Intent i = new Intent(LauncherActivity.this, PlayerActivity.class);
                            i.putExtra(PlayerActivity.EXTRA_START_MUSIC_ID, id);
                            startActivity(i);
                            finish();
                        });
                    });
                })
                .setNegativeButton("لغو", (dialog, which) -> {

                    finish();
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        exec.shutdown();
    }
}