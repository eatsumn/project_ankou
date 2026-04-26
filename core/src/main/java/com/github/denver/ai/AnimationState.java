package com.github.denver.ai;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import com.github.denver.component.*;
import com.github.denver.component.Animation2D.AnimationType;

public enum AnimationState implements State<Entity> {
    IDLE {
        @Override
        public void enter(Entity entity) {
            Animation2D.MAPPER.get(entity).setType(AnimationType.IDLE);
        }

        @Override
        public void update(Entity entity) {

            Attack attack = Attack.MAPPER.get(entity);
            if (attack != null && attack.isAttacking()) {
                Fsm.MAPPER.get(entity).getAnimationFsm().changeState(ATTACK);
                return;
            }

            Move move = Move.MAPPER.get(entity);
            if (move != null && !move.isRooted() && !move.getDirection().isZero()) {
                Fsm.MAPPER.get(entity).getAnimationFsm().changeState(WALK);
                return;
            }


            Damaged damaged = Damaged.MAPPER.get(entity);
            if (damaged != null) {
                Fsm.MAPPER.get(entity).getAnimationFsm().changeState(DAMAGED);
            }
        }

        @Override
        public void exit(Entity entity) {
        }

        @Override
        public boolean onMessage(Entity entity, Telegram telegram) {
            return false;
        }
    },

    ATTACK {


        @Override
        public void enter(Entity entity) {
            Animation2D.MAPPER.get(entity).setType(AnimationType.ATTACK);
        }

        @Override
        public void update(Entity entity) {



            Attack attack = Attack.MAPPER.get(entity);
            if (!attack.isAttacking()) {
                Fsm.MAPPER.get(entity).getAnimationFsm().changeState(IDLE);
            }



        }

        @Override
        public void exit(Entity entity) {
        }

        @Override
        public boolean onMessage(Entity entity, Telegram telegram) {
            return false;
        }
    },

    WALK {

        float time = 0f;


        @Override
        public void enter(Entity entity) {
            Animation2D.MAPPER.get(entity).setType(AnimationType.WALK);
            time = 0f;
        }

        @Override
        public void update(Entity entity) {
            Attack attack = Attack.MAPPER.get(entity);

            Move move = Move.MAPPER.get(entity);
            Transform transform = Transform.MAPPER.get(entity);

            time+=Gdx.graphics.getDeltaTime();

            float oscillation = (float)((Math.sin(time * 8f) + 1f) / 2f);
            float shaped = com.badlogic.gdx.math.Interpolation.sine.apply(-1f, 1f, oscillation);
            float osc_2 = com.badlogic.gdx.math.Interpolation.sine.apply(1f, 1.1f, oscillation);


            float rotation = 10f * shaped;
            float height = 0.1f * osc_2;

            transform.setRotationDeg(rotation);
            transform.setScaling(transform.getScaling().x,height);



            if (attack.isAttacking()) {
                Fsm.MAPPER.get(entity).getAnimationFsm().changeState(ATTACK);
            }

            if ((move.getDirection().isZero() || move.isRooted())) {
                Fsm.MAPPER.get(entity).getAnimationFsm().changeState(IDLE);
            }
        }

        @Override
        public void exit(Entity entity) {
            Transform.MAPPER.get(entity).setRotationDeg(0);
        }

        @Override
        public boolean onMessage(Entity entity, Telegram telegram) {
            return false;
        }
    },


    DAMAGED {
        @Override
        public void enter(Entity entity) {
            Animation2D.MAPPER.get(entity).setType(AnimationType.DAMAGED);
        }

        @Override
        public void update(Entity entity) {
            Animation2D animation2D = Animation2D.MAPPER.get(entity);
            if (animation2D.isFinished()) {
                Fsm.MAPPER.get(entity).getAnimationFsm().changeState(IDLE);
            }
        }

        @Override
        public void exit(Entity entity) {
        }

        @Override
        public boolean onMessage(Entity entity, Telegram telegram) {
            return false;
        }
    }
}
