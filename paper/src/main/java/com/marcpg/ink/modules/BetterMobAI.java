package com.marcpg.ink.modules;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.MobGoals;
import com.destroystokyo.paper.entity.ai.VanillaGoal;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class BetterMobAI implements Listener {
    public enum Mode {
        REVENGE, // Hit Once
        REVENGE_WITH_OTHERS, // Find others, then each hit once.
        KILL, // Kill Attacker
        KILL_WITH_OTHERS // Kill Attacker with Help of other Mobs.
    }

    public static final Random RANDOM = new Random();
    public static final Map<EntityType, Boolean> enabledMobs = new HashMap<>();

    public static boolean panickingGroups;
    public static boolean fightingInstinct;
    public static Mode fightingInstinctMode;

    @EventHandler
    private void onEntityAddToWorld(@NotNull EntityAddToWorldEvent event) {
        // TODO: Give friendly mobs the custom AI with retaliation or killing.
        Entity entity = event.getEntity();
        if (!enabledMobs.containsKey(entity.getType()) || !(entity instanceof Creature creature)) return;

        creature.registerAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        Objects.requireNonNull(creature.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(2.0);

        MobGoals goals = Bukkit.getMobGoals();
        goals.removeGoal(creature, VanillaGoal.PANIC);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(@NotNull EntityDamageByEntityEvent event) {
        if (fightingInstinct) {
            Entity damager = event.getDamager();
            if (!(damager instanceof Creature mob) || damager instanceof Monster) return;
            if (fightingInstinctMode == Mode.REVENGE) {
                mob.setTarget(null);
                mob.setKiller(null);
                Goal<?> goal = Bukkit.getMobGoals().getGoal(mob, VanillaGoal.HURT_BY_TARGET);
                if (goal != null) goal.stop();
            }
        }
        if (panickingGroups && event.getEntity() instanceof Animals animal && event.getDamager() instanceof Player) {
            // This is the only way of panicking that doesn't require NMS, that I found.
            List<Entity> entities = animal.getNearbyEntities(15, 15, 15);
            for (Entity e : entities) {
                if (e instanceof Mob mob && e.getType() == event.getEntityType() && e != animal) {
                    mob.getPathfinder().moveTo(randomLocation(mob.getLocation()), 1.75);
                }
            }
        }
    }

    public Location randomLocation(@NotNull Location base) {
        return new Location(base.getWorld(), base.getX() + RANDOM.nextDouble(-10.0, 10.0), base.getY(), base.getZ() + RANDOM.nextDouble(-10.0, 10.0));
    }
}
