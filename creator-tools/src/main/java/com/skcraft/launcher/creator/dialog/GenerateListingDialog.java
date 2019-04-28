/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.creator.dialog;

import com.jidesoft.swing.SearchableUtils;
import com.jidesoft.swing.TableSearchable;
import com.skcraft.launcher.creator.Creator;
import com.skcraft.launcher.creator.model.swing.ListingType;
import com.skcraft.launcher.creator.model.swing.ListingTypeComboBoxModel;
import com.skcraft.launcher.swing.DefaultTable;
import com.skcraft.launcher.swing.DirectoryField;
import com.skcraft.launcher.swing.SwingHelper;
import com.skcraft.launcher.swing.TableColumnAdjuster;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class GenerateListingDialog extends JDialog {

    @Getter private final DirectoryField destDirField = new DirectoryField();
    @Getter private final JComboBox<ListingType> listingTypeCombo = new JComboBox<>(new ListingTypeComboBoxModel());
    @Getter private final JTable manifestsTable = new DefaultTable();
    @Getter private final JLabel gameKeyWarning = new JLabel("\uc120\ud0dd\ud55c \ubaa9\ub85d \uc720\ud615\uc740 '\uac8c\uc784 \ud0a4'\ub97c \uc0ac\uc6a9\ud558\uc5ec \ubaa8\ub4dc\ud329\uc744 \ucd94\uac00\ud558\ub294 \uac83\uc744 \uc9c0\uc6d0\ud558\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4.", SwingHelper.createIcon(Creator.class, "warning_icon.png"), SwingConstants.LEFT);

    @Getter private final JButton editManifestButton = new JButton("\ud3b8\uc9d1...");

    @Getter private final JButton generateButton = new JButton("\uc0dd\uc131");
    @Getter private final JButton cancelButton = new JButton("\ucde8\uc18c");

    @Getter private final TableColumnAdjuster manifestsTableAdjuster = new TableColumnAdjuster(manifestsTable);

    public GenerateListingDialog(Window parent) {
        super(parent, "\ud328\ud0a4\uc9c0 \ubaa9\ub85d \uc0dd\uc131", ModalityType.DOCUMENT_MODAL);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        initComponents();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        manifestsTableAdjuster.adjustColumns();
        manifestsTableAdjuster.setDynamicAdjustment(true);

        manifestsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        manifestsTable.setAutoCreateRowSorter(true);

        JPanel container = new JPanel();
        container.setLayout(new MigLayout("insets dialog, fill", "[grow 0][grow 100]"));

        container.add(new JLabel("\ucd9c\ub825 \ub514\ub809\ud1a0\ub9ac:"));
        container.add(destDirField, "span");

        container.add(new JLabel("\ud328\ud0a4\uc9c0 \ubaa9\ub85d \uc720\ud615:"));
        container.add(listingTypeCombo, "span");
        container.add(gameKeyWarning, "span, skip 1, hidemode 3");

        container.add(new JLabel("\ud3ec\ud568 \ud560 \ubaa8\ub4dc\ud329:"), "span, gaptop unrel");
        container.add(SwingHelper.wrapScrollPane(manifestsTable), "grow, pushy, span, w 500:650, h 170");
        container.add(editManifestButton, "gapbottom unrel, span, split 2");
        container.add(new JLabel("<html>\uc774\uc804\uc5d0 \uc120\ud0dd\ub41c \ubaa8\ub4dc\ud329\uacfc \uc5c5\ub85c\ub4dc \ub514\ub809\ud1a0\ub9ac\uc5d0 \uc788\ub294 \ubaa8\ub4dc\ud329\uc740 \uc0ac\uc6a9 \uac00\ub2a5\ud55c \uc635\uc158\uc785\ub2c8\ub2e4."), "gapbottom unrel");

        container.add(generateButton, "tag ok, span, split 2, sizegroup bttn");
        container.add(cancelButton, "tag cancel, sizegroup bttn");

        add(container, BorderLayout.CENTER);

        getRootPane().setDefaultButton(generateButton);
        getRootPane().registerKeyboardAction(e -> cancelButton.doClick(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        TableSearchable tableSearchable = SearchableUtils.installSearchable(manifestsTable);
        tableSearchable.setMainIndex(-1);
    }

}
