package com.marcpg.ink;

import com.marcpg.common.Configuration;
import com.marcpg.common.Pooper;
import com.marcpg.common.features.MessageLogging;
import com.marcpg.common.platform.CommandManager;
import com.marcpg.common.platform.EventManager;
import com.marcpg.ink.common.PaperCommandManager;
import com.marcpg.ink.common.PaperEventManager;
import com.marcpg.ink.features.AntiBookBan;
import com.marcpg.ink.features.PaperChatUtilities;
import com.marcpg.ink.features.PaperServerList;
import com.marcpg.ink.features.PaperTimer;
import com.marcpg.ink.moderation.*;
import com.marcpg.ink.modules.*;
import com.marcpg.ink.social.PaperFriendSystem;
import com.marcpg.libpg.data.time.Time;
import com.marcpg.libpg.util.Randomizer;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Ink extends Pooper<InkPlugin, Listener, CommandExecutor> {
    protected Ink(InkPlugin instance) {
        super(instance, new PaperEventManager(), new PaperCommandManager());
    }

    @Override
    public void extraConfiguration(@NotNull YamlDocument doc) throws IOException {
        if (doc.getBoolean("modules.vein-mining.enabled")) {
            if (doc.getBoolean("modules.vein-mining.auto-fill-ores")) {
                VeinMining.veinBlocks.addAll(Arrays.stream(Material.values()).filter(m -> m.name().endsWith("_ORE")).toList());
            }
            for (String materialName : doc.getStringList("modules.vein-mining.ores")) {
                try {
                    VeinMining.veinBlocks.add(Material.valueOf(materialName.toUpperCase().replace(" ", "_").replace("-", "_")));
                } catch (IllegalArgumentException ignored) {}
            }

            VeinMining.requireProperTool = doc.getBoolean("modules.vein-mining.require-proper-tool");
            VeinMining.animated = doc.getBoolean("modules.vein-mining.animated");
            VeinMining.maximumDistance = doc.getInt("modules.vein-mining.max-distance");
        }

        if (doc.getBoolean("modules.death-banning.enabled")) {
            DeathBanning.onlyKilling = doc.getBoolean("modules.death-banning.only-killing");
            DeathBanning.showDeathMessage = doc.getBoolean("modules.death-banning.show-death-message");

            String duration = doc.getString("modules.death-banning.duration");
            if (duration.equalsIgnoreCase("permanent")) {
                DeathBanning.permanent = true;
            } else if (duration.equalsIgnoreCase("kick")) {
                DeathBanning.duration = new Time(0);
            } else {
                Time time = Time.parse(duration);
                if (time.get() <= 0) {
                    Pooper.LOG.warn("Invalid duration in the configuration at \"modules.death-banning.duration\"!");
                } else {
                    DeathBanning.duration = time;
                }
            }
        }

        if (Configuration.doc.getBoolean("modules.global-ender-chest"))
            GlobalEnderChest.load();

        if (doc.getBoolean("modules.better-mob-ai.enabled")) {
            BetterMobAI.panickingGroups = doc.getBoolean("modules.better-mob-ai.panicking-groups");
        }

        // TODO: if (doc.getBoolean("modules.better-sleep.enabled")) {
        //     BetterSleep.mode = BetterSleep.Mode.valueOf(doc.getString("modules.better-sleep.mode").toUpperCase());
        //     BetterSleep.realisticSleepMultiplier = doc.getDouble("modules.better-sleep.realistic-sleep-multiplier");
        //     BetterSleep.playersRequired = doc.getDouble("modules.better-sleep.players-required");
        // }
    }

    @Override
    public void shutdown() {
        if (Configuration.doc.getBoolean("modules.global-ender-chest"))
            GlobalEnderChest.save();
        super.shutdown();
    }

    @Override
    public void events(EventManager<Listener, InkPlugin> manager) {
        super.events(manager);
        manager.register(plugin, new AntiBookBan());
        manager.register(plugin, new BasicEvents());
        manager.register(plugin, new PaperBanning());
        manager.register(plugin, new PaperMuting());

        if (Configuration.chatUtilities.getBoolean("enabled"))
            manager.register(plugin, new PaperChatUtilities());
        if (Configuration.doc.getBoolean("server-list.enabled"))
            manager.register(plugin, new PaperServerList());
        if (Configuration.doc.getBoolean("modules.vein-mining.enabled"))
            manager.register(plugin, new VeinMining());
        if (Configuration.doc.getBoolean("modules.death-banning.enabled"))
            manager.register(plugin, new DeathBanning());
        if (Configuration.doc.getBoolean("modules.better-mob-ai.enabled"))
            manager.register(plugin, new BetterMobAI());
        if (Configuration.doc.getBoolean("modules.custom-afk.enabled"))
            manager.register(plugin, new CustomAFK());
        if (Configuration.doc.getBoolean("modules.no-anvil-cap"))
            manager.register(plugin, new NoAnvilCap());
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
        manager.register(plugin, "timer", new PaperTimer());
        manager.register(plugin, "unmute", new PaperMuting.UnmuteCommand());

        if (MessageLogging.enabled)
            manager.register(plugin, "msg-hist", new Commands.MsgHistCommand());
        if (Configuration.doc.getBoolean("modules.custom-afk.enabled"))
            manager.register(plugin, "afk", new CustomAFK());
        if (Configuration.doc.getBoolean("modules.global-ender-chest"))
            manager.register(plugin, "global-ender-chest", new GlobalEnderChest());

        if (Configuration.doc.getBoolean("modules.utility-block-commands.enabled")) {
            Section utilityBlockCommands = Configuration.doc.getSection("modules.utility-block-commands");
            if (utilityBlockCommands.getBoolean("anvil"))
                manager.register(plugin, "anvil", UtilityBlockCommand.ANVIL);
            if (utilityBlockCommands.getBoolean("cartography-table"))
                manager.register(plugin, "cartography-table", UtilityBlockCommand.CARTOGRAPHY_TABLE, "cartography");
            if (utilityBlockCommands.getBoolean("workbench"))
                manager.register(plugin, "workbench", UtilityBlockCommand.WORKBENCH, "crafting", "crafting-table");
            if (utilityBlockCommands.getBoolean("grindstone"))
                manager.register(plugin, "grindstone", UtilityBlockCommand.GRINDSTONE);
            if (utilityBlockCommands.getBoolean("loom"))
                manager.register(plugin, "loom", UtilityBlockCommand.LOOM);
            if (utilityBlockCommands.getBoolean("smithing-table"))
                manager.register(plugin, "smithing-table", UtilityBlockCommand.SMITHING_TABLE, "smithing");
            if (utilityBlockCommands.getBoolean("stonecutter"))
                manager.register(plugin, "stonecutter", UtilityBlockCommand.STONECUTTER);
            if (utilityBlockCommands.getBoolean("enchanting"))
                manager.register(plugin, "enchanting", UtilityBlockCommand.ENCHANTING, "enchanter");
            if (utilityBlockCommands.getBoolean("ender-chest"))
                manager.register(plugin, "ender-chest", UtilityBlockCommand.ENDER_CHEST, "ec", "ender");
            if (utilityBlockCommands.getBoolean("trash"))
                manager.register(plugin, "trash", UtilityBlockCommand.TRASH, "rubbish");
        }
    }

    @Override
    public Locale getLocale(Audience audience) {
        return audience instanceof Player player ? player.locale() : Locale.getDefault();
    }

    @Override
    public Audience parseAudience(@NotNull String[] args, Audience sender) {
        List<Audience> audiences = new ArrayList<>();
        for (String arg : args) {
            switch (arg) {
                case "@a" -> audiences.add(Bukkit.getServer());
                case "@s" -> audiences.add(sender);
                case "@r" -> {
                    if (Bukkit.getOnlinePlayers().isEmpty())
                        throw new IllegalArgumentException(arg);
                    audiences.add(Randomizer.fromCollection(Bukkit.getOnlinePlayers()));
                }
                default -> {
                    Player player = Bukkit.getPlayer(arg);
                    if (player == null)
                        throw new IllegalArgumentException(arg);
                    audiences.add(player);
                }
            }
        }
        return Audience.audience(audiences);
    }
}
