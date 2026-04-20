package com.github.denver.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.github.denver.components.Move;
import com.github.denver.components.Physic;
import com.github.denver.components.Transform;

public class PhysicsMoveSystem extends IteratingSystem {

    private final Vector2 normalizedDirection = new Vector2();

    public PhysicsMoveSystem(){
        super(Family.all(Move.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {

        Move move = Move.MAPPER.get(entity);
        Body body = Physic.MAPPER.get(entity).getBody();

        if(move.isRooted() || move.getDirection().isZero()) {
            body.setLinearVelocity(0f,0f);
            return;
        };

        normalizedDirection.set(move.getDirection()).nor();
        Physic.MAPPER.get(entity).getBody().setLinearVelocity(
            move.getMaxSpeed() * normalizedDirection.x,
            move.getMaxSpeed() * normalizedDirection.y
        );


    }
}
