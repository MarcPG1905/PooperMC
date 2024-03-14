# Peelocity

Is a all-in-one solution for you Minecraft server network, which aims to provide all features you need inside just one simple plugin. Being lightweight and efficient, Peelocity tries to provide as many features as possible, while having a simple and easy to maintain codebase.  
The modern codebase using Java 17+ allows for fast response times and always up-to-date code.  
Although the plugin is still in development, it is already a valid choice for any server network, that needs a working and lightweight plugin.  
Read more at https://marcpg.com/peelocity (soon...)!

## Requirements

- **SignedVelocity**: https://modrinth.com/plugin/signedvelocity
- **Java 17 or higher**. Find instructions on how to install or update Java [here](https://docs.papermc.io/misc/java-install).

## Releases

You can find our official releases on these platforms:
- GitHub: [github.com/MarcPG1905/Peelocity/releases](https://github.com/MarcPG1905/Peelocity/releases)
- Modrinth (Recommended): [modrinth.com/plugin/peelocity](https://modrinth.com/plugin/peelocity)
- Hangar: [hangar.papermc.io/MarcPG1905/Peelocity](https://hangar.papermc.io/MarcPG1905/Peelocity)
- SpigotMC: [spigotmc.org/resources/peelocity.115129](https://www.spigotmc.org/resources/peelocity.115129/)
- Planet Minecraft: [planetminecraft.com/mod/peelocity-all-in-one-proxy-plugin](https://www.planetminecraft.com/mod/peelocity-all-in-one-proxy-plugin/)

## Features

Most features can be enabled/disabled in the configration (`./plugins/peelocity/pee.yml`).  
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
- **Better Player-Limit:** You can set the custom max players. Also allows for the max players always being one more than the online players.

### Moderation
Everything moderation-related is also sent/logged to a Discord webhook.
- Better Bans with expiration, permanent-bans and reasons.
- Better Kicks with reasons.
- Mutes with expiration and reasons.
- Easy Reports that can be used by anyone and are pretty easy to use.
- A staff chat, where all staff members (pee.staff permission) can chat privately.

### Social Stuff
- A simple friend system with friend requests and not much usage.
- A simple party system with invites, joining matches together and party chatting.

### Other
- A global whitelist for the whole proxy, with a command and storage.
- Join logic to join a gamemode configured in the configuration and also supports a plugin message using `peelocity:joining`, for lobby plugins.
- Player-Cache to also have access to offline players instead of only online players.
- An easy-to-use setup that ships right with the plugin JAR. See below for more info!

## Setup

PeeUp, which is the built-in configuration setup is an easy and fast way of setting up your peelocity configuration for your server.

### Running PeeUp

To run PeeUp, you just have to follow a few simple steps.  
It's built-in, which means no additional downloads. Simply run the plugin JAR archive (`Peelocity-?.jar`) as a Java program, like you run your Velocity server:

```shell
java -jar Peelocity.jar
#         ^^^^^^^^^^^^^ <- Your JAR File!
```

Make sure to replace the `plugins/Peelocity-VERSION.jar` with the actual plugin JAR file!

### Requirements

- Running on any Windows or UNIX-based (macOS, Linux, etc.) machine.
- Running in a proper environment. Will not work in IDE or embedded consoles.
- A working keyboard with at least the basic alphabetical and numeral keys.

### Limitations

- Things like the custom MotDs and Favicons need to be configured manually.
- You **cannot** quit the setup by pressing Ctrl+C, as the program temporarily overrides some input methods. (Fixed soon!)

## Usage

### Configuration

The configuration is designed to be simple and descriptive. You can find everything you need inside the configuration file (./plugin/Peelocity/pee.yml), which ships with useful descriptions.

### Translations

Translations are automatically downloaded over [a simple database](https://marcpg.com/peelocity/translations/), which means that you don't have to do anything, except have a stable internet connection. The downloads itself will only take a few kilobytes on each startup.

### Data Storing

There are multiple ways of storing the data, that you can set in the configuration. YAML and RAM both don't require additional configuration.  
But if you want to use a database, you will need to set it up yourself. For simple instructions, please visit the [database help page](https://marcpg.com/peelocity/database) (soon...)!

## Future Goals

Some of the major features that we hope to add in the future are:
- Simple and lightweight TAB-list utility.
- A wider range of moderation utilities, such as vanishing.
- Some features using inventory-interfaces instead of pure commands.
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

Peelocity is made to be as lightweight as possible. We only use two utility libraries, which are very small:
- [LibPG](https://github.com/MarcPG1905/LibPG) - A utility library provided by MarcPG, which has various features, like downloads, pairs, time formats, etc.
- [boosted-yaml](https://github.com/dejvokep/boosted-yaml) - Provides a reliable and lightweight configuration library with file versions, and more.
