package com.github.denver.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.github.denver.Main;
import com.github.denver.asset.AssetService;
import com.github.denver.asset.AtlasAsset;
import com.github.denver.asset.SkinAsset;
import com.github.denver.asset.SoundAsset;

public class LoadingScreen extends ScreenAdapter {

    private final Main game;
    private final AssetService assetService;

    public LoadingScreen(Main game) {
        this.game = game;
        this.assetService = game.getAssetService();
    }

    /**
     * Queues all required assets for loading.
     */
    @Override
    public void show() {
        for (AtlasAsset atlasAsset : AtlasAsset.values()) {
            assetService.queue(atlasAsset);
        }
        assetService.queue(SkinAsset.DEFAULT);
        for (SoundAsset soundAsset : SoundAsset.values()) {
            assetService.queue(soundAsset);
        }
    }

    /**
     * Updates asset loading progress and transitions to menu when complete.
     */
    @Override
    public void render(float delta) {
        if (assetService.update()) {
            Gdx.app.debug("LoadingScreen", "Finished loading assets");
            createScreens();
            this.game.removeScreen(this);
            this.dispose();
            this.game.setScreen(MenuScreen.class);
        }
    }

    private void createScreens() {
        this.game.addScreen(new GameScreen(this.game));
        this.game.addScreen(new MenuScreen(this.game));
    }
}
