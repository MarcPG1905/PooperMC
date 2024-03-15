package com.marcpg.peelocity;

import com.marcpg.color.Ansi;
import com.marcpg.formular.CLIFormular;
import com.marcpg.formular.question.BooleanQuestion;
import com.marcpg.formular.question.IntegerQuestion;
import com.marcpg.formular.question.MultipleChoiceQuestion;
import com.marcpg.formular.question.TextQuestion;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.route.Route;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

public class PeeUp {
    private static final File file = Paths.get("").toAbsolutePath().resolve("peelocity/pee.yml").toFile();
    private static final Map<String, String> storageMethods = Map.of(
            "Database", "database",
            "YAML (.yml)", "yaml",
            "RAM (restarting will erase data!)", "ram"
    );
    private static final Map<String, String> databaseTypes = Map.of(
            "PostgreSQL (Recommended)", "postgresql",
            "MariaDB (Preferred over MySQL)", "mariadb",
            "MySQL", "mysql",
            "Microsoft SQL Server", "ms_sql_server",
            "Oracle Database", "oracle"
    );

    public static void main(String[] args) {
        if (!file.exists())  {
            System.out.println(Ansi.red("You need to start the plugin one time before running the setup, so the configuration is created!"));
            System.out.println(Ansi.gray("Exiting..."));
            return;
        }

        new CLIFormular(
                "General - PeeUp (Peelocity Setup)",
                "An easy tool for setting up Peelocity's configuration to work with your server.", Color.YELLOW,
                result -> {
                    try {
                        YamlDocument doc = YamlDocument.create(file, GeneralSettings.DEFAULT, DumperSettings.DEFAULT);
                        System.out.println(Ansi.green("\nApplying your configuration..."));
                        result.toIterator().forEachRemaining(r -> {
                            doc.set(Route.fromString(r.id(), '_'), r.id().equals("storage-method") ? storageMethods.get((String) r.result()) : (r.id().equals("database_type") ? databaseTypes.get((String) r.result()) : r.result()));
                            System.out.println(Ansi.gray("Setting " + r.id() + " to " + r.result().toString()));
                        });
                        doc.save();
                        System.out.println(Ansi.green("Done!"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                new TextQuestion("moderator-webhook", "Moderation Webhook", "The Moderation Discord/Guilded webhook that all moderation-related messages will be sent to, such as bans, kicks, mutes, reports, etc. \nSet to 'none' to disable this module!", 150),
                new BooleanQuestion("enable-translations", "Enable Translations?", "Should we download translations other than en_US, which is the default language?"),
                new BooleanQuestion("global-chat", "Global Chatting?", "Should all messages sent by players be sent to the whole proxy server, instead of only the player's current backend-server? May produce lag on very high player counts (20k+ players).", false),
                new BooleanQuestion("whitelist-enabled", "Enable the Whitelist?", "Enables a server-wide whitelist. To add players, you can use '/whitelist add PlayerName'.", false),


                new MultipleChoiceQuestion("storage-method", "Storage Type", "How should things like the whitelist, bans, mutes, etc. be stored?", storageMethods.keySet().toArray(String[]::new)),

                new MultipleChoiceQuestion("database_type", "Database Type", "The type of database that you're using. Supported types will be expanded soon!", databaseTypes.keySet().toArray(String[]::new)).setRequirement("storage-method", "Database"),
                new TextQuestion("database_address", "Address", "Your database's IP, without a port or anything. Just 'localhost', the IPv4 address (123.123.123.123) or a domain name (example.com).", 254).setRequirement("storage-method", "Database"),
                new IntegerQuestion("database_port", "Port", "The port your database runs on. Set to 0 if you want to use your database type's default port!", 0, 65535).setRequirement("storage-method", "Database"),
                new TextQuestion("database_database", "Database Name", "Your database's name, which is the name that you also use to log into the database in your console or at the end of the connection URL.").setRequirement("storage-method", "Database"),
                new TextQuestion("database_user", "Username", "The username that you use to log in to the database. For security, this should be a special user with only permissions to do basic operations and create tables.").setRequirement("storage-method", "Database"),
                new TextQuestion("database_passwd", "Password", "The password of the user account of the username you entered before.").setRequirement("storage-method", "Database"),


                new BooleanQuestion("message-logging_enabled", "Enable Message Logging", "Enables logging all messages sent by players, including party chats, private messages, etc. Message history can be accessed by moderators using the /msg-hist command. This is stored using a simple file-based approach, so it will not use the specified storage-method, but plain text."),
                new IntegerQuestion("message-logging_max-history", "Max Message Logging History", "How many messages are stored for each player. A length of about 20k will result in 1MB for each player on average. \nDefault is 50, which is about 2.5KB per player.", 0, Long.MAX_VALUE).setRequirement("message-logging_enabled", true),


                new BooleanQuestion("chatutility_enabled", "Enable Chat Utilities", "Enables chat utilities, which provide chat colors, styles and mentions/pings. The different features can be configured separately, if this is enabled."),

                new BooleanQuestion("chatutility_colors_enabled", "Enable Chat Colors", "Enabled colors in the chat. They use the MiniMessage format."),
                new BooleanQuestion("chatutility_colors_styles", "Enable Chat Styles", "Enabled styles, like bold and italic in the chat. They use the MiniMessage format.", false).setRequirement("chatutility_colors_enabled", true),
                new BooleanQuestion("chatutility_colors_permission", "Chat Colors/Styles require a permission?", "Sets if the colors/styles require the 'pee.chat.colors' permission.", false).setRequirement("chatutility_colors_enabled", true),

                new BooleanQuestion("chatutility_mentions_enabled", "Enable Mentioning/Pinging", "Enabled mentions/pings in the chat using @PlayerName."),
                new BooleanQuestion("chatutility_mentions_permission", "Mentions/Pings require a permission?", "Sets if the mentions/pings require the 'pee.chat.mention' permission.", false).setRequirement("chatutility_mentions_enabled", true),
                new BooleanQuestion("chatutility_mentions_everyone_enabled", "Enable @Everyone", "Enables the usage of @everyone, which pings everyone on the server.").setRequirement("chatutility_mentions_enabled", true),
                new BooleanQuestion("chatutility_mentions_everyone_permission", "@Everyone requires a permission?", "Sets if the @everyone requires the 'pee.chat.mention' permission.").setRequirement("chatutility_mentions_everyone_enabled", true),


                new BooleanQuestion("server-list_enabled", "Enable custom Server-List", "Enables the custom server-list, which provides features such as custom and random MotDs and favicons, custom player counts, and more."),
                new BooleanQuestion("server-list_custom-motd", "Enable custom MotD", "Enables the custom MotD (server description), which can be randomized. If you enable this, you have to configure it manually!").setRequirement("server-list_enabled", true),
                new BooleanQuestion("server-list_custom-favicon", "Enable custom Favicon", "Enables the custom Favicon (server icon), which can be randomized. If you enable this, you have to configure it manually!").setRequirement("server-list_enabled", true),
                new IntegerQuestion("server-list_show-max-players", "Custom Max-Players", "Sets the custom maximum player count. This is only cosmetic and doesn't actually limit how many players can join! \nSet to -1 to disable this module. \nSet to -2 to always show one more than the player count.", -2, Long.MAX_VALUE).setRequirement("server-list_enabled", true),
                new IntegerQuestion("server-list_show-current-players", "Custom Current-Players", "Sets the custom current player count. Shouldn't be used on big servers, as it's dishonest and can destroy your community's trust! \nSet to -1 to disable this module. (recommended) \nSet to -2 to hide the player count and just display '???'.", -2, Long.MAX_VALUE).setRequirement("server-list_enabled", true)
        ).render();
    }
}
