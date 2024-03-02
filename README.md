# Peelocity

Is a all-in-one solution for you Minecraft server network, which aims to provide all features you need inside just one simple plugin. Being lightweight and efficient, Peelocity tries to provide as many features as possible, while having a simple and easy to maintain codebase.  
The modern codebase using Java 17+ allows for fast response times and always up-to-date code.  
Although the plugin is still in development, it is already a valid choice for any server network, that needs a working and lightweight plugin.  
Read more at https://marcpg.com/peelocity (soon...)!

## Releases

You can find our official releases on these platforms:
- GitHub: [github.com/MarcPG1905/Peelocity/releases](https://github.com/MarcPG1905/Peelocity/releases)
- Modrinth (Recommended): [modrinth.com/plugin/peelocity](https://modrinth.com/plugin/peelocity)
- Hangar: [hangar.papermc.io/MarcPG1905/Peelocity](https://hangar.papermc.io/MarcPG1905/Peelocity)
- SpigotMC: [spigotmc.org/resources/peelocity.115129](https://www.spigotmc.org/resources/peelocity.115129/)
- Planet Minecraft: [planetminecraft.com/mod/peelocity-all-in-one-proxy-plugin](https://www.planetminecraft.com/mod/peelocity-all-in-one-proxy-plugin/)

## Requirements

- **SignedVelocity**: https://modrinth.com/plugin/signedvelocity
- **Java 17 or higher**. Find instructions on how to install or update Java [here](https://docs.papermc.io/misc/java-install).

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
