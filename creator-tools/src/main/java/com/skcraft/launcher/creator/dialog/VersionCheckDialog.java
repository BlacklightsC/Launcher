/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.creator.dialog;

import com.jidesoft.swing.SearchableUtils;
import com.jidesoft.swing.TableSearchable;
import com.skcraft.launcher.swing.DefaultTable;
import com.skcraft.launcher.swing.SwingHelper;
import com.skcraft.launcher.swing.TableColumnAdjuster;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class VersionCheckDialog extends JDialog {

    @Getter private final JTable knownModsTable = new DefaultTable();
    @Getter private final JTable unknownModsTable = new DefaultTable();
    @Getter private final JButton closeButton = new JButton("\ub2eb\uae30");
    private final TableColumnAdjuster updateTableAdjuster = new TableColumnAdjuster(knownModsTable);
    private final TableColumnAdjuster unknownTableAdjuster = new TableColumnAdjuster(unknownModsTable);

    public VersionCheckDialog(Window parent) {
        super(parent, "\uc5c5\ub370\uc774\ud2b8 \ud655\uc778", ModalityType.DOCUMENT_MODAL);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        initComponents();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        updateTableAdjuster.adjustColumns();
        updateTableAdjuster.setDynamicAdjustment(true);

        unknownTableAdjuster.adjustColumns();
        unknownTableAdjuster.setDynamicAdjustment(true);

        knownModsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        knownModsTable.setAutoCreateRowSorter(true);

        unknownModsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        unknownModsTable.setAutoCreateRowSorter(true);

        JPanel container = new JPanel();
        container.setLayout(new MigLayout("insets dialog, fill"));

        container.add(new JLabel("\uc7a0\uc7ac\uc801\uc778 \uc5c5\ub370\uc774\ud2b8:"), "span");
        container.add(SwingHelper.wrapScrollPane(knownModsTable), "grow, pushy, span, w 500:900, h 230");

        container.add(new JLabel("\uc54c \uc218\uc5c6\ub294 \uc0c1\ud0dc:"), "span");
        container.add(SwingHelper.wrapScrollPane(unknownModsTable), "grow, pushy, span, w 500:900, h 150, gapbottom unrel, wrap");

        container.add(new JLabel("\ubc84\uc804 \ub370\uc774\ud130\ub294 NotEnoughMods.com\uc5d0\uc11c \uc81c\uacf5\ud569\ub2c8\ub2e4."), "");
        container.add(closeButton, "tag cancel, sizegroup bttn");

        add(container, BorderLayout.CENTER);

        getRootPane().registerKeyboardAction(e -> closeButton.doClick(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        TableSearchable tableSearchable;

        tableSearchable = SearchableUtils.installSearchable(knownModsTable);
        tableSearchable.setMainIndex(-1);

        tableSearchable = SearchableUtils.installSearchable(unknownModsTable);
        tableSearchable.setMainIndex(-1);
    }

}
