package com.github.denver.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape.Type;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectSet;
import com.github.denver.audio.AudioService;
import com.github.denver.component.Attack;
import com.github.denver.component.Damaged;
import com.github.denver.component.Facing;
import com.github.denver.component.Facing.FacingDirection;
import com.github.denver.component.Ghost;
import com.github.denver.component.Life;
import com.github.denver.component.Move;
import com.github.denver.component.Physic;
import com.github.denver.component.Transform;

public class AttackSystem extends IteratingSystem {
    public static final Rectangle attackAABB = new Rectangle();

    private static final Family GHOST_TRANSFORM = Family.all(Ghost.class, Transform.class).get();

    private final AudioService audioService;
    private final World world;
    private final Vector2 tmpVertex;
    private final ObjectSet<Entity> ghostHitThisSwing = new ObjectSet<>();
    private final Array<Entity> phaseGhostHitBuffer = new Array<>();
    private final Rectangle tmpGhostBounds = new Rectangle();
    private Body attackerBody;
    private float attackDamage;

    public AttackSystem(World world, AudioService audioService) {
        super(Family.all(Attack.class, Facing.class, Physic.class).get());
        this.audioService = audioService;
        this.world = world;
        this.tmpVertex = new Vector2();
        this.attackerBody = null;
        this.attackDamage = 0f;
    }

    /**
     * Processes attack logic including sound effects and damage detection.
     */
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Attack attack = Attack.MAPPER.get(entity);
        // can attack = true means that the attack was not started yet
        if (attack.canAttack()) return;

        if (attack.hasAttackStarted() && attack.getSfx() != null) {
            audioService.playSound(attack.getSfx());
            Move move = Move.MAPPER.get(entity);
            if (move != null) {
                move.setRooted(false);
            }
        }

        attack.decAttackTimer(deltaTime);
        if (attack.canAttack()) {
            FacingDirection facingDirection = Facing.MAPPER.get(entity).getDirection();
            attackerBody = Physic.MAPPER.get(entity).getBody();
            PolygonShape attackPolygonShape = getAttackFixture(attackerBody, facingDirection);
            updateAttackAABB(attackerBody.getPosition(), attackPolygonShape);

            this.attackDamage = attack.getDamage();
            ghostHitThisSwing.clear();
            world.QueryAABB(this::attackCallback, attackAABB.x, attackAABB.y, attackAABB.width, attackAABB.height);
            hitPhaseGhostsOverlappingAttack();

            Move move = Move.MAPPER.get(entity);
            if (move != null) {
                move.setRooted(false);
            }
        }
    }

    private boolean attackCallback(Fixture fixture) {
        Body body = fixture.getBody();
        if (body.equals(attackerBody)) return true;
        if (!(body.getUserData() instanceof Entity entity)) return true;

        Ghost ghost = Ghost.MAPPER.get(entity);
        if (ghost != null) {
            if (ghostHitThisSwing.contains(entity)) {
                return true;
            }
            ghostHitThisSwing.add(entity);
            ghost.applyHit(entity, audioService, getEngine());
            return true;
        }

        Life life = Life.MAPPER.get(entity);
        if (life == null) {
            return true;
        }

        Damaged damaged = Damaged.MAPPER.get(entity);
        if (damaged == null) {
            entity.add(new Damaged(this.attackDamage));
        } else {
            damaged.addDamage(this.attackDamage);
        }
        return true;
    }

    /**
     * Phase ghosts have no Box2D body; overlap their sprite bounds with the attack AABB instead.
     */
    private void hitPhaseGhostsOverlappingAttack() {
        phaseGhostHitBuffer.clear();
        for (Entity entity : getEngine().getEntitiesFor(GHOST_TRANSFORM)) {
            if (Physic.MAPPER.get(entity) != null) {
                continue;
            }
            if (!ghostSpriteOverlapsAttackAabb(entity)) {
                continue;
            }
            if (ghostHitThisSwing.contains(entity)) {
                continue;
            }
            phaseGhostHitBuffer.add(entity);
        }
        for (Entity entity : phaseGhostHitBuffer) {
            ghostHitThisSwing.add(entity);
            Ghost.MAPPER.get(entity).applyHit(entity, audioService, getEngine());
        }
    }

    private boolean ghostSpriteOverlapsAttackAabb(Entity entity) {
        Transform t = Transform.MAPPER.get(entity);
        Vector2 p = t.getPosition();
        Vector2 s = t.getSize();
        Vector2 sc = t.getScaling();
        tmpGhostBounds.set(p.x, p.y, s.x * sc.x, s.y * sc.y);
        return attackAABB.overlaps(tmpGhostBounds);
    }

    private void updateAttackAABB(Vector2 bodyPosition, PolygonShape attackPolygonShape) {
        attackPolygonShape.getVertex(0, tmpVertex);
        tmpVertex.add(bodyPosition);
        attackAABB.setPosition(tmpVertex.x, tmpVertex.y);

        attackPolygonShape.getVertex(2, tmpVertex);
        tmpVertex.add(bodyPosition);
        attackAABB.setSize(tmpVertex.x, tmpVertex.y);
    }

    private PolygonShape getAttackFixture(Body body, FacingDirection direction) {
        Array<Fixture> fixtureList = body.getFixtureList();
        String fixtureName = "attack_sensor_" + direction.getAtlasKey();
        for (Fixture fixture : fixtureList) {
            if (fixtureName.equals(fixture.getUserData()) && Type.Polygon.equals(fixture.getShape().getType())) {
                return (PolygonShape) fixture.getShape();
            }
        }

        throw new GdxRuntimeException("Entity has no polygon attack sensor with userData '" + fixtureName + "'");
    }
}
