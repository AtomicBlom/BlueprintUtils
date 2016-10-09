package com.github.atomicblom.blueprintutils.api.event;

import com.github.atomicblom.blueprintutils.api.ISchematic;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Created by codew on 9/10/2016.
 */
public class PreSetBlockEvent
{
    public final ISchematic schematic;
    public final World world;
    public final BlockPos worldCoord;
    public final BlockPos schematicCoord;
    private IBlockState blockState;
    private boolean shouldSetBlock = true;

    public PreSetBlockEvent(ISchematic schematic, World world, BlockPos worldCoord, BlockPos schematicCoord)
    {
        this.blockState = schematic.getBlockState(schematicCoord);
        this.schematic = schematic;
        this.world = world;
        this.worldCoord = worldCoord;
        this.schematicCoord = schematicCoord;
    }

    public IBlockState getBlockState()
    {
        return blockState;
    }

    public void replaceBlockState(IBlockState blockState)
    {
        this.blockState = blockState;
    }

    public void cancelSetBlock()
    {
        shouldSetBlock = false;
    }

    public boolean shouldSetBlock()
    {
        return shouldSetBlock;
    }
}
