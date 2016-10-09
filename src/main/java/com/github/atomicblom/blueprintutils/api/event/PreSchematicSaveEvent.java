package com.github.atomicblom.blueprintutils.api.event;

import com.github.atomicblom.blueprintutils.api.ISchematic;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.List;
import java.util.Set;

/**
 * This event is fired after the schematic has been Captured, but before it is serialized to the schematic format.
 * This is your opportunity to add Metadata.
 * Register to this event using MinecraftForge.EVENT_BUS
 */
public class PreSchematicSaveEvent extends Event {

    /**
     * The schematic that will be saved.
     */
    public final ISchematic schematic;

    /**
     * The Extended Metadata tag compound provides a facility to add custom metadata to the schematic.
     */
    public final NBTTagCompound extendedMetadata;

    public PreSchematicSaveEvent(final ISchematic schematic) {
        this.schematic = schematic;
        this.extendedMetadata = new NBTTagCompound();
    }

    /**
     * Replaces the block mapping from one name to another. Use this method with care as it is possible that the schematic
     * will not be usable or will have blocks missing if you use an invalid value.
     *
     * Attempting to remap two blocks to the same name will result in a DuplicateMappingException. If you wish for this
     * type of collision, you can work around it by merging the two sets of block into a single BlockType in the
     * PostSchematicCaptureEvent.
     * @param oldState The old name of the block mapping.
     * @param newState The new name of the block Mapping.
     * @return true if a mapping was replaced.
     */
    public boolean replaceMapping(final IBlockState oldState, final IBlockState newState) {
        return schematic.remapBlockState(oldState, newState);
    }

    /**
     * Marks a blockState as not present in a schematic. Use {see getBlockStateUnsafe}
     * to determine which blocks are effected
     *
     * @param blockState
     * @return
     */
    public boolean removeMapping(final IBlockState blockState) {
        return schematic.removeBlockState(blockState);
    }
}