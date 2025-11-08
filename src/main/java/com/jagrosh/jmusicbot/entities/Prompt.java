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
package com.jagrosh.jmusicbot.entities;

import java.util.Scanner;
import javax.swing.JOptionPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class for handling user prompts, supporting both GUI and command-line interfaces.
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Prompt
{
    private final String title;
    private final String noguiMessage;
    
    private boolean nogui;
    private boolean noprompt;
    private Scanner scanner;
    
    /**
     * Constructs a new Prompt with a title.
     *
     * @param title The title of the prompt.
     */
    public Prompt(String title)
    {
        this(title, null);
    }
    
    /**
     * Constructs a new Prompt with a title and a "no GUI" message.
     *
     * @param title        The title of the prompt.
     * @param noguiMessage The message to display when switching to "no GUI" mode.
     */
    public Prompt(String title, String noguiMessage)
    {
        this(title, noguiMessage, "true".equalsIgnoreCase(System.getProperty("nogui")), "true".equalsIgnoreCase(System.getProperty("noprompt")));
    }
    
    /**
     * Constructs a new Prompt with detailed settings.
     *
     * @param title        The title of the prompt.
     * @param noguiMessage The message to display when switching to "no GUI" mode.
     * @param nogui        Whether to start in "no GUI" mode.
     * @param noprompt     Whether to disable prompts.
     */
    public Prompt(String title, String noguiMessage, boolean nogui, boolean noprompt)
    {
        this.title = title;
        this.noguiMessage = noguiMessage == null ? "Switching to nogui mode. You can manually start in nogui mode by including the -Dnogui=true flag." : noguiMessage;
        this.nogui = nogui;
        this.noprompt = noprompt;
    }
    
    /**
     * Checks if the prompt is in "no GUI" mode.
     *
     * @return True if in "no GUI" mode, false otherwise.
     */
    public boolean isNoGUI()
    {
        return nogui;
    }
    
    /**
     * Displays an alert to the user.
     *
     * @param level   The severity level of the alert.
     * @param context The context of the alert.
     * @param message The message to display.
     */
    public void alert(Level level, String context, String message)
    {
        if(nogui)
        {
            Logger log = LoggerFactory.getLogger(context);
            switch(level)
            {
                case INFO: 
                    log.info(message); 
                    break;
                case WARNING: 
                    log.warn(message); 
                    break;
                case ERROR: 
                    log.error(message); 
                    break;
                default: 
                    log.info(message); 
                    break;
            }
        }
        else
        {
            try 
            {
                int option = 0;
                switch(level)
                {
                    case INFO: 
                        option = JOptionPane.INFORMATION_MESSAGE; 
                        break;
                    case WARNING: 
                        option = JOptionPane.WARNING_MESSAGE; 
                        break;
                    case ERROR: 
                        option = JOptionPane.ERROR_MESSAGE; 
                        break;
                    default:
                        option = JOptionPane.PLAIN_MESSAGE;
                        break;
                }
                JOptionPane.showMessageDialog(null, "<html><body><p style='width: 400px;'>"+message, title, option);
            }
            catch(Exception e) 
            {
                nogui = true;
                alert(Level.WARNING, context, noguiMessage);
                alert(level, context, message);
            }
        }
    }
    
    /**
     * Prompts the user for input.
     *
     * @param content The message to display to the user.
     * @return The user's input, or null if prompts are disabled.
     */
    public String prompt(String content)
    {
        if(noprompt)
            return null;
        if(nogui)
        {
            if(scanner==null)
                scanner = new Scanner(System.in);
            try
            {
                System.out.println(content);
                if(scanner.hasNextLine())
                    return scanner.nextLine();
                return null;
            }
            catch(Exception e)
            {
                alert(Level.ERROR, title, "Unable to read input from command line.");
                e.printStackTrace();
                return null;
            }
        }
        else
        {
            try 
            {
                return JOptionPane.showInputDialog(null, content, title, JOptionPane.QUESTION_MESSAGE);
            }
            catch(Exception e) 
            {
                nogui = true;
                alert(Level.WARNING, title, noguiMessage);
                return prompt(content);
            }
        }
    }
    
    /**
     * Enum representing the severity level of an alert.
     */
    public static enum Level
    {
        INFO, WARNING, ERROR;
    }
}
