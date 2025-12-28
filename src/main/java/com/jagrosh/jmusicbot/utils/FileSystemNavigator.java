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
package com.jagrosh.jmusicbot.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class for navigating the file system safely.
 * Ensures all navigation stays within the configured root directory.
 */
public class FileSystemNavigator {

    private final Path root;

    /**
     * Constructs a new FileSystemNavigator.
     *
     * @param rootPath The absolute or relative path to the root directory.
     */
    public FileSystemNavigator(String rootPath) {
        this.root = Paths.get(rootPath).toAbsolutePath().normalize();
    }

    /**
     * Gets the root path.
     * @return The root path.
     */
    public Path getRoot() {
        return root;
    }

    /**
     * lists the contents of a directory relative to the root.
     *
     * @param relativePath The path relative to the root.
     * @return A list of Paths representing the contents.
     * @throws IOException If an I/O error occurs.
     * @throws SecurityException If the path attempts to escape the root.
     */
    public List<Path> listContents(String relativePath) throws IOException {
        Path target = root.resolve(relativePath).normalize();

        if (!target.startsWith(root)) {
            throw new SecurityException("Access denied: Cannot navigate outside the root directory.");
        }

        if (!Files.exists(target) || !Files.isDirectory(target)) {
            return Collections.emptyList();
        }

        try (Stream<Path> stream = Files.list(target)) {
            return stream
                .filter(p -> {
                    try {
                        return !Files.isHidden(p);
                    } catch (IOException e) {
                        return false;
                    }
                })
                .sorted((p1, p2) -> {
                    boolean d1 = Files.isDirectory(p1);
                    boolean d2 = Files.isDirectory(p2);
                    if (d1 && !d2) return -1;
                    if (!d1 && d2) return 1;
                    return p1.getFileName().toString().compareToIgnoreCase(p2.getFileName().toString());
                })
                .collect(Collectors.toList());
        }
    }

    /**
     * Checks if a path is a directory.
     * @param path The path to check.
     * @return True if directory.
     */
    public boolean isDirectory(Path path) {
        return Files.isDirectory(path);
    }

    /**
     * Resolves a relative path against the root.
     * @param relativePath
     * @return Absolute path
     */
    public Path resolve(String relativePath) {
        return root.resolve(relativePath).normalize();
    }
}
