package com.github.denver.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.MathUtils;
import com.github.denver.component.Ghost;
import com.github.denver.tiled.TiledAshleyConfigurator;
import com.github.denver.ui.model.GameViewModel;

import java.util.function.Supplier;

/**
 * Run timer, ghost spawns every 10s, and session end at 70s.
 */
public class GameSessionSystem extends EntitySystem {

    public static final float SESSION_DURATION_SEC = 70f;
    public static final float SPAWN_INTERVAL_SEC = 10f;

    private final GameViewModel viewModel;
    private final TiledAshleyConfigurator tiledAshleyConfigurator;
    private final Supplier<TiledMap> currentMap;

    private float sessionTime;
    private float nextSpawnAt = SPAWN_INTERVAL_SEC;
    private boolean completed;

    public GameSessionSystem(
        GameViewModel viewModel,
        TiledAshleyConfigurator tiledAshleyConfigurator,
        Supplier<TiledMap> currentMap
    ) {
        this.viewModel = viewModel;
        this.tiledAshleyConfigurator = tiledAshleyConfigurator;
        this.currentMap = currentMap;
    }

    public void resetSession() {
        sessionTime = 0f;
        nextSpawnAt = SPAWN_INTERVAL_SEC;
        completed = false;
        viewModel.resetRunStats();
    }

    @Override
    public void update(float deltaTime) {
        if (completed) {
            viewModel.setSessionTime(SESSION_DURATION_SEC);
            return;
        }

        sessionTime += deltaTime;
        viewModel.setSessionTime(sessionTime);

        TiledMap map = currentMap.get();
        if (map != null) {
            int tw = map.getProperties().get("tilewidth", 0, Integer.class);
            int th = map.getProperties().get("tileheight", 0, Integer.class);
            int mw = map.getProperties().get("width", 0, Integer.class);
            int mh = map.getProperties().get("height", 0, Integer.class);
            float mapPxW = mw * tw;
            float mapPxH = mh * th;
            float margin = 24f;

            while (nextSpawnAt < SESSION_DURATION_SEC && sessionTime + 1e-4f >= nextSpawnAt) {
                for (int i = 0; i < 2; i++) {
                    int ghostType = MathUtils.random(2);
                    float x = MathUtils.random(margin, Math.max(margin + 1f, mapPxW - margin));
                    float y = MathUtils.random(margin, Math.max(margin + 1f, mapPxH - margin));
                    Entity spawned = tiledAshleyConfigurator.spawnGhost(map, ghostType, x, y);
                    Ghost ghost = Ghost.MAPPER.get(spawned);
                    if (ghost != null) {
                        ghost.setKillListener(viewModel::incrementGhostKills);
                    }
                }
                nextSpawnAt += SPAWN_INTERVAL_SEC;
            }
        }

        if (sessionTime >= SESSION_DURATION_SEC) {
            sessionTime = SESSION_DURATION_SEC;
            viewModel.setSessionTime(sessionTime);
            completed = true;
            viewModel.setSessionComplete(true);
        }
    }
}
