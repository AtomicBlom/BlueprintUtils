package com.github.atomicblom.blueprintutils.world.schematic.loader;

import net.minecraft.tileentity.TileEntity;

/**
 * Created by codew on 9/10/2016.
 */
public interface ITileEntityLoadedEvent
{
    boolean onTileEntityAdded(TileEntity tileEntity);
}
