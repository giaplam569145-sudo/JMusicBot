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
package com.jagrosh.jmusicbot.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class for navigating the file system safely.
 *
 * @author giaplam569145-sudo
 */
public class FileSystemNavigator
{
    /**
     * Lists files and directories in the given path, ensuring it is within the root.
     *
     * @param path The path to list contents of.
     * @param root The root directory to confine navigation to.
     * @return A list of paths found in the directory.
     * @throws IOException If an I/O error occurs.
     * @throws SecurityException If the path is outside the allowed root.
     */
    public static List<Path> getContents(Path path, Path root) throws IOException
    {
        Path absolutePath = path.toAbsolutePath().normalize();
        Path absoluteRoot = root.toAbsolutePath().normalize();

        if (!absolutePath.startsWith(absoluteRoot))
        {
            throw new SecurityException("Access denied: Path is outside the allowed root.");
        }

        try (Stream<Path> stream = Files.list(absolutePath))
        {
            return stream.filter(p -> {
                if (Files.isDirectory(p)) return true;
                String name = p.getFileName().toString().toLowerCase();
                return name.endsWith(".mp3") || name.endsWith(".flac") || name.endsWith(".wav")
                    || name.endsWith(".ogg") || name.endsWith(".m4a") || name.endsWith(".mp4")
                    || name.endsWith(".webm") || name.endsWith(".mkv");
            }).sorted((p1, p2) -> {
                if (Files.isDirectory(p1) && !Files.isDirectory(p2)) return -1;
                if (!Files.isDirectory(p1) && Files.isDirectory(p2)) return 1;
                return p1.getFileName().toString().compareToIgnoreCase(p2.getFileName().toString());
            }).collect(Collectors.toList());
        }
    }

    /**
     * Checks if the given path is the root directory.
     *
     * @param path The path to check.
     * @param root The root directory.
     * @return True if the path is the root.
     */
    public static boolean isRoot(Path path, Path root)
    {
        return path.toAbsolutePath().normalize().equals(root.toAbsolutePath().normalize());
    }
}
