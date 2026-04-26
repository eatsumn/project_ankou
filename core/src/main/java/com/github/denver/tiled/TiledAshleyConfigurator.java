package com.github.denver.tiled;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FileTextureData;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.github.denver.Main;
import com.github.denver.asset.AssetService;
import com.github.denver.asset.AtlasAsset;
import com.github.denver.asset.SoundAsset;
import com.github.denver.component.*;
import com.github.denver.component.Animation2D.AnimationType;
import com.github.denver.component.Facing.FacingDirection;

public class TiledAshleyConfigurator {
    private static final Vector2 DEFAULT_PHYSIC_SCALING = new Vector2(1f, 1f);

    /** Empty object properties when spawning ghosts without a Tiled map object. */
    private static final MapProperties SPAWN_OBJECT_PROPERTIES = new MapProperties();

    private final Engine engine;
    private final World physicWorld;
    private final MapObjects tmpMapObjects;
    private final Vector2 tmpVec2;
    private final AssetService assetService;

    public TiledAshleyConfigurator(Engine engine, World physicWorld, AssetService assetService) {
        this.engine = engine;
        this.physicWorld = physicWorld;
        this.tmpMapObjects = new MapObjects();
        this.tmpVec2 = new Vector2();
        this.assetService = assetService;
    }

    public void onLoadTile(TiledMapTile tile, float x, float y) {
        createBody(tile.getObjects(),
            new Vector2(x, y),
            DEFAULT_PHYSIC_SCALING,
            BodyType.StaticBody,
            Vector2.Zero,
            "environment");
    }

    public void onLoadTrigger(String triggerName, MapObject mapObject) {
        if (mapObject instanceof RectangleMapObject rectMapObj) {
            Entity entity = this.engine.createEntity();
            Rectangle rect = rectMapObj.getRectangle();
            addEntityTransform(
                rect.getX(), rect.getY(), 0,
                rect.getWidth(), rect.getHeight(),
                1f, 1f,
                0,
                entity);
            addEntityPhysic(
                rectMapObj,
                BodyType.StaticBody,
                tmpVec2.set(rect.getX(), rect.getY()).scl(Main.UNIT_SCALE),
                entity);
            entity.add(new Trigger(triggerName));
            entity.add(new Tiled(rectMapObj));
            this.engine.addEntity(entity);
        } else {
            throw new GdxRuntimeException("Unsupported map object type for trigger: " + mapObject.getClass().getSimpleName());
        }
    }

    /**
     * Creates and configures an entity from a Tiled map object with all necessary components.
     */
    public void onLoadObject(TiledMapTileMapObject tileMapObject) {
        Entity entity = this.engine.createEntity();
        TiledMapTile tile = tileMapObject.getTile();
        TextureRegion textureRegion = getTextureRegion(tile);
        float sortOffsetY = tile.getProperties().get("sortOffsetY", 0, Integer.class);
        sortOffsetY *= Main.UNIT_SCALE;
        int z = tile.getProperties().get("z", 1, Integer.class);

        addEntityTransform(
            tileMapObject.getX(), tileMapObject.getY(), z,
            textureRegion.getRegionWidth(), textureRegion.getRegionHeight(),
            tileMapObject.getScaleX(), tileMapObject.getScaleY(),
            sortOffsetY,
            entity);
        Integer ghostType = readGhostType(tile, tileMapObject.getProperties());
        Ghost.Kind ghostKind = ghostType == null ? null : Ghost.kindFromGhostType(ghostType);
        if (ghostKind != null) {
            entity.add(new Ghost(ghostKind));
            if (ghostKind == Ghost.Kind.BOUNCE_WALLS) {
                Ghost.MAPPER.get(entity).setRangeCheck(readRangeCheck(tile, tileMapObject.getProperties()));
            }
        }

        BodyType bodyType = getObjectBodyType(tile);
        if (ghostKind == Ghost.Kind.PHASE_THROUGH) {
            // no Box2D body: passes through tile colliders; GhostAiSystem clamps to map edges
        } else if (tile.getObjects().getCount() > 0) {
            addEntityPhysic(
                tile.getObjects(),
                bodyType,
                Vector2.Zero,
                entity);
        } else if (ghostKind != null) {
            addGhostDefaultDynamicHitbox(entity, bodyType);
        } else {
            addEntityPhysic(
                tile.getObjects(),
                bodyType,
                Vector2.Zero,
                entity);
        }
        addEntityAnimation(tile, entity);
        addEntityMove(tile, entity, ghostType != null ? 2f : 0f);
        addEntityController(tileMapObject, entity);
        addEntityCameraFollow(tileMapObject, entity);
        addEntityLife(tile, entity);
        addEntityPlayer(tileMapObject, entity);
        addEntityWeapon(tileMapObject, entity);
        addEntityAttack(tile, entity);
        entity.add(new Facing(FacingDirection.DOWN));
        if (ghostKind == null) {
            entity.add(new Fsm(entity));
        }
        entity.add(new Graphic(textureRegion, Color.WHITE.cpy()));
        applyOptionalObjectPivot(tileMapObject, entity);
        entity.add(new Tiled(tileMapObject));

        if (ghostKind != null) {
            configureGhostCombat(tile, tileMapObject.getProperties(), entity);
        }

        this.engine.addEntity(entity);
    }

    /**
     * Spawns a ghost from the map's {@code objects} tileset (tile ids 11–13 for types 0–2) at Tiled pixel coordinates.
     */
    public Entity spawnGhost(TiledMap tiledMap, int ghostType0to2, float tiledPixelX, float tiledPixelY) {
        TiledMapTile tile = resolveGhostTileForType(tiledMap, ghostType0to2);
        int ghostType = MathUtils.clamp(ghostType0to2, 0, 2);
        Entity entity = this.engine.createEntity();
        TextureRegion textureRegion = getTextureRegion(tile);
        float sortOffsetY = tile.getProperties().get("sortOffsetY", 0, Integer.class);
        sortOffsetY *= Main.UNIT_SCALE;
        int z = tile.getProperties().get("z", 1, Integer.class);

        addEntityTransform(
            tiledPixelX, tiledPixelY, z,
            textureRegion.getRegionWidth(), textureRegion.getRegionHeight(),
            1f, 1f,
            sortOffsetY,
            entity);
        Ghost.Kind ghostKind = Ghost.kindFromGhostType(ghostType);
        entity.add(new Ghost(ghostKind));
        if (ghostKind == Ghost.Kind.BOUNCE_WALLS) {
            Ghost.MAPPER.get(entity).setRangeCheck(readRangeCheck(tile, SPAWN_OBJECT_PROPERTIES));
        }

        BodyType bodyType = getObjectBodyType(tile);
        if (ghostKind == Ghost.Kind.PHASE_THROUGH) {
            // no body
        } else if (tile.getObjects().getCount() > 0) {
            addEntityPhysic(tile.getObjects(), bodyType, Vector2.Zero, entity);
        } else {
            addGhostDefaultDynamicHitbox(entity, bodyType);
        }
        addEntityAnimation(tile, entity);
        addEntityMove(tile, entity, 2f);
        entity.add(new Facing(FacingDirection.DOWN));
        entity.add(new Graphic(textureRegion, Color.WHITE.cpy()));
        configureGhostCombat(tile, SPAWN_OBJECT_PROPERTIES, entity);
        this.engine.addEntity(entity);
        return entity;
    }

    private static TiledMapTile resolveGhostTileForType(TiledMap map, int ghostType0to2) {
        int tileId = 11 + MathUtils.clamp(ghostType0to2, 0, 2);
        for (TiledMapTileSet ts : map.getTileSets()) {
            TiledMapTile t = ts.getTile(tileId);
            if (t != null) {
                return t;
            }
        }
        throw new GdxRuntimeException("Ghost tile id " + tileId + " not found in map tilesets");
    }

    private void configureGhostCombat(TiledMapTile tile, MapProperties objectProperties, Entity entity) {
        Ghost ghost = Ghost.MAPPER.get(entity);
        Move move = Move.MAPPER.get(entity);
        if (ghost == null || move == null) {
            return;
        }
        float baseSpeed = move.getMaxSpeed();
        float speedAdd = readSpeedAdd(tile, objectProperties);
        SoundAsset hitSfx = readGhostSound(tile, objectProperties, "ghostHitSound", SoundAsset.GHOST_HIT);
        SoundAsset deathSfx = readGhostSound(tile, objectProperties, "ghostDeadSound", SoundAsset.GHOST_DEAD);
        ghost.configureForCombat(baseSpeed, speedAdd, hitSfx, deathSfx);
    }

    private static float readSpeedAdd(TiledMapTile tile, MapProperties objectProperties) {
        if (objectProperties.containsKey("speedAdd")) {
            return objectProperties.get("speedAdd", 0f, Float.class);
        }
        MapProperties tileProps = tile.getProperties();
        if (tileProps.containsKey("speedAdd")) {
            return tileProps.get("speedAdd", 0f, Float.class);
        }
        return 0f;
    }

    private static SoundAsset readGhostSound(
        TiledMapTile tile,
        MapProperties objectProperties,
        String propertyName,
        SoundAsset defaultSound
    ) {
        String value = null;
        if (objectProperties.containsKey(propertyName)) {
            value = objectProperties.get(propertyName, String.class);
        }
        if (value == null || value.isBlank()) {
            if (tile.getProperties().containsKey(propertyName)) {
                value = tile.getProperties().get(propertyName, String.class);
            }
        }
        if (value == null || value.isBlank()) {
            return defaultSound;
        }
        return SoundAsset.valueOf(value.trim());
    }

    private void addEntityWeapon(TiledMapTileMapObject tileMapObject, Entity entity) {
        String name = tileMapObject.getName();
        boolean nameIsWeapon = name != null && name.equalsIgnoreCase("weapon");
        boolean onPlayer = tileMapObject.getProperties().get("onPlayer", false, Boolean.class);
        if (nameIsWeapon || onPlayer) {
            entity.add(new Weapon());
        }
    }

    /**
     * Optional object properties {@code pivotFracX}, {@code pivotFracY} in 0–1 from the sprite's bottom-left
     * (rotation origin for {@link com.github.denver.system.RenderSystem}).
     */
    private void applyOptionalObjectPivot(TiledMapTileMapObject tileMapObject, Entity entity) {
        MapProperties p = tileMapObject.getProperties();
        if (!p.containsKey("pivotFracX") && !p.containsKey("pivotFracY")) {
            return;
        }
        Graphic graphic = Graphic.MAPPER.get(entity);
        if (graphic == null) {
            return;
        }
        float x = p.get("pivotFracX", graphic.getOriginFracX(), Float.class);
        float y = p.get("pivotFracY", graphic.getOriginFracY(), Float.class);
        graphic.setOriginFrac(x, y);
    }

    private BodyType getObjectBodyType(TiledMapTile tile) {
        String classType = tile.getProperties().get("type", "", String.class);
        if ("Prop".equals(classType)) {
            return BodyType.StaticBody;
        }

        String bodyTypeStr = tile.getProperties().get("bodyType", "DynamicBody", String.class);
        return BodyType.valueOf(bodyTypeStr);
    }

    private void addEntityAttack(TiledMapTile tile, Entity entity) {
        float damage = tile.getProperties().get("damage", 0f, Float.class);
        if (damage == 0f) return;

        float damageDelay = tile.getProperties().get("damageDelay", 0f, Float.class);
        String soundAssetStr = tile.getProperties().get("attackSound", String.class);
        SoundAsset soundAsset = null;
        if (soundAssetStr != null) {
            soundAsset = SoundAsset.valueOf(soundAssetStr);
        }
        entity.add(new Attack(damage, damageDelay, soundAsset));
    }

    private void addEntityPlayer(TiledMapTileMapObject tileMapObject, Entity entity) {
        if ("Player".equals(tileMapObject.getName())) {
            entity.add(new Player());
        }
    }

    private void addEntityLife(TiledMapTile tile, Entity entity) {
        int life = tile.getProperties().get("life", 0, Integer.class);
        if (life == 0) return;

        float lifeReg = tile.getProperties().get("lifeReg", 0f, Float.class);
        entity.add(new Life(life, lifeReg));
    }

    private TextureRegion getTextureRegion(TiledMapTile tile) {
        String atlasAssetStr = tile.getProperties().get("atlasAsset", "OBJECTS", String.class);
        AtlasAsset atlasAsset = AtlasAsset.valueOf(atlasAssetStr);
        FileTextureData textureData = (FileTextureData) tile.getTextureRegion().getTexture().getTextureData();
        String atlasKey = textureData.getFileHandle().nameWithoutExtension();
        TextureAtlas textureAtlas = assetService.get(atlasAsset);
        TextureAtlas.AtlasRegion region = textureAtlas.findRegion(atlasKey + "/" + atlasKey);
        if (region != null) {
            return region;
        }

        // Region not part of an atlas, or the object has an animation.
        // If it has an animation, then its region is updated in the AnimationSystem.
        // If it has no region, then we render the region of the Tiled editor to show something, but
        // that will add one render call due to texture swapping.
        return tile.getTextureRegion();
    }

    private void addEntityCameraFollow(TiledMapTileMapObject tileMapObject, Entity entity) {
        boolean cameraFollow = tileMapObject.getProperties().get("camFollow", false, Boolean.class);
        if (!cameraFollow) return;

        entity.add(new CameraFollow());
    }


    private void addEntityController(TiledMapTileMapObject tileMapObject, Entity entity) {
        boolean controller = tileMapObject.getProperties().get("controller", false, Boolean.class);
        if (!controller) return;

        entity.add(new Controller());
    }

    private void addEntityMove(TiledMapTile tile, Entity entity, float defaultSpeedIfZero) {
        float speed = tile.getProperties().get("speed", 0f, Float.class);
        if (speed <= 0f) {
            speed = defaultSpeedIfZero;
        }
        if (speed <= 0f) {
            return;
        }
        entity.add(new Move(speed));
    }

    private static float readRangeCheck(TiledMapTile tile, MapProperties objectProperties) {
        if (objectProperties.containsKey("rangeCheck")) {
            return objectProperties.get("rangeCheck", 4f, Float.class);
        }
        if (objectProperties.containsKey("checkRange")) {
            return objectProperties.get("checkRange", 4f, Float.class);
        }
        MapProperties tileProps = tile.getProperties();
        if (tileProps.containsKey("rangeCheck")) {
            return tileProps.get("rangeCheck", 4f, Float.class);
        }
        if (tileProps.containsKey("checkRange")) {
            return tileProps.get("checkRange", 4f, Float.class);
        }
        return 4f;
    }

    private static Integer readGhostType(TiledMapTile tile, MapProperties objectProperties) {
        if (objectProperties.containsKey("ghostType")) {
            return objectProperties.get("ghostType", 0, Integer.class);
        }
        MapProperties tileProps = tile.getProperties();
        if (tileProps.containsKey("ghostType")) {
            return tileProps.get("ghostType", 0, Integer.class);
        }
        if (tileProps.containsKey("ghostType2")) {
            return tileProps.get("ghostType2", 2, Integer.class);
        }
        return null;
    }

    private void addGhostDefaultDynamicHitbox(Entity entity, BodyType bodyType) {
        Transform tf = Transform.MAPPER.get(entity);
        Vector2 pos = tf.getPosition();
        Vector2 size = tf.getSize();
        Vector2 sc = tf.getScaling();
        float bw = size.x * sc.x;
        float bh = size.y * sc.y;
        float hx = Math.max(0.06f, bw * 0.35f);
        float hy = Math.max(0.06f, bh * 0.35f);
        tmpVec2.set(bw * 0.5f, bh * 0.5f);

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = bodyType;
        bodyDef.position.set(pos.x + tmpVec2.x, pos.y + tmpVec2.y);
        bodyDef.fixedRotation = true;
        Body body = physicWorld.createBody(bodyDef);
        body.setUserData(entity);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(hx, hy, Vector2.Zero, 0f);
        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.density = 1f;
        body.createFixture(fd);
        shape.dispose();

        entity.add(new Physic(body, new Vector2(body.getPosition())));
    }

    private void addEntityAnimation(TiledMapTile tile, Entity entity) {
        String animationStr = tile.getProperties().get("animation", "", String.class);
        if (animationStr.isBlank()) {
            return;
        }
        AnimationType animationType = AnimationType.valueOf(animationStr);

        String atlasAssetStr = tile.getProperties().get("atlasAsset", "OBJECTS", String.class);
        AtlasAsset atlasAsset = AtlasAsset.valueOf(atlasAssetStr);
        FileTextureData textureData = (FileTextureData) tile.getTextureRegion().getTexture().getTextureData();
        String atlasKey = textureData.getFileHandle().nameWithoutExtension();
        float speed = tile.getProperties().get("animationSpeed", 0f, Float.class);

        entity.add(new Animation2D(atlasAsset, atlasKey, animationType, Animation.PlayMode.LOOP, speed));
    }

    private void addEntityPhysic(MapObject mapObject, @SuppressWarnings("SameParameterValue") BodyType bodyType, Vector2 relativeTo, Entity entity) {
        if (tmpMapObjects.getCount() > 0) tmpMapObjects.remove(0);

        tmpMapObjects.add(mapObject);
        addEntityPhysic(tmpMapObjects, bodyType, relativeTo, entity);
    }

    private void addEntityPhysic(MapObjects mapObjects, BodyType bodyType, Vector2 relativeTo, Entity entity) {
        if (mapObjects.getCount() == 0) return;

        Transform transform = Transform.MAPPER.get(entity);
        Body body = createBody(mapObjects,
            transform.getPosition(),
            transform.getScaling(),
            bodyType,
            relativeTo,
            entity);

        entity.add(new Physic(body, new Vector2(body.getPosition())));
    }

    private Body createBody(MapObjects mapObjects,
                            Vector2 position,
                            Vector2 scaling,
                            BodyType bodyType,
                            Vector2 relativeTo,
                            Object userData) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = bodyType;
        bodyDef.position.set(position);
        bodyDef.fixedRotation = true;

        Body body = this.physicWorld.createBody(bodyDef);
        body.setUserData(userData);
        for (MapObject object : mapObjects) {
            FixtureDef fixtureDef = TiledPhysics.fixtureDefOf(object, scaling, relativeTo);
            Fixture fixture = body.createFixture(fixtureDef);
            fixture.setUserData(object.getName());
            fixtureDef.shape.dispose();
        }
        return body;
    }

    private static void addEntityTransform(
        float x, float y, int z,
        float w, float h,
        float scaleX, float scaleY,
        float sortOffsetY,
        Entity entity
    ) {
        Vector2 position = new Vector2(x, y);
        Vector2 size = new Vector2(w, h);
        Vector2 scaling = new Vector2(scaleX, scaleY);

        position.scl(Main.UNIT_SCALE);
        size.scl(Main.UNIT_SCALE);

        entity.add(new Transform(position, z, size, scaling, 0f, sortOffsetY));
    }

}
