/*
 * Copyright 2024 John Grosh (jagrosh).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.valentinebrother.jmusicbot.config;

import com.valentinebrother.jmusicbot.BotConfig;
import com.valentinebrother.jmusicbot.entities.Prompt;
import com.valentinebrother.jmusicbot.utils.OtherUtil;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Responsible for loading and validating the bot configuration.
 */
public class ConfigLoader
{
    private final static String START_TOKEN = "/// START OF JMUSICBOT CONFIG ///";
    private final static String END_TOKEN = "/// END OF JMUSICBOT CONFIG ///";

    /**
     * Loads the configuration from the file system.
     * @param prompt User prompt for interaction if config is missing.
     * @return Loaded BotConfig or null if invalid.
     */
    public BotConfig loadConfig(Prompt prompt)
    {
        Path path = getConfigPath();

        try
        {
            // load in the config file, plus the default values
            Config config = ConfigFactory.parseFile(path.toFile()).withFallback(ConfigFactory.load());

            BotConfig botConfig = new BotConfig(config, path);

            // Validate and potentially update config (ask for token/owner)
            if (validateAndUpdate(botConfig, prompt, path)) {
                 return botConfig;
            } else {
                 return null;
            }
        }
        catch (ConfigException ex)
        {
            prompt.alert(Prompt.Level.ERROR, "Config", ex + ": " + ex.getMessage() + "\n\nConfig Location: " + path.toAbsolutePath().toString());
            return null;
        }
    }

    private boolean validateAndUpdate(BotConfig botConfig, Prompt prompt, Path path)
    {
        boolean write = false;
        String token = botConfig.getToken();
        long owner = botConfig.getOwnerId();

        // validate bot token
        if(token==null || token.isEmpty() || token.equalsIgnoreCase("BOT_TOKEN_HERE"))
        {
            token = prompt.prompt("Please provide a bot token."
                    + "\nInstructions for obtaining a token can be found here:"
                    + "\nhttps://github.com/jagrosh/MusicBot/wiki/Getting-a-Bot-Token."
                    + "\nBot Token: ");
            if(token==null)
            {
                prompt.alert(Prompt.Level.WARNING, "Config", "No token provided! Exiting.\n\nConfig Location: " + path.toAbsolutePath().toString());
                return false;
            }
            else
            {
                botConfig.setToken(token);
                write = true;
            }
        }

        // validate bot owner
        if(owner<=0)
        {
            try
            {
                owner = Long.parseLong(prompt.prompt("Owner ID was missing, or the provided owner ID is not valid."
                    + "\nPlease provide the User ID of the bot's owner."
                    + "\nInstructions for obtaining your User ID can be found here:"
                    + "\nhttps://github.com/jagrosh/MusicBot/wiki/Finding-Your-User-ID"
                    + "\nOwner User ID: "));
            }
            catch(NumberFormatException | NullPointerException ex)
            {
                owner = 0;
            }
            if(owner<=0)
            {
                prompt.alert(Prompt.Level.ERROR, "Config", "Invalid User ID! Exiting.\n\nConfig Location: " + path.toAbsolutePath().toString());
                return false;
            }
            else
            {
                botConfig.setOwnerId(owner);
                write = true;
            }
        }

        if(write)
            writeToFile(path, token, owner, prompt);

        return true;
    }

    private void writeToFile(Path path, String token, long owner, Prompt prompt)
    {
        byte[] bytes = loadDefaultConfig().replace("BOT_TOKEN_HERE", token)
                .replace("0 // OWNER ID", Long.toString(owner))
                .trim().getBytes();
        try
        {
            Files.write(path, bytes);
        }
        catch(IOException ex)
        {
            prompt.alert(Prompt.Level.WARNING, "Config", "Failed to write new config options to config.txt: "+ex
                + "\nPlease make sure that the files are not on your desktop or some other restricted area.\n\nConfig Location: "
                + path.toAbsolutePath().toString());
        }
    }

    private static String loadDefaultConfig()
    {
        // Note: usage of JMusicBot.class might need change if we decouple strict dependency but it is fine for now.
        // Or we can move this resource loading to OtherUtil fully.
        String original = OtherUtil.loadResource(com.valentinebrother.jmusicbot.JMusicBot.class, "/reference.conf");
        return original==null
                ? "token = BOT_TOKEN_HERE\r\nowner = 0 // OWNER ID"
                : original.substring(original.indexOf(START_TOKEN)+START_TOKEN.length(), original.indexOf(END_TOKEN)).trim();
    }

    private static Path getConfigPath()
    {
        Path path = OtherUtil.getPath(System.getProperty("config.file", System.getProperty("config", "config.txt")));
        if(path.toFile().exists())
        {
            if(System.getProperty("config.file") == null)
                System.setProperty("config.file", System.getProperty("config", path.toAbsolutePath().toString()));
            ConfigFactory.invalidateCaches();
        }
        return path;
    }

    public static void writeDefaultConfig()
    {
        Prompt prompt = new Prompt(null, null, true, true);
        prompt.alert(Prompt.Level.INFO, "JMusicBot Config", "Generating default config file");
        Path path = ConfigLoader.getConfigPath();
        byte[] bytes = ConfigLoader.loadDefaultConfig().getBytes();
        try
        {
            prompt.alert(Prompt.Level.INFO, "JMusicBot Config", "Writing default config file to " + path.toAbsolutePath().toString());
            Files.write(path, bytes);
        }
        catch(Exception ex)
        {
            prompt.alert(Prompt.Level.ERROR, "JMusicBot Config", "An error occurred writing the default config file: " + ex.getMessage());
        }
    }
}
