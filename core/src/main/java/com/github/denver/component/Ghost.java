package com.github.denver.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.github.denver.asset.SoundAsset;
import com.github.denver.audio.AudioService;

/**
 * Enemy ghost driven by {@link com.github.denver.system.GhostAiSystem}. {@code ghostType} on the tile: 0 = bounce off
 * walls (retargets when the player is within {@link #getRangeCheck()} world units), 1 = phase through obstacles
 * (map edge only), 2 = wander toward random points per quadrant.
 */
public class Ghost implements Component {
    public static final ComponentMapper<Ghost> MAPPER = ComponentMapper.getFor(Ghost.class);

    public static final int HITS_TO_DESTROY = 3;

    public enum Kind {
        BOUNCE_WALLS,
        PHASE_THROUGH,
        QUADRANT_WANDER
    }

    private final Kind kind;

    private boolean wallHitPending;
    private float bounceCooldown;

    /** World-space radius: when the player is this close, type-0 ghosts pick a new random direction. */
    private float rangeCheck = 4f;
    /** Type 0: time until another random direction while the player remains in range. */
    private float proximityRetargetCooldown;

    /** Type 1: velocity direction in world space (any angle). */
    private final Vector2 phaseHeading = new Vector2();

    private int wanderQuadrant;
    private final Vector2 wanderTarget = new Vector2();
    private boolean wanderTargetSet;
    private float wanderStuckTimer;
    private final Vector2 wanderProgressAnchor = new Vector2();
    private float wanderProgressTimer;

    private int hitCount;
    private float speedAdd;
    private float baseMoveSpeed = -1f;
    private SoundAsset hitSound = SoundAsset.GHOST_HIT;
    private SoundAsset deathSound = SoundAsset.GHOST_DEAD;

    private Runnable killListener;

    public Ghost(Kind kind) {
        this.kind = kind;
    }

    /**
     * Call after {@link Move} exists: stores base speed and per-hit speed bonus from Tiled.
     */
    public void configureForCombat(float baseMoveSpeed, float speedAdd, SoundAsset hitSound, SoundAsset deathSound) {
        this.baseMoveSpeed = baseMoveSpeed;
        this.speedAdd = speedAdd;
        this.hitSound = hitSound;
        this.deathSound = deathSound;
    }

    public void setKillListener(Runnable killListener) {
        this.killListener = killListener;
    }

    /**
     * Player attack landed (via {@link com.github.denver.system.AttackSystem}). Third hit removes the entity.
     */
    public void applyHit(Entity self, AudioService audio, Engine engine) {
        hitCount++;
        Move move = Move.MAPPER.get(self);
        if (hitCount >= HITS_TO_DESTROY) {
            audio.playSound(deathSound);
            if (killListener != null) {
                killListener.run();
            }
            engine.removeEntity(self);
        } else {
            audio.playSound(hitSound);
            if (move != null && baseMoveSpeed >= 0f) {
                move.setMaxSpeed(baseMoveSpeed + speedAdd * hitCount);
            }
        }
    }

    public static Kind kindFromGhostType(int ghostType) {
        return switch (MathUtils.clamp(ghostType, 0, 2)) {
            case 0 -> Kind.BOUNCE_WALLS;
            case 1 -> Kind.PHASE_THROUGH;
            default -> Kind.QUADRANT_WANDER;
        };
    }

    public Kind getKind() {
        return kind;
    }

    public void notifyWallHit() {
        if (kind == Kind.BOUNCE_WALLS) {
            wallHitPending = true;
        }
    }

    public boolean isWallHitPending() {
        return wallHitPending;
    }

    public void clearWallHit() {
        wallHitPending = false;
    }

    public float getBounceCooldown() {
        return bounceCooldown;
    }

    public void setBounceCooldown(float bounceCooldown) {
        this.bounceCooldown = bounceCooldown;
    }

    public void decBounceCooldown(float dt) {
        bounceCooldown = Math.max(0f, bounceCooldown - dt);
    }

    public float getRangeCheck() {
        return rangeCheck;
    }

    public void setRangeCheck(float rangeCheck) {
        this.rangeCheck = rangeCheck;
    }

    public float getProximityRetargetCooldown() {
        return proximityRetargetCooldown;
    }

    public void setProximityRetargetCooldown(float proximityRetargetCooldown) {
        this.proximityRetargetCooldown = proximityRetargetCooldown;
    }

    public void decProximityRetargetCooldown(float dt) {
        proximityRetargetCooldown = Math.max(0f, proximityRetargetCooldown - dt);
    }

    public Vector2 getPhaseHeading() {
        return phaseHeading;
    }

    public int getWanderQuadrant() {
        return wanderQuadrant;
    }

    public void setWanderQuadrant(int wanderQuadrant) {
        this.wanderQuadrant = wanderQuadrant;
    }

    public Vector2 getWanderTarget() {
        return wanderTarget;
    }

    public boolean isWanderTargetSet() {
        return wanderTargetSet;
    }

    public void setWanderTargetSet(boolean wanderTargetSet) {
        this.wanderTargetSet = wanderTargetSet;
    }

    public float getWanderStuckTimer() {
        return wanderStuckTimer;
    }

    public void setWanderStuckTimer(float wanderStuckTimer) {
        this.wanderStuckTimer = wanderStuckTimer;
    }

    public Vector2 getWanderProgressAnchor() {
        return wanderProgressAnchor;
    }

    public float getWanderProgressTimer() {
        return wanderProgressTimer;
    }

    public void setWanderProgressTimer(float wanderProgressTimer) {
        this.wanderProgressTimer = wanderProgressTimer;
    }
}
