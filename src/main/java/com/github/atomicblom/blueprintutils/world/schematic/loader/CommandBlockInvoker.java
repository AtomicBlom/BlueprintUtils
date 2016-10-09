package com.github.atomicblom.blueprintutils.world.schematic.loader;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.CommandBlockBaseLogic;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

public class CommandBlockInvoker implements ITileEntityLoadedEvent
{
    @Override
    public boolean onTileEntityAdded(TileEntity tileEntity)
    {
        if (tileEntity instanceof TileEntityCommandBlock)
        {
            final World world = tileEntity.getWorld();

            TileEntityCommandBlock commandBlock = (TileEntityCommandBlock) tileEntity;
            final BlockPos pos = tileEntity.getPos();
            IBlockState block = world.getBlockState(pos);
            CommandBlockBaseLogic commandblocklogic = commandBlock.getCommandBlockLogic();

            final GameRules gameRules = commandblocklogic.getServer().worldServers[0].getGameRules();
            Boolean commandBlockOutputSetting = gameRules.getBoolean("commandBlockOutput");
            gameRules.setOrCreateGameRule("commandBlockOutput", "false");

            commandblocklogic.trigger(world);
            world.updateComparatorOutputLevel(pos, block.getBlock());

            if (world.getTileEntity(pos) instanceof TileEntityCommandBlock)
            {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
            }
            gameRules.setOrCreateGameRule("commandBlockOutput", commandBlockOutputSetting.toString());
            return true;
        }
        return false;
    }
}
