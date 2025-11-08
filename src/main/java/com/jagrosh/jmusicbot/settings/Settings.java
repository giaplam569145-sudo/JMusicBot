/*
 * Copyright 2016 John Grosh <john.a.grosh@gmail.com>.
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
package com.jagrosh.jmusicbot.settings;

import com.jagrosh.jdautilities.command.GuildSettingsProvider;
import java.util.Collection;
import java.util.Collections;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

/**
 * Manages guild-specific settings for the bot.
 * This class holds settings such as text/voice channels, DJ roles, volume, and default playlists.
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class Settings implements GuildSettingsProvider
{
    private final SettingsManager manager;
    protected long textId;
    protected long voiceId;
    protected long roleId;
    private int volume;
    private String defaultPlaylist;
    private RepeatMode repeatMode;
    private QueueType queueType;
    private String prefix;
    private double skipRatio;

    /**
     * Constructs a new Settings object from string representations of IDs.
     *
     * @param manager         The settings manager.
     * @param textId          The ID of the text channel.
     * @param voiceId         The ID of the voice channel.
     * @param roleId          The ID of the DJ role.
     * @param volume          The volume level.
     * @param defaultPlaylist The default playlist.
     * @param repeatMode      The repeat mode.
     * @param prefix          The command prefix.
     * @param skipRatio       The skip ratio.
     * @param queueType       The queue type.
     */
    public Settings(SettingsManager manager, String textId, String voiceId, String roleId, int volume, String defaultPlaylist, RepeatMode repeatMode, String prefix, double skipRatio, QueueType queueType)
    {
        this.manager = manager;
        try
        {
            this.textId = Long.parseLong(textId);
        }
        catch(NumberFormatException e)
        {
            this.textId = 0;
        }
        try
        {
            this.voiceId = Long.parseLong(voiceId);
        }
        catch(NumberFormatException e)
        {
            this.voiceId = 0;
        }
        try
        {
            this.roleId = Long.parseLong(roleId);
        }
        catch(NumberFormatException e)
        {
            this.roleId = 0;
        }
        this.volume = volume;
        this.defaultPlaylist = defaultPlaylist;
        this.repeatMode = repeatMode;
        this.prefix = prefix;
        this.skipRatio = skipRatio;
        this.queueType = queueType;
    }
    
    /**
     * Constructs a new Settings object from long representations of IDs.
     *
     * @param manager         The settings manager.
     * @param textId          The ID of the text channel.
     * @param voiceId         The ID of the voice channel.
     * @param roleId          The ID of the DJ role.
     * @param volume          The volume level.
     * @param defaultPlaylist The default playlist.
     * @param repeatMode      The repeat mode.
     * @param prefix          The command prefix.
     * @param skipRatio       The skip ratio.
     * @param queueType       The queue type.
     */
    public Settings(SettingsManager manager, long textId, long voiceId, long roleId, int volume, String defaultPlaylist, RepeatMode repeatMode, String prefix, double skipRatio, QueueType queueType)
    {
        this.manager = manager;
        this.textId = textId;
        this.voiceId = voiceId;
        this.roleId = roleId;
        this.volume = volume;
        this.defaultPlaylist = defaultPlaylist;
        this.repeatMode = repeatMode;
        this.prefix = prefix;
        this.skipRatio = skipRatio;
        this.queueType = queueType;
    }
    
    // Getters

    /**
     * Gets the text channel for the guild.
     *
     * @param guild The guild.
     * @return The {@link TextChannel}, or null if not set.
     */
    public TextChannel getTextChannel(Guild guild)
    {
        return guild == null ? null : guild.getTextChannelById(textId);
    }
    
    /**
     * Gets the voice channel for the guild.
     *
     * @param guild The guild.
     * @return The {@link VoiceChannel}, or null if not set.
     */
    public VoiceChannel getVoiceChannel(Guild guild)
    {
        return guild == null ? null : guild.getVoiceChannelById(voiceId);
    }
    
    /**
     * Gets the DJ role for the guild.
     *
     * @param guild The guild.
     * @return The {@link Role}, or null if not set.
     */
    public Role getRole(Guild guild)
    {
        return guild == null ? null : guild.getRoleById(roleId);
    }
    
    /**
     * Gets the volume.
     *
     * @return The volume level.
     */
    public int getVolume()
    {
        return volume;
    }
    
    /**
     * Gets the default playlist.
     *
     * @return The default playlist name.
     */
    public String getDefaultPlaylist()
    {
        return defaultPlaylist;
    }
    
    /**
     * Gets the repeat mode.
     *
     * @return The {@link RepeatMode}.
     */
    public RepeatMode getRepeatMode()
    {
        return repeatMode;
    }
    
    /**
     * Gets the command prefix.
     *
     * @return The prefix.
     */
    public String getPrefix()
    {
        return prefix;
    }
    
    /**
     * Gets the skip ratio.
     *
     * @return The skip ratio.
     */
    public double getSkipRatio()
    {
        return skipRatio;
    }

    /**
     * Gets the queue type.
     *
     * @return The {@link QueueType}.
     */
    public QueueType getQueueType()
    {
        return queueType;
    }

    @Override
    public Collection<String> getPrefixes()
    {
        return prefix == null ? Collections.emptySet() : Collections.singleton(prefix);
    }
    
    // Setters

    /**
     * Sets the text channel.
     *
     * @param tc The text channel to set.
     */
    public void setTextChannel(TextChannel tc)
    {
        this.textId = tc == null ? 0 : tc.getIdLong();
        this.manager.writeSettings();
    }
    
    /**
     * Sets the voice channel.
     *
     * @param vc The voice channel to set.
     */
    public void setVoiceChannel(VoiceChannel vc)
    {
        this.voiceId = vc == null ? 0 : vc.getIdLong();
        this.manager.writeSettings();
    }
    
    /**
     * Sets the DJ role.
     *
     * @param role The DJ role to set.
     */
    public void setDJRole(Role role)
    {
        this.roleId = role == null ? 0 : role.getIdLong();
        this.manager.writeSettings();
    }
    
    /**
     * Sets the volume.
     *
     * @param volume The volume level to set.
     */
    public void setVolume(int volume)
    {
        this.volume = volume;
        this.manager.writeSettings();
    }
    
    /**
     * Sets the default playlist.
     *
     * @param defaultPlaylist The default playlist name to set.
     */
    public void setDefaultPlaylist(String defaultPlaylist)
    {
        this.defaultPlaylist = defaultPlaylist;
        this.manager.writeSettings();
    }
    
    /**
     * Sets the repeat mode.
     *
     * @param mode The repeat mode to set.
     */
    public void setRepeatMode(RepeatMode mode)
    {
        this.repeatMode = mode;
        this.manager.writeSettings();
    }
    
    /**
     * Sets the command prefix.
     *
     * @param prefix The prefix to set.
     */
    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
        this.manager.writeSettings();
    }

    /**
     * Sets the skip ratio.
     *
     * @param skipRatio The skip ratio to set.
     */
    public void setSkipRatio(double skipRatio)
    {
        this.skipRatio = skipRatio;
        this.manager.writeSettings();
    }

    /**
     * Sets the queue type.
     *
     * @param queueType The queue type to set.
     */
    public void setQueueType(QueueType queueType)
    {
        this.queueType = queueType;
        this.manager.writeSettings();
    }
}
