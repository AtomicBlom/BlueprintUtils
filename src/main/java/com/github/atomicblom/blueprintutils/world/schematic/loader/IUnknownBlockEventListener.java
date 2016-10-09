package com.github.atomicblom.blueprintutils.world.schematic.loader;

import com.github.atomicblom.blueprintutils.api.event.UnknownBlockEvent;

/**
 * Created by codew on 9/10/2016.
 */
public interface IUnknownBlockEventListener
{
    void unknownBlock(UnknownBlockEvent event);
}
