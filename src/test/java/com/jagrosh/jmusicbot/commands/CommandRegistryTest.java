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
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.BotConfig;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CommandRegistryTest
{
    @Test
    public void testGetCommands()
    {
        Bot mockBot = mock(Bot.class);
        BotConfig mockConfig = mock(BotConfig.class);
        when(mockBot.getConfig()).thenReturn(mockConfig);
        when(mockConfig.useEval()).thenReturn(false);

        CommandRegistry registry = new CommandRegistry(mockBot);
        Command[] commands = registry.getCommands();

        assertNotNull(commands);
        assertTrue(commands.length > 0);

        // Verify EvalCmd is not present
        for(Command cmd : commands)
        {
            assertNotEquals("eval", cmd.getName());
        }
    }

    @Test
    public void testGetCommandsWithEval()
    {
        Bot mockBot = mock(Bot.class);
        BotConfig mockConfig = mock(BotConfig.class);
        when(mockBot.getConfig()).thenReturn(mockConfig);
        when(mockConfig.useEval()).thenReturn(true);

        CommandRegistry registry = new CommandRegistry(mockBot);
        Command[] commands = registry.getCommands();

        assertNotNull(commands);
        assertTrue(commands.length > 0);

        // Verify EvalCmd IS present
        boolean evalFound = false;
        for(Command cmd : commands)
        {
            if("eval".equals(cmd.getName())) {
                evalFound = true;
                break;
            }
        }
        assertTrue("Eval command should be present when enabled", evalFound);
    }
}
