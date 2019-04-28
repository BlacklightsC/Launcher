/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.dialog;

import com.skcraft.launcher.swing.ActionListeners;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class AboutDialog extends JDialog {

    public AboutDialog(Window parent) {
        super(parent, "\uc815\ubcf4", ModalityType.DOCUMENT_MODAL);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        initComponents();
        setResizable(false);
        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        JPanel container = new JPanel();
        container.setLayout(new MigLayout("insets dialog"));

        container.add(new JLabel("<html>GNU \uc57d\uc18c \uc77c\ubc18 \uacf5\uc911 \uc0ac\uc6a9 \ud5c8\uac00\uc11c, \ubc84\uc804 3"), "wrap, gapbottom unrel");
        container.add(new JLabel("<html>\ub204\uad6c\ub098 \uc0ac\uc6a9\ud560 \uc218\uc788\ub294 \uc624\ud508 \uc18c\uc2a4 \uc0ac\uc6a9\uc790 \uc815\uc758 \ub7f0\ucc98 \ud50c\ub7ab\ud3fc\uc778<br>" +
                "SKCraft Launcher\ub97c \uc0ac\uc6a9\ud558\uace0 \uc788\uc2b5\ub2c8\ub2e4."), "wrap, gapbottom unrel");
        container.add(new JLabel("<html>SKCraft\ub294 \uadc0\ud558\uac00 \uc0ac\uc6a9\ud558\ub294 \ub7f0\ucc98\uc758 \ubc84\uc804\uc744 \ubc18\ub4dc\uc2dc<br>" +
                "\ubcf4\uc99d\ud558\uc9c0\ub294 \uc54a\uc2b5\ub2c8\ub2e4."), "wrap, gapbottom unrel");

        JButton okButton = new JButton("\ud655\uc778");
        JButton sourceCodeButton = new JButton("\uc6f9\uc0ac\uc774\ud2b8");

        container.add(sourceCodeButton, "span, split 3, sizegroup bttn");
        container.add(okButton, "tag ok, sizegroup bttn");

        add(container, BorderLayout.CENTER);

        getRootPane().setDefaultButton(okButton);
        getRootPane().registerKeyboardAction(ActionListeners.dispose(this), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        okButton.addActionListener(ActionListeners.dispose(this));
        sourceCodeButton.addActionListener(ActionListeners.openURL(this, "https://github.com/SKCraft/Launcher"));
    }

    public static void showAboutDialog(Window parent) {
        AboutDialog dialog = new AboutDialog(parent);
        dialog.setVisible(true);
    }
}

