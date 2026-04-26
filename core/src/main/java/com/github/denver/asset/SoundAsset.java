package com.github.denver.asset;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.audio.Sound;

public enum SoundAsset implements Asset<Sound> {
    SWORD_HIT("sword_hit.wav"),
    LIFE_REG("life_reg.wav"),
    TRAP("trap.wav"),
    SWING("swing.wav"),
    GHOST_HIT("ghost_hit.wav"),
    GHOST_DEAD("ghost_dead.wav"),
    ;

    private final AssetDescriptor<Sound> descriptor;

    SoundAsset(String musicFile) {
        this.descriptor = new AssetDescriptor<>("audio/" + musicFile, Sound.class);
    }

    @Override
    public AssetDescriptor<Sound> getDescriptor() {
        return descriptor;
    }
}
