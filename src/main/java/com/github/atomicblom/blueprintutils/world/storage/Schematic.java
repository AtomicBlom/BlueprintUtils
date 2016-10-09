package com.github.atomicblom.blueprintutils.world.storage;

import com.github.atomicblom.blueprintutils.api.ISchematic;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Schematic implements ISchematic {
    private static final ItemStack DEFAULT_ICON = new ItemStack(Blocks.GRASS);

    private ItemStack icon;
    private final int[][][] blockStates;
    private final BiMap<Integer, IBlockState> palette;
    private final Map<Integer, AtomicInteger> usedBlockStates;
    private final List<TileEntity> tileEntities = new ArrayList<TileEntity>();
    private final List<Entity> entities = new ArrayList<Entity>();
    private final int width;
    private final int height;
    private final int length;

    private int paletteIndex = 0;

    public Schematic(final ItemStack icon, final int width, final int height, final int length) {
        this.icon = icon;
        this.blockStates = new int[width][height][length];
        this.palette = HashBiMap.create();
        this.usedBlockStates = Maps.newHashMap();
        palette.put(0, Blocks.AIR.getDefaultState());
        usedBlockStates.put(paletteIndex, new AtomicInteger(width * height * length));
        paletteIndex++;

        this.width = width;
        this.height = height;
        this.length = length;
    }

    @Override
    public IBlockState getBlockState(final BlockPos pos) {
        if (!isValid(pos)) {
            return Blocks.AIR.getDefaultState();
        }

        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();

        final int i = blockStates[x][y][z];

        if (!palette.containsKey(i)) {
            return Blocks.AIR.getDefaultState();
        }

        return palette.get(i);
    }

    @Override
    public IBlockState getBlockStateUnsafe(final BlockPos pos) {
        if (!isValid(pos)) {
            return null;
        }

        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();

        final int i = blockStates[x][y][z];

        if (!palette.containsKey(i)) {
            return null;
        }

        return palette.get(i);
    }

    @Override
    public boolean setBlockState(final BlockPos pos, final IBlockState blockState) {
        if (!isValid(pos)) {
            return false;
        }

        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();

        final int previousState = blockStates[x][y][z];
        final AtomicInteger previousBlockUsed = this.usedBlockStates.get(previousState);
        if (previousBlockUsed != null) {
            final int blocksInSchematic = previousBlockUsed.decrementAndGet();
            if (blocksInSchematic == 0) {
                usedBlockStates.remove(previousState);
            }
        }

        final BiMap<IBlockState, Integer> inverse = palette.inverse();
        final int paletteId;
        if (inverse.containsKey(blockState))
        {
            paletteId = inverse.get(blockState);
        } else
        {
            paletteId = paletteIndex;
            paletteIndex++;
            palette.put(paletteId, blockState);
        }

        this.blockStates[x][y][z] = paletteId;

        AtomicInteger newBlockUsed = this.usedBlockStates.get(paletteId);
        if (newBlockUsed == null) {
            newBlockUsed = new AtomicInteger();
            usedBlockStates.put(paletteId, newBlockUsed);
        }
        newBlockUsed.incrementAndGet();

        return true;
    }

    @Override
    public TileEntity getTileEntity(final BlockPos pos) {
        for (final TileEntity tileEntity : this.tileEntities) {
            if (tileEntity.getPos().equals(pos)) {
                return tileEntity;
            }
        }

        return null;
    }

    @Override
    public Iterable<TileEntity> getTileEntities() {
        return this.tileEntities;
    }

    @Override
    public void setTileEntity(final BlockPos pos, final TileEntity tileEntity) {
        if (!isValid(pos)) {
            return;
        }

        removeTileEntity(pos);

        if (tileEntity != null) {
            this.tileEntities.add(tileEntity);
        }
    }

    @Override
    public void removeTileEntity(final BlockPos pos) {
        final Iterator<TileEntity> iterator = this.tileEntities.iterator();

        while (iterator.hasNext()) {
            final TileEntity tileEntity = iterator.next();
            if (tileEntity.getPos().equals(pos)) {
                iterator.remove();
            }
        }
    }

    @Override
    public List<Entity> getEntities() {
        return this.entities;
    }

    @Override
    public void addEntity(final Entity entity) {
        if (entity == null || entity.getUniqueID() == null || entity instanceof EntityPlayer) {
            return;
        }

        for (final Entity e : this.entities) {
            if (entity.getUniqueID().equals(e.getUniqueID())) {
                return;
            }
        }

        this.entities.add(entity);
    }

    @Override
    public void removeEntity(final Entity entity) {
        if (entity == null || entity.getUniqueID() == null) {
            return;
        }

        final Iterator<Entity> iterator = this.entities.iterator();
        while (iterator.hasNext()) {
            final Entity e = iterator.next();
            if (entity.getUniqueID().equals(e.getUniqueID())) {
                iterator.remove();
            }
        }
    }

    public boolean remapBlockState(IBlockState oldBlockState, IBlockState newBlockState) {
        final BiMap<IBlockState, Integer> palette = this.palette.inverse();
        if (!palette.containsKey(oldBlockState)) {
            return false;
        }
        final Integer oldIndex = palette.get(oldBlockState);

        this.palette.remove(oldIndex);
        final MutableBlockPos pos = new MutableBlockPos();
        if (palette.containsKey(newBlockState)) {
            final Integer newIndex = palette.get(oldBlockState);
            for (int x = 0; x < width; ++x) {
                for (int y = 0; y < height; ++y) {
                    for (int z = 0; z < length; ++z) {
                        pos.setPos(x, y, z);
                        if (blockStates[x][y][z] == oldIndex) {
                            blockStates[x][y][z] = newIndex;
                        }
                    }
                }
            }

            final AtomicInteger atomicInteger = usedBlockStates.get(newIndex);
            atomicInteger.addAndGet(usedBlockStates.get(oldIndex).get());
            usedBlockStates.remove(oldIndex);
        } else {
            this.palette.put(oldIndex, newBlockState);
        }
        return true;
    }

    public boolean removeBlockState(IBlockState blockState) {
        final BiMap<IBlockState, Integer> palette = this.palette.inverse();
        if (!palette.containsKey(blockState)) {
            return false;
        }

        final Integer oldIndex = palette.get(blockState);
        this.palette.remove(oldIndex);
        this.usedBlockStates.remove(oldIndex);

        return true;
    }

    @Override
    public ItemStack getIcon() {
        return this.icon;
    }

    @Override
    public void setIcon(final ItemStack icon) {
        if (icon != null) {
            this.icon = icon;
        } else {
            this.icon = DEFAULT_ICON.copy();
        }
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getLength() {
        return this.length;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    private boolean isValid(final BlockPos pos) {
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();

        return !(x < 0 || y < 0 || z < 0 || x >= this.width || y >= this.height || z >= this.length);
    }
}