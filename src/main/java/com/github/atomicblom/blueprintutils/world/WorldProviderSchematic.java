package com.github.atomicblom.blueprintutils.world;

import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;

public class WorldProviderSchematic extends WorldProvider {
    @Override
    public DimensionType getDimensionType() {
        // TODO: this shouldn't be null...
        return null;
    }
}