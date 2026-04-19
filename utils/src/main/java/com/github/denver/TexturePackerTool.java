package com.github.denver;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;

public class TexturePackerTool {
    public static void main(String[] args) {
        String ObjectIn = "assets_raw/objects";
        String OutDir = "assets/graphics";
        String objectFileName = "objects";

        TexturePacker.process(ObjectIn, OutDir, objectFileName);

        String CharacterIn = "assets_raw/characters";
        String characterFileName = "characters";

        TexturePacker.process(CharacterIn, OutDir, characterFileName);


    }
}

