package com.marcpg.ink.common;

import com.marcpg.common.platform.CommandManager;
import com.marcpg.ink.InkPlugin;
import org.bukkit.command.CommandExecutor;

import java.util.Objects;

public class PaperCommandManager extends CommandManager<CommandExecutor, InkPlugin> {
    @Override
    public void register(InkPlugin plugin, String name, CommandExecutor command, String... aliases) {
        super.register(plugin, name, command);
        Objects.requireNonNull(plugin.getCommand(name)).setExecutor(command);
    }

    @Override
    public void unregister(InkPlugin plugin, String name) {
        super.unregister(plugin, name);
    }
}
