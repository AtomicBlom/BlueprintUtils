package com.github.atomicblom.blueprintutils.api.event;

import net.minecraft.block.Block;
import net.minecraftforge.fml.common.registry.FMLControlledNamespacedRegistry;

public class UnknownBlockEvent
{

    public final String name;
    public final FMLControlledNamespacedRegistry<Block> blockRegistry;
    Short newId;

    public UnknownBlockEvent(String name, FMLControlledNamespacedRegistry<Block> blockRegistry)
    {
        this.name = name;

        this.blockRegistry = blockRegistry;
    }

    public void remap(Block block)
    {
        newId = (short) blockRegistry.getId(block);
    }

    public boolean isRemapped()
    {
        return newId != null;
    }
}
