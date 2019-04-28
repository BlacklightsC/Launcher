/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.creator.dialog;

import com.skcraft.launcher.swing.DirectoryField;
import com.skcraft.launcher.swing.SwingHelper;
import com.skcraft.launcher.swing.TextFieldPopupMenu;
import lombok.Data;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;

public class BuildDialog extends JDialog {

    private final DirectoryField destDirField = new DirectoryField();
    private final JTextField versionText = new JTextField(20);
    private final JTextField manifestFilenameText = new JTextField(30);
    @Getter
    private BuildOptions options;

    public BuildDialog(Window parent) {
        super(parent, "\ub9b4\ub9ac\uc988 \ube4c\ub4dc", ModalityType.DOCUMENT_MODAL);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        initComponents();
        setResizable(false);
        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        versionText.setComponentPopupMenu(TextFieldPopupMenu.INSTANCE);
        manifestFilenameText.setComponentPopupMenu(TextFieldPopupMenu.INSTANCE);

        JPanel container = new JPanel();
        container.setLayout(new MigLayout("insets dialog"));

        container.add(new JLabel("\ubc84\uc804:"));
        container.add(versionText, "span");

        container.add(new JLabel("\ub9e4\ub2c8\ud398\uc2a4\ud2b8 \ud30c\uc77c\uba85:"));
        container.add(manifestFilenameText, "span");

        container.add(new JLabel("\ucd9c\ub825 \ub514\ub809\ud1a0\ub9ac:"));
        container.add(destDirField, "span");

        JButton buildButton = new JButton("\ube4c\ub4dc");
        JButton cancelButton = new JButton("\ucde8\uc18c");

        container.add(buildButton, "tag ok, span, split 2, sizegroup bttn");
        container.add(cancelButton, "tag cancel, sizegroup bttn");

        add(container, BorderLayout.CENTER);

        getRootPane().setDefaultButton(buildButton);
        getRootPane().registerKeyboardAction(e -> cancelButton.doClick(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        buildButton.addActionListener(e -> returnValue());
        cancelButton.addActionListener(e -> dispose());
    }

    private void returnValue() {
        String version = versionText.getText().trim();
        String manifestFilename = manifestFilenameText.getText().trim();

        if (version.isEmpty()) {
            SwingHelper.showErrorDialog(this, "\ubc84\uc804\uc744 \uc785\ub825\ud574\uc57c \ud569\ub2c8\ub2e4.", "\uc624\ub958");
            return;
        }

        if (manifestFilename.isEmpty()) {
            SwingHelper.showErrorDialog(this, "\ub9e4\ub2c8\ud398\uc2a4\ud2b8 \ud30c\uc77c\uba85\uc744 \uc785\ub825\ud574\uc57c \ud569\ub2c8\ub2e4.", "\uc624\ub958");
            return;
        }

        if (destDirField.getPath().isEmpty()) {
            SwingHelper.showErrorDialog(this, "\ucd9c\ub825 \ub514\ub809\ud1a0\ub9ac\ub97c \uc785\ub825\ud574\uc57c \ud569\ub2c8\ub2e4.", "\uc624\ub958");
            return;
        }

        options = new BuildOptions(version, manifestFilename, new File(destDirField.getPath()));
        dispose();
    }

    public static BuildOptions showBuildDialog(Window parent, String version, String manifestName, File destDir) {
        BuildDialog dialog = new BuildDialog(parent);
        dialog.versionText.setText(version);
        dialog.manifestFilenameText.setText(manifestName);
        dialog.destDirField.setPath(destDir.getAbsolutePath());
        dialog.setVisible(true);
        return dialog.getOptions();
    }

    @Data
    public static class BuildOptions {
        private final String version;
        private final String manifestFilename;
        private final File destDir;
    }

}
