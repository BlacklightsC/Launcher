/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.creator.dialog;

import com.google.common.base.Joiner;
import com.skcraft.launcher.builder.FeaturePattern;
import com.skcraft.launcher.builder.FnPatternList;
import com.skcraft.launcher.creator.model.swing.RecommendationComboBoxModel;
import com.skcraft.launcher.model.modpack.Feature;
import com.skcraft.launcher.model.modpack.Feature.Recommendation;
import com.skcraft.launcher.swing.SwingHelper;
import com.skcraft.launcher.swing.TextFieldPopupMenu;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class FeaturePatternDialog extends JDialog {

    private static final Joiner NEW_LINE_JOINER = Joiner.on("\n");

    private final JTextField nameText = new JTextField(20);
    private final JTextArea descArea = new JTextArea(3, 40);
    private final JComboBox recommendationCombo = new JComboBox(new RecommendationComboBoxModel());
    private final JCheckBox selectedCheck = new JCheckBox("\uae30\ubcf8\uac12\uc744 \uc0ac\uc6a9\ud568\uc73c\ub85c \uc124\uc815");
    private final JTextArea includeArea = new JTextArea(8, 40);
    private final JTextArea excludeArea = new JTextArea(3, 40);

    private final FeaturePattern pattern;
    private boolean saved = false;

    public FeaturePatternDialog(Window parent, FeaturePattern pattern) {
        super(parent, "\uae30\ub2a5 \uc124\uc815", ModalityType.DOCUMENT_MODAL);

        this.pattern = pattern;

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        initComponents();
        setResizable(false);
        pack();
        setLocationRelativeTo(parent);

        copyFrom();
    }

    private void initComponents() {
        nameText.setComponentPopupMenu(TextFieldPopupMenu.INSTANCE);
        descArea.setComponentPopupMenu(TextFieldPopupMenu.INSTANCE);
        includeArea.setComponentPopupMenu(TextFieldPopupMenu.INSTANCE);
        excludeArea.setComponentPopupMenu(TextFieldPopupMenu.INSTANCE);

        descArea.setFont(nameText.getFont());
        includeArea.setFont(nameText.getFont());
        excludeArea.setFont(nameText.getFont());

        JPanel container = new JPanel();
        container.setLayout(new MigLayout("insets dialog"));

        container.add(new JLabel("\uae30\ub2a5 \uc774\ub984:"));
        container.add(nameText, "span");

        container.add(new JLabel("\ucd94\ucc9c\ub3c4:"));
        container.add(recommendationCombo, "span");

        container.add(selectedCheck, "span");

        container.add(new JLabel("\uc124\uba85:"), "wrap");
        container.add(SwingHelper.wrapScrollPane(descArea), "span");

        container.add(new JLabel("\ud3ec\ud568 \ud56d\ubaa9:"), "wrap");
        container.add(SwingHelper.wrapScrollPane(includeArea), "span");

        container.add(new JLabel("\uc81c\uc678 \ud56d\ubaa9:"), "wrap");
        container.add(SwingHelper.wrapScrollPane(excludeArea), "span, gapbottom unrel");

        JButton okButton = new JButton("\ud655\uc778");
        JButton cancelButton = new JButton("\ucde8\uc18c");

        container.add(okButton, "tag ok, span, split 2, sizegroup bttn");
        container.add(cancelButton, "tag cancel, sizegroup bttn");

        getRootPane().setDefaultButton(okButton);
        getRootPane().registerKeyboardAction(e -> cancelButton.doClick(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        add(container, BorderLayout.CENTER);

        okButton.addActionListener(e -> {
            if (nameText.getText().trim().isEmpty()) {
                SwingHelper.showErrorDialog(FeaturePatternDialog.this, "'\uae30\ub2a5 \uc774\ub984' \ub780\uc740 \ube44\uc6cc\ub458 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.", "\uc785\ub825 \uc624\ub958");
                return;
            }

            if (descArea.getText().trim().isEmpty()) {
                SwingHelper.showErrorDialog(FeaturePatternDialog.this, "'\uc124\uba85' \ub780\uc740 \ube44\uc6cc\ub458 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.", "\uc785\ub825 \uc624\ub958");
                return;
            }

            if (includeArea.getText().trim().isEmpty()) {
                SwingHelper.showErrorDialog(FeaturePatternDialog.this, "'\ud3ec\ud568 \ud56d\ubaa9' \ub780\uc740 \ube44\uc6cc\ub458 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.", "\uc785\ub825 \uc624\ub958");
                return;
            }

            copyTo();
            saved = true;
            dispose();
        });

        cancelButton.addActionListener(e -> dispose());
    }

    private void copyFrom() {
        if (pattern.getFeature() == null) {
            pattern.setFeature(new Feature());
        }

        if (pattern.getFilePatterns() == null) {
            pattern.setFilePatterns(new FnPatternList());
        }

        SwingHelper.setTextAndResetCaret(nameText, pattern.getFeature().getName());
        SwingHelper.setTextAndResetCaret(descArea, pattern.getFeature().getDescription());
        recommendationCombo.setSelectedItem(pattern.getFeature().getRecommendation());
        selectedCheck.setSelected(pattern.getFeature().isSelected());
        SwingHelper.setTextAndResetCaret(includeArea, NEW_LINE_JOINER.join(pattern.getFilePatterns().getInclude()));
        SwingHelper.setTextAndResetCaret(excludeArea, NEW_LINE_JOINER.join(pattern.getFilePatterns().getExclude()));
    }

    private void copyTo() {
        pattern.getFeature().setName(nameText.getText().trim());
        pattern.getFeature().setDescription(descArea.getText().trim());
        pattern.getFeature().setRecommendation((Recommendation) recommendationCombo.getSelectedItem());
        pattern.getFeature().setSelected(selectedCheck.isSelected());
        pattern.getFilePatterns().setInclude(SwingHelper.linesToList(includeArea.getText()));
        pattern.getFilePatterns().setExclude(SwingHelper.linesToList(excludeArea.getText()));
    }

    public static boolean showEditor(Window window, FeaturePattern pattern) {
        FeaturePatternDialog dialog = new FeaturePatternDialog(window, pattern);
        dialog.setVisible(true);
        return dialog.saved;
    }

}
