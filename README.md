<img align="right" src="https://i.imgur.com/zrE80HY.png" height="200" width="200">

# JMusicBot

> **Note:** This is an unofficial fork of the original [JMusicBot by jagrosh](https://github.com/jagrosh/MusicBot), licensed under the [Apache License 2.0](LICENSE). This project has been updated and adapted to be compatible with modern versions of JDA and other dependencies, and it is not officially affiliated with the original project.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

A cross-platform Discord music bot with a clean interface, and that is easy to set up and run yourself!

[![Setup](http://i.imgur.com/VvXYp5j.png)](https://jmusicbot.com/setup)

## Changes from Original
This fork was created to bring JMusicBot up to a modern technical standard and ensure compatibility with the current Discord API. The key changes include:

*   **Updated to JDA 5:** The project has been fully ported to a stable version of JDA 5.x. This was necessary as JDA 4.x is no longer actively maintained.
*   **Switched Lavaplayer Fork:** Instead of the original Lavaplayer by `sedmelluq`, this fork now uses the actively maintained fork by `dev.arbjerg`, which is compatible with JDA 5 and offers better audio playback stability.
*   **Extensive Code Refactoring:** Due to breaking changes in JDA 5, large parts of the code have been refactored, especially regarding event handling, message/channel management, and Gateway Intents.
*   **Stability Improvements:** The new dependencies and adjustments aim for a more stable voice connection and overall reliability.
*   **General Maintenance:** Includes minor bug fixes and dependency updates.

## Features
  * Easy to run (just make sure Java is installed, and run!)
  * Fast loading of songs
  * No external keys needed (besides a Discord Bot token)
  * Smooth playback
  * Server-specific setup for the "DJ" role that can moderate the music
  * Clean and beautiful menus
  * Supports many sites, including Youtube, Soundcloud, and more
  * Supports many online radio/streams
  * Supports local files
  * Playlist support (both web/youtube, and local)

## Supported sources and formats
JMusicBot supports all sources and formats supported by [lavaplayer](https://github.com/sedmelluq/lavaplayer#supported-formats):
### Sources
  * YouTube
  * SoundCloud
  * Bandcamp
  * Vimeo
  * Twitch streams
  * Local files
  * HTTP URLs
### Formats
  * MP3
  * FLAC
  * WAV
  * Matroska/WebM (AAC, Opus or Vorbis codecs)
  * MP4/M4A (AAC codec)
  * OGG streams (Opus, Vorbis and FLAC codecs)
  * AAC streams
  * Stream playlists (M3U and PLS)

## Example
![Loading Example...](https://i.imgur.com/kVtTKvS.gif)

## Setup
Please see the [Setup Page](https://jmusicbot.com/setup) to run this bot yourself!

## Questions/Suggestions/Bug Reports
**Please check the [Issues List](https://github.com/giaplam569145-sudo/JMusicBot/issues) before suggesting a feature**. If you have a question, need troubleshooting help, or want to brainstorm a new feature, please start a [Discussion](https://github.com/giaplam569145-sudo/JMusicBot/discussions). If you'd like to suggest a feature or report a reproducible bug, please open an [Issue](https://github.com/giaplam569145-sudo/JMusicBot/issues) on this repository. If you like this bot, be sure to add a star to the libraries that make this possible: [**JDA**](https://github.com/DV8FromTheWorld/JDA) and [**lavaplayer**](https://github.com/sedmelluq/lavaplayer)!

## Commands
### Music
*   `!play <song|URL>`: Plays a song or adds it to the queue.
*   `!playlists`: Shows the available playlists.
*   `!nowplaying`: Shows the currently playing song.
*   `!remove <position|ALL>`: Removes a song from the queue.
*   `!scsearch <query>`: Searches SoundCloud.
*   `!search <query>`: Searches YouTube.
*   `!seek <time>`: Seeks to a specific time in the song.
*   `!shuffle`: Shuffles the queue.
*   `!skip`: Votes to skip the current song.
*   `!lyrics [song]`: Shows the lyrics of a song.

### DJ
*   `!forceremove <user>`: Removes all entries by a user from the queue.
*   `!forceskip`: Skips the current song.
*   `!movetrack <from> <to>`: Moves a track in the queue.
*   `!pause`: Pauses the current song.
*   `!playnext <song|URL>`: Plays a song next.
*   `!repeat [off|all|single]`: Sets the repeat mode.
*   `!skipto <position>`: Skips to a specific song in the queue.
*   `!stop`: Stops the music and clears the queue.
*   `!volume [0-150]`: Sets the volume.

### Admin
*   `!prefix <prefix|NONE>`: Sets a server-specific prefix.
*   `!queuetype [linear|fair]`: Changes the queue type.
*   `!setdj <rolename|NONE>`: Sets the DJ role.
*   `!settc <channel|NONE>`: Sets the text channel for music commands.
*   `!setvc <channel|NONE>`: Sets the voice channel for music.
*   `!setskip <0-100>`: Sets the skip percentage.

### Owner
*   `!autoplaylist <name|NONE>`: Sets the default playlist.
*   `!debug`: Shows debug info.
*   `!eval <code>`: Evaluates Nashorn code.
*   `!playlist <append|delete|make>`: Manages playlists.
*   `!setavatar <url>`: Sets the bot's avatar.
*   `!setgame [game]`: Sets the bot's game.
*   `!setname <name>`: Sets the bot's name.
*   `!setstatus <status>`: Sets the bot's status.
*   `!shutdown`: Shuts down the bot.

## Distribution
When distributing builds of this project, you must comply with the terms of the Apache License 2.0. This includes retaining the `LICENSE` file and any `NOTICE` files, as well as providing clear attribution to the original JMusicBot project by jagrosh.

## Editing
This bot (and the source code here) might not be easy to edit for inexperienced programmers. The main purpose of having the source public is to show the capabilities of the libraries, to allow others to understand how the bot works, and to allow those knowledgeable about java, JDA, and Discord bot development to contribute. There are many requirements and dependencies required to edit and compile it, and there will not be support provided for people looking to make changes on their own. Instead, consider making a feature request (see the above section). If you choose to make edits, please do so in accordance with the Apache 2.0 License.
