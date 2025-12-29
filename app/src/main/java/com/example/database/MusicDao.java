package com.example.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MusicDao {
    @Insert
    long insert(Music music);

    @Query("SELECT * FROM music_table ORDER BY id ASC")
    List<Music> getAllOrdered();

    @Query("SELECT * FROM music_table WHERE id = :id LIMIT 1")
    Music getById(long id);

    @Query("DELETE FROM music_table")
    void deleteAll();
}