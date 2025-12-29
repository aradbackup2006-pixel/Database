package com.example.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "music_table")
public class Music {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String url;
    public String title;

    public Music(String url, String title) {
        this.url = url;
        this.title = title;
    }
}