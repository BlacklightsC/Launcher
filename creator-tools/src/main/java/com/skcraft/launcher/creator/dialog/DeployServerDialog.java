/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.creator.dialog;

import com.skcraft.launcher.swing.DirectoryField;
import com.skcraft.launcher.swing.SwingHelper;
import lombok.Data;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;

public class DeployServerDialog extends JDialog {

    private final DirectoryField destDirField = new DirectoryField();
    private final JCheckBox cleanModsCheck = new JCheckBox("\ubc30\ud3ec\ud558\uae30 \uc804\uc5d0 \"mods/\" \ud3f4\ub354 \uc0ad\uc81c");
    @Getter
    private DeployOptions options;

    public DeployServerDialog(Window parent) {
        super(parent, "\uc11c\ubc84 \ud30c\uc77c \ubc30\ud3ec", ModalityType.DOCUMENT_MODAL);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        initComponents();
        setResizable(false);
        pack();
        setLocationRelativeTo(parent);

        cleanModsCheck.setSelected(true);
    }

    private void initComponents() {
        JPanel container = new JPanel();
        container.setLayout(new MigLayout("insets dialog"));

        container.add(new JLabel("\ucd9c\ub825 \ub514\ub809\ud1a0\ub9ac:"));
        container.add(destDirField, "span");

        container.add(cleanModsCheck, "span, gapbottom unrel");

        JButton buildButton = new JButton("\ubc30\ud3ec");
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
        String dir = destDirField.getPath();

        if (dir.isEmpty()) {
            SwingHelper.showErrorDialog(this, "\ub514\ub809\ud1a0\ub9ac\ub97c \uc785\ub825\ud574\uc57c \ud569\ub2c8\ub2e4.", "\uc624\ub958");
            return;
        }

        File dirFile = new File(dir);

        if (!dirFile.isDirectory()) {
            SwingHelper.showErrorDialog(this, "\uc120\ud0dd\ub41c \ub514\ub809\ud1a0\ub9ac\uac00 \uc874\uc7ac\ud558\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4.", "\uc624\ub958");
            return;
        }

        options = new DeployOptions(dirFile,cleanModsCheck.isSelected());
        dispose();
    }

    public static DeployOptions showDeployDialog(Window parent, File destDir) {
        DeployServerDialog dialog = new DeployServerDialog(parent);
        if (destDir != null) {
            dialog.destDirField.setPath(destDir.getAbsolutePath());
        }
        dialog.setVisible(true);
        return dialog.getOptions();
    }

    @Data
    public static class DeployOptions {
        private final File destDir;
        private final boolean cleanMods;
    }

}
