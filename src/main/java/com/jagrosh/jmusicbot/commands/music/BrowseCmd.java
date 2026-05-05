/*
 * Copyright 2025 giaplam569145-sudo
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
package com.jagrosh.jmusicbot.commands.music;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.audio.RequestMetadata;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import com.jagrosh.jmusicbot.utils.FileInfo;
import com.jagrosh.jmusicbot.utils.FileSystemNavigator;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.jagrosh.jmusicbot.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

/**
 * Command to browse and play local files.
 *
 * @author giaplam569145-sudo
 */
public class BrowseCmd extends MusicCommand
{
    public BrowseCmd(Bot bot)
    {
        super(bot);
        this.name = "browse";
        this.help = "browses local files to play";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.beListening = true;
        this.bePlaying = false;
    }

    @Override
    public void doCommand(CommandEvent event)
    {
        String browserFolder = bot.getConfig().getBrowserFolder();
        FileSystemNavigator navigator;
        try
        {
            navigator = new FileSystemNavigator(browserFolder);
        }
        catch (IOException e)
        {
            event.replyError("Failed to access browser folder: " + e.getMessage());
            return;
        }

        // Check if root exists is handled inside listItems implicitly by returning empty list or throwing,
        // but we can just try to open it.
        try {
            // Test access
            navigator.listItems("");
        } catch (Exception e) {
             event.replyError("Failed to access browser folder: " + e.getMessage());
             return;
        }

        try
        {
            new BrowserMenu(bot.getWaiter(), event, navigator).display();
        }
        catch(IOException e)
        {
            event.replyError("Failed to access file system: " + e.getMessage());
        }
    }

    private class BrowserMenu
    {
        private final EventWaiter waiter;
        private final CommandEvent event;
        private final FileSystemNavigator navigator;
        private String currentPath;
        private Message menuMessage;
        private int page = 0;

        // Navigation buttons
        private final static String UP = "browse_up";
        private final static String CLOSE = "browse_close";
        private final static String SELECT = "browse_select";
        private final static String NEXT = "browse_next";
        private final static String PREV = "browse_prev";

        public BrowserMenu(EventWaiter waiter, CommandEvent event, FileSystemNavigator navigator)
        {
            this.waiter = waiter;
            this.event = event;
            this.navigator = navigator;
            this.currentPath = ""; // Start at root
        }

        public void display() throws IOException
        {
            if (menuMessage == null)
            {
                event.getChannel().sendMessage(renderMessageCreate(currentPath)).queue(m ->
                {
                    menuMessage = m;
                    waitForInteraction();
                });
            }
            else
            {
                menuMessage.editMessage(renderMessageEdit(currentPath)).queue();
                waitForInteraction();
            }
        }

        private MessageCreateData renderMessageCreate(String path) throws IOException
        {
            List<FileInfo> contents = navigator.listItems(path);
            String folderName = path.isEmpty() ? "/" : Paths.get(path).getFileName().toString();

            StringSelectMenu.Builder menuBuilder = StringSelectMenu.create(SELECT)
                    .setPlaceholder("Select a file or folder in " + folderName)
                    .setMinValues(1)
                    .setMaxValues(1);

            int itemsPerPage = 25;
            int totalPages = (int) Math.ceil((double) contents.size() / itemsPerPage);
            if (page >= totalPages) page = Math.max(0, totalPages - 1);
            if (page < 0) page = 0;

            int start = page * itemsPerPage;
            int end = Math.min(start + itemsPerPage, contents.size());
            List<FileInfo> pageContents = contents.subList(start, end);

            if (pageContents.isEmpty())
            {
                menuBuilder.addOption("Empty directory", "empty", "No audio files or folders found.");
                menuBuilder.setDisabled(true);
            }
            else
            {
                for (int i = 0; i < pageContents.size(); i++)
                {
                    FileInfo item = pageContents.get(i);
                    String name = item.getName();
                    if (name.length() > 100) name = name.substring(0, 97) + "...";

                    // Use index + start as value to map back to original list index
                    String value = String.valueOf(start + i);

                    String emoji = item.isDirectory() ? "\uD83D\uDCC1" : "\uD83C\uDFB5"; // 📁 or 🎵
                    menuBuilder.addOption(name, value, item.isDirectory() ? "Folder" : "Audio File", net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode(emoji));
                }
            }

            Button upButton = Button.secondary(UP, "Up").withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\u2B06\uFE0F")); // ⬆️
            if (path.isEmpty())
            {
                upButton = upButton.asDisabled();
            }

            Button prevButton = Button.secondary(PREV, "Prev").withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\u2B05\uFE0F")); // ⬅️
            if (page == 0) prevButton = prevButton.asDisabled();

            Button nextButton = Button.secondary(NEXT, "Next").withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\u27A1\uFE0F")); // ➡️
            if (page >= totalPages - 1) nextButton = nextButton.asDisabled();

            Button closeButton = Button.danger(CLOSE, "Close").withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\u274C")); // ❌

            return new MessageCreateBuilder()
                    .setContent("**Browsing:** `/" + path.replace(File.separator, "/") + "` (Page " + (page + 1) + "/" + Math.max(1, totalPages) + ")")
                    .setComponents(
                            ActionRow.of(menuBuilder.build()),
                            ActionRow.of(prevButton, upButton, closeButton, nextButton)
                    ).build();
        }

        private MessageEditData renderMessageEdit(String path) throws IOException
        {
            MessageCreateData data = renderMessageCreate(path);
            return MessageEditData.fromCreateData(data);
        }

        private void waitForInteraction()
        {
            waiter.waitForEvent(net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent.class,
                e ->
                {
                    if (e.getUser().getIdLong() != event.getAuthor().getIdLong()) return false;
                    if (e instanceof ButtonInteractionEvent)
                    {
                        return ((ButtonInteractionEvent)e).getMessageIdLong() == menuMessage.getIdLong();
                    }
                    if (e instanceof StringSelectInteractionEvent)
                    {
                        return ((StringSelectInteractionEvent)e).getMessageIdLong() == menuMessage.getIdLong();
                    }
                    return false;
                },
                e ->
                {
                    try
                    {
                        if (e instanceof ButtonInteractionEvent)
                        {
                            ButtonInteractionEvent bie = (ButtonInteractionEvent) e;
                            bie.deferEdit().queue();
                            String cid = bie.getComponentId();
                            if (cid.equals(UP))
                            {
                                if (!currentPath.isEmpty())
                                {
                                    Path p = Paths.get(currentPath);
                                    Path parent = p.getParent();
                                    currentPath = parent == null ? "" : parent.toString();
                                    page = 0; // Reset page on navigation
                                    display();
                                }
                            }
                            else if (cid.equals(CLOSE))
                            {
                                menuMessage.delete().queue();
                            }
                            else if (cid.equals(NEXT))
                            {
                                page++;
                                display();
                            }
                            else if (cid.equals(PREV))
                            {
                                page--;
                                display();
                            }
                        }
                        else if (e instanceof StringSelectInteractionEvent)
                        {
                            StringSelectInteractionEvent ssie = (StringSelectInteractionEvent) e;
                            ssie.deferEdit().queue();
                            String selected = ssie.getValues().get(0);
                            if (selected.equals("empty")) return;

                            int index = Integer.parseInt(selected);
                            List<FileInfo> contents = navigator.listItems(currentPath);
                            if (index >= 0 && index < contents.size())
                            {
                                FileInfo selectedItem = contents.get(index);
                                if (selectedItem.isDirectory())
                                {
                                    currentPath = selectedItem.getRelativePath();
                                    page = 0; // Reset page on navigation
                                    display();
                                }
                                else
                                {
                                    // Play the file
                                    playFile(navigator.getAbsolutePath(selectedItem.getRelativePath()));
                                }
                            }
                            else
                            {
                                event.replyError("File not found (index mismatch).");
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        event.replyError("Error navigating: " + ex.getMessage());
                    }
                },
                2, TimeUnit.MINUTES, () -> menuMessage.delete().queue()); // Changed timeout to 2 mins as per my previous logic
        }

        private void playFile(Path file)
        {
            bot.getPlayerManager().loadItemOrdered(event.getGuild(), file.toAbsolutePath().toString(), new ResultHandler(menuMessage, event));
        }
    }

    // Copied and adapted from PlayCmd.ResultHandler to reuse playback logic
    private class ResultHandler implements AudioLoadResultHandler
    {
        private final Message m;
        private final CommandEvent event;

        private ResultHandler(Message m, CommandEvent event)
        {
            this.m = m;
            this.event = event;
        }

        private void loadSingle(AudioTrack track)
        {
            if(bot.getConfig().isTooLong(track))
            {
                m.editMessage(FormatUtil.filter(event.getClient().getWarning()+" This track (**"+track.getInfo().title+"**) is longer than the allowed maximum: `"
                        + TimeUtil.formatTime(track.getDuration())+"` > `"+ TimeUtil.formatTime(bot.getConfig().getMaxSeconds()*1000)+"`")).queue();
                return;
            }
            AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
            int pos = handler.addTrack(new QueuedTrack(track, RequestMetadata.fromResultHandler(track, event)))+1;
            String addMsg = FormatUtil.filter(event.getClient().getSuccess()+" Added **"+track.getInfo().title
                    +"** (`"+ TimeUtil.formatTime(track.getDuration())+"`) "+(pos==0?"to begin playing":" to the queue at position "+pos));
            m.editMessage(addMsg).setComponents().queue(); // Remove components (close the menu)
        }

        @Override
        public void trackLoaded(AudioTrack track)
        {
            loadSingle(track);
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist)
        {
            // Should not happen for single file, but handle anyway
            if(playlist.getTracks().size()==1 || playlist.isSearchResult())
            {
                AudioTrack single = playlist.getSelectedTrack()==null ? playlist.getTracks().get(0) : playlist.getSelectedTrack();
                loadSingle(single);
            }
        }

        @Override
        public void noMatches()
        {
            m.editMessage(FormatUtil.filter(event.getClient().getWarning()+" No matches found.")).setComponents().queue();
        }

        @Override
        public void loadFailed(FriendlyException throwable)
        {
            if(throwable.severity==FriendlyException.Severity.COMMON)
                m.editMessage(event.getClient().getError()+" Error loading: "+throwable.getMessage()).setComponents().queue();
            else
                m.editMessage(event.getClient().getError()+" Error loading track.").setComponents().queue();
        }
    }
}
