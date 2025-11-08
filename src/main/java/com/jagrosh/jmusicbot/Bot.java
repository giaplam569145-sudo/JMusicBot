/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
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
// Modified by giaplam569145-sudo, 2024: Refactored for JDA 5.x compatibility.
package com.jagrosh.jmusicbot;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jmusicbot.audio.AloneInVoiceHandler;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.NowplayingHandler;
import com.jagrosh.jmusicbot.audio.PlayerManager;
import com.jagrosh.jmusicbot.gui.GUI;
import com.jagrosh.jmusicbot.playlist.PlaylistLoader;
import com.jagrosh.jmusicbot.settings.SettingsManager;
import java.util.Objects;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;

/**
 * The main bot class, responsible for coordinating all modules and components of the JMusicBot.
 * This class holds references to managers, handlers, and the JDA instance.
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class Bot
{
    private final EventWaiter waiter;
    private final ScheduledExecutorService threadpool;
    private final BotConfig config;
    private final SettingsManager settings;
    private final PlayerManager players;
    private final PlaylistLoader playlists;
    private final NowplayingHandler nowplaying;
    private final AloneInVoiceHandler aloneInVoiceHandler;
    
    private boolean shuttingDown = false;
    private JDA jda;
    private GUI gui;
    
    /**
     * Constructs the Bot object, initializing all managers and handlers.
     *
     * @param waiter   The EventWaiter for handling asynchronous events.
     * @param config   The bot's configuration settings.
     * @param settings The manager for guild-specific settings.
     */
    public Bot(EventWaiter waiter, BotConfig config, SettingsManager settings)
    {
        this.waiter = waiter;
        this.config = config;
        this.settings = settings;
        this.playlists = new PlaylistLoader(config);
        this.threadpool = Executors.newSingleThreadScheduledExecutor();
        this.players = new PlayerManager(this);
        this.players.init();
        this.nowplaying = new NowplayingHandler(this);
        this.nowplaying.init();
        this.aloneInVoiceHandler = new AloneInVoiceHandler(this);
        this.aloneInVoiceHandler.init();
    }
    
    /**
     * Gets the bot's configuration.
     *
     * @return The {@link BotConfig} object.
     */
    public BotConfig getConfig()
    {
        return config;
    }
    
    /**
     * Gets the settings manager.
     *
     * @return The {@link SettingsManager} object.
     */
    public SettingsManager getSettingsManager()
    {
        return settings;
    }
    
    /**
     * Gets the event waiter.
     *
     * @return The {@link EventWaiter} object.
     */
    public EventWaiter getWaiter()
    {
        return waiter;
    }
    
    /**
     * Gets the scheduled executor service.
     *
     * @return The {@link ScheduledExecutorService} for background tasks.
     */
    public ScheduledExecutorService getThreadpool()
    {
        return threadpool;
    }
    
    /**
     * Gets the player manager.
     *
     * @return The {@link PlayerManager} for handling audio players.
     */
    public PlayerManager getPlayerManager()
    {
        return players;
    }
    
    /**
     * Gets the playlist loader.
     *
     * @return The {@link PlaylistLoader} for managing playlists.
     */
    public PlaylistLoader getPlaylistLoader()
    {
        return playlists;
    }
    
    /**
     * Gets the now playing handler.
     *
     * @return The {@link NowplayingHandler} for managing "now playing" messages.
     */
    public NowplayingHandler getNowplayingHandler()
    {
        return nowplaying;
    }

    /**
     * Gets the alone in voice handler.
     *
     * @return The {@link AloneInVoiceHandler} for managing the bot's behavior when left alone in a voice channel.
     */
    public AloneInVoiceHandler getAloneInVoiceHandler()
    {
        return aloneInVoiceHandler;
    }
    
    /**
     * Gets the JDA instance.
     *
     * @return The {@link JDA} object.
     */
    public JDA getJDA()
    {
        return jda;
    }
    
    /**
     * Closes the audio connection for a specific guild.
     *
     * @param guildId The ID of the guild.
     */
    public void closeAudioConnection(long guildId)
    {
        Guild guild = jda.getGuildById(guildId);
        if(guild!=null)
            threadpool.submit(() -> guild.getAudioManager().closeAudioConnection());
    }
    
    /**
     * Resets the bot's Rich Presence (game status).
     */
    public void resetGame()
    {
        Activity game = config.getGame()==null || config.getGame().getName().equalsIgnoreCase("none") ? null : config.getGame();
        if(!Objects.equals(jda.getPresence().getActivity(), game))
            jda.getPresence().setActivity(game);
    }

    /**
     * Shuts down the bot, closing all connections and exiting the application.
     */
    public void shutdown()
    {
        if(shuttingDown)
            return;
        shuttingDown = true;
        threadpool.shutdownNow();
        if(jda.getStatus()!=JDA.Status.SHUTTING_DOWN)
        {
            jda.getGuilds().stream().forEach(g -> 
            {
                g.getAudioManager().closeAudioConnection();
                AudioHandler ah = (AudioHandler)g.getAudioManager().getSendingHandler();
                if(ah!=null)
                {
                    ah.stopAndClear();
                    ah.getPlayer().destroy();
                }
            });
            jda.shutdown();
        }
        if(gui!=null)
            gui.dispose();
        System.exit(0);
    }

    /**
     * Sets the JDA instance.
     *
     * @param jda The {@link JDA} instance to set.
     */
    public void setJDA(JDA jda)
    {
        this.jda = jda;
    }
    
    /**
     * Sets the GUI instance.
     *
     * @param gui The {@link GUI} instance to set.
     */
    public void setGUI(GUI gui)
    {
        this.gui = gui;
    }
}
