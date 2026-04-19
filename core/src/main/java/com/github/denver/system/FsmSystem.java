package com.github.denver.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.github.denver.components.Fsm;


public class FsmSystem extends IteratingSystem {
    public FsmSystem() {
        super(Family.all(Fsm.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float v) {
        Fsm.MAPPER.get(entity).getAnimationFsm().update();
    }
}
