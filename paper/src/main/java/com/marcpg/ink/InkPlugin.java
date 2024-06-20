package com.marcpg.ink;

import com.alessiodp.libby.BukkitLibraryManager;
import com.marcpg.common.Platform;
import com.marcpg.common.Pooper;
import com.marcpg.common.logger.SLF4JLogger;
import com.marcpg.ink.common.PaperAsyncScheduler;
import com.marcpg.ink.common.PaperFaviconHandler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class InkPlugin extends JavaPlugin {
    static {
        Pooper.PLATFORM = Platform.PAPER;
        try {
            Class.forName("org.purpurmc.purpur.event.PlayerAFKEvent");
            Pooper.PLATFORM = Platform.PURPUR;
        } catch (ClassNotFoundException ignored) {}
    }

    @Override
    public void onEnable() {
        try { // Ensure that the PaperAPI is supported!
            Class.forName("io.papermc.paper.text.PaperComponents");
        } catch (ClassNotFoundException e) {
            getLogger().severe("==================================================================");
            getLogger().severe("          PooperMC can only run on Paper or forks of it!          ");
            getLogger().severe("Running PooperMC on pure CraftBukkit or SpigotMC is not supported!");
            getLogger().severe("==================================================================");
            throw new RuntimeException("Unsupported platform, read message above!");
        }

        Pooper.LOG = new SLF4JLogger(getSLF4JLogger());
        Pooper.DATA_DIR = getDataFolder().toPath();
        Pooper.SCHEDULER = new PaperAsyncScheduler(this, Bukkit.getScheduler());

        try { // The actual startup logic:
            new Ink(this);
            Pooper.INSTANCE.startup(new PaperFaviconHandler(), new BukkitLibraryManager(this, getDataFolder().getName()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        Pooper.INSTANCE.shutdown();
    }
}
