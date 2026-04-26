<?xml version="1.0" encoding="UTF-8"?>
<tileset version="1.10" tiledversion="1.12.1" name="objects" tilewidth="400" tileheight="500" tilecount="18" columns="0">
 <grid orientation="orthogonal" width="1" height="1"/>
 <tile id="1" type="Object">
  <properties>
   <property name="animation" value="IDLE"/>
   <property name="animationSpeed" type="float" value="0.5"/>
   <property name="attackSound" value="SWING"/>
   <property name="damage" type="float" value="7"/>
   <property name="damageDelay" type="float" value="0.3"/>
   <property name="life" type="int" value="12"/>
   <property name="lifeReg" type="float" value="0.25"/>
   <property name="speed" type="float" value="3.5"/>
  </properties>
  <image source="objects/player.png" width="400" height="500"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="165.313" y="324.375" width="80.4375" height="44.6875">
    <ellipse/>
   </object>
   <object id="2" name="attack_sensor_down" x="67" y="315.438" width="286" height="134.063">
    <properties>
     <property name="sensor" type="bool" value="true"/>
    </properties>
   </object>
   <object id="3" name="attack_sensor_up" x="67" y="163.5" width="286" height="134.063">
    <properties>
     <property name="sensor" type="bool" value="true"/>
    </properties>
   </object>
   <object id="4" name="attack_sensor_left" x="67" y="163.5" width="134.063" height="286">
    <properties>
     <property name="sensor" type="bool" value="true"/>
    </properties>
   </object>
   <object id="5" name="attack_sensor_right" x="218.938" y="163.5" width="134.063" height="286">
    <properties>
     <property name="sensor" type="bool" value="true"/>
    </properties>
   </object>
  </objectgroup>
 </tile>
 <tile id="2" type="Prop">
  <image source="objects/house.png" width="80" height="112"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="7" y="82" width="67" height="26"/>
  </objectgroup>
 </tile>
 <tile id="4" type="Prop">
  <image source="objects/chest.png" width="16" height="16"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="0" y="4" width="16" height="10"/>
  </objectgroup>
 </tile>
 <tile id="5" type="Prop">
  <image source="objects/oak_tree.png" width="41" height="63"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="13" y="54">
    <polygon points="0,0 6,1 11,1 16,-1 16,-2 14,-5 13,-13 3,-13 3,-6 2,-5 1,-3 0,-1"/>
   </object>
  </objectgroup>
 </tile>
 <tile id="6" type="Object">
  <properties>
   <property name="animation" value="IDLE"/>
   <property name="z" type="int" value="0"/>
  </properties>
  <image source="objects/trap.png" width="16" height="16"/>
 </tile>
 <tile id="7" type="Object">
  <properties>
   <property name="animation" value="IDLE"/>
   <property name="animationSpeed" type="float" value="1"/>
   <property name="bodyType" propertytype="BodyType" value="StaticBody"/>
   <property name="life" type="int" value="99999"/>
   <property name="lifeReg" type="float" value="9999"/>
  </properties>
  <image source="objects/training_dummy.png" width="32" height="32"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="3" y="12" width="26" height="16"/>
  </objectgroup>
 </tile>
 <tile id="8" type="Prop">
  <image source="Church.png" width="64" height="64"/>
 </tile>
 <tile id="9" type="Prop">
  <image source="Bush.png" width="32" height="32"/>
 </tile>
 <tile id="10">
  <image source="objects/weapon_idle.png" width="400" height="370"/>
 </tile>
 <tile id="11" type="Object">
  <properties>
   <property name="checkRange" type="float" value="1.5"/>
   <property name="ghostDeadSound" value="GHOST_DEAD"/>
   <property name="ghostHitSound" value="GHOST_HIT"/>
   <property name="ghostType" type="int" value="0"/>
   <property name="speed" type="float" value="5"/>
   <property name="speedAdd" type="float" value="0.35"/>
  </properties>
  <image source="objects/ghost0.png" width="32" height="32"/>
 </tile>
 <tile id="12" type="Object">
  <properties>
   <property name="ghostDeadSound" value="GHOST_DEAD"/>
   <property name="ghostHitSound" value="GHOST_HIT"/>
   <property name="ghostType" type="int" value="1"/>
   <property name="speed" type="float" value="4"/>
   <property name="speedAdd" type="float" value="0.35"/>
  </properties>
  <image source="objects/ghost2.png" width="32" height="32"/>
 </tile>
 <tile id="13" type="Object">
  <properties>
   <property name="ghostDeadSound" value="GHOST_DEAD"/>
   <property name="ghostHitSound" value="GHOST_HIT"/>
   <property name="ghostType" type="int" value="2"/>
   <property name="speed" type="float" value="4"/>
   <property name="speedAdd" type="float" value="0.35"/>
  </properties>
  <image source="objects/ghost1.png" width="32" height="32"/>
 </tile>
 <tile id="14" type="Prop">
  <image source="objects/ran_1.png" width="16" height="16"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="3.81818" y="11.9091">
    <polygon points="0,0 5.27273,-0.636364 5.45455,1.54545 -0.545455,1.81818"/>
   </object>
  </objectgroup>
 </tile>
 <tile id="15" type="Prop">
  <image source="objects/ran_2.png" width="16" height="16"/>
  <objectgroup draworder="index" id="3">
   <object id="2" x="3.18182" y="11.7273">
    <polygon points="0,0 7.36364,-0.272727 8.54545,1.45455 0.727273,2.18182"/>
   </object>
  </objectgroup>
 </tile>
 <tile id="16" type="Prop">
  <image source="objects/ran_3.png" width="16" height="16"/>
 </tile>
 <tile id="17" type="Prop">
  <image source="objects/ran_4.png" width="16" height="16"/>
 </tile>
 <tile id="18" type="Prop">
  <image source="objects/ran_5.png" width="16" height="16"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="6" y="9" width="7" height="3"/>
  </objectgroup>
 </tile>
 <tile id="19" type="Prop">
  <image source="objects/ran_6.png" width="16" height="16"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="6" y="7" width="4" height="3"/>
   <object id="3" x="6" y="7" width="4" height="3"/>
  </objectgroup>
 </tile>
</tileset>
