package com.github.denver.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.github.denver.Main;
import com.github.denver.component.Ghost;
import com.github.denver.component.Move;
import com.github.denver.component.Player;
import com.github.denver.component.Transform;

/**
 * AI for {@link Ghost}: type 0 retargets randomly when the player is in range (and on wall hits), type 1 moves at
 * arbitrary angles through obstacles until map edges, type 2 wanders by quadrant.
 */
public class GhostAiSystem extends IteratingSystem {

    private static final Family FAMILY = Family.all(Ghost.class, Move.class, Transform.class).get();
    private static final Family PLAYER = Family.all(Player.class, Transform.class).get();

    private static final float BOUNCE_COOLDOWN_SEC = 0.18f;
    private static final float PROXIMITY_RETARGET_MIN = 0.2f;
    private static final float PROXIMITY_RETARGET_MAX = 0.55f;
    private static final float WANDER_STUCK_SEC = 1.5f;
    private static final float WANDER_PROGRESS_SAMPLE_SEC = 0.35f;
    private static final float WANDER_REACHED_EPS = 0.18f;
    private static final float WANDER_MOVE_EPS = 0.04f;
    private static final float EDGE_EPS = 0.02f;

    private static final Vector2 TMP = new Vector2();
    private static final Vector2 TMP_CENTER_A = new Vector2();
    private static final Vector2 TMP_CENTER_B = new Vector2();

    private float mapWidth;
    private float mapHeight;

    public GhostAiSystem() {
        super(FAMILY);
    }

    public void setMapBoundsFrom(TiledMap tiledMap) {
        int width = tiledMap.getProperties().get("width", 0, Integer.class);
        int tileW = tiledMap.getProperties().get("tilewidth", 0, Integer.class);
        int height = tiledMap.getProperties().get("height", 0, Integer.class);
        int tileH = tiledMap.getProperties().get("tileheight", 0, Integer.class);
        this.mapWidth = width * tileW * Main.UNIT_SCALE;
        this.mapHeight = height * tileH * Main.UNIT_SCALE;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Ghost ghost = Ghost.MAPPER.get(entity);
        Move move = Move.MAPPER.get(entity);
        Transform transform = Transform.MAPPER.get(entity);

        switch (ghost.getKind()) {
            case BOUNCE_WALLS -> processBounce(ghost, move, transform, deltaTime);
            case PHASE_THROUGH -> processPhase(ghost, move, transform, deltaTime);
            case QUADRANT_WANDER -> processWander(ghost, move, transform, deltaTime);
        }
    }

    private void processBounce(Ghost ghost, Move move, Transform transform, float deltaTime) {
        ghost.decBounceCooldown(deltaTime);
        ghost.decProximityRetargetCooldown(deltaTime);

        Entity player = getPlayer();
        if (player != null) {
            Transform playerTf = Transform.MAPPER.get(player);
            entityCenterTo(TMP_CENTER_A, transform);
            entityCenterTo(TMP_CENTER_B, playerTf);
            float range = ghost.getRangeCheck();
            if (TMP_CENTER_A.dst(TMP_CENTER_B) <= range && ghost.getProximityRetargetCooldown() <= 0f) {
                pickRandomUnitDirection(move.getDirection());
                ghost.setProximityRetargetCooldown(
                    MathUtils.random(PROXIMITY_RETARGET_MIN, PROXIMITY_RETARGET_MAX));
            }
        }

        if (move.getDirection().isZero()) {
            pickRandomUnitDirection(move.getDirection());
        }

        if (ghost.isWallHitPending() && ghost.getBounceCooldown() <= 0f) {
            ghost.clearWallHit();
            pickRandomUnitDirection(move.getDirection());
            ghost.setBounceCooldown(BOUNCE_COOLDOWN_SEC);
        }
    }

    private Entity getPlayer() {
        ImmutableArray<Entity> players = getEngine().getEntitiesFor(PLAYER);
        return players.size() > 0 ? players.first() : null;
    }

    private static void entityCenterTo(Vector2 out, Transform t) {
        Vector2 p = t.getPosition();
        Vector2 s = t.getSize();
        Vector2 sc = t.getScaling();
        out.set(p.x + s.x * sc.x * 0.5f, p.y + s.y * sc.y * 0.5f);
    }

    private void processPhase(Ghost ghost, Move move, Transform transform, float deltaTime) {
        if (mapWidth <= 0f || mapHeight <= 0f) {
            return;
        }
        Vector2 h = ghost.getPhaseHeading();
        if (h.isZero()) {
            pickRandomUnitDirection(h);
        }
        float speed = move.getMaxSpeed();
        Vector2 pos = transform.getPosition();
        Vector2 size = transform.getSize();
        Vector2 sc = transform.getScaling();
        float halfW = size.x * sc.x * 0.5f;
        float halfH = size.y * sc.y * 0.5f;
        float cx = pos.x + halfW;
        float cy = pos.y + halfH;

        cx += h.x * speed * deltaTime;
        cy += h.y * speed * deltaTime;

        float minX = halfW + EDGE_EPS;
        float maxX = mapWidth - halfW - EDGE_EPS;
        float minY = halfH + EDGE_EPS;
        float maxY = mapHeight - halfH - EDGE_EPS;

        boolean hitEdge = false;
        if (cx < minX) {
            cx = minX;
            hitEdge = true;
        } else if (cx > maxX) {
            cx = maxX;
            hitEdge = true;
        }
        if (cy < minY) {
            cy = minY;
            hitEdge = true;
        } else if (cy > maxY) {
            cy = maxY;
            hitEdge = true;
        }
        if (hitEdge) {
            pickRandomUnitDirection(h);
        }

        pos.set(cx - halfW, cy - halfH);
        move.getDirection().set(h);
    }

    private void processWander(Ghost ghost, Move move, Transform transform, float deltaTime) {
        if (mapWidth <= 0f || mapHeight <= 0f) {
            return;
        }
        Vector2 pos = transform.getPosition();
        if (!ghost.isWanderTargetSet()) {
            ghost.setWanderQuadrant(quadrantAt(pos.x, pos.y));
            pickWanderTarget(ghost, ghost.getWanderQuadrant());
            ghost.setWanderTargetSet(true);
            ghost.getWanderProgressAnchor().set(pos);
            ghost.setWanderProgressTimer(0f);
            ghost.setWanderStuckTimer(0f);
        }

        Vector2 target = ghost.getWanderTarget();
        if (pos.dst(target) < WANDER_REACHED_EPS) {
            pickWanderTarget(ghost, ghost.getWanderQuadrant());
            ghost.getWanderProgressAnchor().set(pos);
            ghost.setWanderProgressTimer(0f);
            ghost.setWanderStuckTimer(0f);
        }

        ghost.setWanderProgressTimer(ghost.getWanderProgressTimer() + deltaTime);
        if (ghost.getWanderProgressTimer() >= WANDER_PROGRESS_SAMPLE_SEC) {
            if (pos.dst(ghost.getWanderProgressAnchor()) < WANDER_MOVE_EPS) {
                ghost.setWanderStuckTimer(ghost.getWanderStuckTimer() + ghost.getWanderProgressTimer());
            } else {
                ghost.setWanderStuckTimer(0f);
            }
            ghost.getWanderProgressAnchor().set(pos);
            ghost.setWanderProgressTimer(0f);
        }

        if (ghost.getWanderStuckTimer() >= WANDER_STUCK_SEC) {
            int next = (ghost.getWanderQuadrant() + 1) % 4;
            ghost.setWanderQuadrant(next);
            pickWanderTarget(ghost, next);
            ghost.setWanderStuckTimer(0f);
            ghost.getWanderProgressAnchor().set(pos);
            ghost.setWanderProgressTimer(0f);
        }

        TMP.set(target).sub(pos);
        if (TMP.len2() < 1e-6f) {
            move.getDirection().setZero();
        } else {
            move.getDirection().set(TMP).nor();
        }
    }

    private int quadrantAt(float x, float y) {
        float midX = mapWidth * 0.5f;
        float midY = mapHeight * 0.5f;
        int qx = x < midX ? 0 : 1;
        int qy = y < midY ? 0 : 1;
        return qx + 2 * qy;
    }

    private void pickWanderTarget(Ghost ghost, int quadrant) {
        float midX = mapWidth * 0.5f;
        float midY = mapHeight * 0.5f;
        float m = 0.15f;
        float x0 = (quadrant % 2 == 0) ? m : midX + m;
        float x1 = (quadrant % 2 == 0) ? midX - m : mapWidth - m;
        float y0 = (quadrant < 2) ? m : midY + m;
        float y1 = (quadrant < 2) ? midY - m : mapHeight - m;
        ghost.getWanderTarget().set(
            MathUtils.random(x0, x1),
            MathUtils.random(y0, y1)
        );
    }

    private static void pickRandomUnitDirection(Vector2 out) {
        float a = MathUtils.random(MathUtils.PI2);
        out.set(MathUtils.cos(a), MathUtils.sin(a));
    }

    public float getMapWidth() {
        return mapWidth;
    }

    public float getMapHeight() {
        return mapHeight;
    }
}
