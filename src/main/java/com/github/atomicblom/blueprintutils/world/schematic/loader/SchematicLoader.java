package com.github.atomicblom.blueprintutils.world.schematic.loader;

import com.github.atomicblom.blueprintutils.api.ISchematic;
import com.github.atomicblom.blueprintutils.api.event.PreSetBlockEvent;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by codew on 9/10/2016.
 */
public class SchematicLoader
{
    private final Logger _logger;
    private List<ITileEntityLoadedEvent> tileEntityLoadedEventListeners = new LinkedList<ITileEntityLoadedEvent>();
    private List<IPreSetBlockEventListener> setBlockEventListeners;
    private List<IUnknownBlockEventListener> unknownBlockEventListener;

    public SchematicLoader()
    {
        _logger = LogManager.getLogger("SchematicLoader");
    }

    public void renderSchematicToSingleChunk(ISchematic schematic, World world,
                                             BlockPos origin,
                                             int chunkX, int chunkZ)
    {
        final int minX = origin.getX();
        final int maxX = minX + schematic.getWidth();
        final int minY = origin.getY();
        final int maxY = minY + schematic.getHeight();
        final int minZ = origin.getZ();
        final int maxZ = minZ + schematic.getLength();

        final int localMinX = minX < (chunkX << 4) ? 0 : (minX & 15);
        final int localMaxX = maxX > ((chunkX << 4) + 15) ? 15 : (maxX & 15);
        final int localMinZ = minZ < (chunkZ << 4) ? 0 : (minZ & 15);
        final int localMaxZ = maxZ > ((chunkZ << 4) + 15) ? 15 : (maxZ & 15);

        Chunk c = world.getChunkFromChunkCoords(chunkX, chunkZ);

        int blockCount = 0;
        Block ignore = Blocks.AIR;

        LinkedList<TileEntity> createdTileEntities = new LinkedList<TileEntity>();

        for (int chunkLocalZ = localMinZ; chunkLocalZ <= localMaxZ; chunkLocalZ++)
        {
            for (int y = minY; y < maxY; y++)
            {
                for (int chunkLocalX = localMinX; chunkLocalX <= localMaxX; chunkLocalX++)
                {
                    ++blockCount;
                    final int x = chunkLocalX | (chunkX << 4);
                    final int z = chunkLocalZ | (chunkZ << 4);

                    final int schematicX = x - minX;
                    final int schematicY = y - minY;
                    final int schematicZ = z - minZ;

                    try
                    {
                        BlockPos worldCoord = new BlockPos(x, y, z);
                        BlockPos schematicCoord = new BlockPos(schematicX, schematicY, schematicZ);
                        PreSetBlockEvent event = new PreSetBlockEvent(schematic, world, worldCoord, schematicCoord);

                        final IBlockState blockState = event.getBlockState();

                        if (blockState != null && setBlockEventListeners != null)
                        {
                            for (final IPreSetBlockEventListener listener : setBlockEventListeners)
                            {
                                listener.preBlockSet(event);
                            }
                        }

                        if (event.shouldSetBlock() && blockState != null && c.setBlockState(worldCoord, blockState) != null)
                        {
                            world.markAndNotifyBlock(new BlockPos(x, y, z), c, blockState, blockState, 2);

                            final Block block = blockState.getBlock();
                            if (block.hasTileEntity(blockState))
                            {
                                TileEntity tileEntity = schematic.getTileEntity(schematicCoord);

                                c.addTileEntity(new BlockPos(chunkLocalX, y, chunkLocalZ), tileEntity);
                                tileEntity.getBlockType();
                                try
                                {
                                    tileEntity.validate();
                                } catch (Exception e)
                                {
                                    _logger.error(String.format("TileEntity validation for %s failed!", tileEntity.getClass()), e);
                                }

                                createdTileEntities.add(tileEntity);
                            }
                        }
                    } catch (Exception e)
                    {
                        _logger.error("Something went wrong!", e);
                    }
                }
            }
        }

        for (final TileEntity tileEntity : createdTileEntities)
        {
            for (ITileEntityLoadedEvent tileEntityHandler : tileEntityLoadedEventListeners)
            {
                if (tileEntityHandler.onTileEntityAdded(tileEntity))
                {
                    break;
                }
            }
        }

        c.enqueueRelightChecks();
        c.setChunkModified();

    }

    public void renderSchematicInOneShot(ISchematic schematic, World world, BlockPos pos)
    {
        long start = System.currentTimeMillis();

        boolean useChunkRendering = true;


        if (useChunkRendering)
        {
            int chunkXStart = pos.getX() >> 4;
            int chunkXEnd = ((pos.getX() + schematic.getWidth()) >> 4) + 1;
            int chunkZStart = pos.getZ() >> 4;
            int chunkZEnd = ((pos.getZ() + schematic.getLength()) >> 4) + 1;

            for (int chunkX = chunkXStart; chunkX <= chunkXEnd; ++chunkX)
            {
                for (int chunkZ = chunkZStart; chunkZ <= chunkZEnd; ++chunkZ)
                {
                    renderSchematicToSingleChunk(schematic, world, pos, chunkX, chunkZ);
                }
            }
        } else
        {

            _logger.info(String.format("%s - Setting Blocks", System.currentTimeMillis()));
            for (int schematicZ = 0; schematicZ < schematic.getLength(); ++schematicZ)
            {
                _logger.info(String.format("%s - Working at z = " + schematicZ, System.currentTimeMillis()));
                for (int schematicX = 0; schematicX < schematic.getWidth(); ++schematicX)
                {
                    for (int schematicY = 0; schematicY < schematic.getHeight(); ++schematicY)
                    {
                        final BlockPos worldCoord = pos.add(schematicX, schematicY, schematicZ);

                        IBlockState blockState = schematic.getBlockState(worldCoord);
                        if (blockState.getBlock() != Blocks.AIR)
                        {
                            BlockPos schematicCoord = new BlockPos(schematicX, schematicY, schematicZ);
                            PreSetBlockEvent event = new PreSetBlockEvent(schematic, world, worldCoord, schematicCoord);

                            if (setBlockEventListeners != null)
                            {
                                for (final IPreSetBlockEventListener listener : setBlockEventListeners)
                                {
                                    listener.preBlockSet(event);
                                }
                            }

                            world.setBlockState(worldCoord, event.getBlockState(), 2);
                        }
                    }
                }
            }

            _logger.info(String.format("%s - Creating Tile Entities", System.currentTimeMillis()));
            for (TileEntity tileEntity : schematic.getTileEntities())
            {
                world.setTileEntity(tileEntity.getPos().add(pos), tileEntity);
                tileEntity.getBlockType();
                try
                {
                    tileEntity.validate();
                } catch (Exception e)
                {
                    _logger.error(String.format("TileEntity validation for %s failed!", tileEntity.getClass()), e);
                }

                for (ITileEntityLoadedEvent tileEntityHandler : tileEntityLoadedEventListeners)
                {
                    if (tileEntityHandler.onTileEntityAdded(tileEntity))
                    {
                        break;
                    }
                }
            }
        }

        long end = System.currentTimeMillis();
        _logger.info(String.format("Writing schematic took %d millis", end - start));
    }

    public void addSetBlockEventListener(IPreSetBlockEventListener listener)
    {
        if (setBlockEventListeners == null)
        {
            setBlockEventListeners = new LinkedList<IPreSetBlockEventListener>();
        }
        setBlockEventListeners.add(listener);
    }

    public void addUnknownBlockEventListener(IUnknownBlockEventListener listener)
    {
        if (unknownBlockEventListener == null)
        {
            unknownBlockEventListener = new LinkedList<IUnknownBlockEventListener>();
        }
        unknownBlockEventListener.add(listener);
    }
}
