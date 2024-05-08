
[![PooperMC](https://marcpg.com/pooper/banner.png)](https://marcpg.com/pooper/)


# PooperMC

Formally known as Peelocity, is an all-in-one solution for your Minecraft server network, which aims to provide all the features you need inside just one simple plugin. Being lightweight and efficient, PooperMC tries to provide as many features as possible, while having a simple and easy-to-maintain codebase.  
The modern codebase using Java 17+ allows for fast response times and always up-to-date code.  
Although the plugin is still in development, it is already a valid choice for any server network, that needs a working and lightweight plugin.  
Read more at https://marcpg.com/poopermc (soon...)!

## Requirements

- **Java 17 or higher**. Find instructions on how to install or update Java [here](https://docs.papermc.io/misc/java-install).
- **SignedVelocity** (Only for Velocity): https://modrinth.com/plugin/signedvelocity

## Releases

You can find our official releases on these platforms:
- Modrinth (Recommended): [modrinth.com/plugin/pooper](https://modrinth.com/plugin/pooper)
- GitHub: [github.com/MarcPG1905/PooperMC/releases](https://github.com/MarcPG1905/PooperMC/releases)
- Hangar: [hangar.papermc.io/MarcPG1905/PooperMC](https://hangar.papermc.io/MarcPG1905/PooperMC)
- SpigotMC: [spigotmc.org/resources/poopermc.115129](https://www.spigotmc.org/resources/poopermc.115129/)
- Planet Minecraft: [planetminecraft.com/mod/peelocity-all-in-one-proxy-plugin](https://www.planetminecraft.com/mod/peelocity-all-in-one-proxy-plugin/)
- CurseForge: [curseforge.com/minecraft/bukkit-plugins/pooper](https://www.curseforge.com/minecraft/bukkit-plugins/pooper)

## Features

Most features can be enabled/disabled in the configuration.  
Some aren't enabled by default, so make sure to check out the configuration!

### Chat Utilities
- **Message Logging:** All messages are logged and can be seen by moderators using `/msg-hist`
- **Private Messaging:** You can globally message someone using `/msg PlayerName` or `/w` to reply.
- **Mentions:** By writing `@PlayerName`, you can ping another player or `@everyone`.
- **Chat Colors:** You can use MiniMessage's colors and styles in the chat.

### Custom Server List
- **Better MotD:** Simple custom MotDs, that use MiniMessage and can be randomized.
- **Better Favicons:** Custom server icons from image links, that can be randomized.
- **Online Player-Count:** You can set a custom/fake online player count, although that's dishonest and not recommended.
- **Better Player-Limit:** You can set the custom max players. Also allows for the max players to always be one more than the online players.

### Moderation
Everything moderation-related is also sent/logged to a Discord-Webhook.
- Better Bans with expiration, permanent bans, and reasons.
- Better Kicks with reasons.
- Mutes with expiration and reasons.
- Easy Reports that can be used by anyone and are pretty easy to use.
- A staff chat, where all staff members (pee.staff permission) can chat privately.

### Social Stuff
- A simple friend system with friend requests and not much usage.
- A simple party system with invites, joining matches together, and party chatting.

### Plugin-Only
- Optional **Vein-Mining**, which is fully configurable with animation, proper item damage, etc.

### Other
- A global whitelist for the whole proxy, with a command and storage.
- Join logic to join a game mode configured in the configuration and also supports a plugin message using `pooper:joining`, for lobby plugins.
- Player-Cache to also have access to offline players instead of only online players.
- An easy-to-use setup that ships right with the plugin JAR. See below for more info!

## Setup

PeeUp, which is a simple configuration setup, is an easy and fast way of setting up your PooperMC configuration for your server.

### Running PeeUp

To run PeeUp, you just have to follow a few simple steps:

1. Download the Setup JAR file.
2. Move the JAR file into your plugins folder or your server folder.
3. Run the Setup:
```shell
java -jar PooperMC-VERSION-Setup.jar
#         ^^^^^^^^^^^^^^^^^^^^^^ <- Your JAR File!
```

Make sure to replace the `PooperMC-VERSION-Setup.jar` with the actual JAR file!

### Requirements

- Running on any Windows or UNIX-based (macOS, Linux, etc.) machine.
- Running in a proper environment. Will not work in IDE or embedded consoles.
- A working keyboard with at least the basic alphabetical and numeral keys.

### Limitations

- Things like the custom MotDs and Favicons need to be configured manually.
- That's it.

## Usage

### Configuration

The configuration is designed to be simple and descriptive. You can find everything you need inside the configuration file, which ships with useful descriptions.

### Translations

Translations are automatically downloaded over [a simple database](https://marcpg.com/poopermc/translations/), which means that you don't have to do anything, except have a stable internet connection. The downloads itself will only take a few kilobytes on each startup.

### Data Storing

There are multiple ways of storing the data, that you can set in the configuration. YAML and RAM both don't require additional configuration.  
But if you want to use a database, you will need to set it up yourself. For simple instructions, please visit the [database help page](https://marcpg.com/poopermc/database) (soon...)!

## Future Goals

Some of the major features that we hope to add in the future are:
- Simple and lightweight TAB-list utility.
- A wider range of moderation utilities, such as vanishing.
- Some features use inventory interfaces instead of pure commands.
- More translations, to make the plugin and all servers using it, accessible to anyone.

## Contact

### Discord

You can join one of my Discord communities and just ping me, for a quick response:
- [.gg/hectus](https://discord.gg/hectus)
- [.gg/XWfa7gvCJ5](https://discord.gg/XWfa7gvCJ5)

### Direct Contact

If you don't use Discord or like to contact me otherwise, please rely on one of these methods:
- E-Mail: [me@marcpg.com](mailto:me@marcpg.com)
- Discord: @marcpg1905

## For Developers

### Used Libraries

PooperMC is made to be as lightweight as possible. We only use three very small utility libraries:
- [LibPG](https://github.com/MarcPG1905/LibPG) - A utility library provided by MarcPG, which has various features, like downloads, pairs, time formats, etc.
- [boosted-yaml](https://github.com/dejvokep/boosted-yaml) - Provides a reliable and lightweight configuration library with file versions, and more.
- [libby](https://github.com/AlessioDP/libby) - Allows for dynamically downloading the database drivers, to have a smaller file size.
