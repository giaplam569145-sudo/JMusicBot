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
package com.jagrosh.jmusicbot.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.examples.command.AboutCommand;
import com.jagrosh.jdautilities.examples.command.PingCommand;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.commands.admin.*;
import com.jagrosh.jmusicbot.commands.dj.*;
import com.jagrosh.jmusicbot.commands.general.*;
import com.jagrosh.jmusicbot.commands.music.*;
import com.jagrosh.jmusicbot.commands.owner.*;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.Permission;

/**
 * Registry for all bot commands.
 * Handles the instantiation and collection of commands to be registered with the CommandClient.
 */
public class CommandRegistry
{
    public final static Permission[] RECOMMENDED_PERMS = {Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ADD_REACTION,
                                Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_MANAGE,
                                Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.NICKNAME_CHANGE};

    private final Bot bot;

    public CommandRegistry(Bot bot)
    {
        this.bot = bot;
    }

    public Command[] getCommands()
    {
        List<Command> commands = new ArrayList<>();

        // General Commands
        commands.add(new AboutCommand(Color.BLUE.brighter(),
                                "a music bot that is easy to set up and run yourself",
                                new String[]{"High-quality music playback", "Fair Queue scheduling", "Custom playlists"},
                                RECOMMENDED_PERMS));
        commands.add(new PingCommand());
        commands.add(new SettingsCmd(bot));

        // Music Commands
        commands.add(new LyricsCmd(bot));
        commands.add(new NowplayingCmd(bot));
        commands.add(new PlayCmd(bot));
        commands.add(new PlaylistsCmd(bot));
        commands.add(new QueueCmd(bot));
        commands.add(new RemoveCmd(bot));
        commands.add(new SearchCmd(bot));
        commands.add(new SCSearchCmd(bot));
        commands.add(new SeekCmd(bot));
        commands.add(new ShuffleCmd(bot));
        commands.add(new SkipCmd(bot));

        // DJ Commands
        commands.add(new ForceRemoveCmd(bot));
        commands.add(new ForceskipCmd(bot));
        commands.add(new MoveTrackCmd(bot));
        commands.add(new PauseCmd(bot));
        commands.add(new PlaynextCmd(bot));
        commands.add(new RepeatCmd(bot));
        commands.add(new SkiptoCmd(bot));
        commands.add(new StopCmd(bot));
        commands.add(new VolumeCmd(bot));

        // Admin Commands
        commands.add(new PrefixCmd(bot));
        commands.add(new QueueTypeCmd(bot));
        commands.add(new SetdjCmd(bot));
        commands.add(new SkipratioCmd(bot));
        commands.add(new SettcCmd(bot));
        commands.add(new SetvcCmd(bot));
        commands.add(new AutoplaylistCmd(bot));
        commands.add(new PlaylistCmd(bot));
        commands.add(new SetavatarCmd(bot));
        commands.add(new SetgameCmd(bot));
        commands.add(new SetnameCmd(bot));
        commands.add(new SetstatusCmd(bot));

        // Owner Commands
        commands.add(new DebugCmd(bot));
        commands.add(new ShutdownCmd(bot));

        // Eval Command (Conditional)
        if(bot.getConfig().useEval())
            commands.add(new EvalCmd(bot));

        return commands.toArray(new Command[0]);
    }
}
