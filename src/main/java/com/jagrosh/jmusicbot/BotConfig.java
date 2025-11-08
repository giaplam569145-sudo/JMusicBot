/*
 * Copyright 2018 John Grosh (jagrosh)
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
package com.jagrosh.jmusicbot;

import com.jagrosh.jmusicbot.entities.Prompt;
import com.jagrosh.jmusicbot.utils.OtherUtil;
import com.jagrosh.jmusicbot.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.typesafe.config.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

/**
 * Manages the bot's configuration, loading settings from a configuration file.
 * This class handles everything from the bot's token and prefix to playback settings and emojis.
 *
 * @author John Grosh (jagrosh)
 */
public class BotConfig
{
    private final Prompt prompt;
    private final static String CONTEXT = "Config";
    private final static String START_TOKEN = "/// START OF JMUSICBOT CONFIG ///";
    private final static String END_TOKEN = "/// END OF JMUSICBOT CONFIG ///";
    
    private Path path = null;
    private String token, prefix, altprefix, helpWord, playlistsFolder, logLevel,
            successEmoji, warningEmoji, errorEmoji, loadingEmoji, searchingEmoji,
            evalEngine;
    private boolean stayInChannel, songInGame, npImages, updatealerts, useEval, dbots;
    private long owner, maxSeconds, aloneTimeUntilStop;
    private int maxYTPlaylistPages;
    private double skipratio;
    private OnlineStatus status;
    private Activity game;
    private Config aliases, transforms;

    private boolean valid = false;
    
    /**
     * Constructs the BotConfig object.
     *
     * @param prompt The prompt to use for user interaction.
     */
    public BotConfig(Prompt prompt)
    {
        this.prompt = prompt;
    }
    
    /**
     * Loads the configuration from the file.
     */
    public void load()
    {
        valid = false;
        
        // read config from file
        try 
        {
            // get the path to the config, default config.txt
            path = getConfigPath();
            
            // load in the config file, plus the default values
            Config config = ConfigFactory.parseFile(path.toFile()).withFallback(ConfigFactory.load());
            //Config config = ConfigFactory.load();
            
            // set values
            token = config.getString("token");
            prefix = config.getString("prefix");
            altprefix = config.getString("altprefix");
            helpWord = config.getString("help");
            owner = config.getLong("owner");
            successEmoji = config.getString("success");
            warningEmoji = config.getString("warning");
            errorEmoji = config.getString("error");
            loadingEmoji = config.getString("loading");
            searchingEmoji = config.getString("searching");
            game = OtherUtil.parseGame(config.getString("game"));
            status = OtherUtil.parseStatus(config.getString("status"));
            stayInChannel = config.getBoolean("stayinchannel");
            songInGame = config.getBoolean("songinstatus");
            npImages = config.getBoolean("npimages");
            updatealerts = config.getBoolean("updatealerts");
            logLevel = config.getString("loglevel");
            useEval = config.getBoolean("eval");
            evalEngine = config.getString("evalengine");
            maxSeconds = config.getLong("maxtime");
            maxYTPlaylistPages = config.getInt("maxytplaylistpages");
            aloneTimeUntilStop = config.getLong("alonetimeuntilstop");
            playlistsFolder = config.getString("playlistsfolder");
            aliases = config.getConfig("aliases");
            transforms = config.getConfig("transforms");
            skipratio = config.getDouble("skipratio");
            dbots = owner == 113156185389092864L;
            
            // we may need to write a new config file
            boolean write = false;

            // validate bot token
            if(token==null || token.isEmpty() || token.equalsIgnoreCase("BOT_TOKEN_HERE"))
            {
                token = prompt.prompt("Please provide a bot token."
                        + "\nInstructions for obtaining a token can be found here:"
                        + "\nhttps://github.com/jagrosh/MusicBot/wiki/Getting-a-Bot-Token."
                        + "\nBot Token: ");
                if(token==null)
                {
                    prompt.alert(Prompt.Level.WARNING, CONTEXT, "No token provided! Exiting.\n\nConfig Location: " + path.toAbsolutePath().toString());
                    return;
                }
                else
                {
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
                    prompt.alert(Prompt.Level.ERROR, CONTEXT, "Invalid User ID! Exiting.\n\nConfig Location: " + path.toAbsolutePath().toString());
                    return;
                }
                else
                {
                    write = true;
                }
            }
            
            if(write)
                writeToFile();
            
            // if we get through the whole config, it's good to go
            valid = true;
        }
        catch (ConfigException ex)
        {
            prompt.alert(Prompt.Level.ERROR, CONTEXT, ex + ": " + ex.getMessage() + "\n\nConfig Location: " + path.toAbsolutePath().toString());
        }
    }
    
    private void writeToFile()
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
            prompt.alert(Prompt.Level.WARNING, CONTEXT, "Failed to write new config options to config.txt: "+ex
                + "\nPlease make sure that the files are not on your desktop or some other restricted area.\n\nConfig Location: " 
                + path.toAbsolutePath().toString());
        }
    }
    
    private static String loadDefaultConfig()
    {
        String original = OtherUtil.loadResource(new JMusicBot(), "/reference.conf");
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
    
    /**
     * Writes the default configuration file.
     */
    public static void writeDefaultConfig()
    {
        Prompt prompt = new Prompt(null, null, true, true);
        prompt.alert(Prompt.Level.INFO, "JMusicBot Config", "Generating default config file");
        Path path = BotConfig.getConfigPath();
        byte[] bytes = BotConfig.loadDefaultConfig().getBytes();
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
    
    /**
     * Checks if the configuration is valid.
     *
     * @return True if the configuration is valid, false otherwise.
     */
    public boolean isValid()
    {
        return valid;
    }
    
    /**
     * Gets the location of the configuration file.
     *
     * @return The absolute path of the configuration file.
     */
    public String getConfigLocation()
    {
        return path.toFile().getAbsolutePath();
    }
    
    /**
     * Gets the bot's prefix.
     *
     * @return The command prefix.
     */
    public String getPrefix()
    {
        return prefix;
    }
    
    /**
     * Gets the bot's alternative prefix.
     *
     * @return The alternative command prefix.
     */
    public String getAltPrefix()
    {
        return "NONE".equalsIgnoreCase(altprefix) ? null : altprefix;
    }
    
    /**
     * Gets the bot's token.
     *
     * @return The Discord bot token.
     */
    public String getToken()
    {
        return token;
    }
    
    /**
     * Gets the skip ratio.
     *
     * @return The ratio of votes required to skip a song.
     */
    public double getSkipRatio()
    {
        return skipratio;
    }
    
    /**
     * Gets the owner's ID.
     *
     * @return The bot owner's user ID.
     */
    public long getOwnerId()
    {
        return owner;
    }
    
    /**
     * Gets the success emoji.
     *
     * @return The emoji for success messages.
     */
    public String getSuccess()
    {
        return successEmoji;
    }
    
    /**
     * Gets the warning emoji.
     *
     * @return The emoji for warning messages.
     */
    public String getWarning()
    {
        return warningEmoji;
    }
    
    /**
     * Gets the error emoji.
     *
     * @return The emoji for error messages.
     */
    public String getError()
    {
        return errorEmoji;
    }
    
    /**
     * Gets the loading emoji.
     *
     * @return The emoji for loading messages.
     */
    public String getLoading()
    {
        return loadingEmoji;
    }
    
    /**
     * Gets the searching emoji.
     *
     * @return The emoji for searching messages.
     */
    public String getSearching()
    {
        return searchingEmoji;
    }
    
    /**
     * Gets the bot's activity (game).
     *
     * @return The bot's {@link Activity}.
     */
    public Activity getGame()
    {
        return game;
    }
    
    /**
     * Checks if the bot's game is set to "none".
     *
     * @return True if the game is "none", false otherwise.
     */
    public boolean isGameNone()
    {
        return game != null && game.getName().equalsIgnoreCase("none");
    }
    
    /**
     * Gets the bot's online status.
     *
     * @return The bot's {@link OnlineStatus}.
     */
    public OnlineStatus getStatus()
    {
        return status;
    }
    
    /**
     * Gets the help word.
     *
     * @return The help command trigger word.
     */
    public String getHelp()
    {
        return helpWord;
    }
    
    /**
     * Checks if the bot should stay in the voice channel.
     *
     * @return True if the bot should stay, false otherwise.
     */
    public boolean getStay()
    {
        return stayInChannel;
    }
    
    /**
     * Checks if the song should be displayed in the bot's status.
     *
     * @return True if the song should be in the status, false otherwise.
     */
    public boolean getSongInStatus()
    {
        return songInGame;
    }
    
    /**
     * Gets the playlists folder.
     *
     * @return The path to the playlists folder.
     */
    public String getPlaylistsFolder()
    {
        return playlistsFolder;
    }
    
    /**
     * Checks if the bot is configured for DBots.
     *
     * @return True if configured for DBots, false otherwise.
     */
    public boolean getDBots()
    {
        return dbots;
    }
    
    /**
     * Checks if update alerts are enabled.
     *
     * @return True if update alerts are enabled, false otherwise.
     */
    public boolean useUpdateAlerts()
    {
        return updatealerts;
    }

    /**
     * Gets the log level.
     *
     * @return The configured log level.
     */
    public String getLogLevel()
    {
        return logLevel;
    }

    /**
     * Checks if the eval command is enabled.
     *
     * @return True if eval is enabled, false otherwise.
     */
    public boolean useEval()
    {
        return useEval;
    }
    
    /**
     * Gets the eval engine.
     *
     * @return The scripting engine for the eval command.
     */
    public String getEvalEngine()
    {
        return evalEngine;
    }
    
    /**
     * Checks if "now playing" images are enabled.
     *
     * @return True if NP images are enabled, false otherwise.
     */
    public boolean useNPImages()
    {
        return npImages;
    }
    
    /**
     * Gets the maximum seconds for a track.
     *
     * @return The maximum allowed track duration in seconds.
     */
    public long getMaxSeconds()
    {
        return maxSeconds;
    }
    
    /**
     * Gets the maximum number of pages for YouTube playlists.
     *
     * @return The maximum number of pages to load from a YouTube playlist.
     */
    public int getMaxYTPlaylistPages()
    {
        return maxYTPlaylistPages;
    }
    
    /**
     * Gets the maximum time for a track in a formatted string.
     *
     * @return The maximum track duration as a formatted string.
     */
    public String getMaxTime()
    {
        return TimeUtil.formatTime(maxSeconds * 1000);
    }

    /**
     * Gets the time until the bot stops when alone in a voice channel.
     *
     * @return The time in seconds.
     */
    public long getAloneTimeUntilStop()
    {
        return aloneTimeUntilStop;
    }
    
    /**
     * Checks if a track is too long.
     *
     * @param track The {@link AudioTrack} to check.
     * @return True if the track is too long, false otherwise.
     */
    public boolean isTooLong(AudioTrack track)
    {
        if(maxSeconds<=0)
            return false;
        return Math.round(track.getDuration()/1000.0) > maxSeconds;
    }

    /**
     * Gets the aliases for a command.
     *
     * @param command The command to get aliases for.
     * @return An array of aliases.
     */
    public String[] getAliases(String command)
    {
        try
        {
            return aliases.getStringList(command).toArray(new String[0]);
        }
        catch(NullPointerException | ConfigException.Missing e)
        {
            return new String[0];
        }
    }
    
    /**
     * Gets the transforms configuration.
     *
     * @return The {@link Config} object for transforms.
     */
    public Config getTransforms()
    {
        return transforms;
    }
}
