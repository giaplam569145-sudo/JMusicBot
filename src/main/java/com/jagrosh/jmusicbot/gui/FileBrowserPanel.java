/*
 * Copyright 2024 giaplam569145-sudo
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
package com.jagrosh.jmusicbot.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;

/**
 * A panel that displays a simple file browser.
 *
 * @author giaplam569145-sudo
 */
public class FileBrowserPanel extends JPanel {

    private final JTextField pathField;
    private final JList<File> fileList;
    private final DefaultListModel<File> listModel;
    private File currentDirectory;

    public FileBrowserPanel() {
        super(new BorderLayout());

        // Top panel for navigation
        JPanel topPanel = new JPanel(new BorderLayout());
        JButton upButton = new JButton("Up");
        pathField = new JTextField();
        pathField.setEditable(false);

        topPanel.add(upButton, BorderLayout.WEST);
        topPanel.add(pathField, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        // Center panel for file list
        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        fileList.setCellRenderer(new FileRenderer());
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(fileList);
        add(scrollPane, BorderLayout.CENTER);

        // Initial directory
        currentDirectory = new File(System.getProperty("user.dir"));
        refreshFileList();

        // Event listeners
        upButton.addActionListener(e -> navigateUp());

        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    File selectedFile = fileList.getSelectedValue();
                    if (selectedFile != null && selectedFile.isDirectory()) {
                        currentDirectory = selectedFile;
                        refreshFileList();
                    }
                }
            }
        });
    }

    private void navigateUp() {
        File parent = currentDirectory.getParentFile();
        if (parent != null) {
            currentDirectory = parent;
            refreshFileList();
        }
    }

    private void refreshFileList() {
        pathField.setText(currentDirectory.getAbsolutePath());
        listModel.clear();

        File[] files = currentDirectory.listFiles();
        if (files != null) {
            Arrays.sort(files, (f1, f2) -> {
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
                return f1.getName().compareToIgnoreCase(f2.getName());
            });

            for (File file : files) {
                listModel.addElement(file);
            }
        }
    }

    private static class FileRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof File) {
                File file = (File) value;
                setText(file.getName());
                try {
                    setIcon(FileSystemView.getFileSystemView().getSystemIcon(file));
                } catch (Exception e) {
                    // Ignore icon errors
                }
            }
            return this;
        }
    }
}
