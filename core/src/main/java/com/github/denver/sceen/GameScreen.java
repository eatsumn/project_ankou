package com.github.denver.sceen;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import com.github.denver.Main;
import com.github.denver.asset.MapAsset;
import com.github.denver.audio.AudioService;
import com.github.denver.input.GameControllerState;
import com.github.denver.input.KeyboardController;
import com.github.denver.system.*;
import com.github.denver.tiled.TiledAshleyConfigurator;
import com.github.denver.tiled.TiledService;

import javax.sound.sampled.AudioSystem;
import java.util.function.Consumer;


public class GameScreen extends ScreenAdapter {
   private final Engine engine;
   private final TiledService tiledService;
   private final TiledAshleyConfigurator tiledAshleyConfigurator;
   private final KeyboardController keyboardController;
   private final Main game;
   private final World physicWorld;
   private final AudioService audioService;


   public GameScreen(Main game) {
       this.game = game;
       this.physicWorld = new World(Vector2.Zero, true);
       this.physicWorld.setAutoClearForces(false);
       this.tiledService = new TiledService(game.getAssetService(), this.physicWorld);
       this.engine = new Engine();
       this.tiledAshleyConfigurator = new TiledAshleyConfigurator(this.engine, game.getAssetService(), physicWorld);
       this.keyboardController = new KeyboardController(GameControllerState.class, engine);
       this.audioService = game.getAudioService();


       this.engine.addSystem(new ControllerSystem(game.getAudioService()));
       this.engine.addSystem(new PhysicsMoveSystem());
       this.engine.addSystem(new FsmSystem());
       this.engine.addSystem(new FacingSystem());
       this.engine.addSystem(new PhysicSystem(physicWorld, 1/60f));
       this.engine.addSystem(new AnimationSystem(game.getAssetService()));
       this.engine.addSystem(new CameraSystem(game.getCamera()));
       this.engine.addSystem(new RenderSystem(game.getBatch(), game.getViewport(), game.getCamera()));
       this.engine.addSystem(new PhysicDebugRenderSystem(physicWorld, game.getCamera()));

   }

   @Override
   public void show() {
        game.setInputProcessors(keyboardController);
        keyboardController.setActiveState(GameControllerState.class);


        Consumer<TiledMap> renderConsumer = this.engine.getSystem(RenderSystem.class)::setMap;
        Consumer<TiledMap> cameraConsumer = this.engine.getSystem(CameraSystem.class)::setMap;
        Consumer<TiledMap> audioConsumer = audioService::setMap;
        this.tiledService.setMapChangeConsumer(renderConsumer.andThen(cameraConsumer).andThen(audioConsumer));
        this.tiledService.setLoadObjectConsumer(this.tiledAshleyConfigurator::onLoadObject);
        this.tiledService.setLoadTileConsumer(tiledAshleyConfigurator::onLoadTile);



        TiledMap tiledMap = this.tiledService.loadMap(MapAsset.MAIN);
        this.tiledService.setMap(tiledMap);


   }

    @Override
    public void hide() {
        this.engine.removeAllEntities();
    }

    @Override
    public void render(float delta) {
       delta = Math.min(delta, 1 / 30f);
       this.engine.update(delta);

   }

    @Override
    public void dispose() {
        for (EntitySystem system : this.engine.getSystems()) {
            if (system instanceof Disposable disposable) {
                disposable.dispose();
            }
        }

        this.physicWorld.dispose();

    }
}
