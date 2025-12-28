/*
 * Copyright 2024
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
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.audio.RequestMetadata;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import com.jagrosh.jmusicbot.utils.FileSystemNavigator;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.jagrosh.jmusicbot.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Command for browsing and playing local files using a UI.
 */
public class BrowseCmd extends MusicCommand {

    private final FileSystemNavigator navigator;
    private final Map<Long, BrowseSession> sessions = new ConcurrentHashMap<>();

    public BrowseCmd(Bot bot) {
        super(bot);
        this.name = "browse";
        this.help = "browse and play local files";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.beListening = true;
        this.bePlaying = false;
        String root = bot.getConfig().getBrowseFolder();
        if (root == null)
            root = bot.getConfig().getPlaylistsFolder();
        if (root == null)
            root = ".";
        this.navigator = new FileSystemNavigator(root);
    }

    @Override
    public void doCommand(CommandEvent event) {
        try {
            BrowseSession session = new BrowseSession("", 1);
            MessageCreateData msg = buildBrowseMessage(session);

            event.getChannel().sendMessage(msg).queue(m -> {
                sessions.put(m.getIdLong(), session);
                waitForInteraction(event, m);
            });
        } catch (IOException e) {
            event.replyError("Failed to access file system: " + e.getMessage());
        }
    }

    private MessageCreateData buildBrowseMessage(BrowseSession session) throws IOException {
        List<Path> allItems = navigator.listContents(session.path);
        int totalItems = allItems.size();
        int maxPages = (int) Math.ceil((double) totalItems / 25.0);
        if (maxPages == 0) maxPages = 1;

        // Adjust page if out of bounds
        if (session.page > maxPages) session.page = maxPages;
        if (session.page < 1) session.page = 1;

        int start = (session.page - 1) * 25;
        int end = Math.min(start + 25, totalItems);
        List<Path> items = allItems.subList(start, end);
        session.currentItems = items; // Store for index-based selection

        StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("browse:select")
                .setPlaceholder("Select a file or folder (Page " + session.page + "/" + maxPages + ")")
                .setRequiredRange(1, 1);

        for (int i = 0; i < items.size(); i++) {
            Path item = items.get(i);
            String name = item.getFileName().toString();
            String label = name.length() > 90 ? name.substring(0, 87) + "..." : name;
            boolean isDir = navigator.isDirectory(item);
            // Use index as value to avoid length limits and special chars
            String value = "idx:" + i;

            menuBuilder.addOption(label, value, isDir ? "Folder" : "Audio File", isDir ? Emoji.fromUnicode("📁") : Emoji.fromUnicode("🎵"));
        }

        if (items.isEmpty()) {
            menuBuilder.addOption("Empty", "empty", "No files found", Emoji.fromUnicode("❌"));
            menuBuilder.setDisabled(true);
        }

        List<ActionRow> rows = new ArrayList<>();
        rows.add(ActionRow.of(menuBuilder.build()));

        List<Button> buttons = new ArrayList<>();
        if (!session.path.isEmpty()) {
            buttons.add(Button.secondary("browse:up", "Up").withEmoji(Emoji.fromUnicode("⬆️")));
        }
        if (session.page > 1) {
            buttons.add(Button.primary("browse:prev", "Prev").withEmoji(Emoji.fromUnicode("⬅️")));
        }
        if (session.page < maxPages) {
            buttons.add(Button.primary("browse:next", "Next").withEmoji(Emoji.fromUnicode("➡️")));
        }
        buttons.add(Button.danger("browse:close", "Close").withEmoji(Emoji.fromUnicode("✖️")));
        rows.add(ActionRow.of(buttons));

        String pathDisplay = session.path.isEmpty() ? "(root)" : session.path;
        return new MessageCreateBuilder()
                .setContent("Browsing: `/" + pathDisplay + "` (Page " + session.page + "/" + maxPages + ")")
                .setComponents(rows)
                .build();
    }

    private MessageEditData buildBrowseEditMessage(BrowseSession session) throws IOException {
        MessageCreateData data = buildBrowseMessage(session);
        return new MessageEditBuilder()
                .setContent(data.getContent())
                .setComponents(data.getComponents())
                .build();
    }

    private void waitForInteraction(CommandEvent event, Message message) {
        bot.getWaiter().waitForEvent(net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent.class,
            e -> {
                if (e instanceof StringSelectInteractionEvent) {
                    return ((StringSelectInteractionEvent) e).getMessageIdLong() == message.getIdLong()
                            && ((StringSelectInteractionEvent) e).getUser().getIdLong() == event.getAuthor().getIdLong();
                } else if (e instanceof ButtonInteractionEvent) {
                    return ((ButtonInteractionEvent) e).getMessageIdLong() == message.getIdLong()
                            && ((ButtonInteractionEvent) e).getUser().getIdLong() == event.getAuthor().getIdLong();
                }
                return false;
            },
            e -> {
                if (e instanceof StringSelectInteractionEvent) {
                    handleSelect((StringSelectInteractionEvent) e, event);
                } else if (e instanceof ButtonInteractionEvent) {
                    handleButton((ButtonInteractionEvent) e, event);
                }
            },
            2, TimeUnit.MINUTES, () -> {
                message.delete().queue(s->{}, f->{});
                sessions.remove(message.getIdLong());
            }
        );
    }

    private void handleSelect(StringSelectInteractionEvent event, CommandEvent commandEvent) {
        BrowseSession session = sessions.get(event.getMessage().getIdLong());
        if (session == null) {
            event.reply("Session expired.").setEphemeral(true).queue();
            return;
        }

        String selected = event.getValues().get(0);

        if (selected.startsWith("idx:")) {
            int index = Integer.parseInt(selected.substring(4));
            if (index >= 0 && index < session.currentItems.size()) {
                Path item = session.currentItems.get(index);
                if (navigator.isDirectory(item)) {
                    Path relPath = navigator.getRoot().relativize(item).normalize();
                    session.path = relPath.toString();
                    session.page = 1;
                    updateMessage(event, session, commandEvent);
                } else {
                    playFile(event, item, commandEvent);
                }
            } else {
                event.reply("Invalid selection.").setEphemeral(true).queue();
            }
        }
    }

    private void handleButton(ButtonInteractionEvent event, CommandEvent commandEvent) {
        BrowseSession session = sessions.get(event.getMessage().getIdLong());
        if (session == null) {
            event.reply("Session expired.").setEphemeral(true).queue();
            return;
        }

        String id = event.getComponentId();
        if (id.equals("browse:close")) {
            event.deferEdit().queue();
            event.getMessage().delete().queue();
            sessions.remove(event.getMessage().getIdLong());
            return;
        }

        try {
            if (id.equals("browse:up")) {
                if (!session.path.isEmpty()) {
                   Path p = Paths.get(session.path);
                   Path parent = p.getParent();
                   session.path = parent == null ? "" : parent.toString();
                   session.page = 1;
                }
            } else if (id.equals("browse:next")) {
                session.page++;
            } else if (id.equals("browse:prev")) {
                session.page--;
            }
            updateMessage(event, session, commandEvent);
        } catch (Exception ex) {
            event.reply("Error: " + ex.getMessage()).setEphemeral(true).queue();
        }
    }

    private void updateMessage(net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback event, BrowseSession session, CommandEvent commandEvent) {
        try {
            MessageEditData msg = buildBrowseEditMessage(session);
            event.editMessage(msg).queue(m -> event.getHook().retrieveOriginal().queue(original -> waitForInteraction(commandEvent, original)));
        } catch (IOException e) {
            if (event instanceof net.dv8tion.jda.api.interactions.callbacks.IReplyCallback) {
                ((net.dv8tion.jda.api.interactions.callbacks.IReplyCallback) event).reply("Error updating browser: " + e.getMessage()).setEphemeral(true).queue();
            }
        }
    }

    private void playFile(StringSelectInteractionEvent event, Path filePath, CommandEvent commandEvent) {
        // Ensure bot is in voice channel
        net.dv8tion.jda.api.entities.GuildVoiceState botState = event.getGuild().getSelfMember().getVoiceState();
        if (botState == null || botState.getChannel() == null) {
            net.dv8tion.jda.api.entities.GuildVoiceState userState = event.getMember().getVoiceState();
            if (userState != null && userState.getChannel() != null) {
                try {
                    event.getGuild().getAudioManager().openAudioConnection(userState.getChannel());
                } catch (Exception ex) {
                     event.reply("Could not join voice channel: " + ex.getMessage()).setEphemeral(true).queue();
                     return;
                }
            } else {
                 event.reply("You need to be in a voice channel to play music!").setEphemeral(true).queue();
                 return;
            }
        }

        event.reply("Loading `" + filePath.getFileName() + "`...").setEphemeral(true).queue();

        bot.getPlayerManager().loadItemOrdered(commandEvent.getGuild(), filePath.toAbsolutePath().toString(), new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                AudioHandler handler = (AudioHandler)commandEvent.getGuild().getAudioManager().getSendingHandler();
                int pos = handler.addTrack(new QueuedTrack(track, RequestMetadata.fromResultHandler(track, commandEvent)))+1;
                event.getChannel().sendMessage(FormatUtil.filter(commandEvent.getClient().getSuccess()+" Added **"+track.getInfo().title
                    +"** (`"+ TimeUtil.formatTime(track.getDuration())+"`) "+(pos==0?"to begin playing":" to the queue at position "+pos))).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                 trackLoaded(playlist.getTracks().get(0));
            }

            @Override
            public void noMatches() {
                event.getChannel().sendMessage(commandEvent.getClient().getError() + " Could not find track.").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                event.getChannel().sendMessage(commandEvent.getClient().getError() + " Error loading track: " + exception.getMessage()).queue();
            }
        });

        waitForInteraction(commandEvent, event.getMessage());
    }

    private static class BrowseSession {
        String path;
        int page;
        List<Path> currentItems;

        BrowseSession(String path, int page) {
            this.path = path;
            this.page = page;
        }
    }
}
