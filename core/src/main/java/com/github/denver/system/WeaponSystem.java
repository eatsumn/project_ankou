package com.github.denver.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.denver.component.Attack;
import com.github.denver.component.Facing;
import com.github.denver.component.Facing.FacingDirection;
import com.github.denver.component.Player;
import com.github.denver.component.Transform;
import com.github.denver.component.Weapon;

public class WeaponSystem extends IteratingSystem {

    /**
     * Degrees added to the aim angle so the texture's forward direction matches the mouse.
     * Default -90: many weapon sprites are drawn pointing up (+Y) while atan2 measures from +X.
     */
    private static final float AIM_ROTATION_OFFSET_DEG = -90f;

    /** Extra rotation (degrees) at the middle of an attack; follows a sine arc over the attack window. */
    private static final float SWING_ARC_DEG = 85f;

    private static final Family PLAYER = Family.all(Player.class, Transform.class).get();

    private final Viewport viewport;
    private final Vector3 mouseWorld = new Vector3();

    public WeaponSystem(Viewport viewport) {
        super(Family.all(Weapon.class, Transform.class).get());
        this.viewport = viewport;
    }

    @Override
    public void update(float deltaTime) {
        viewport.apply();
        super.update(deltaTime);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Entity player = getPlayer();
        if (player == null) {
            return;
        }
        Transform playerTransform = Transform.MAPPER.get(player);
        Transform weaponTransform = Transform.MAPPER.get(entity);
        weaponTransform.getPosition().set(playerTransform.getPosition().x, playerTransform.getPosition().y + 0.8f);

        int pz = playerTransform.getZ();
        Facing facing = Facing.MAPPER.get(player);
        if (facing != null && facing.getDirection() == FacingDirection.UP) {
            weaponTransform.setZ(pz - 1);
        } else {
            weaponTransform.setZ(pz + 1);
        }

        mouseWorld.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(mouseWorld);

        float wx = weaponTransform.getPosition().x;
        float wy = weaponTransform.getPosition().y;
        float aimDeg =
            MathUtils.atan2(mouseWorld.y - wy, mouseWorld.x - wx) * MathUtils.radiansToDegrees
                + AIM_ROTATION_OFFSET_DEG;

        Attack attack = Attack.MAPPER.get(player);
        float swingDeg = 0f;
        if (attack != null && attack.isAttacking()) {
            float t = attack.getAttackSwingPhase();
            swingDeg = SWING_ARC_DEG * MathUtils.sin(MathUtils.PI * t);
        }

        weaponTransform.setRotationDeg(aimDeg + swingDeg);
    }

    private Entity getPlayer() {
        ImmutableArray<Entity> players = getEngine().getEntitiesFor(PLAYER);
        return players.size() > 0 ? players.first() : null;
    }
}
