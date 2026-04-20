package com.github.denver.sceen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.github.denver.Main;
import com.github.denver.asset.AssetService;
import com.github.denver.asset.AtlasAsset;
import com.github.denver.asset.SoundAsset;

import javax.sound.sampled.DataLine;

public class LoadingScreen extends ScreenAdapter {

    private final Main game;
    private final AssetService assetService;

    public LoadingScreen(Main game, AssetService assetService) {
        this.game = game;
        this.assetService = assetService;
    }

    @Override
    public void show() {
        for (AtlasAsset atlas : AtlasAsset.values()) {
            assetService.queue(atlas);
        }
        for (SoundAsset sound : SoundAsset.values()) {
            assetService.queue(sound);
        }
    }

    @Override
    public void render(float delta) {
        if (this.assetService.update()){
            Gdx.app.debug("LoadingScren", "Finished asset loading");
            createScreen();
            this.game.removeScreen(this);
            this.dispose();
            this.game.setScreen(GameScreen.class);
        }

    }

    private void createScreen() {
        this.game.addScreen(new GameScreen(this.game));
    }
}
