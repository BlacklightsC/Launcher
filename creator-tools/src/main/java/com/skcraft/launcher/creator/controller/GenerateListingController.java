/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.creator.controller;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.skcraft.concurrency.Deferred;
import com.skcraft.concurrency.Deferreds;
import com.skcraft.concurrency.SettableProgress;
import com.skcraft.launcher.creator.dialog.GenerateListingDialog;
import com.skcraft.launcher.creator.dialog.ManifestEntryDialog;
import com.skcraft.launcher.creator.model.creator.ManifestEntry;
import com.skcraft.launcher.creator.model.creator.Workspace;
import com.skcraft.launcher.creator.model.swing.ListingType;
import com.skcraft.launcher.creator.model.swing.ManifestEntryTableModel;
import com.skcraft.launcher.dialog.ProgressDialog;
import com.skcraft.launcher.persistence.Persistence;
import com.skcraft.launcher.swing.SwingHelper;
import com.skcraft.launcher.util.SwingExecutor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

public class GenerateListingController {

    private final GenerateListingDialog dialog;
    private final Workspace workspace;
    private final List<ManifestEntry> manifestEntries;
    private final ListeningExecutorService executor;
    private final ManifestEntryTableModel manifestTableModel;

    public GenerateListingController(GenerateListingDialog dialog, Workspace workspace, List<ManifestEntry> manifestEntries, ListeningExecutorService executor) {
        this.dialog = dialog;
        this.workspace = workspace;
        this.manifestEntries = manifestEntries;
        this.executor = executor;

        this.manifestTableModel = new ManifestEntryTableModel(manifestEntries);
        dialog.getManifestsTable().setModel(manifestTableModel);
        dialog.getManifestsTableAdjuster().adjustColumns();

        initListeners();

        setListingType(workspace.getPackageListingType());
    }

    public void setOutputDir(File dir) {
        dialog.getDestDirField().setPath(dir.getAbsolutePath());
    }

    public void setListingType(ListingType type) {
        dialog.getListingTypeCombo().setSelectedItem(type);
    }

    public void show() {
        dialog.setVisible(true);
    }

    public Optional<ManifestEntry> getManifestFromIndex(int selectedIndex) {
        if (selectedIndex >= 0) {
            ManifestEntry manifest = manifestEntries.get(selectedIndex);
            if (manifest != null) {
                return Optional.fromNullable(manifest);
            }
        }
        return Optional.absent();
    }

    public Optional<ManifestEntry> getSelectedManifest() {
        JTable table = dialog.getManifestsTable();
        int selectedIndex = table.getSelectedRow();
        if (selectedIndex >= 0) {
            selectedIndex = table.convertRowIndexToModel(selectedIndex);
            ManifestEntry manifest = manifestEntries.get(selectedIndex);
            if (manifest != null) {
                return Optional.fromNullable(manifest);
            }
        }

        SwingHelper.showErrorDialog(dialog, "\ubaa9\ub85d\uc5d0\uc11c \ubaa8\ub4dc\ud329\uc744 \uc120\ud0dd\ud558\uc138\uc694.", "\uc624\ub958");
        return Optional.absent();
    }

    private void updateManifestEntryInTable(ManifestEntry manifestEntry) {
        int index = manifestEntries.indexOf(manifestEntry);
        if (index >= 0) {
            manifestTableModel.fireTableRowsUpdated(index, index);
        }
    }

    private void initListeners() {
        dialog.getManifestsTable().addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    JTable table = (JTable) e.getSource();
                    Point point = e.getPoint();
                    int selectedIndex = table.rowAtPoint(point);
                    if (selectedIndex >= 0) {
                        selectedIndex = table.convertRowIndexToModel(selectedIndex);
                        Optional<ManifestEntry> optional = getManifestFromIndex(selectedIndex);
                        if (optional.isPresent()) {
                            if (showModifyDialog(optional.get())) {
                                updateManifestEntryInTable(optional.get());
                            }
                        }
                    }
                }
            }
        });

        dialog.getListingTypeCombo().addItemListener(e -> {
            ListingType type = (ListingType) e.getItem();
            dialog.getGameKeyWarning().setVisible(!type.isGameKeyCompatible());
        });

        dialog.getEditManifestButton().addActionListener(e -> {
            Optional<ManifestEntry> optional = getSelectedManifest();
            if (optional.isPresent()) {
                if (showModifyDialog(optional.get())) {
                    updateManifestEntryInTable(optional.get());
                }
            }
        });

        dialog.getGenerateButton().addActionListener(e -> tryGenerate());

        dialog.getCancelButton().addActionListener(e -> dialog.dispose());
    }

    private boolean showModifyDialog(ManifestEntry manifestEntry) {
        ManifestEntryDialog modifyDialog = new ManifestEntryDialog(dialog);
        modifyDialog.setTitle(manifestEntry.getManifestInfo().getLocation() + " \ud3b8\uc9d1");
        ManifestEntryController controller = new ManifestEntryController(modifyDialog, manifestEntry);
        return controller.show();
    }

    private boolean tryGenerate() {
        String path = dialog.getDestDirField().getPath().trim();

        if (path.isEmpty()) {
            SwingHelper.showErrorDialog(dialog, "\ub514\ub809\ud1a0\ub9ac\ub97c \uc785\ub825\ud574\uc57c \ud569\ub2c8\ub2e4.", "\uc624\ub958");
            return false;
        }

        List<ManifestEntry> selected = manifestEntries.stream()
                .filter(ManifestEntry::isSelected)
                .sorted()
                .collect(Collectors.toCollection(Lists::newArrayList));

        if (selected.isEmpty()) {
            SwingHelper.showErrorDialog(dialog, "\uc801\uc5b4\ub3c4 \ud558\ub098\uc758 \ud328\ud0a4\uc9c0\uac00 \ud328\ud0a4\uc9c0 \ubaa9\ub85d\uc5d0 \ub098\ud0c0\ub098\ub3c4\ub85d \uc120\ud0dd\ub418\uc5b4\uc57c \ud569\ub2c8\ub2e4.", "\uc624\ub958");
            return false;
        }

        ListingType listingType = (ListingType) dialog.getListingTypeCombo().getSelectedItem();
        File destDir = new File(path);
        destDir.mkdirs();
        File file = new File(destDir, listingType.getFilename());

        workspace.setPackageListingEntries(selected);
        workspace.setPackageListingType(listingType);
        Persistence.commitAndForget(workspace);

        SettableProgress progress = new SettableProgress("\ud328\ud0a4\uc9c0 \ubaa9\ub85d \uc0dd\uc131 \uc911...", -1);

        Deferred<?> deferred = Deferreds.makeDeferred(executor.submit(() -> listingType.generate(selected)))
                .thenTap(() -> progress.set("\uc774\uc804 \ud328\ud0a4\uc9c0 \ubaa9\ub85d \ud30c\uc77c \uc0ad\uc81c \uc911...", -1))
                .thenApply(input -> {
                    for (ListingType otherListingType : ListingType.values()) {
                        File f = new File(destDir, otherListingType.getFilename());
                        if (f.exists()) {
                            f.delete();
                        }
                    }

                    return input;
                })
                .thenTap(() -> progress.set("\ub514\uc2a4\ud06c\uc5d0 \ud328\ud0a4\uc9c0 \ubaa9\ub85d \uc791\uc131 \uc911 ...", -1))
                .thenApply(input -> {
                    try {
                        Files.write(input, file, Charset.forName("UTF-8"));
                        return file;
                    } catch (IOException e) {
                        throw new RuntimeException("\ub514\uc2a4\ud06c\uc5d0 \ud328\ud0a4\uc9c0 \ubaa9\ub85d \ud30c\uc77c\uc744 \uae30\ub85d\ud558\uc9c0 \ubabb\ud588\uc2b5\ub2c8\ub2e4.", e);
                    }
                })
                .handleAsync(v -> {
                    if (listingType.isGameKeyCompatible()) {
                        SwingHelper.showMessageDialog(dialog, "\ud328\ud0a4\uc9c0 \ubaa9\ub85d\uc744 \uc0dd\uc131\ud588\uc2b5\ub2c8\ub2e4.", "\uc131\uacf5", null, JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        SwingHelper.showMessageDialog(dialog, "\ud328\ud0a4\uc9c0 \ubaa9\ub85d\uc744 \uc0dd\uc131\ud588\uc2b5\ub2c8\ub2e4.\n\n" +
                                "\uac8c\uc784 \ud0a4\uac00 \uc124\uc815\ub41c \ubaa8\ub4dc\ud329\uc740 \ucd94\uac00\ub418\uc9c0 \uc54a\uc558\uc2b5\ub2c8\ub2e4.",
                                "\uc131\uacf5", null, JOptionPane.INFORMATION_MESSAGE);
                    }
                    dialog.dispose();
                    SwingHelper.browseDir(destDir, dialog);
                }, ex -> {}, SwingExecutor.INSTANCE);

        ProgressDialog.showProgress(dialog, deferred, progress, "\ud328\ud0a4\uc9c0 \ubaa9\ub85d \uc791\uc131 \uc911...", "\ud328\ud0a4\uc9c0 \ubaa9\ub85d \uc791\uc131 \uc911...");
        SwingHelper.addErrorDialogCallback(dialog, deferred);

        return true;
    }

}
