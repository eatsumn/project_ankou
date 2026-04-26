package com.github.denver.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.SortedIteratingSystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.denver.Main;
import com.github.denver.component.Facing;
import com.github.denver.component.Graphic;
import com.github.denver.component.Transform;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RenderSystem extends SortedIteratingSystem implements Disposable {
    private final Batch batch;
    private final OrthographicCamera camera;
    private final Viewport viewport;

    private final BatchTiledMapRenderer tiledRenderer;
    private final List<MapLayer> fgdLayers;
    private final List<MapLayer> bgdLayers;

    public RenderSystem(Batch batch, Viewport viewport, OrthographicCamera camera) {
        super(
            Family.all(Transform.class, Graphic.class).get(),
            Comparator.comparing(Transform.MAPPER::get)
        );

        this.batch = batch;
        this.viewport = viewport;
        this.camera = camera;
        this.tiledRenderer = new OrthogonalTiledMapRenderer(null, Main.UNIT_SCALE, batch);
        this.fgdLayers = new ArrayList<>();
        this.bgdLayers = new ArrayList<>();
    }

    /**
     * Renders the scene with background, entities, and foreground layers.
     */
    @Override
    public void update(float deltaTime) {
        AnimatedTiledMapTile.updateAnimationBaseTime();
        viewport.apply();

        batch.begin();
        batch.setColor(Color.WHITE);
        this.tiledRenderer.setView(camera);
        bgdLayers.forEach(tiledRenderer::renderMapLayer);

        forceSort();
        super.update(deltaTime);

        batch.setColor(Color.WHITE);
        fgdLayers.forEach(tiledRenderer::renderMapLayer);
        batch.end();
    }

    /**
     * Renders a single entity with its transform and graphic components.
     */
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Transform transform = Transform.MAPPER.get(entity);
        Graphic graphic = Graphic.MAPPER.get(entity);
        if (graphic.getRegion() == null) {
            return;
        }


        Facing facing = Facing.MAPPER.get(entity);
        Vector2 position = transform.getPosition();
        Vector2 scaling = transform.getScaling();
        Vector2 size = transform.getSize();
        float originX = size.x * graphic.getOriginFracX();
        float originY = size.y * graphic.getOriginFracY();
        float width = graphic.isFlipX() ? scaling.x : -scaling.x;
        batch.setColor(graphic.getColor());
        batch.draw(
            graphic.getRegion(),
            position.x - (1f - scaling.x) * originX,
            position.y - (1f - scaling.y) * originY,
            originX, originY,
            size.x, size.y,
            width, scaling.y,
            transform.getRotationDeg()
        );
    }

    /**
     * Sets up the map and organizes layers into background and foreground.
     */
    public void setMap(TiledMap tiledMap) {
        this.tiledRenderer.setMap(tiledMap);

        this.fgdLayers.clear();
        this.bgdLayers.clear();
        List<MapLayer> currentLayers = bgdLayers;
        for (MapLayer layer : tiledMap.getLayers()) {
            if ("objects".equals(layer.getName())) {
                currentLayers = fgdLayers;
                continue;
            }
            if (layer.getClass().equals(MapLayer.class)) {
                continue;
            }
            currentLayers.add(layer);
        }
    }

    @Override
    public void dispose() {
        this.tiledRenderer.dispose();
    }
}
