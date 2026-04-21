package com.github.denver.ui.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.TimeUtils;
import com.github.denver.Main;
import com.github.denver.asset.SoundAsset;
import com.github.denver.audio.AudioService;
import com.github.denver.screen.GameScreen;

public class MenuViewModel extends ViewModel {

    private final AudioService audioService;
    private long lastSndPlayTime;

    public MenuViewModel(Main game) {
        super(game);
        this.audioService = game.getAudioService();
        this.lastSndPlayTime = 0L;
    }

    public float getMusicVolume() {
        return audioService.getMusicVolume();
    }

    public void setMusicVolume(float volume) {
        this.audioService.setMusicVolume(volume);
    }

    public float getSoundVolume() {
        return audioService.getSoundVolume();
    }

    public void setSoundVolume(float soundVolume) {
        this.audioService.setSoundVolume(soundVolume);
        if (TimeUtils.timeSinceMillis(lastSndPlayTime) > 500L) {
            this.lastSndPlayTime = TimeUtils.millis();
            this.audioService.playSound(SoundAsset.SWORD_HIT);
        }
    }

    public void startGame() {
        game.setScreen(GameScreen.class);
    }

    public void quitGame() {
        Gdx.app.exit();
    }
}
