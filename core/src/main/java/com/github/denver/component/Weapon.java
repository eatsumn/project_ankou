package com.github.denver.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;

public class Weapon implements Component {
    public static final ComponentMapper<Weapon> MAPPER = ComponentMapper.getFor(Weapon.class);
}
