/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.creator.controller;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.skcraft.concurrency.Deferred;
import com.skcraft.concurrency.Deferreds;
import com.skcraft.concurrency.SettableProgress;
import com.skcraft.launcher.creator.controller.task.DirectoryWalker;
import com.skcraft.launcher.creator.dialog.VersionCheckDialog;
import com.skcraft.launcher.creator.model.creator.ModFile;
import com.skcraft.launcher.creator.model.swing.ModFileTableModel;
import com.skcraft.launcher.creator.util.ModInfoReader;
import com.skcraft.launcher.creator.util.ModInfoReader.ModInfo;
import com.skcraft.launcher.creator.util.NemModList;
import com.skcraft.launcher.creator.util.NemModList.ModEntry;
import com.skcraft.launcher.dialog.ProgressDialog;
import com.skcraft.launcher.swing.SwingHelper;
import com.skcraft.launcher.util.SwingExecutor;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class VersionCheckController {

    private final VersionCheckDialog dialog;
    private final ListeningExecutorService executor;

    public VersionCheckController(VersionCheckDialog dialog, ListeningExecutorService executor) {
        this.dialog = dialog;
        this.executor = executor;
    }

    public void showUpdates(File dir, String gameVersion, Window parentWindow) {
        initListeners();

        DirectoryWalker walker = new DirectoryWalker(dir);
        walker.setRecursive(false);
        walker.setFileFilter(pathname -> pathname.getName().endsWith(".jar"));

        ModInfoReader binaryInspector = new ModInfoReader();
        NemModList nemModList = new NemModList();

        SettableProgress progress = new SettableProgress("\ubaa8\ub4dc \uc815\ubcf4 \uac80\uc0c9 \uc911...", -1);

        Deferred<?> deferred = Deferreds.makeDeferred(executor.submit(walker), executor)
                .thenTap(() -> progress.set("NotEnoughMods\uc5d0\uc11c \ubc84\uc804 \ub370\uc774\ud130 \uac80\uc0c9 \uc911...", -1))
                .thenTap(() -> {
                    try {
                        nemModList.load(gameVersion);
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException("NotEnoughMods\uc5d0\uc11c \ubaa8\ub4dc \uc815\ubcf4\ub97c \uac80\uc0c9\ud558\ub294 \ub370 \uc2e4\ud328\ud588\uc2b5\ub2c8\ub2e4. Minecraft \ubc84\uc804\uc744 \uc9c0\uc6d0\ud558\uc9c0 \uc54a\ub294 \uac83 \uac19\uc2b5\ub2c8\ub2e4.", e);
                    }
                })
                .thenTap(() -> progress.set("\ub9e4\ub2c8\ud398\uc2a4\ud2b8\uc5d0 \ub300\ud55c \ubaa8\ub4dc \ud30c\uc77c \uc2a4\uce94 \uc911...", -1))
                .thenApply(files -> {
                    List<ModFile> mods = Lists.newArrayList();

                    for (File file : files) {
                        ModFile mod = new ModFile();
                        mod.setGameVersion(gameVersion);
                        mod.setFile(file);

                        List<? extends ModInfo> infoList = binaryInspector.detectMods(file);
                        if (!infoList.isEmpty()) {
                            ModInfo info = infoList.get(0);
                            mod.setModId(info.getModId());
                            mod.setName(info.getName());
                            mod.setVersion(info.getVersion());

                            if (info.getUrl() != null) {
                                mod.setUrl(getFirstUrl(info.getUrl(), "http://" + info.getUrl()));
                            }
                        }

                        if (mod.getModId() != null) {
                            ModEntry entry = nemModList.get(mod.getModId());

                            if (entry != null) {
                                mod.setLatestVersion(entry.getLatestVersion());
                                mod.setLatestDevVersion(entry.getLatestDevVersion());
                                if (entry.getUrl() != null) {
                                    mod.setUrl(entry.getUrl());
                                }
                            }
                        }

                        mods.add(mod);
                    }

                    return mods;
                })
                .handleAsync(mods -> {
                    List<ModFile> known = Lists.newArrayList();
                    List<ModFile> unknown = Lists.newArrayList();

                    for (ModFile mod : mods) {
                        if (mod.getVersion() != null && mod.getLatestVersion() != null) {
                            if (!mod.getCleanVersion().equals(mod.getLatestVersion()) && !mod.getCleanVersion().equals(mod.getLatestDevVersion())) {
                                known.add(mod);
                            }
                        } else {
                            unknown.add(mod);
                        }
                    }

                    dialog.getKnownModsTable().setModel(new ModFileTableModel(known));
                    dialog.getUnknownModsTable().setModel(new ModFileTableModel(unknown));
                    dialog.getKnownModsTable().getRowSorter().toggleSortOrder(1);
                    dialog.getUnknownModsTable().getRowSorter().toggleSortOrder(1);
                    dialog.setVisible(true);
                }, ex -> {
                }, SwingExecutor.INSTANCE);

        ProgressDialog.showProgress(parentWindow, deferred, progress, "\ubaa8\ub4dc \uc5c5\ub370\uc774\ud2b8 \ud655\uc778 \uc911...", "\ubaa8\ub4dc \uc5c5\ub370\uc774\ud2b8 \ud655\uc778 \uc911...");
        SwingHelper.addErrorDialogCallback(parentWindow, deferred);
    }

    private void initListeners() {
        dialog.getCloseButton().addActionListener(e -> dialog.dispose());

        ModTableMouseListener mouseListener = new ModTableMouseListener();
        dialog.getKnownModsTable().addMouseListener(mouseListener);
        dialog.getUnknownModsTable().addMouseListener(mouseListener);
    }

    @Nullable
    private static URL getFirstUrl(String... options) {
        for (String option : options) {
            try {
                return new URL(option);
            } catch (MalformedURLException ignored) {
            }
        }

        return null;
    }

    private class ModTableMouseListener extends MouseAdapter {

        public void mousePressed(MouseEvent e) {
            if (e.getClickCount() == 2) {
                JTable table = (JTable) e.getSource();
                Point point = e.getPoint();
                int selectedIndex = table.rowAtPoint(point);
                if (selectedIndex >= 0) {
                    selectedIndex = table.convertRowIndexToModel(selectedIndex);
                    ModFile mod = ((ModFileTableModel) table.getModel()).getMod(selectedIndex);
                    if (mod != null && mod.getUrl() != null) {
                        SwingHelper.openURL(mod.getUrl(), dialog);
                    }
                }
            }
        }

    }

}
