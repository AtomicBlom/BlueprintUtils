package com.github.atomicblom.blueprintutils.world.schematic;

import com.github.atomicblom.blueprintutils.api.event.PreSchematicSaveEvent;
import com.github.atomicblom.blueprintutils.reference.Names;
import com.github.atomicblom.blueprintutils.api.ISchematic;
import com.github.atomicblom.blueprintutils.api.event.PostSchematicCaptureEvent;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public abstract class SchematicFormat {
    public static final Map<String, SchematicFormat> FORMATS = new HashMap<String, SchematicFormat>();
    public static String FORMAT_DEFAULT;

    public abstract ISchematic readFromNBT(NBTTagCompound tagCompound);

    public abstract boolean writeToNBT(NBTTagCompound tagCompound, ISchematic schematic);

    public static ISchematic readFromFile(final File file) {
        final Logger logger = LogManager.getLogger("SchematicReader");
        try {


            final NBTTagCompound tagCompound = SchematicUtil.readTagCompoundFromFile(file);

            SchematicFormat schematicFormat = null;
            if (tagCompound.hasKey(Names.NBT.MATERIALS))
            {
                final String format = tagCompound.getString(Names.NBT.MATERIALS);
                schematicFormat = FORMATS.get(format);

                if (schematicFormat == null) {
                    throw new UnsupportedFormatException(format);
                }
            } else {
                if (tagCompound.hasKey("blocks") && tagCompound.hasKey("palette") && tagCompound.hasKey("size")) {
                    //Likely to be Mojang Template format.
                    schematicFormat = FORMATS.get(Names.NBT.FORMAT_TEMPLATE);
                }
            }
            if (schematicFormat == null) {
                throw new UnsupportedFormatException("Unable to identify the format of " + file.getAbsolutePath());
            }

            return schematicFormat.readFromNBT(tagCompound);
        } catch (final Exception ex) {
            logger.error("Failed to read schematic!", ex);
        }

        return null;
    }

    public static ISchematic readFromFile(final File directory, final String filename) {
        return readFromFile(new File(directory, filename));
    }

    public static boolean writeToFile(final File file, final ISchematic schematic) {
        final Logger logger = LogManager.getLogger("SchematicWriter");
        try {
            final PreSchematicSaveEvent event = new PreSchematicSaveEvent(schematic);
            MinecraftForge.EVENT_BUS.post(event);

            final NBTTagCompound tagCompound = new NBTTagCompound();

            final SchematicFormat schematicFormat = FORMATS.get(Names.NBT.FORMAT_ALPHA);
            schematicFormat.writeToNBT(tagCompound, schematic);

            final DataOutputStream dataOutputStream = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(file)));

            try
            {
                final Class<NBTTagCompound> nbtTagCompoundClass = NBTTagCompound.class;
                final Method writeEntry = nbtTagCompoundClass.getDeclaredMethod("writeEntry", String.class, NBTBase.class, DataOutput.class);
                writeEntry.setAccessible(true);
                writeEntry.invoke(null, Names.NBT.ROOT, tagCompound, dataOutputStream);
            } catch (Exception e) {
                logger.error("wtf", e);
            } finally {
                dataOutputStream.close();
            }

            return true;
        } catch (final Exception ex) {
            logger.error("Failed to write schematic!", ex);
        }

        return false;
    }

    public static boolean writeToFile(final File directory, final String filename, final ISchematic schematic) {
        return writeToFile(new File(directory, filename), schematic);
    }

    static {
        // TODO?
        // FORMATS.put(Names.NBT.FORMAT_CLASSIC, new SchematicClassic());
        FORMATS.put(Names.NBT.FORMAT_ALPHA, new SchematicAlpha());
        FORMATS.put(Names.NBT.FORMAT_TEMPLATE, new SchematicTemplate());
        FORMAT_DEFAULT = Names.NBT.FORMAT_ALPHA;
    }
}