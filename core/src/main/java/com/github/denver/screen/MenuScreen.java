package com.github.denver.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.denver.Main;
import com.github.denver.asset.MusicAsset;
import com.github.denver.asset.SkinAsset;
import com.github.denver.input.KeyboardController;
import com.github.denver.input.UiControllerState;
import com.github.denver.ui.model.MenuViewModel;
import com.github.denver.ui.view.MenuView;

public class MenuScreen extends ScreenAdapter {

    private final Main game;
    private final Stage stage;
    private final Skin skin;
    private final Viewport uiViewport;
    private final KeyboardController keyboardController;

    public MenuScreen(Main game) {
        this.game = game;
        this.uiViewport = new FitViewport(800f, 450f);
        this.stage = new Stage(uiViewport, game.getBatch());
        this.skin = game.getAssetService().get(SkinAsset.DEFAULT);
        this.keyboardController = new KeyboardController(UiControllerState.class, null, stage);
    }

    @Override
    public void resize(int width, int height) {
        uiViewport.update(width, height, true);
    }

    @Override
    public void show() {
        this.game.setInputProcessors(stage, keyboardController);

        this.stage.addActor(new MenuView(stage, skin, new MenuViewModel(game)));
        this.game.getAudioService().playMusic(MusicAsset.MENU);
    }

    @Override
    public void hide() {
        this.stage.clear();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.2f, 0.3f, 1); // RGBA
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        uiViewport.apply();
        stage.getBatch().setColor(Color.WHITE);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
