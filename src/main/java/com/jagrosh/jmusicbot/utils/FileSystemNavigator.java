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
package com.jagrosh.jmusicbot.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class for navigating the file system safely.
 */
public class FileSystemNavigator {

    private final Path root;

    public FileSystemNavigator(String rootPath) {
        this.root = Paths.get(rootPath).toAbsolutePath().normalize();
    }

    /**
     * Lists files and directories in the given path relative to the root.
     *
     * @param relativePath The path relative to the root.
     * @return A list of FileInfo objects representing the contents.
     * @throws IOException If an I/O error occurs.
     * @throws SecurityException If the path tries to escape the root.
     */
    public List<FileInfo> listItems(String relativePath) throws IOException, SecurityException {
        Path targetPath = root.resolve(relativePath).toAbsolutePath().normalize();

        if (!targetPath.startsWith(root)) {
            throw new SecurityException("Access denied: Path attempts to escape the root directory.");
        }

        if (!Files.exists(targetPath) || !Files.isDirectory(targetPath)) {
             return Collections.emptyList();
        }

        try (Stream<Path> stream = Files.list(targetPath)) {
            return stream
                    .map(path -> new FileInfo(
                            path.getFileName().toString(),
                            Files.isDirectory(path),
                            root.relativize(path).toString()
                    ))
                    .sorted((a, b) -> {
                        if (a.isDirectory() && !b.isDirectory()) return -1;
                        if (!a.isDirectory() && b.isDirectory()) return 1;
                        return a.getName().compareToIgnoreCase(b.getName());
                    })
                    .collect(Collectors.toList());
        }
    }

    public Path getAbsolutePath(String relativePath) throws SecurityException {
         Path targetPath = root.resolve(relativePath).toAbsolutePath().normalize();
         if (!targetPath.startsWith(root)) {
            throw new SecurityException("Access denied: Path attempts to escape the root directory.");
        }
        return targetPath;
    }

    public static class FileInfo {
        private final String name;
        private final boolean isDirectory;
        private final String relativePath;

        public FileInfo(String name, boolean isDirectory, String relativePath) {
            this.name = name;
            this.isDirectory = isDirectory;
            this.relativePath = relativePath;
        }

        public String getName() {
            return name;
        }

        public boolean isDirectory() {
            return isDirectory;
        }

        public String getRelativePath() {
            return relativePath;
        }
    }
}
