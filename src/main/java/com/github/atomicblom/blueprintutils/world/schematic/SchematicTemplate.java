package com.github.atomicblom.blueprintutils.world.schematic;

import com.github.atomicblom.blueprintutils.api.ISchematic;
import com.github.atomicblom.blueprintutils.world.WorldDummy;
import com.github.atomicblom.blueprintutils.world.storage.Schematic;
import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ObjectIntIdentityMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.structure.template.Template.BlockInfo;
import net.minecraft.world.gen.structure.template.Template.EntityInfo;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

public class SchematicTemplate extends SchematicFormat
{
    @Override
    public ISchematic readFromNBT(NBTTagCompound compound)
    {
        final List<BlockInfo> blocks = Lists.<BlockInfo>newArrayList();
        final List<EntityInfo> entities = Lists.<EntityInfo>newArrayList();

        blocks.clear();
        entities.clear();
        NBTTagList nbttaglist = compound.getTagList("size", 3);
        BlockPos size = new BlockPos(nbttaglist.getIntAt(0), nbttaglist.getIntAt(1), nbttaglist.getIntAt(2));
        String author = compound.getString("author");
        BlockPallette blockPalette = new BlockPallette();
        NBTTagList nbttaglist1 = compound.getTagList("palette", 10);

        for (int i = 0; i < nbttaglist1.tagCount(); ++i)
        {
            blockPalette.addMapping(NBTUtil.readBlockState(nbttaglist1.getCompoundTagAt(i)), i);
        }

        NBTTagList nbtBlockList = compound.getTagList("blocks", 10);

        Schematic schematic = new Schematic(new ItemStack(Item.getByNameOrId("structureBlock")), size.getX(), size.getY(), size.getZ());
        for (int j = 0; j < nbtBlockList.tagCount(); ++j)
        {
            NBTTagCompound blockData = nbtBlockList.getCompoundTagAt(j);
            NBTTagList nbtBlockPosition = blockData.getTagList("pos", 3);
            BlockPos blockPos = new BlockPos(nbtBlockPosition.getIntAt(0), nbtBlockPosition.getIntAt(1), nbtBlockPosition.getIntAt(2));
            IBlockState blockState = blockPalette.stateFor(blockData.getInteger("state"));

            NBTTagCompound blockNbt = blockData.hasKey("nbt") ? blockData.getCompoundTag("nbt") : null;

            //blocks.add(new BlockInfo(blockPos, blockState, blockNbt));
            schematic.setBlockState(blockPos, blockState);
            final Block block = blockState.getBlock();
            if (block.hasTileEntity(blockState) && blockNbt != null) {
                final TileEntity tileEntity = block.createTileEntity(WorldDummy.instance(), blockState);
                tileEntity.readFromNBT(blockNbt);
                schematic.setTileEntity(blockPos, tileEntity);
            }
        }

        NBTTagList entityList = compound.getTagList("entities", 10);

        for (int k = 0; k < entityList.tagCount(); ++k)
        {
            NBTTagCompound entityData = entityList.getCompoundTagAt(k);
            NBTTagList nbtEntityPosition = entityData.getTagList("pos", 6);
            Vec3d vec3d = new Vec3d(nbtEntityPosition.getDoubleAt(0), nbtEntityPosition.getDoubleAt(1), nbtEntityPosition.getDoubleAt(2));
            //NBTTagList nbtEntityBlockPos = entityData.getTagList("blockPos", 3);
            //BlockPos blockPos = new BlockPos(nbtEntityBlockPos.getIntAt(0), nbtEntityBlockPos.getIntAt(1), nbtEntityBlockPos.getIntAt(2));

            if (entityData.hasKey("nbt"))
            {
                NBTTagCompound nbtData = entityData.getCompoundTag("nbt");
                Entity entity = EntityList.createEntityFromNBT(nbtData, WorldDummy.instance());
                entity.setPosition(vec3d.xCoord, vec3d.yCoord, vec3d.zCoord);

                schematic.addEntity(entity);

                //entities.add(new EntityInfo(vec3d, blockPos, nbtData));
            }
        }

        return schematic;
    }

    @Override
    public boolean writeToNBT(NBTTagCompound tagCompound, ISchematic schematic)
    {
        return false;
    }

    static class BlockPallette implements Iterable<IBlockState>
    {
        public static final IBlockState DEFAULT_BLOCK_STATE = Blocks.AIR.getDefaultState();
        final ObjectIntIdentityMap<IBlockState> ids;
        private int lastId;

        private BlockPallette()
        {
            this.ids = new ObjectIntIdentityMap(16);
        }

        public int idFor(IBlockState p_189954_1_)
        {
            int i = this.ids.get(p_189954_1_);

            if (i == -1)
            {
                i = this.lastId++;
                this.ids.put(p_189954_1_, i);
            }

            return i;
        }

        @Nullable
        public IBlockState stateFor(int p_189955_1_)
        {
            IBlockState iblockstate = (IBlockState)this.ids.getByValue(p_189955_1_);
            return iblockstate == null ? DEFAULT_BLOCK_STATE : iblockstate;
        }

        public Iterator<IBlockState> iterator()
        {
            return this.ids.iterator();
        }

        public void addMapping(IBlockState p_189956_1_, int p_189956_2_)
        {
            this.ids.put(p_189956_1_, p_189956_2_);
        }
    }
}
