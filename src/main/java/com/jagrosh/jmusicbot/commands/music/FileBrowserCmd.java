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
package com.jagrosh.jmusicbot.commands.music;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import com.jagrosh.jmusicbot.utils.FileInfo;
import com.jagrosh.jmusicbot.utils.FileSystemNavigator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.audio.RequestMetadata;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.jagrosh.jmusicbot.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Command that opens a file browser to select music to play.
 */
public class FileBrowserCmd extends MusicCommand {

    private final FileSystemNavigator navigator;

    public FileBrowserCmd(Bot bot) {
        super(bot);
        this.name = "browse";
        this.help = "browses local files to play";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.beListening = true;
        this.bePlaying = false;

        // Use a configured folder if available, otherwise default to "music" or "."
        String configuredPath = bot.getConfig().getBrowserFolder();
        if (configuredPath == null) configuredPath = ".";
        this.navigator = new FileSystemNavigator(configuredPath);
    }

    @Override
    public void doCommand(CommandEvent event) {
        // Start browsing at root
        sendBrowserMessage(event);
    }

    private void sendBrowserMessage(CommandEvent event) {
        // We defer the creation of the message and listener to the helper method
        // But first we need to send the initial message

        // Initial state
        String initialPath = "";
        int initialPage = 1;

        try {
            List<FileInfo> items = navigator.listItems(initialPath);
            EmbedBuilder eb = createEmbed(initialPath, initialPage, items);
            List<ActionRow> rows = createComponents(initialPath, initialPage, items, event.getAuthor().getId());

            event.getChannel().sendMessageEmbeds(eb.build())
                .setComponents(rows)
                .queue(msg -> {
                    // Create listener with initial state
                    BrowserListener listener = new BrowserListener(msg.getIdLong(), event.getAuthor().getIdLong(), initialPath, initialPage);
                    event.getJDA().addEventListener(listener);

                    // Auto-delete after 2 mins to save resources/cleanup and remove listener
                    msg.delete().queueAfter(2, TimeUnit.MINUTES,
                        success -> event.getJDA().removeEventListener(listener),
                        failure -> event.getJDA().removeEventListener(listener) // Remove even if delete fails (e.g. already deleted)
                    );
                });

        } catch (IOException | SecurityException e) {
            event.replyError("Error accessing files: " + e.getMessage());
        }
    }

    private EmbedBuilder createEmbed(String path, int page, List<FileInfo> items) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("File Browser: " + (path.isEmpty() ? "/" : path));
        eb.setColor(Color.CYAN);

        if (items.isEmpty()) {
            eb.setDescription("This directory is empty.");
            return eb;
        }

        int itemsPerPage = 25;
        int totalPages = (int) Math.ceil((double) items.size() / itemsPerPage);

        int start = (page - 1) * itemsPerPage;
        int end = Math.min(start + itemsPerPage, items.size());
        List<FileInfo> pageItems = items.subList(start, end);

        StringBuilder desc = new StringBuilder();
        for (FileInfo item : pageItems) {
            String icon = item.isDirectory() ? "📁 " : "🎵 ";
            desc.append(icon).append("`").append(item.getName()).append("`\n");
        }

        eb.setDescription(desc.toString());
        eb.setFooter("Page " + page + "/" + totalPages);
        return eb;
    }

    private List<ActionRow> createComponents(String path, int page, List<FileInfo> items, String userId) {
         List<ActionRow> rows = new ArrayList<>();

         if (items.isEmpty()) {
             // Just a close button
             rows.add(ActionRow.of(Button.danger("browser-close:" + userId, "Close")));
             if (!path.isEmpty()) {
                  // Add UP button if empty folder
                  rows.add(ActionRow.of(Button.secondary("browser-up:" + userId, "Up")));
             }
             return rows;
         }

        int itemsPerPage = 25;
        int totalPages = (int) Math.ceil((double) items.size() / itemsPerPage);

        int start = (page - 1) * itemsPerPage;
        int end = Math.min(start + itemsPerPage, items.size());
        List<FileInfo> pageItems = items.subList(start, end);

        StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("browser-select:" + userId)
                .setPlaceholder("Select a file or folder");

        if (!path.isEmpty()) {
             menuBuilder.addOption(".. (Up)", "UP");
        }

        for (FileInfo item : pageItems) {
            String value = item.getName();
            // Truncate if necessary for value (100 char limit), though strictly name shouldn't be that long hopefully
            if (value.length() > 100) value = value.substring(0, 100);

            menuBuilder.addOption(item.getName(), value, item.isDirectory() ? "Folder" : "File", item.isDirectory() ? net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("📁") : net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("🎵"));
        }

        rows.add(ActionRow.of(menuBuilder.build()));

        List<Button> buttons = new ArrayList<>();
        if (page > 1) {
            buttons.add(Button.primary("browser-prev:" + userId, "Previous"));
        }
        if (page < totalPages) {
            buttons.add(Button.primary("browser-next:" + userId, "Next"));
        }
        buttons.add(Button.danger("browser-close:" + userId, "Close"));

        if (!buttons.isEmpty()) {
            rows.add(ActionRow.of(buttons));
        }
        return rows;
    }

    // Stateful listener
    private class BrowserListener extends ListenerAdapter {
        private final long messageId;
        private final long userId;
        private String currentPath;
        private int currentPage;

        public BrowserListener(long messageId, long userId, String initialPath, int initialPage) {
            this.messageId = messageId;
            this.userId = userId;
            this.currentPath = initialPath;
            this.currentPage = initialPage;
        }

        @Override
        public void onStringSelectInteraction(StringSelectInteractionEvent event) {
            if (event.getMessage().getIdLong() != messageId) return;
            if (event.getUser().getIdLong() != userId) {
                event.reply("You cannot use this browser.").setEphemeral(true).queue();
                return;
            }

            String selected = event.getValues().get(0);

            if (selected.equals("UP")) {
                 goUp(event);
                 return;
            }

            try {
                // Construct full relative path
                String newPath = currentPath.isEmpty() ? selected : currentPath + File.separator + selected;

                Path absPath = navigator.getAbsolutePath(newPath);
                if (Files.isDirectory(absPath)) {
                    // Navigate into directory
                    currentPath = newPath;
                    currentPage = 1;
                    updateMessage(event);
                } else {
                    // It's a file, play it
                    playFile(event, absPath.toString());
                }
            } catch (Exception e) {
                event.reply("Error: " + e.getMessage()).setEphemeral(true).queue();
            }
        }

        @Override
        public void onButtonInteraction(ButtonInteractionEvent event) {
             if (event.getMessage().getIdLong() != messageId) return;
             if (event.getUser().getIdLong() != userId) {
                event.reply("You cannot use this browser.").setEphemeral(true).queue();
                return;
            }

            String[] idParts = event.getComponentId().split(":");
            String action = idParts[0];

            if (action.equals("browser-close")) {
                event.getMessage().delete().queue();
                event.getJDA().removeEventListener(this);
                return;
            } else if (action.equals("browser-up")) {
                goUp(event);
                return;
            } else if (action.equals("browser-prev")) {
                currentPage--;
                updateMessage(event);
            } else if (action.equals("browser-next")) {
                currentPage++;
                updateMessage(event);
            }
        }

        private void goUp(net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback event) {
             Path p = Paths.get(currentPath);
             String parent = p.getParent() == null ? "" : p.getParent().toString();
             currentPath = parent;
             currentPage = 1;
             updateMessage(event);
        }

        private void updateMessage(net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback event) {
             try {
                List<FileInfo> items = navigator.listItems(currentPath);
                // Adjust page if out of bounds (e.g. dir content changed)
                int itemsPerPage = 25;
                int totalPages = (int) Math.ceil((double) items.size() / itemsPerPage);
                if (currentPage < 1) currentPage = 1;
                if (currentPage > totalPages && totalPages > 0) currentPage = totalPages;

                EmbedBuilder eb = createEmbed(currentPath, currentPage, items);
                List<ActionRow> rows = createComponents(currentPath, currentPage, items, String.valueOf(userId));

                event.editMessageEmbeds(eb.build()).setComponents(rows).queue();

            } catch (Exception e) {
                 if (event instanceof net.dv8tion.jda.api.interactions.callbacks.IReplyCallback) {
                     ((net.dv8tion.jda.api.interactions.callbacks.IReplyCallback) event)
                         .reply("Error updating view: " + e.getMessage()).setEphemeral(true).queue();
                 }
            }
        }

        private void playFile(StringSelectInteractionEvent event, String absPath) {
             event.deferReply().queue();
             bot.getPlayerManager().loadItemOrdered(event.getGuild(), absPath, new ResultHandler(event));
        }
    }

    // Adapted ResultHandler for Interaction
    private class ResultHandler implements AudioLoadResultHandler
    {
        private final StringSelectInteractionEvent event;

        private ResultHandler(StringSelectInteractionEvent event)
        {
            this.event = event;
        }

        @Override
        public void trackLoaded(AudioTrack track)
        {
            if(bot.getConfig().isTooLong(track))
            {
                event.getHook().sendMessage(FormatUtil.filter("This track (**"+track.getInfo().title+"**) is longer than the allowed maximum: `"
                        + TimeUtil.formatTime(track.getDuration())+"` > `"+ TimeUtil.formatTime(bot.getConfig().getMaxSeconds()*1000)+"`")).queue();
                return;
            }
            AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();

            RequestMetadata.RequestInfo requestInfo = new RequestMetadata.RequestInfo(track.getInfo().uri, track.getInfo().uri);
            RequestMetadata metadata = new RequestMetadata(event.getUser(), requestInfo);

            int pos = handler.addTrack(new QueuedTrack(track, metadata))+1;
            String addMsg = FormatUtil.filter(bot.getConfig().getSuccess()+" Added **"+track.getInfo().title
                    +"** (`"+ TimeUtil.formatTime(track.getDuration())+"`) "+(pos==0?"to begin playing":" to the queue at position "+pos));
            event.getHook().sendMessage(addMsg).queue();
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist)
        {
             event.getHook().sendMessage("Loaded playlist " + playlist.getName()).queue();
             if(!playlist.getTracks().isEmpty()) trackLoaded(playlist.getTracks().get(0));
        }

        @Override
        public void noMatches()
        {
            event.getHook().sendMessage("No audio found at " + event.getValues().get(0)).queue();
        }

        @Override
        public void loadFailed(FriendlyException throwable)
        {
            event.getHook().sendMessage("Error loading track: " + throwable.getMessage()).queue();
        }
    }
}
