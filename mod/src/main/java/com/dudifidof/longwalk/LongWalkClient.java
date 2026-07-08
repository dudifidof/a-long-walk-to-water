package com.dudifidof.longwalk;

import com.dudifidof.longwalk.command.RunCommand;
import com.dudifidof.longwalk.command.TntCommand;
import com.dudifidof.longwalk.navigation.AutoWalker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class LongWalkClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        RunCommand.register();
        TntCommand.register();
        ClientTickEvents.END_CLIENT_TICK.register(AutoWalker::onClientTick);
    }
}
