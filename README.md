![PooperMC](https://marcpg.com/media/banner.png)

# PooperMC

is an all-in-one solution for your Minecraft server network that aims to provide all the features you need in just one simple plugin. Being lightweight and efficient, PooperMC tries to provide as many features as possible while having a simple and easy-to-maintain codebase.  
The modern codebase using Java 17+ allows for fast response times and always-up-to-date code.  
Although the plugin is still in development, it is already a valid choice for any server network that needs a working and lightweight plugin.  
Read more at https://marcpg.com/pooper (soon...)!

## Requirements

- **Java 17 or higher**. Find instructions on how to install or update Java [here](https://docs.papermc.io/misc/java-install).  
- **SignedVelocity** (only for Velocity): https://modrinth.com/plugin/signedvelocity

## Releases

You can find our official releases on these platforms:  
- Modrinth (recommended): [modrinth.com/plugin/pooper](https://modrinth.com/plugin/pooper)  
- GitHub: [github.com/MarcPG1905/PooperMC/releases](https://github.com/MarcPG1905/PooperMC/releases)  
- Hangar: [hangar.papermc.io/MarcPG1905/PooperMC](https://hangar.papermc.io/MarcPG1905/PooperMC)  
- SpigotMC: [spigotmc.org/resources/poopermc.115129](https://www.spigotmc.org/resources/poopermc.115129/)  
- Planet Minecraft: [planetminecraft.com/mod/peelocity-all-in-one-proxy-plugin](https://www.planetminecraft.com/mod/peelocity-all-in-one-proxy-plugin/)  
- CurseForge: [curseforge.com/minecraft/bukkit-plugins/pooper](https://www.curseforge.com/minecraft/bukkit-plugins/pooper)

## Features

Most features can be enabled or disabled in the configuration.  
Some aren't enabled by default, so make sure to check out the configuration!

### Chat Utilities  
Some stuff that improves the chatting experience.  
- **Message Logging:** All messages are logged and can be seen by moderators using `/msg-hist`.  
- **Private Messaging:** You can globally message someone using `/msg PlayerName` or `/w` to reply.  
- **Mentions:** By writing `@PlayerName`, you can ping another player or `@everyone`.  
- **Chat Colors:** You can use MiniMessage's colors and styles in the chat.

### Custom Server List  
Refers to everything you see when looking at the server in your multiplayer server list.  
- **Better MotD:** Simple custom MotDs that use MiniMessage and can be randomized.  
- **Better Favicons:** Custom server icons from image links that can be randomized.  
- **Online Player Count:** You can set a custom or fake online player count, although that's dishonest and not recommended.  
- **Better Player Limit:** You can set the custom max players. It also allows for the maximum number of players to always be one more than the online players.

### Moderation  
Everything moderation-related is also sent to a Discord Webhook.  
- **Banning:** Bans with expiration, permanent bans, and reasons.  
- **Kicking:** Kicks with reasons.  
- **Muting:** Mutes with expiration and reasons.  
- **Reporting:** Easy reports that can be used by anyone and are pretty easy to use.  
- **Staff-Chat:** A staff chat where all staff members (pee.staff permission) can chat privately.

### Social Stuff  
Some social-stuff that's nice for bigger servers with multiple game modes.  
- **Friend-System:** A simple friend system with friend requests and not much usage.  
- **Party-System:** A simple party system with invites, joining matches together, and party chatting.

### Paper-Modules  
All modules are disabled by default and are fully optional. You can enable them in the configuration under `modules`.  
- **Vein-Mining:** Fully configurable vein-mining with animation, proper item damage, etc.  
- **Better Mob-AI:** Allows for stuff like whole groups panicking if one gets hit or generally a more intelligent AI.  
- **Death-Banning:** Will ban or kick a player if they die or get killed. Fully customizable.  
- **No Anvil-Cap:** Will remove the anvil cost limit and instead just make the cost grow slower, starting at level 40, to not reach absurd costs like 500 levels.  
- **Better Sleep:** Will optimize sleep based on configuration, like more realistically skipping the night, more robust required player calculations, or even dreams.  
- **Custom AFK:** A simple AFK feature that can be configured to your liking.  
- **Utility Block Commands:** Commands for opening utility blocks, like the crafting table, a loom, or your ender chest, without needing to place down a block.  
- **Global Ender Chest:** A global ender chest that everyone on the server can access. Acts like a community chest.

### Other  
Some stuff that can't be categorized but is still nice to have.  
- **Global Whitelist:** A whitelist that works on the proxy instead of only the backend servers.  
- **Custom Join-Logic:** Utilities for joining a game mode configured in the configuration, which also support plugin messaging over `pooper:joining`, for lobby plugins.  
- **Player-Cache:** Caches all players with their name and UUID for later use, even if they are offline.

## Usage

### Configuration

The configuration is designed to be simple and descriptive. You can find everything you need inside the configuration file, which ships with useful descriptions.

### Translations

Translations are automatically downloaded over [a simple database](https://marcpg.com/poopermc/translations/), which means that you don't have to do anything except have a stable internet connection. The download itself will only take a few kilobytes on each startup.

### Data Storing

There are multiple ways of storing the data that you can set in the configuration. YAML and RAM both don't require additional configuration.  
But if you want to use a database, you will need to set it up yourself. For simple instructions, please visit the [database help page](https://marcpg.com/poopermc/database) (soon...)!

## Future Goals

Some of the major features that we hope to add in the future are:  
- Simple and lightweight TAB-list utility.  
- A wider range of moderation utilities, such as vanishing.  
- Some features use inventory interfaces instead of pure commands.  
- More translations are needed to make the plugin and all servers using it accessible to anyone.

## Contact

### Discord

You can join one of my Discord communities and just ping me for a quick response:  
- [Hectus Discord](https://discord.gg/txYEmBafB7)

### Direct Contact

If you don't use Discord or would like to contact me otherwise, please rely on one of these methods:  
- E-Mail: [me@marcpg.com](mailto:me@marcpg.com)  
- Discord: `@marcpg1905`

## For Developers

### Used Libraries

PooperMC is made to be as lightweight as possible. We only use three very small utility libraries:  
- [LibPG](https://github.com/MarcPG1905/LibPG) - A utility library provided by MarcPG that has various features, like downloads, pairs, time formats, etc.  
- [boosted-yaml](https://github.com/dejvokep/boosted-yaml) - Provides a reliable and lightweight configuration library with file versions and more.  
- [libby](https://github.com/AlessioDP/libby) - Allows for dynamically downloading the database drivers to have a smaller file size.
