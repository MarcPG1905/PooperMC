package com.marcpg.peelocity;

import com.alessiodp.libby.LibraryManager;
import com.marcpg.common.Configuration;
import com.marcpg.common.Pooper;
import com.marcpg.common.features.MessageLogging;
import com.marcpg.common.platform.CommandManager;
import com.marcpg.common.platform.EventManager;
import com.marcpg.common.platform.FaviconHandler;
import com.marcpg.common.storage.DatabaseStorage;
import com.marcpg.peelocity.common.VelocityCommandManager;
import com.marcpg.peelocity.common.VelocityEventManager;
import com.marcpg.peelocity.features.VelocityChatUtilities;
import com.marcpg.peelocity.features.VelocityPrivateMessaging;
import com.marcpg.peelocity.features.VelocityServerList;
import com.marcpg.peelocity.features.VelocityWhitelist;
import com.marcpg.peelocity.moderation.*;
import com.marcpg.peelocity.social.VelocityFriendSystem;
import com.marcpg.peelocity.social.VelocityPartySystem;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;

import java.io.IOException;

import static com.marcpg.common.Configuration.doc;

public class Peelocity extends Pooper<PeelocityPlugin, Object, Command> {
    protected Peelocity(PeelocityPlugin plugin) {
        super(plugin, new VelocityEventManager(), new VelocityCommandManager());
    }

    @Override
    public void loadBasic(FaviconHandler<?> faviconHandler, LibraryManager libraryManager) throws IOException {
        super.loadBasic(faviconHandler, libraryManager);
        VelocityChatUtilities.signedVelocityInstalled = PeelocityPlugin.SERVER.getPluginManager().isLoaded("signedvelocity");
    }

    @Override
    public void additionalLogic() {
        PeelocityPlugin.SERVER.getChannelRegistrar().register(Joining.PLUGIN_MESSAGE_IDENTIFIER);
        try {
            PlayerCache.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (VelocityWhitelist.STORAGE instanceof DatabaseStorage<String> storage) storage.shutdown();
        try {
            PlayerCache.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void events(EventManager<Object, PeelocityPlugin> manager) {
        super.events(manager);
        manager.register(plugin, new VelocityBanning());
        manager.register(plugin, new VelocityMuting());
        manager.register(plugin, new BasicEvents());
        manager.register(plugin, new VelocityPartySystem());
        manager.register(plugin, new Joining());

        if (Configuration.chatUtilities.getBoolean("enabled"))
            manager.register(plugin, new VelocityChatUtilities());
        if (doc.getBoolean("server-list.enabled"))
            manager.register(plugin, new VelocityServerList());
        if (Configuration.whitelist)
            manager.register(plugin, new VelocityWhitelist());

        manager.register(plugin, new Object() {
            @Subscribe(order = PostOrder.LAST)
            public void onLogin(LoginEvent event) {
                PlayerCache.PLAYERS.put(event.getPlayer().getUniqueId(), event.getPlayer().getUsername());
            }
        });
    }

    @Override
    public void commands(CommandManager<Command, PeelocityPlugin> manager) {
        super.commands(manager);
        manager.register(plugin, "ban", VelocityBanning.banCommand());
        manager.register(plugin, "config", PeelocityPlugin.configCommand(), "peelocity-configuration", "pooper-velocity-configuration");
        manager.register(plugin, "friend", VelocityFriendSystem.command());
        manager.register(plugin, "hub", Joining.hubCommand(), "lobby");
        manager.register(plugin, "join", Joining.joinCommand(), "play");
        manager.register(plugin, "kick", VelocityKicking.command());
        manager.register(plugin, "msg", VelocityPrivateMessaging.msgCommand(), "dm", "tell", "whisper");
        manager.register(plugin, "mute", VelocityMuting.muteCommand());
        manager.register(plugin, "pardon", VelocityBanning.pardonCommand(), "unban");
        manager.register(plugin, "party", VelocityPartySystem.command());
        manager.register(plugin, "peelocity", PeelocityPlugin.command(), "velocity-plugin", "pooper-velocity");
        manager.register(plugin, "report", VelocityReporting.command(), "snitch");
        manager.register(plugin, "staff", VelocityStaffChat.command(), "staff-chat", "sc");
        manager.register(plugin, "unmute", VelocityMuting.unmuteCommand());
        manager.register(plugin, "w", VelocityPrivateMessaging.wCommand(), "reply");
        if (Configuration.whitelist)
            manager.register(plugin, "whitelist", VelocityWhitelist.command());
        if (MessageLogging.enabled)
            manager.register(plugin, "msg-hist", Commands.msgHist(), "message-history", "chat-activity");
    }
}
