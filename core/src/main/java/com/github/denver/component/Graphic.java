package com.github.denver.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Graphic implements Component {
    public static final ComponentMapper<Graphic> MAPPER = ComponentMapper.getFor(Graphic.class);

    private TextureRegion region;
    private final Color color;
    private boolean flipX;

    /**
     * Rotation / draw origin as a fraction of the sprite's logical width and height from its bottom-left
     * (LibGDX {@code Batch.draw} origin). (0.5, 0.5) = center; (0.5, 0.15) ≈ grip on a tall sword.
     */
    private float originFracX = 0.5f;
    private float originFracY = 0.5f;

    public Graphic(TextureRegion region, Color color) {
        this.region = region;
        this.color = color;
        this.flipX = false;
    }

    public void setOriginFrac(float originFracX, float originFracY) {
        this.originFracX = originFracX;
        this.originFracY = originFracY;
    }

    public float getOriginFracX() {
        return originFracX;
    }

    public float getOriginFracY() {
        return originFracY;
    }

    public void setRegion(TextureRegion region) {
        this.region = region;
    }

    public TextureRegion getRegion() {
        return region;
    }

    public Color getColor() {
        return color;
    }

    public void setFlipX(boolean isFlipped){
        this.flipX = isFlipped;
    }


    public boolean isFlipX() {
        return this.flipX;
    }
}
