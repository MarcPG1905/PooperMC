package com.marcpg.ink;

import com.marcpg.common.Configuration;
import com.marcpg.common.Pooper;
import com.marcpg.common.features.MessageLogging;
import com.marcpg.common.platform.CommandManager;
import com.marcpg.common.platform.EventManager;
import com.marcpg.ink.common.PaperCommandManager;
import com.marcpg.ink.common.PaperEventManager;
import com.marcpg.ink.features.*;
import com.marcpg.ink.moderation.*;
import com.marcpg.ink.modules.BetterMobAI;
import com.marcpg.ink.modules.DeathBanning;
import com.marcpg.ink.modules.NoAnvilCap;
import com.marcpg.ink.social.PaperFriendSystem;
import com.marcpg.libpg.data.time.Time;
import com.marcpg.libpg.util.Randomizer;
import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Ink extends Pooper<InkPlugin, Listener, CommandExecutor> {
    protected Ink(InkPlugin instance) {
        super(instance, new PaperEventManager(), new PaperCommandManager());
    }

    @Override
    public void additionalLogic() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, PeelocityChecker.CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, PeelocityChecker.CHANNEL, new PeelocityChecker());

        Pooper.SCHEDULER.delayed(() -> Bukkit.getOnlinePlayers().stream().findFirst().ifPresent(PeelocityChecker::check), new Time(1, Time.Unit.MINUTES));
    }

    @Override
    public void shutdown() {
        super.shutdown();

        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, PeelocityChecker.CHANNEL);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, PeelocityChecker.CHANNEL);
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

        if (doc.getBoolean("modules.better-mob-ai.enabled")) {
            BetterMobAI.panickingGroups = doc.getBoolean("modules.better-mob-ai.panicking-groups");
            // TODO: BetterMobAI.fightingInstinct = doc.getBoolean("modules.better-mob-ai.fighting-instinct");

            // TODO: for (Map.Entry<String, Object> entity : doc.getSection("modules.better-mob-ai.mobs").getStringRouteMappedValues(false).entrySet()) {
            //     try {
            //         BetterMobAI.enabledMobs.put(EntityType.valueOf(entity.getKey().toUpperCase()), (Boolean) entity.getValue());
            //     } catch (IllegalArgumentException e) {
            //         Pooper.LOG.warn("Invalid entity type in the configuration at \"modules.better-mob-ai.mobs." + entity.getKey() + "\"!");
            //     }
            // }
        }

        // TODO: if (doc.getBoolean("modules.better-sleep.enabled")) {
        //     BetterSleep.mode = BetterSleep.Mode.valueOf(doc.getString("modules.better-sleep.mode").toUpperCase());
        //     BetterSleep.realisticSleepMultiplier = doc.getDouble("modules.better-sleep.realistic-sleep-multiplier");
        //     BetterSleep.playersRequired = doc.getDouble("modules.better-sleep.players-required");
        // }
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
        if (Configuration.doc.getBoolean("vein-mining.enabled"))
            manager.register(plugin, new VeinMining());
        if (Configuration.doc.getBoolean("modules.death-banning.enabled"))
            manager.register(plugin, new DeathBanning());
        if (Configuration.doc.getBoolean("modules.better-mob-ai.enabled"))
            manager.register(plugin, new BetterMobAI());
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
