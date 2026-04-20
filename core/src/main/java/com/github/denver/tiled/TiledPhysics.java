package com.github.denver.tiled;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.github.denver.Main;

public class TiledPhysics {

    public static FixtureDef fixtureDefOf(MapObject mapObject, Vector2 scaling, Vector2 relativeTo) {
        if (mapObject instanceof RectangleMapObject rectMapObj) {
            return rectangleFixtureDef(rectMapObj, scaling, relativeTo);
        } else {
            throw new GdxRuntimeException("Unsupported MapObject: " + mapObject);
        }
    }

    private static FixtureDef rectangleFixtureDef(RectangleMapObject rectMapObj, Vector2 scaling, Vector2 relativeTo) {
        Rectangle rectangle = rectMapObj.getRectangle();
        float rectX = rectangle.x;
        float rectY = rectangle.y;
        float rectW = rectangle.width;
        float rectH = rectangle.height;

        float boxX = rectX * Main.UNIT_SCALE * scaling.x - relativeTo.x;
        float boxY = rectY * Main.UNIT_SCALE * scaling.y - relativeTo.y;
        float boxW = rectW * Main.UNIT_SCALE * scaling.x * 0.5f;
        float boxH = rectH * Main.UNIT_SCALE * scaling.y * 0.5f;

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(boxW, boxH, new Vector2(boxX + boxW, boxY + boxH), 0f);
        return fixtureDefOfMapObjectAndShape(rectMapObj, shape);
    }

    private static FixtureDef fixtureDefOfMapObjectAndShape(MapObject mapObject, Shape shape) {
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.friction = 0f;//mapObject.getProperties().get("friction", 0f, Float.class);
        fixtureDef.restitution = 0f;// mapObject.getProperties().get("restitution", 0f, Float.class);
        fixtureDef.density = 0f ;//mapObject.getProperties().get("density", 0f, Float.class);
        fixtureDef.isSensor = false ;//mapObject.getProperties().get("sensor", false, Boolean.class);
        return fixtureDef;
    }
}
