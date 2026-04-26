package com.github.denver.screen;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.denver.Main;
import com.github.denver.asset.MapAsset;
import com.github.denver.asset.SkinAsset;
import com.github.denver.audio.AudioService;
import com.github.denver.input.GameControllerState;
import com.github.denver.input.KeyboardController;
import com.github.denver.system.*;
import com.github.denver.tiled.TiledAshleyConfigurator;
import com.github.denver.tiled.TiledService;
import com.github.denver.ui.model.GameViewModel;
import com.github.denver.ui.view.GameView;

import java.util.function.Consumer;

public class GameScreen extends ScreenAdapter {
    private final Main game;
    private final Stage stage;
    private final Skin skin;
    private final GameViewModel viewModel;
    private final Viewport uiViewport;
    private final TiledService tiledService;
    private final Engine engine;
    private final TiledAshleyConfigurator tiledAshleyConfigurator;
    private final World physicWorld;
    private final KeyboardController keyboardController;
    private final AudioService audioService;
    private final GhostAiSystem ghostAiSystem;
    private final GameSessionSystem gameSessionSystem;

    public GameScreen(Main game) {
        this.game = game;
        this.uiViewport = new FitViewport(320f, 180f);
        this.stage = new Stage(uiViewport, game.getBatch());
        this.skin = game.getAssetService().get(SkinAsset.DEFAULT);
        this.viewModel = new GameViewModel(game);
        this.audioService = game.getAudioService();
        this.physicWorld = new World(Vector2.Zero, true);
        this.physicWorld.setAutoClearForces(false);
        this.tiledService = new TiledService(game.getAssetService(), this.physicWorld);
        this.engine = new Engine();
        this.tiledAshleyConfigurator = new TiledAshleyConfigurator(this.engine, this.physicWorld, this.game.getAssetService());
        this.keyboardController = new KeyboardController(GameControllerState.class, engine, null);
        this.ghostAiSystem = new GhostAiSystem();
        this.gameSessionSystem = new GameSessionSystem(viewModel, tiledAshleyConfigurator, tiledService::getCurrentMap);

        // add ECS systems
        this.engine.addSystem(this.ghostAiSystem);
        this.engine.addSystem(new PhysicMoveSystem());
        this.engine.addSystem(new PhysicSystem(physicWorld, 1 / 60f));
        this.engine.addSystem(new FacingSystem());
        this.engine.addSystem(new AttackSystem(physicWorld, audioService));
        this.engine.addSystem(new FsmSystem());
        // DamagedSystem must run after FsmSystem to correctly
        // detect when a damaged animation should be played.
        // This is done by checking if an entity has a Damaged component,
        // and this component is removed in the DamagedSystem.
        this.engine.addSystem(new DamagedSystem(viewModel));
        this.engine.addSystem(new TriggerSystem(audioService));
        this.engine.addSystem(new LifeSystem(this.viewModel));
        this.engine.addSystem(new AnimationSystem(game.getAssetService()));
        this.engine.addSystem(new CameraSystem(game.getCamera()));
        this.engine.addSystem(new WeaponSystem(game.getViewport()));
        this.engine.addSystem(new RenderSystem(game.getBatch(), game.getViewport(), game.getCamera()));
        this.engine.addSystem(new PhysicDebugRenderSystem(this.physicWorld, game.getCamera()));
        this.engine.addSystem(new ControllerSystem(game));
        this.engine.addSystem(this.gameSessionSystem);

        game.getCamera().zoom = 1.5f;

    }

    @Override
    public void show() {
        this.game.setInputProcessors(stage, keyboardController);
        keyboardController.setActiveState(GameControllerState.class);

        this.stage.addActor(new GameView(stage, skin, this.viewModel));

        Consumer<TiledMap> renderConsumer = this.engine.getSystem(RenderSystem.class)::setMap;
        Consumer<TiledMap> cameraConsumer = this.engine.getSystem(CameraSystem.class)::setMap;
        Consumer<TiledMap> audioConsumer = this.audioService::setMap;
        Consumer<TiledMap> ghostBoundsConsumer = this.ghostAiSystem::setMapBoundsFrom;
        this.tiledService.setMapChangeConsumer(
            renderConsumer.andThen(cameraConsumer).andThen(audioConsumer).andThen(ghostBoundsConsumer));
        this.tiledService.setLoadTriggerConsumer(tiledAshleyConfigurator::onLoadTrigger);
        this.tiledService.setLoadObjectConsumer(tiledAshleyConfigurator::onLoadObject);
        this.tiledService.setLoadTileConsumer(tiledAshleyConfigurator::onLoadTile);

        this.gameSessionSystem.resetSession();
        TiledMap startMap = this.tiledService.loadMap(MapAsset.MAIN);
        this.tiledService.setMap(startMap);
        this.viewModel.attachGhostKillListeners(this.engine);
    }

    @Override
    public void hide() {
        this.engine.removeAllEntities();
        this.stage.clear();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        uiViewport.update(width, height, true);
    }

    @Override
    public void render(float delta) {
        delta = Math.min(1 / 30f, delta);
        if (!viewModel.isSessionComplete()) {
            engine.update(delta);
        }

        uiViewport.apply();
        stage.getBatch().setColor(Color.WHITE);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        super.dispose();
        for (EntitySystem system : this.engine.getSystems()) {
            if (system instanceof Disposable disposable) {
                disposable.dispose();
            }
        }
        this.physicWorld.dispose();
        this.stage.dispose();
    }
}

