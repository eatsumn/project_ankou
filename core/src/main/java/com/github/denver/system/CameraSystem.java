package com.github.denver.system;


import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.github.denver.Main;
import com.github.denver.components.CameraFollow;
import com.github.denver.components.Transform;


public class CameraSystem extends IteratingSystem {
    public static final float CAM_OFFSET_Y = 1f;

    private final Camera camera;
    private Vector2 targetPosition;
    private final float smoothingFactor;
    private float mapW;
    private float mapH;


    public CameraSystem(Camera camera) {
        super(Family.all(CameraFollow.class, Transform.class).get());
        this.camera = camera;
        this.targetPosition = new Vector2();
        this.smoothingFactor = 4f;

    }


    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Transform transform = Transform.MAPPER.get(entity);

        calcTargetPosition(transform.getPosition());


        float progress = smoothingFactor * deltaTime;
        float smoothedX = MathUtils.lerp(camera.position.x, this.targetPosition.x, progress);
        float smoothedY = MathUtils.lerp(camera.position.y, this.targetPosition.y, progress);
        camera.position.set(smoothedX, smoothedY, camera.position.z);
    }


    private void calcTargetPosition(Vector2 entityPosition) {
        float targetX = entityPosition.x;
        float camHalfW = camera.viewportWidth * 0.5f;
        if (mapW > camHalfW) {
            float min = Math.min(camHalfW, mapW - camHalfW);
            float max = Math.max(camHalfW, mapW - camHalfW);
            targetX = MathUtils.clamp(targetX, min, max);
        }

        float targetY = entityPosition.y;
        float camHalfH = camera.viewportHeight * 0.5f + CAM_OFFSET_Y;
        if (mapH > camHalfH) {
            float min = Math.min(camHalfH, mapH - camHalfH);
            float max = Math.max(camHalfH, mapH - camHalfH);
            targetY = MathUtils.clamp(targetY, min, max);
        }

        this.targetPosition.set(targetX, targetY);
    }

    public void setMap(TiledMap tiledMap) {
        int width = tiledMap.getProperties().get("width", 0, Integer.class);
        int tileW = tiledMap.getProperties().get("tilewidth", 0, Integer.class);
        int height = tiledMap.getProperties().get("height", 0, Integer.class);
        int tileH = tiledMap.getProperties().get("tileheight", 0, Integer.class);
        mapW = width * tileW * Main.UNIT_SCALE;
        mapH = height * tileH * Main.UNIT_SCALE;

        Entity camEntity = getEntities().first();
        if (camEntity == null) {
            return;
        }

        processEntity(camEntity, 0f);

    }
}

