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
package com.jagrosh.jmusicbot.utils;

import com.jagrosh.jmusicbot.audio.RequestMetadata.UserInfo;
import java.util.List;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

/**
 * A utility class for formatting various strings and creating lists for display in Discord.
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class FormatUtil {

    /**
     * Formats a username with a discriminator.
     *
     * @param username The username.
     * @param discrim  The discriminator.
     * @return The formatted username.
     */
    public static String formatUsername(String username, String discrim)
    {
        if(discrim == null || discrim.equals("0000"))
        {
            return username;
        }
        else
        {
            return username + "#" + discrim;
        }
    }

    /**
     * Formats a username from a {@link UserInfo} object.
     *
     * @param userinfo The user info.
     * @return The formatted username.
     */
    public static String formatUsername(UserInfo userinfo)
    {
        return formatUsername(userinfo.username, userinfo.discrim);
    }

    /**
     * Formats a username from a {@link User} object.
     *
     * @param user The user.
     * @return The formatted username.
     */
    public static String formatUsername(User user)
    {
        return formatUsername(user.getName(), user.getDiscriminator());
    }

    /**
     * Creates a progress bar.
     *
     * @param percent The percentage to display.
     * @return A string representing the progress bar.
     */
    public static String progressBar(double percent)
    {
        String str = "";
        for(int i=0; i<12; i++)
            if(i == (int)(percent*12))
                str+="\uD83D\uDD18"; // 🔘
            else
                str+="▬";
        return str;
    }
    
    /**
     * Returns a volume icon based on the volume level.
     *
     * @param volume The volume level.
     * @return A string containing the volume icon.
     */
    public static String volumeIcon(int volume)
    {
        if(volume == 0)
            return "\uD83D\uDD07"; // 🔇
        if(volume < 30)
            return "\uD83D\uDD08"; // 🔈
        if(volume < 70)
            return "\uD83D\uDD09"; // 🔉
        return "\uD83D\uDD0A";     // 🔊
    }
    
    /**
     * Creates a formatted list of text channels.
     *
     * @param list  The list of text channels.
     * @param query The query used to find the channels.
     * @return A formatted string listing the channels.
     */
    public static String listOfTChannels(List<TextChannel> list, String query)
    {
        String out = " Multiple text channels found matching \""+query+"\":";
        for(int i=0; i<6 && i<list.size(); i++)
            out+="\n - "+list.get(i).getName()+" (<#"+list.get(i).getId()+">)";
        if(list.size()>6)
            out+="\n**And "+(list.size()-6)+" more...**";
        return out;
    }
    
    /**
     * Creates a formatted list of voice channels.
     *
     * @param list  The list of voice channels.
     * @param query The query used to find the channels.
     * @return A formatted string listing the channels.
     */
    public static String listOfVChannels(List<VoiceChannel> list, String query)
    {
        String out = " Multiple voice channels found matching \""+query+"\":";
        for(int i=0; i<6 && i<list.size(); i++)
            out+="\n - "+list.get(i).getAsMention()+" (ID:"+list.get(i).getId()+")";
        if(list.size()>6)
            out+="\n**And "+(list.size()-6)+" more...**";
        return out;
    }
    
    /**
     * Creates a formatted list of roles.
     *
     * @param list  The list of roles.
     * @param query The query used to find the roles.
     * @return A formatted string listing the roles.
     */
    public static String listOfRoles(List<Role> list, String query)
    {
        String out = " Multiple roles found matching \""+query+"\":";
        for(int i=0; i<6 && i<list.size(); i++)
            out+="\n - "+list.get(i).getName()+" (ID:"+list.get(i).getId()+")";
        if(list.size()>6)
            out+="\n**And "+(list.size()-6)+" more...**";
        return out;
    }
    
    /**
     * Filters a string to prevent mentions and other exploits.
     *
     * @param input The string to filter.
     * @return The filtered string.
     */
    public static String filter(String input)
    {
        return input.replace("\u202E","")
                .replace("@everyone", "@\u0435veryone") // cyrillic letter e
                .replace("@here", "@h\u0435re") // cyrillic letter e
                .trim();
    }
}
