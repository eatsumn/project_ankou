package com.github.denver.ui.model;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector2;
import com.github.denver.Main;
import com.github.denver.asset.SoundAsset;
import com.github.denver.audio.AudioService;
import com.github.denver.component.Ghost;
import com.github.denver.screen.MenuScreen;

import java.util.Map;

public class GameViewModel extends ViewModel {
    public static final String LIFE_POINTS = "lifePoints";
    public static final String MAX_LIFE = "maxLife";
    public static final String PLAYER_DAMAGE = "playerDamage";
    public static final String GHOST_KILLS = "ghostKills";
    public static final String SESSION_COMPLETE = "sessionComplete";

    private static final Family GHOST_FAMILY = Family.all(Ghost.class).get();

    private final AudioService audioService;
    private int lifePoints;
    private int maxLife;
    private Map.Entry<Vector2, Integer> playerDamage;
    private final Vector2 tmpVec2;

    private int ghostKills;
    private float sessionTime;
    private boolean sessionComplete;

    public GameViewModel(Main game) {
        super(game);
        this.audioService = game.getAudioService();
        this.lifePoints = 0;
        this.maxLife = 0;
        this.playerDamage = null;
        this.tmpVec2 = new Vector2();
        this.ghostKills = 0;
        this.sessionTime = 0f;
        this.sessionComplete = false;
    }

    public void setMaxLife(int maxLife) {
        if (this.maxLife != maxLife) {
            this.propertyChangeSupport.firePropertyChange(MAX_LIFE, this.maxLife, maxLife);
        }
        this.maxLife = maxLife;
    }

    public int getMaxLife() {
        return maxLife;
    }

    public void setLifePoints(int lifePoints) {
        if (this.lifePoints != lifePoints) {
            this.propertyChangeSupport.firePropertyChange(LIFE_POINTS, this.lifePoints, lifePoints);
            if (this.lifePoints != 0 && this.lifePoints < lifePoints) {
                audioService.playSound(SoundAsset.LIFE_REG);
            }
        }
        this.lifePoints = lifePoints;
    }

    public int getLifePoints() {
        return lifePoints;
    }

    public void updateLifeInfo(float maxLife, float life) {
        setMaxLife((int) maxLife);
        setLifePoints((int) life);
    }

    public void playerDamage(int amount, float x, float y) {
        Vector2 position = new Vector2(x, y);
        this.playerDamage = Map.entry(position, amount);
        this.propertyChangeSupport.firePropertyChange(PLAYER_DAMAGE, null, this.playerDamage);
    }

    public Vector2 toScreenCoords(Vector2 position) {
        tmpVec2.set(position);
        game.getViewport().project(tmpVec2);
        return tmpVec2;

    }

    public void resetRunStats() {
        int oldKills = ghostKills;
        boolean wasComplete = sessionComplete;
        ghostKills = 0;
        sessionTime = 0f;
        sessionComplete = false;
        this.propertyChangeSupport.firePropertyChange(GHOST_KILLS, oldKills, ghostKills);
        this.propertyChangeSupport.firePropertyChange(SESSION_COMPLETE, wasComplete, false);
    }

    public void setSessionTime(float sessionTime) {
        this.sessionTime = sessionTime;
    }

    public float getSessionTime() {
        return sessionTime;
    }

    public boolean isSessionComplete() {
        return sessionComplete;
    }

    public void setSessionComplete(boolean sessionComplete) {
        if (this.sessionComplete != sessionComplete) {
            this.propertyChangeSupport.firePropertyChange(SESSION_COMPLETE, this.sessionComplete, sessionComplete);
        }
        this.sessionComplete = sessionComplete;
    }

    public int getGhostKills() {
        return ghostKills;
    }

    public void incrementGhostKills() {
        int prev = ghostKills;
        ghostKills++;
        this.propertyChangeSupport.firePropertyChange(GHOST_KILLS, prev, ghostKills);
    }

    public void attachGhostKillListeners(Engine engine) {
        Runnable listener = this::incrementGhostKills;
        ImmutableArray<Entity> ghosts = engine.getEntitiesFor(GHOST_FAMILY);
        for (Entity entity : ghosts) {
            Ghost ghost = Ghost.MAPPER.get(entity);
            if (ghost != null) {
                ghost.setKillListener(listener);
            }
        }
    }

    public void returnToMenu() {
        game.setScreen(MenuScreen.class);
    }
}
