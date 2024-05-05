package com.marcpg.ink;

import com.marcpg.common.Configuration;
import com.marcpg.common.Pooper;
import com.marcpg.common.features.MessageLogging;
import com.marcpg.common.platform.CommandManager;
import com.marcpg.common.platform.EventManager;
import com.marcpg.ink.common.PaperCommandManager;
import com.marcpg.ink.common.PaperEventManager;
import com.marcpg.ink.features.PaperChatUtilities;
import com.marcpg.ink.features.PaperServerList;
import com.marcpg.ink.features.VeinMining;
import com.marcpg.ink.moderation.*;
import com.marcpg.ink.social.PaperFriendSystem;
import dev.dejvokep.boostedyaml.YamlDocument;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class Ink extends Pooper<InkPlugin, Listener, CommandExecutor> {
    protected Ink(InkPlugin instance) {
        super(instance, new PaperEventManager(), new PaperCommandManager());
    }

    @Override
    public void extraConfiguration(@NotNull YamlDocument doc) {
        if (doc.getBoolean("vein-mining.enabled")) {
            if (doc.getBoolean("vein-mining.auto-fill-ores")) {
                VeinMining.veinBlocks.addAll(Arrays.stream(Material.values()).filter(m -> m.name().endsWith("_ORE")).toList());
            }
            for (String materialName : doc.getStringList("vein-mining.ores")) {
                try {
                    VeinMining.veinBlocks.add(Material.valueOf(materialName.toUpperCase().replace(" ", "_").replace("-", "_")));
                } catch (IllegalArgumentException ignored) {}
            }

            VeinMining.requireProperTool = doc.getBoolean("vein-mining.require-proper-tool");
            VeinMining.animated = doc.getBoolean("vein-mining.animated");
            VeinMining.maximumDistance = doc.getInt("vein-mining.max-distance");
        }
    }

    @Override
    public void events(EventManager<Listener, InkPlugin> manager) {
        super.events(manager);
        manager.register(plugin, new PaperBanning());
        manager.register(plugin, new PaperMuting());
        manager.register(plugin, new BasicEvents());
        if (Configuration.chatUtilities.getBoolean("enabled"))
            manager.register(plugin, new PaperChatUtilities());
        if (Configuration.doc.getBoolean("server-list.enabled"))
            manager.register(plugin, new PaperServerList());
        if (Configuration.doc.getBoolean("vein-mining.enabled"))
            manager.register(plugin, new VeinMining());
    }

    @Override
    public void commands(CommandManager<CommandExecutor, InkPlugin> manager) {
        super.commands(manager);
        manager.register(plugin, "ban", new PaperBanning.BanCommand());
        manager.register(plugin, "config", new Commands.ConfigCommand());
        manager.register(plugin, "friend", new PaperFriendSystem());
        manager.register(plugin, "ink", new Commands.InkCommand());
        manager.register(plugin, "kick", new PaperKicking());
        manager.register(plugin, "mute", new PaperMuting.MuteCommand());
        manager.register(plugin, "pardon", new PaperBanning.PardonCommand());
        manager.register(plugin, "report", new PaperReporting());
        manager.register(plugin, "staff", new PaperStaffChat());
        manager.register(plugin, "unmute", new PaperMuting.UnmuteCommand());
        if (MessageLogging.enabled)
            manager.register(plugin, "msg-hist", new Commands.MsgHistCommand());
    }
}
