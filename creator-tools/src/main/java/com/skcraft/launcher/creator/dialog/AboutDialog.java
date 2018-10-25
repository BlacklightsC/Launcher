/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.creator.dialog;

import com.skcraft.launcher.LauncherUtils;
import com.skcraft.launcher.creator.Creator;
import com.skcraft.launcher.swing.SwingHelper;
import lombok.extern.java.Log;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;

@Log
public class AboutDialog extends JDialog {

    private String version;

    public AboutDialog(Window parent) {
        super(parent, "\uc815\ubcf4", ModalityType.DOCUMENT_MODAL);

        try {
            Properties properties = LauncherUtils.loadProperties(Creator.class, "creator.properties", "com.skcraft.creator.propertiesFile");
            version = properties.getProperty("version", "????");
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to get version", e);
            version = "????";
        }

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        initComponents();
        setResizable(false);
        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        JPanel container = new JPanel();
        container.setLayout(new MigLayout("insets dialog"));

        container.add(new JLabel(SwingHelper.createIcon(Creator.class, "about_header.png")), "dock north");
        container.add(new JLabel("<html>\ubc84\uc804 " + version), "wrap");
        container.add(new JLabel("<html>GNU \uc57d\uc18c \uc77c\ubc18 \uacf5\uc911 \uc0ac\uc6a9 \ud5c8\uac00\uc11c, \ubc84\uc804 3"), "wrap, gapbottom unrel");
        container.add(new JLabel("<html>SKCraft \ud300\uc5d0\uc11c \uc81c\uc791\ud588\uc2b5\ub2c8\ub2e4. \uc6b0\ub9ac \ud648\ud398\uc774\uc9c0\uc5d0 \ubc29\ubb38 \ud574\uc8fc\uc138\uc694!"), "wrap, gapbottom unrel");

        JButton okButton = new JButton("\ud655\uc778");
        JButton sourceCodeButton = new JButton("\uc18c\uc2a4 \ucf54\ub4dc");
        JButton skCraftButton = new JButton("\uc6f9\uc0ac\uc774\ud2b8");

        container.add(sourceCodeButton, "span, split 3, sizegroup bttn");
        container.add(skCraftButton, "sizegroup bttn");
        container.add(okButton, "tag ok, sizegroup bttn");

        add(container, BorderLayout.CENTER);

        getRootPane().setDefaultButton(okButton);
        getRootPane().registerKeyboardAction(e -> okButton.doClick(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        okButton.addActionListener(e -> dispose());
        sourceCodeButton.addActionListener(e -> SwingHelper.openURL("https://github.com/SKCraft/Launcher", this));
        skCraftButton.addActionListener(e -> SwingHelper.openURL("http://www.skcraft.com", this));
    }

    public static void showAboutDialog(Window parent) {
        AboutDialog dialog = new AboutDialog(parent);
        dialog.setVisible(true);
    }
}

