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
package com.jagrosh.jmusicbot;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.audio.RequestMetadata;
import com.jagrosh.jmusicbot.utils.FileInfo;
import com.jagrosh.jmusicbot.utils.FileSystemNavigator;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.jagrosh.jmusicbot.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import java.awt.Color;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener to handle bare bot mentions and display an interactive menu.
 */
public class MentionListener extends ListenerAdapter {
    private final Bot bot;
    private final Pattern mentionPattern;
    private final Logger log = LoggerFactory.getLogger(MentionListener.class);

    public MentionListener(Bot bot) {
        this.bot = bot;
        this.mentionPattern = Pattern.compile("^<@!?(\\d+)>$");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String content = event.getMessage().getContentRaw().trim();
        Matcher matcher = mentionPattern.matcher(content);

        if (matcher.matches()) {
            String mentionedId = matcher.group(1);
            if (mentionedId.equals(event.getJDA().getSelfUser().getId())) {
                // Check permissions
                if (!event.getGuild().getSelfMember().hasPermission(event.getChannel().asGuildMessageChannel(), Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_SEND)) {
                    return;
                }
                new MentionMenu(bot, event).display();
            }
        }
    }

    private class MentionMenu {
        private final Bot bot;
        private final MessageReceivedEvent event;
        private final EventWaiter waiter;
        private Message menuMessage;

        // Button IDs
        private static final String PAUSE = "mm_pause";
        private static final String RESUME = "mm_resume";
        private static final String SKIP = "mm_skip";
        private static final String STOP = "mm_stop";
        private static final String LYRICS = "mm_lyrics";
        private static final String ADD_TO_QUEUE = "mm_add";
        private static final String SHOW_QUEUE = "mm_queue";
        private static final String CLOSE = "mm_close";
        private static final String PLAY_NOW = "mm_playnow";
        private static final String BROWSE = "mm_browse";
        private static final String HELP = "mm_help";

        public MentionMenu(Bot bot, MessageReceivedEvent event) {
            this.bot = bot;
            this.event = event;
            this.waiter = bot.getWaiter();
        }

        public void display() {
            MessageCreateData data = renderMenu();
            event.getChannel().sendMessage(data).queue(m -> {
                menuMessage = m;
                waitForInteraction();
            });
        }

        public void update() {
            if (menuMessage != null) {
                menuMessage.editMessage(MessageEditData.fromCreateData(renderMenu())).queue();
                waitForInteraction();
            }
        }

        private MessageCreateData renderMenu() {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("🎵 JMusicBot - Main Menu");
            eb.setColor(Color.decode("#1DB954")); // Music Green

            AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            AudioTrack track = (handler != null) ? handler.getPlayer().getPlayingTrack() : null;
            boolean isPlaying = track != null;
            boolean isPaused = (handler != null) && handler.getPlayer().isPaused();

            // Thumbnail
            String thumbnail = event.getJDA().getSelfUser().getEffectiveAvatarUrl();
            if (isPlaying && track instanceof YoutubeAudioTrack && bot.getConfig().useNPImages()) {
                thumbnail = "https://img.youtube.com/vi/" + track.getIdentifier() + "/mqdefault.jpg";
            }
            eb.setThumbnail(thumbnail);

            if (isPlaying) {
                // Now Playing
                String artist = track.getInfo().author;
                String title = track.getInfo().title;
                eb.addField("🎵 Now Playing", "**" + title + "** - " + artist, false);

                // Progress
                double progress = (double) track.getPosition() / track.getDuration();
                String progressStr = FormatUtil.progressBar(progress) + " " +
                        TimeUtil.formatTime(track.getPosition()) + " / " + TimeUtil.formatTime(track.getDuration());
                eb.addField("⏱️ Progress", progressStr, false);

                // Lyrics
                try {
                    String q = URLEncoder.encode(artist + " " + title, StandardCharsets.UTF_8.toString());
                    String url = "https://genius.com/search?q=" + q;
                    eb.addField("📝 Lyrics", "[Lyrics auf Genius](" + url + ")", false);
                } catch (Exception e) {
                    // Ignore lyrics if encoding fails
                }
            } else {
                eb.setDescription("🔇 Aktuell läuft keine Musik. Starte etwas!");
            }

            // Buttons
            List<ActionRow> rows = new ArrayList<>();
            List<Button> row1 = new ArrayList<>();
            List<Button> row2 = new ArrayList<>();

            if (isPlaying) {
                if (isPaused) {
                    // WENN PAUSED
                    row1.add(Button.success(RESUME, "Resume").withEmoji(Emoji.fromUnicode("▶️")));
                } else {
                    // WENN MUSIK LÄUFT
                    row1.add(Button.secondary(PAUSE, "Pause").withEmoji(Emoji.fromUnicode("⏸️")));
                }
                row1.add(Button.secondary(SKIP, "Skip").withEmoji(Emoji.fromUnicode("⏭️")));
                row1.add(Button.danger(STOP, "Stop").withEmoji(Emoji.fromUnicode("⏹️")));

                // Lyrics button (Link button preferred)
                 try {
                    String artist = track.getInfo().author;
                    String title = track.getInfo().title;
                    String q = URLEncoder.encode(artist + " " + title, StandardCharsets.UTF_8.toString());
                    String url = "https://genius.com/search?q=" + q;
                    row1.add(Button.link(url, "Lyrics").withEmoji(Emoji.fromUnicode("📝")));
                } catch (Exception e) {
                    row1.add(Button.secondary(LYRICS, "Lyrics").withEmoji(Emoji.fromUnicode("📝")).asDisabled());
                }

                row2.add(Button.secondary(ADD_TO_QUEUE, "Add to Queue").withEmoji(Emoji.fromUnicode("➕")));
                row2.add(Button.secondary(SHOW_QUEUE, "Show Queue").withEmoji(Emoji.fromUnicode("📜")));
                row2.add(Button.danger(CLOSE, "Close").withEmoji(Emoji.fromUnicode("❌")));

            } else {
                // WENN NICHTS LÄUFT
                row1.add(Button.success(PLAY_NOW, "Play Now").withEmoji(Emoji.fromUnicode("▶️")));
                row1.add(Button.secondary(BROWSE, "Browse Files").withEmoji(Emoji.fromUnicode("📂")));
                row1.add(Button.secondary(SHOW_QUEUE, "Show Queue").withEmoji(Emoji.fromUnicode("📜")));

                row2.add(Button.primary(HELP, "Help").withEmoji(Emoji.fromUnicode("ℹ️")));
                row2.add(Button.danger(CLOSE, "Close").withEmoji(Emoji.fromUnicode("❌")));
            }

            rows.add(ActionRow.of(row1));
            rows.add(ActionRow.of(row2));

            return new MessageCreateBuilder()
                    .setEmbeds(eb.build())
                    .setComponents(rows)
                    .build();
        }

        private void waitForInteraction() {
            waiter.waitForEvent(ButtonInteractionEvent.class,
                e -> {
                    if (e.getMessageIdLong() != menuMessage.getIdLong()) return false;
                    return true;
                },
                e -> {
                    if (e.getUser().getIdLong() != event.getAuthor().getIdLong()) {
                        e.reply("❌ Das ist nicht dein Menü!").setEphemeral(true).queue();
                        waitForInteraction(); // Continue waiting
                        return;
                    }

                    String id = e.getComponentId();
                    AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();

                    if (id.equals(CLOSE)) {
                        e.deferEdit().queue();
                        menuMessage.delete().queue();
                        return;
                    }

                    handleButton(e, id, handler);
                },
                2, TimeUnit.MINUTES, () -> {
                    if (menuMessage != null) menuMessage.delete().queue(null, ignore -> {});
                });
        }

        private void handleButton(ButtonInteractionEvent e, String id, AudioHandler handler) {

             if (handler == null) {
                 if (id.equals(PLAY_NOW) || id.equals(BROWSE) || id.equals(ADD_TO_QUEUE)) {
                     // setUpHandler returns AudioHandler
                     handler = bot.getPlayerManager().setUpHandler(event.getGuild());
                     if (handler == null) {
                         e.reply("Could not set up audio player.").setEphemeral(true).queue();
                         waitForInteraction();
                         return;
                     }
                 }
             }

             switch (id) {
                 case PAUSE:
                     if (handler != null && handler.getPlayer().getPlayingTrack() != null) {
                         handler.getPlayer().setPaused(true);
                         e.deferEdit().queue();
                         update();
                     } else {
                         e.reply("Nothing playing.").setEphemeral(true).queue();
                         waitForInteraction();
                     }
                     break;
                 case RESUME:
                     if (handler != null && handler.getPlayer().getPlayingTrack() != null) {
                         handler.getPlayer().setPaused(false);
                         e.deferEdit().queue();
                         update();
                     } else {
                         e.reply("Nothing to resume.").setEphemeral(true).queue();
                         waitForInteraction();
                     }
                     break;
                 case STOP:
                     if (handler != null) {
                         handler.stopAndClear();
                         e.deferEdit().queue();
                         update();
                     } else {
                         e.reply("Nothing to stop.").setEphemeral(true).queue();
                         waitForInteraction();
                     }
                     break;
                 case SKIP:
                     if (handler != null) {
                         handler.getPlayer().stopTrack();
                         e.deferEdit().queue();
                         update();
                     } else {
                         e.reply("Nothing to skip.").setEphemeral(true).queue();
                         waitForInteraction();
                     }
                     break;
                 case PLAY_NOW:
                 case ADD_TO_QUEUE:
                 case BROWSE:
                     openFileBrowser(e);
                     break;
                 case SHOW_QUEUE:
                     if (handler == null || handler.getQueue().isEmpty()) {
                         e.reply("The queue is empty.").setEphemeral(true).queue();
                     } else {
                         List<String> tracks = new ArrayList<>();
                         handler.getQueue().getList().forEach(qt -> tracks.add(qt.getTrack().getInfo().title));
                         StringBuilder sb = new StringBuilder("**Queue:**\n");
                         for(int i=0; i<Math.min(10, tracks.size()); i++) {
                             sb.append(i+1).append(". ").append(tracks.get(i)).append("\n");
                         }
                         if (tracks.size() > 10) sb.append("And ").append(tracks.size()-10).append(" more...");
                         e.reply(sb.toString()).setEphemeral(true).queue();
                     }
                     waitForInteraction();
                     break;
                 case HELP:
                     e.reply("Commands: " + bot.getConfig().getPrefix() + "play <title|URL>, " + bot.getConfig().getPrefix() + "help").setEphemeral(true).queue();
                     waitForInteraction();
                     break;
                 case LYRICS:
                     e.reply("Lyrics not available").setEphemeral(true).queue();
                     waitForInteraction();
                     break;
                 default:
                     e.deferEdit().queue();
                     waitForInteraction();
             }
        }

        private void openFileBrowser(ButtonInteractionEvent buttonEvent) {
             String browserFolder = bot.getConfig().getBrowserFolder();
             FileSystemNavigator navigator = new FileSystemNavigator(browserFolder);
             try {
                 navigator.listItems("");
                 new FileBrowserMenu(waiter, buttonEvent, navigator, this).display();
             } catch (Exception ex) {
                 buttonEvent.reply("Failed to open browser: " + ex.getMessage()).setEphemeral(true).queue();
                 waitForInteraction();
             }
        }
    }

    private class FileBrowserMenu {
        private final EventWaiter waiter;
        private final ButtonInteractionEvent initialEvent;
        private final FileSystemNavigator navigator;
        private final MentionMenu parentMenu;
        private String currentPath = "";
        private int page = 0;
        private Message browserMessage;

        private final static String UP = "fb_up";
        private final static String BACK = "fb_back";
        private final static String NEXT = "fb_next";
        private final static String PREV = "fb_prev";
        private final static String ITEM_PREFIX = "fb_item_";

        public FileBrowserMenu(EventWaiter waiter, ButtonInteractionEvent event, FileSystemNavigator navigator, MentionMenu parent) {
            this.waiter = waiter;
            this.initialEvent = event;
            this.navigator = navigator;
            this.parentMenu = parent;
        }

        public void display() throws IOException {
             // We use editMessage on the interaction
             initialEvent.editMessage(MessageEditData.fromCreateData(renderMessage(currentPath))).queue(m -> {
                 // m is InteractionHook
                 m.retrieveOriginal().queue(msg -> {
                     browserMessage = msg;
                     waitForInteraction();
                 });
             });
        }

        public void update() throws IOException {
            browserMessage.editMessage(MessageEditData.fromCreateData(renderMessage(currentPath))).queue();
            waitForInteraction();
        }

        private MessageCreateData renderMessage(String path) throws IOException {
            List<FileInfo> contents = navigator.listItems(path);
            int itemsPerPage = bot.getConfig().getBrowserPageSize();
            if (itemsPerPage <= 0) itemsPerPage = 20;
            int totalPages = (int) Math.ceil((double) contents.size() / itemsPerPage);
            if (totalPages == 0) totalPages = 1;
            if (page >= totalPages) page = Math.max(0, totalPages - 1);
            if (page < 0) page = 0;

            int start = page * itemsPerPage;
            int end = Math.min(start + itemsPerPage, contents.size());
            List<FileInfo> pageContents = contents.subList(start, end);

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("📂 File Browser");
            eb.setDescription("**Current Path:** `" + (path.isEmpty() ? "/" : path) + "`\n\n");
            eb.setColor(Color.BLUE);

            if (contents.isEmpty()) eb.appendDescription("No audio files or directories found.");
            else {
                for (int i = 0; i < pageContents.size(); i++) {
                    FileInfo item = pageContents.get(i);
                    String emoji = item.isDirectory() ? "📁" : "🎵";
                    eb.appendDescription("`" + (i + 1) + ".` " + emoji + " " + item.getName() + "\n");
                }
            }
            eb.setFooter("Page " + (page + 1) + "/" + totalPages);

            List<ActionRow> rows = new ArrayList<>();
            List<Button> currentButtonRow = new ArrayList<>();

            for (int i = 0; i < pageContents.size(); i++) {
                FileInfo item = pageContents.get(i);
                String name = item.getName();
                if (name.length() > 50) name = name.substring(0, 47) + "...";
                String emoji = item.isDirectory() ? "📁" : "🎵";
                ButtonStyle style = item.isDirectory() ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY;
                currentButtonRow.add(Button.of(style, ITEM_PREFIX + (start + i), (i+1) + " " + name).withEmoji(Emoji.fromUnicode(emoji)));

                if (currentButtonRow.size() == 5) {
                    rows.add(ActionRow.of(currentButtonRow));
                    currentButtonRow = new ArrayList<>();
                }
            }
            if (!currentButtonRow.isEmpty()) rows.add(ActionRow.of(currentButtonRow));

            Button upButton = Button.secondary(UP, "Up").withEmoji(Emoji.fromUnicode("⬆️"));
            if (path.isEmpty()) upButton = upButton.asDisabled();
            Button prevButton = Button.secondary(PREV, "Prev").withEmoji(Emoji.fromUnicode("⬅️"));
            if (page == 0) prevButton = prevButton.asDisabled();
            Button nextButton = Button.secondary(NEXT, "Next").withEmoji(Emoji.fromUnicode("➡️"));
            if (page >= totalPages - 1) nextButton = nextButton.asDisabled();
            Button backButton = Button.danger(BACK, "Back").withEmoji(Emoji.fromUnicode("↩️"));

            rows.add(ActionRow.of(prevButton, upButton, backButton, nextButton));

            return new MessageCreateBuilder().setEmbeds(eb.build()).setComponents(rows).build();
        }

        private void waitForInteraction() {
            waiter.waitForEvent(ButtonInteractionEvent.class,
                e -> e.getMessageIdLong() == browserMessage.getIdLong() && e.getUser().getIdLong() == initialEvent.getUser().getIdLong(),
                e -> {
                    try {
                        String cid = e.getComponentId();

                        if (cid.equals(BACK)) {
                             e.deferEdit().queue();
                             parentMenu.update();
                             return;
                        }

                        e.deferEdit().queue();
                        if (cid.equals(UP)) {
                            if (!currentPath.isEmpty()) {
                                Path p = Paths.get(currentPath);
                                Path parent = p.getParent();
                                currentPath = parent == null ? "" : parent.toString();
                                page = 0;
                            }
                        } else if (cid.equals(NEXT)) {
                            page++;
                        } else if (cid.equals(PREV)) {
                            page--;
                        } else if (cid.startsWith(ITEM_PREFIX)) {
                            int index = Integer.parseInt(cid.substring(ITEM_PREFIX.length()));
                            List<FileInfo> contents = navigator.listItems(currentPath);
                            if (index >= 0 && index < contents.size()) {
                                FileInfo item = contents.get(index);
                                if (item.isDirectory()) {
                                    currentPath = item.getRelativePath();
                                    page = 0;
                                } else {
                                    playFile(navigator.getAbsolutePath(item.getRelativePath()), e);
                                }
                            }
                        }
                        update();
                    } catch (IOException ex) {
                         // handle error
                    }
                }, 2, TimeUnit.MINUTES, () -> {});
        }

        private void playFile(Path file, ButtonInteractionEvent event) {
            bot.getPlayerManager().loadItemOrdered(event.getGuild(), file.toAbsolutePath().toString(), new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    add(track);
                }
                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                     if(playlist.getTracks().size()==1 || playlist.isSearchResult())
                    {
                        AudioTrack single = playlist.getSelectedTrack()==null ? playlist.getTracks().get(0) : playlist.getSelectedTrack();
                        add(single);
                    }
                }
                @Override
                public void noMatches() {
                    browserMessage.editMessage(new MessageEditBuilder().setContent("No matches found.").build()).queue();
                }
                @Override
                public void loadFailed(FriendlyException exception) {
                     browserMessage.editMessage(new MessageEditBuilder().setContent("Failed to load: " + exception.getMessage()).build()).queue();
                }

                private void add(AudioTrack track) {
                    AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                    if (handler != null) {
                         int pos = handler.addTrack(new QueuedTrack(track, new RequestMetadata(event.getUser(), new RequestMetadata.RequestInfo(file.getFileName().toString(), file.toUri().toString()))));
                         String msg = "Added **" + track.getInfo().title + "** " + (pos==-1 ? "to play now." : "to queue pos " + pos + ".");
                         event.getHook().sendMessage(msg).setEphemeral(true).queue();
                    }
                }
            });
        }
    }
}
