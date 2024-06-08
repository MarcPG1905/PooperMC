package com.marcpg.ink.modules;

import com.marcpg.common.Pooper;
import com.marcpg.common.moderation.Banning;
import com.marcpg.common.util.InvalidCommandArgsException;
import com.marcpg.ink.common.PaperPlayer;
import com.marcpg.libpg.data.time.Time;
import com.marcpg.libpg.lang.Translation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DeathBanning implements Listener {
    public static Time duration;
    public static boolean permanent;
    public static boolean onlyKilling;
    public static boolean showDeathMessage;

    @EventHandler
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (onlyKilling && !(player.getKiller() instanceof Player)) return;

        Component reason = Translation.component(player.locale(), "module.death_banning.title").color(NamedTextColor.RED)
                .append(showDeathMessage && event.deathMessage() != null ? Component.newline().append(Objects.requireNonNull(event.deathMessage())).color(NamedTextColor.GRAY) : Component.empty());

        if (duration.get() > 0) { // Banning
            try {
                Banning.ban(event.getEntity().getName(), PaperPlayer.ofPlayer(player), permanent, duration, reason);
            } catch (InvalidCommandArgsException e) {
                Pooper.LOG.error("Could not death-ban the player \"" + player.getName() + "\": " + e.getMessage());
            }
        } else { // Kicking
            player.kick(reason);
        }
    }
}
