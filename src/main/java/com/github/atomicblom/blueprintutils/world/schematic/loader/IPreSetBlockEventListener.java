package com.github.atomicblom.blueprintutils.world.schematic.loader;

import com.github.atomicblom.blueprintutils.api.event.PreSetBlockEvent;

/**
 * Created by codew on 9/10/2016.
 */
public interface IPreSetBlockEventListener
{
    void preBlockSet(PreSetBlockEvent event);
}
