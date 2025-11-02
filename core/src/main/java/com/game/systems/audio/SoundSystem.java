package com.game.systems.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.game.systems.settings.GameSettings;

import java.util.*;

/**
 * Singleton audio manager for the game.
 * Handles sound effects, background music, and volume controls.
 *
 * Features:
 * - Lazy initialization (automatically initializes on first use)
 * - Lazy-loading of sound assets (only loads when first played)
 * - Simple sound limiting (max 16 concurrent sounds)
 * - Separate volume controls for master, music, and SFX
 * - Persistent volume settings via GameSettings
 * - Background music playback with looping support
 *
 * Usage:
 * - Play sounds: SoundSystem.getInstance().playSound(SoundRegistry.SWORD_SWING)
 * - Play music: SoundSystem.getInstance().playMusic(MusicTrack.ADVENTURE_BEGIN, true)
 * - No explicit initialization required - instance is created automatically
 */
public class SoundSystem {
    private static SoundSystem instance;

    // Sound effect management
    private final Map<SoundRegistry, Sound> loadedSounds;
    private final Queue<Long> activeSoundIds;  // Track active sound IDs for limiting
    private static final int MAX_CONCURRENT_SOUNDS = 16;

    // Music management
    private Music currentMusic;
    private MusicTrack currentTrack;
    private final Map<MusicTrack, Music> loadedMusic;

    // Volume settings
    private float masterVolume = 1.0f;
    private float musicVolume = 0.7f;
    private float sfxVolume = 0.8f;

    /**
     * Private constructor for singleton pattern.
     */
    private SoundSystem() {
        this.loadedSounds = new HashMap<>();
        this.activeSoundIds = new LinkedList<>();
        this.loadedMusic = new HashMap<>();

        // Load volume settings from persistent storage
        loadVolumeSettings();
    }

    /**
     * Gets the singleton instance.
     * Automatically initializes on first access (lazy initialization).
     */
    public static SoundSystem getInstance() {
        if (instance == null) {
            instance = new SoundSystem();
            System.out.println("SoundSystem auto-initialized on first access");
        }
        return instance;
    }

    // ==================== SOUND EFFECT METHODS ====================

    /**
     * Plays a sound effect at full volume (respects master and SFX volume).
     * @param soundId The sound to play
     */
    public void playSound(SoundRegistry soundId) {
        playSound(soundId, 1.0f);
    }

    /**
     * Plays a sound effect with a volume multiplier.
     * @param soundId The sound to play
     * @param volumeMultiplier Volume multiplier (0.0 to 1.0+)
     */
    public void playSound(SoundRegistry soundId, float volumeMultiplier) {
        if (soundId == null) return;

        try {
            // Lazy-load the sound if not already cached
            Sound sound = loadedSounds.get(soundId);
            if (sound == null) {
                sound = Gdx.audio.newSound(Gdx.files.internal(soundId.getPath()));
                loadedSounds.put(soundId, sound);
            }

            // Calculate final volume (master * sfx * multiplier)
            float finalVolume = masterVolume * sfxVolume * volumeMultiplier;

            // Play the sound and track its ID
            long soundId_long = sound.play(finalVolume);

            // Add to active sounds queue
            activeSoundIds.add(soundId_long);

            // Enforce sound limit (stop oldest sound if over limit)
            if (activeSoundIds.size() > MAX_CONCURRENT_SOUNDS) {
                Long oldestSoundId = activeSoundIds.poll();
                if (oldestSoundId != null) {
                    sound.stop(oldestSoundId);
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to play sound: " + soundId + " - " + e.getMessage());
        }
    }

    /**
     * Stops all currently playing sound effects.
     */
    public void stopAllSounds() {
        for (Sound sound : loadedSounds.values()) {
            sound.stop();
        }
        activeSoundIds.clear();
    }

    // ==================== MUSIC METHODS ====================

    /**
     * Plays a music track.
     * @param track The music track to play
     * @param loop Whether to loop the track
     */
    public void playMusic(MusicTrack track, boolean loop) {
        if (track == null) return;

        try {
            // Stop current music if playing
            if (currentMusic != null && currentMusic.isPlaying()) {
                currentMusic.stop();
            }

            // Lazy-load the music if not already cached
            Music music = loadedMusic.get(track);
            if (music == null) {
                music = Gdx.audio.newMusic(Gdx.files.internal(track.getPath()));
                loadedMusic.put(track, music);
            }

            // Configure and play
            currentMusic = music;
            currentTrack = track;
            currentMusic.setLooping(loop);
            currentMusic.setVolume(masterVolume * musicVolume);
            currentMusic.play();

            System.out.println("Playing music: " + track);

        } catch (Exception e) {
            System.err.println("Failed to play music: " + track + " - " + e.getMessage());
        }
    }

    /**
     * Stops the currently playing music.
     */
    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
            currentTrack = null;
        }
    }

    /**
     * Pauses the currently playing music.
     */
    public void pauseMusic() {
        if (currentMusic != null && currentMusic.isPlaying()) {
            currentMusic.pause();
        }
    }

    /**
     * Resumes the currently paused music.
     */
    public void resumeMusic() {
        if (currentMusic != null && !currentMusic.isPlaying()) {
            currentMusic.play();
        }
    }

    /**
     * Checks if music is currently playing.
     */
    public boolean isMusicPlaying() {
        return currentMusic != null && currentMusic.isPlaying();
    }

    /**
     * Gets the currently playing music track.
     */
    public MusicTrack getCurrentTrack() {
        return currentTrack;
    }

    // ==================== VOLUME CONTROL METHODS ====================

    /**
     * Sets the master volume (affects all audio).
     * @param volume Volume level (0.0 to 1.0)
     */
    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0f, Math.min(1f, volume));

        // Update current music volume
        if (currentMusic != null) {
            currentMusic.setVolume(masterVolume * musicVolume);
        }

        // Save to settings
        saveVolumeSettings();
    }

    /**
     * Sets the music volume.
     * @param volume Volume level (0.0 to 1.0)
     */
    public void setMusicVolume(float volume) {
        this.musicVolume = Math.max(0f, Math.min(1f, volume));

        // Update current music volume
        if (currentMusic != null) {
            currentMusic.setVolume(masterVolume * musicVolume);
        }

        // Save to settings
        saveVolumeSettings();
    }

    /**
     * Sets the sound effects volume.
     * @param volume Volume level (0.0 to 1.0)
     */
    public void setSfxVolume(float volume) {
        this.sfxVolume = Math.max(0f, Math.min(1f, volume));

        // Save to settings
        saveVolumeSettings();
    }

    /**
     * Gets the master volume.
     */
    public float getMasterVolume() {
        return masterVolume;
    }

    /**
     * Gets the music volume.
     */
    public float getMusicVolume() {
        return musicVolume;
    }

    /**
     * Gets the sound effects volume.
     */
    public float getSfxVolume() {
        return sfxVolume;
    }

    // ==================== SETTINGS PERSISTENCE ====================

    /**
     * Loads volume settings from GameSettings.
     */
    private void loadVolumeSettings() {
        try {
            GameSettings settings = GameSettings.getInstance();
            this.masterVolume = settings.getMasterVolume();
            this.musicVolume = settings.getMusicVolume();
            this.sfxVolume = settings.getSfxVolume();
            System.out.println("Loaded volume settings - Master: " + masterVolume +
                               ", Music: " + musicVolume + ", SFX: " + sfxVolume);
        } catch (Exception e) {
            System.err.println("Failed to load volume settings, using defaults: " + e.getMessage());
        }
    }

    /**
     * Saves volume settings to GameSettings.
     */
    private void saveVolumeSettings() {
        try {
            GameSettings settings = GameSettings.getInstance();
            settings.setMasterVolume(masterVolume);
            settings.setMusicVolume(musicVolume);
            settings.setSfxVolume(sfxVolume);
        } catch (Exception e) {
            System.err.println("Failed to save volume settings: " + e.getMessage());
        }
    }

    // ==================== CLEANUP ====================

    /**
     * Disposes all loaded audio resources.
     * Call this when shutting down the game.
     */
    public void dispose() {
        // Stop and dispose all sounds
        for (Sound sound : loadedSounds.values()) {
            sound.dispose();
        }
        loadedSounds.clear();
        activeSoundIds.clear();

        // Stop and dispose all music
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
        }
        for (Music music : loadedMusic.values()) {
            music.dispose();
        }
        loadedMusic.clear();

        System.out.println("SoundSystem disposed");
    }
}
