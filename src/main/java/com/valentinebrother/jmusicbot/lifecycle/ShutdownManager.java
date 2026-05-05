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
package com.valentinebrother.jmusicbot.lifecycle;

import com.valentinebrother.jmusicbot.Bot;
import com.valentinebrother.jmusicbot.audio.AudioHandler;
import com.valentinebrother.jmusicbot.gui.GUI;
import net.dv8tion.jda.api.JDA;

/**
 * Manages the shutdown process of the bot.
 */
public class ShutdownManager
{
    private final Bot bot;
    private boolean shuttingDown = false;

    public ShutdownManager(Bot bot)
    {
        this.bot = bot;
    }

    public void shutdown()
    {
        if(shuttingDown)
            return;
        shuttingDown = true;
        bot.getThreadpool().shutdownNow();
        JDA jda = bot.getJDA();
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
        GUI gui = bot.getGUI();
        if(gui!=null)
            gui.dispose();
        System.exit(0);
    }
}
