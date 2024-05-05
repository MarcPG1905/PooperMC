package com.marcpg.peelocity.common;

import com.marcpg.common.platform.CommandManager;
import com.marcpg.peelocity.PeelocityPlugin;
import com.velocitypowered.api.command.Command;

public class VelocityCommandManager extends CommandManager<Command, PeelocityPlugin> {
    @Override
    public void register(PeelocityPlugin plugin, String name, Command command, String... aliases) {
        super.register(plugin, name, command);
        PeelocityPlugin.SERVER.getCommandManager().register(name, command, aliases);
    }

    @Override
    public void unregister(PeelocityPlugin plugin, String name) {
        super.unregister(plugin, name);
        PeelocityPlugin.SERVER.getCommandManager().unregister(name);
    }
}
