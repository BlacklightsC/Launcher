/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.creator.controller;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.skcraft.concurrency.Deferred;
import com.skcraft.concurrency.Deferreds;
import com.skcraft.concurrency.SettableProgress;
import com.skcraft.launcher.InstanceList;
import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.auth.OfflineSession;
import com.skcraft.launcher.auth.Session;
import com.skcraft.launcher.builder.BuilderConfig;
import com.skcraft.launcher.builder.FnPatternList;
import com.skcraft.launcher.creator.Creator;
import com.skcraft.launcher.creator.model.creator.*;
import com.skcraft.launcher.creator.controller.task.*;
import com.skcraft.launcher.creator.dialog.*;
import com.skcraft.launcher.creator.dialog.BuildDialog.BuildOptions;
import com.skcraft.launcher.creator.dialog.DeployServerDialog.DeployOptions;
import com.skcraft.launcher.creator.model.swing.PackTableModel;
import com.skcraft.launcher.creator.server.TestServer;
import com.skcraft.launcher.creator.server.TestServerBuilder;
import com.skcraft.launcher.creator.swing.PackDirectoryFilter;
import com.skcraft.launcher.dialog.ConfigurationDialog;
import com.skcraft.launcher.dialog.ConsoleFrame;
import com.skcraft.launcher.dialog.LoginDialog;
import com.skcraft.launcher.dialog.ProgressDialog;
import com.skcraft.launcher.model.modpack.LaunchModifier;
import com.skcraft.launcher.persistence.Persistence;
import com.skcraft.launcher.swing.PopupMouseAdapter;
import com.skcraft.launcher.swing.SwingHelper;
import com.skcraft.launcher.util.MorePaths;
import com.skcraft.launcher.util.SwingExecutor;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class PackManagerController {

    private static final DateFormat VERSION_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    private static final Pattern FILENAME_SANITIZE = Pattern.compile("[^a-z0-9_\\-\\.]+");

    @Getter private final File workspaceDir;
    @Getter private final Creator creator;
    @Getter private final File workspaceFile;
    @Getter private final File dataDir;
    @Getter private final File distDir;
    @Getter private final File launcherDir;
    @Getter private File webRoot;
    @Getter private Workspace workspace;
    @Getter private final Launcher launcher;
    @Getter private final ListeningExecutorService executor;
    @Getter private final TestServer testServer;

    private File lastServerDestDir;

    private final PackManagerFrame frame;
    private PackTableModel packTableModel;

    public PackManagerController(PackManagerFrame frame, File workspaceDir, Creator creator) throws IOException {
        this.workspaceDir = workspaceDir;
        this.creator = creator;
        this.dataDir = Workspace.getDataDir(workspaceDir);
        workspaceFile = Workspace.getWorkspaceFile(workspaceDir);

        this.distDir = new File(workspaceDir, "_upload");
        launcherDir = new File(dataDir, "staging/launcher");
        File launcherConfigDir = new File(creator.getDataDir(), "launcher");
        this.webRoot = new File(dataDir, "staging/www");

        launcherDir.mkdirs();
        launcherConfigDir.mkdirs();
        webRoot.mkdirs();

        this.launcher = new Launcher(launcherDir, launcherConfigDir);
        this.executor = launcher.getExecutor();
        this.frame = frame;

        TestServerBuilder builder = new TestServerBuilder();
        builder.setBaseDir(webRoot);
        builder.setPort(0);
        testServer = builder.build();
    }

    public void show() {
        frame.setVisible(true);
        frame.setTitle("\ubaa8\ub4dc\ud329 \ud3b8\uc9d1\uae30 - [" + workspaceDir.getAbsolutePath() + "]");

        initListeners();
        loadWorkspace();

        Deferreds.makeDeferred(executor.submit(() -> {
            startServer();
            return null;
        }))
                .handle(
                        result -> {
                        },
                        (ex) -> SwingHelper.showErrorDialog(frame, "\ub85c\uceec \uc6f9 \uc11c\ubc84\ub97c \uc2dc\uc791\ud558\ub294 \ub370 \uc2e4\ud328\ud588\uc2b5\ub2c8\ub2e4. \ubaa8\ub4c8 \ud328\ud0a4\uc9c0\ub97c \ud14c\uc2a4\ud2b8 \ud560 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.", "\uc624\ub958", ex)
                );
    }

    private void startServer() throws Exception {
        testServer.start();

        launcher.getProperties().setProperty("newsUrl", "http://localhost:" + testServer.getLocalPort() + "/news.html");
        launcher.getProperties().setProperty("packageListUrl", "http://localhost:" + testServer.getLocalPort() + "/packages.json");
        launcher.getProperties().setProperty("selfUpdateUrl", "http://localhost:" + testServer.getLocalPort() + "/latest.json");
    }

    private void loadWorkspace() {
        PackLoader loader = new PackLoader();

        SettableProgress progress = new SettableProgress("\uc791\uc5c5 \uacf5\uac04 \ubd88\ub7ec\uc624\ub294 \uc911...", -1);

        Deferred<?> deferred = Deferreds.makeDeferred(executor.submit(() -> {
            Workspace workspace = Persistence.load(workspaceFile, Workspace.class);
            workspace.setDirectory(workspaceDir);
            workspace.load();
            if (!workspaceFile.exists()) {
                Persistence.commitAndForget(workspace);
            }
            this.workspace = workspace;
            return workspace;
        }))
                .thenTap(() -> progress.observe(loader))
                .thenApply(loader)
                .thenApplyAsync(packs -> {
                    JTable table = frame.getPackTable();
                    packTableModel = new PackTableModel(packs);
                    table.setModel(packTableModel);
                    packTableModel.fireTableDataChanged();
                    table.getRowSorter().toggleSortOrder(1);
                    if (packTableModel.getRowCount() > 0) {
                        table.addRowSelectionInterval(0, 0);
                    }

                    return packs;
                }, SwingExecutor.INSTANCE);

        ProgressDialog.showProgress(frame, deferred, progress, "\uc791\uc5c5 \uacf5\uac04 \ubd88\ub7ec\uc624\ub294 \uc911...", "\uc791\uc5c5 \uacf5\uac04 \ubd88\ub7ec\uc624\ub294 \uc911...");
        SwingHelper.addErrorDialogCallback(frame, deferred);
    }

    private boolean checkPackLoaded(Pack pack) {
        if (pack.isLoaded()) {
            return true;
        } else {
            SwingHelper.showErrorDialog(frame, "\uc120\ud0dd\ud55c \ud328\ud0a4\uc9c0\ub97c \ubd88\ub7ec\uc62c \uc218 \uc5c6\uc2b5\ub2c8\ub2e4. \uc791\uc5c5 \uacf5\uac04\uc5d0\uc11c \uc81c\uac70\ud558\uac70\ub098 \uc704\uce58\ub97c \ubcc0\uacbd\ud574\uc57c\ud569\ub2c8\ub2e4.", "\uc624\ub958");
            return false;
        }
    }

    public Optional<Pack> getPackFromIndex(int selectedIndex, boolean requireLoaded) {
        if (selectedIndex >= 0) {
            Pack pack = workspace.getPacks().get(selectedIndex);
            if (pack != null && (!requireLoaded || checkPackLoaded(pack))) {
                return Optional.fromNullable(pack);
            }
        }
        return Optional.absent();
    }

    public Optional<Pack> getSelectedPack(boolean requireLoaded) {
        JTable table = frame.getPackTable();
        int selectedIndex = table.getSelectedRow();
        if (selectedIndex >= 0) {
            selectedIndex = table.convertRowIndexToModel(selectedIndex);
            Pack pack = workspace.getPacks().get(selectedIndex);
            if (pack != null && (!requireLoaded || checkPackLoaded(pack))) {
                return Optional.fromNullable(pack);
            }
        }

        SwingHelper.showErrorDialog(frame, "\ubaa9\ub85d\uc5d0\uc11c \ubaa8\ub4dc\ud329\uc744 \uc120\ud0dd\ud558\uc138\uc694.", "\uc624\ub958");
        return Optional.absent();
    }

    public boolean writeWorkspace() {
        try {
            Persistence.commit(workspace);
            return true;
        } catch (IOException e) {
            SwingHelper.showErrorDialog(frame, "\uc791\uc5c5 \uacf5\uac04\uc758 \ud604\uc7ac \uc0c1\ud0dc\ub97c \ub514\uc2a4\ud06c\uc5d0 \uae30\ub85d\ud558\uc9c0 \ubabb\ud588\uc2b5\ub2c8\ub2e4." +
                    "\ucd5c\uadfc \ubd88\ub7ec\uc628 \ubaa8\ub4dc\ud329 \ubaa9\ub85d\uc758 \ubcc0\uacbd \uc0ac\ud56d\uc740 \ub2e4\uc74c \ubd88\ub7ec\uc624\uae30\uc2dc \uc791\uc5c5 \uc601\uc5ed\uc5d0 \ub098\ud0c0\ub098\uc9c0 \uc54a\uc744 \uc218 \uc788\uc2b5\ub2c8\ub2e4.", "\uc624\ub958", e);
            return false;
        }
    }

    public boolean writeBuilderConfig(Pack pack, BuilderConfig config) {
        try {
            Persistence.write(pack.getConfigFile(), config, Persistence.L2F_LIST_PRETTY_PRINTER);
            return true;
        } catch (IOException e) {
            SwingHelper.showErrorDialog(frame, "modpack.json\uc744 \ub514\uc2a4\ud06c\uc5d0 \uae30\ub85d\ud558\uc9c0 \ubabb\ud558\uc600\uc2b5\ub2c8\ub2e4.", "\uc624\ub958", e);
            return false;
        }
    }

    private boolean isOfflineEnabled() {
        CreatorConfig config = creator.getConfig();

        if (config.isOfflineEnabled()) {
            return true;
        } else {
            Session session = LoginDialog.showLoginRequest(frame, launcher);
            if (session != null) {
                config.setOfflineEnabled(true);
                Persistence.commitAndForget(config);
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean canAddPackDir(File dir) {
        try {
            if (dir.exists() && !dir.isDirectory()) {
                SwingHelper.showErrorDialog(frame, "\uc120\ud0dd\ud55c \uacbd\ub85c\uac00 \ub514\ub809\ud1a0\ub9ac\uac00 \uc544\ub2cc \uc774\ubbf8 \uc874\uc7ac\ud558\ub294 \ud30c\uc77c\uc785\ub2c8\ub2e4.", "\uc624\ub958");
                return false;
            } else if (dir.getCanonicalPath().equals(workspaceDir.getCanonicalPath())) {
                SwingHelper.showErrorDialog(frame, "\uc791\uc5c5 \uacf5\uac04\uc744 \ub514\ub809\ud1a0\ub9ac\ub85c \uc120\ud0dd\ud560 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.", "\uc624\ub958");
                return false;
            } else if (workspace.hasPack(dir)) {
                SwingHelper.showErrorDialog(frame, "\uc774 \uc791\uc5c5 \uacf5\uac04\uc5d0\ub294 \uc774\ubbf8 \ud574\ub2f9 \ub514\ub809\ud1a0\ub9ac\ub97c \uc0ac\uc6a9\ud558\ub294 \ubaa8\ub4dc\ud329\uc774 \uc788\uc2b5\ub2c8\ub2e4.", "\uc624\ub958");
                return false;
            } else {
                return true;
            }
        } catch (IOException e) {
            SwingHelper.showErrorDialog(frame, "\ubaa8\ub4dc\ud329\uc744 \ucd94\uac00 \ud560 \uc218 \uc788\ub294\uc9c0 \ud655\uc778\ud558\ub294 \uc911 \uc608\uae30\uce58 \uc54a\uc740 \uc624\ub958\uac00 \ubc1c\uc0dd\ud588\uc2b5\ub2c8\ub2e4.", "\uc624\ub958", e);
            return false;
        }
    }

    public boolean addPackToWorkspace(Pack pack) {
        pack.load();

        try {
            File base = workspaceDir;
            File child = pack.getDirectory();
            if (MorePaths.isSubDirectory(base, child)) {
                pack.setLocation(MorePaths.relativize(base, child));
            }
        } catch (IOException e) {
            SwingHelper.showErrorDialog(frame, "\uc791\uc5c5 \uacf5\uac04 \ub610\ub294 \ubaa8\ub4dc\ud329 \ub514\ub809\ud1a0\ub9ac\uac00 \uc81c\uac70\ub418\uc5b4 \uc608\uae30\uce58 \uc54a\uc740 \uc624\ub958\uac00 \ubc1c\uc0dd\ud588\uc2b5\ub2c8\ub2e4. ", "\uc624\ub958", e);
            return false;
        }

        List<Pack> packs = workspace.getPacks();
        pack.setWorkspace(workspace);
        packs.add(pack);
        packTableModel.fireTableRowsInserted(packs.size() - 1, packs.size() - 1);
        writeWorkspace();

        return true;
    }

    public void updatePackInWorkspace(Pack pack) {
        List<Pack> packs = workspace.getPacks();
        pack.load();
        int index = packs.indexOf(pack);
        if (index >= 0) {
            packTableModel.fireTableRowsUpdated(index, index);
        }
        writeWorkspace();
    }

    public boolean removePackFromWorkspace(Pack pack) {
        if (workspace.getPacks().remove(pack)) {
            packTableModel.fireTableDataChanged();
            writeWorkspace();
            return true;
        } else {
            return false;
        }
    }

    private void addDefaultConfig(BuilderConfig config) {
        LaunchModifier launchModifier = new LaunchModifier();
        launchModifier.setFlags(ImmutableList.of("-Dfml.ignoreInvalidMinecraftCertificates=true"));
        config.setLaunchModifier(launchModifier);

        FnPatternList userFiles = new FnPatternList();
        userFiles.setInclude(Lists.newArrayList("options.txt", "optionsshaders.txt"));
        userFiles.setExclude(Lists.<String>newArrayList());
        config.setUserFiles(userFiles);
    }

    private void initListeners() {
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                try {
                    testServer.stop();
                } catch (Exception ignored) {
                }
                System.exit(0); // TODO: Proper shutdown
            }
        });

        frame.getPackTable().addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    JTable table = (JTable) e.getSource();
                    Point point = e.getPoint();
                    int selectedIndex = table.rowAtPoint(point);
                    if (selectedIndex >= 0) {
                        selectedIndex = table.convertRowIndexToModel(selectedIndex);
                        Optional<Pack> optional = getPackFromIndex(selectedIndex, true);
                        if (optional.isPresent()) {
                            if (e.isControlDown()) {
                                SwingHelper.browseDir(optional.get().getDirectory(), frame);
                            } else {
                                startTest(optional.get(), false);
                            }
                        }
                    }
                }
            }
        });

        frame.getPackTable().addMouseListener(new PopupMouseAdapter() {
            @Override
            protected void showPopup(MouseEvent e) {
                JTable table = (JTable) e.getSource();
                Point point = e.getPoint();
                int selectedIndex = table.rowAtPoint(point);
                if (selectedIndex >= 0) {
                    table.setRowSelectionInterval(selectedIndex, selectedIndex);
                    Optional<Pack> optional = getSelectedPack(false);
                    if (optional.isPresent()) {
                        popupPackMenu(e.getComponent(), e.getX(), e.getY(), optional.get());
                    }
                }
            }
        });

        frame.getNewPackMenuItem().addActionListener(event -> tryAddPackViaDialog());

        frame.getNewPackAtLocationMenuItem().addActionListener(e -> tryAddPackViaDirectory(true));

        frame.getImportPackMenuItem().addActionListener(event -> tryAddPackViaDirectory(false));

        frame.getRemovePackItem().addActionListener(e -> {
            Optional<Pack> optional = getSelectedPack(false);

            if (optional.isPresent()) {
                Pack pack = optional.get();

                if (!pack.isLoaded() || SwingHelper.confirmDialog(frame, "\uc774 \ubaa8\ub4dc\ud329\uc744 \uc81c\uac70\ud558\uc2dc\uaca0\uc2b5\ub2c8\uae4c? \ud30c\uc77c\uc740 \uc0ad\uc81c\ub418\uc9c0 \uc54a\uc73c\uba70, " +
					    "\ub098\uc911\uc5d0 '\uae30\uc874 \ud328\ud0a4\uc9c0 \ucd94\uac00\ud558\uae30'\ub97c \ud1b5\ud574 \ubaa8\ub4dc\ud329\uc744 \ub2e4\uc2dc \ubd88\ub7ec\uc62c \uc218 \uc788\uc2b5\ub2c8\ub2e4.", "\ud655\uc778")) {
                    removePackFromWorkspace(pack);
                }
            }
        });

        frame.getDeletePackItem().addActionListener(e -> {
            Optional<Pack> optional = getSelectedPack(false);

            if (optional.isPresent()) {
                Pack pack = optional.get();

                String input = JOptionPane.showInputDialog(
                        frame,
                        "'" + pack.getDirectory().getAbsolutePath() + "'\ub97c \uc0ad\uc81c\ud558\uc2dc\uaca0\uc2b5\ub2c8\uae4c?" +
                                "\ub3d9\uc758\ud560 \uacbd\uc6b0, \uc544\ub798\uc5d0 'delete'\uc744 \uc785\ub825\ud558\uc138\uc694.",
                        "\ud655\uc778",
                        JOptionPane.WARNING_MESSAGE);

                if (input != null && input.replaceAll("'", "").equalsIgnoreCase("delete")) {
                    removePackFromWorkspace(pack);

                    DirectoryDeleter deleter = new DirectoryDeleter(pack.getDirectory());
                    Deferred<?> deferred = Deferreds.makeDeferred(executor.submit(deleter), executor);
                    ProgressDialog.showProgress(frame, deferred, deleter, "\ubaa8\ub4dc\ud329 \uc0ad\uc81c \uc911...", "\ubaa8\ub4dc\ud329 \uc0ad\uc81c \uc911...");
                    SwingHelper.addErrorDialogCallback(frame, deferred);
                } else if (input != null) {
                    SwingHelper.showMessageDialog(frame, "\uc62c\ubc14\ub978 \ub2e8\uc5b4\ub97c \uc785\ub825\ud558\uc9c0 \uc54a\uc558\uc2b5\ub2c8\ub2e4. \uc544\ubb34\uac83\ub3c4 \uc0ad\uc81c\ub418\uc9c0 \uc54a\uc558\uc2b5\ub2c8\ub2e4.", "\uc2e4\ud328", null, JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        frame.getChangePackLocationMenuItem().addActionListener(e -> {
            Optional<Pack> optional = getSelectedPack(false);

            if (optional.isPresent()) {
                Pack pack = optional.get();

                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("\ud328\ud0a4\uc9c0\ub97c \uc62e\uae38 \ud3f4\ub354\ub97c \uc120\ud0dd\ud558\uc138\uc694");
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setFileFilter(new PackDirectoryFilter());

                File dir = workspaceDir;

                do {
                    chooser.setCurrentDirectory(dir);
                    int returnVal = chooser.showOpenDialog(frame);

                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        dir = chooser.getSelectedFile();
                    } else {
                        return;
                    }
                } while (!canAddPackDir(dir));

                pack.setLocation(dir.getAbsolutePath());
                updatePackInWorkspace(pack);
            }
        });

        frame.getRefreshMenuItem().addActionListener(e -> loadWorkspace());

        frame.getQuitMenuItem().addActionListener(e -> {
            frame.dispose();
        });

        frame.getEditConfigMenuItem().addActionListener(e -> {
            Optional<Pack> optional = getSelectedPack(true);

            if (optional.isPresent()) {
                Pack pack = optional.get();
                File file = pack.getConfigFile();
                BuilderConfig config = Persistence.read(file, BuilderConfig.class);

                if (BuilderConfigDialog.showEditor(frame, config)) {
                    writeBuilderConfig(pack, config);
                    updatePackInWorkspace(pack);
                }
            }
        });

        frame.getOpenFolderMenuItem().addActionListener(e -> {
            Optional<Pack> optional = getSelectedPack(true);

            if (optional.isPresent()) {
                SwingHelper.browseDir(optional.get().getDirectory(), frame);
            }
        });

        frame.getCheckProblemsMenuItem().addActionListener(e -> {
            Optional<Pack> optional = getSelectedPack(true);

            if (optional.isPresent()) {
                ProblemChecker checker = new ProblemChecker(optional.get());
                Deferred<?> deferred = Deferreds.makeDeferred(executor.submit(checker), executor)
                        .handleAsync(this::showProblems, (ex) -> {
                        }, SwingExecutor.INSTANCE);
                SwingHelper.addErrorDialogCallback(frame, deferred);
            }
        });

        frame.getTestMenuItem().addActionListener(e -> {
            Optional<Pack> optional = getSelectedPack(true);

            if (optional.isPresent()) {
                Pack pack = optional.get();
                startTest(pack, false);
            }
        });

        frame.getTestOnlineMenuItem().addActionListener(e -> {
            Optional<Pack> optional = getSelectedPack(true);

            if (optional.isPresent()) {
                Pack pack = optional.get();
                startTest(pack, true);
            }
        });

        frame.getOptionsMenuItem().addActionListener(e -> {
            ConfigurationDialog configDialog = new ConfigurationDialog(frame, launcher);
            configDialog.setVisible(true);
        });

        frame.getClearInstanceMenuItem().addActionListener(e -> {
            DirectoryDeleter deleter = new DirectoryDeleter(launcher.getInstancesDir());
            Deferred<?> deferred = Deferreds.makeDeferred(executor.submit(deleter), executor);
            ProgressDialog.showProgress(frame, deferred, deleter, "\ud14c\uc2a4\ud2b8 \uc778\uc2a4\ud134\uc2a4 \uc0ad\uc81c \uc911...", "\ud14c\uc2a4\ud2b8 \uc778\uc2a4\ud134\uc2a4 \uc0ad\uc81c \uc911...");
            SwingHelper.addErrorDialogCallback(frame, deferred);
        });

        frame.getClearWebRootMenuItem().addActionListener(e -> {
            DirectoryDeleter deleter = new DirectoryDeleter(webRoot);
            Deferred<?> deferred = Deferreds.makeDeferred(executor.submit(deleter), executor);
            ProgressDialog.showProgress(frame, deferred, deleter, "\uc6f9 \uc11c\ubc84 \ud30c\uc77c \uc0ad\uc81c \uc911...", "\uc6f9 \uc11c\ubc84 \ud30c\uc77c \uc0ad\uc81c \uc911...");
            SwingHelper.addErrorDialogCallback(frame, deferred);
        });

        frame.getBuildMenuItem().addActionListener(e -> {
            Optional<Pack> optional = getSelectedPack(true);

            if (optional.isPresent()) {
                Pack pack = optional.get();
                buildPack(pack);
            }
        });

        frame.getDeployServerMenuItem().addActionListener(e -> {
            Optional<Pack> optional = getSelectedPack(true);

            if (optional.isPresent()) {
                Pack pack = optional.get();
                DeployOptions options = DeployServerDialog.showDeployDialog(frame, lastServerDestDir);

                if (options != null) {
                    ConsoleFrame.showMessages();

                    File destDir = options.getDestDir();
                    destDir.mkdirs();
                    lastServerDestDir = destDir;

                    ServerDeploy deploy = new ServerDeploy(pack.getSourceDir(), options);
                    Deferred<?> deferred = Deferreds.makeDeferred(executor.submit(deploy), executor)
                            .handleAsync(r -> SwingHelper.showMessageDialog(frame, "\uc11c\ubc84 \ubc30\ud3ec\uac00 \uc644\ub8cc\ub418\uc5c8\uc2b5\ub2c8\ub2e4!", "\uc131\uacf5", null, JOptionPane.INFORMATION_MESSAGE),
                                    ex -> {
                                    },
                                    SwingExecutor.INSTANCE);
                    ProgressDialog.showProgress(frame, deferred, deploy, "\ud30c\uc77c \ubc30\ud3ec \uc911...", "\uc11c\ubc84 \ud30c\uc77c \ubc30\ud3ec \uc911...");
                    SwingHelper.addErrorDialogCallback(frame, deferred);
                }
            }
        });

        frame.getGeneratePackagesMenuItem().addActionListener(e -> {
            List<ManifestEntry> entries = workspace.getPackageListingEntries();
            ManifestInfoEnumerator enumerator = new ManifestInfoEnumerator(distDir);
            Deferred<?> deferred = Deferreds.makeDeferred(executor.submit(() -> enumerator.apply(entries)))
                    .handleAsync(loaded -> {
                        GenerateListingDialog dialog = new GenerateListingDialog(frame);
                        GenerateListingController controller = new GenerateListingController(dialog, workspace, loaded, executor);
                        controller.setOutputDir(distDir);
                        controller.show();
                    }, ex -> {
                    }, SwingExecutor.INSTANCE);
            ProgressDialog.showProgress(frame, deferred, new SettableProgress("\uac80\uc0c9 \uc911...", -1), "\ub9e4\ub2c8\ud398\uc2a4\ud2b8 \uac80\uc0c9 \uc911...", "\ub9e4\ub2c8\ud398\uc2a4\ud2b8 \uac80\uc0c9 \uc911...");
            SwingHelper.addErrorDialogCallback(frame, deferred);
        });

        frame.getVersionCheckMenuItem().addActionListener(e -> {
            Optional<Pack> optional = getSelectedPack(true);

            if (optional.isPresent()) {
                Pack pack = optional.get();

                VersionCheckDialog dialog = new VersionCheckDialog(frame);
                VersionCheckController controller = new VersionCheckController(dialog, executor);
                    controller.showUpdates(pack.getModsDir(), pack.getCachedConfig().getGameVersion(), frame);
            }
        });

        frame.getOpenOutputFolderMenuItem().addActionListener(e -> SwingHelper.browseDir(distDir, frame));

        frame.getOpenWorkspaceFolderMenuItem().addActionListener(e1 -> SwingHelper.browseDir(workspaceDir, frame));

        frame.getOpenLauncherFolderMenuItem().addActionListener(e1 -> SwingHelper.browseDir(launcherDir, frame));

        frame.getOpenWebRootMenuItem().addActionListener(e1 -> SwingHelper.browseDir(webRoot, frame));

        frame.getOpenConsoleMenuItem().addActionListener(e -> {
            ConsoleFrame.showMessages();
        });

        frame.getDocsMenuItem().addActionListener(e -> {
            SwingHelper.openURL("https://github.com/SKCraft/Launcher/wiki", frame);
        });

        frame.getAboutMenuItem().addActionListener(e -> {
            AboutDialog.showAboutDialog(frame);
        });

        SwingHelper.addActionListeners(frame.getNewPackButton(), frame.getNewPackMenuItem().getActionListeners());
        SwingHelper.addActionListeners(frame.getImportButton(), frame.getImportPackMenuItem().getActionListeners());
        SwingHelper.addActionListeners(frame.getEditConfigButton(), frame.getEditConfigMenuItem().getActionListeners());
        SwingHelper.addActionListeners(frame.getOpenFolderButton(), frame.getOpenFolderMenuItem().getActionListeners());
        SwingHelper.addActionListeners(frame.getCheckProblemsButton(), frame.getCheckProblemsMenuItem().getActionListeners());
        SwingHelper.addActionListeners(frame.getTestButton(), frame.getTestMenuItem().getActionListeners());
        SwingHelper.addActionListeners(frame.getBuildButton(), frame.getBuildMenuItem().getActionListeners());
    }

    private void popupPackMenu(Component component, int x, int y, Pack pack) {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem menuItem;

        menuItem = new JMenuItem("modpack.json \ud3b8\uc9d1...");
        menuItem.addActionListener(e -> frame.getEditConfigMenuItem().doClick());
        popup.add(menuItem);

        menuItem = new JMenuItem("\ub514\ub809\ud1a0\ub9ac \uc5f4\uae30");
        menuItem.addActionListener(e -> frame.getOpenFolderMenuItem().doClick());
        popup.add(menuItem);

        menuItem = new JMenuItem("\ubb38\uc81c \uac80\uc0c9");
        menuItem.addActionListener(e -> frame.getCheckProblemsMenuItem().doClick());
        popup.add(menuItem);

        popup.addSeparator();

        menuItem = new JMenuItem("\ud14c\uc2a4\ud2b8");
        menuItem.addActionListener(e -> frame.getTestMenuItem().doClick());
        popup.add(menuItem);

        menuItem = new JMenuItem("\uc628\ub77c\uc778\uc73c\ub85c \ud14c\uc2a4\ud2b8");
        menuItem.addActionListener(e -> frame.getTestOnlineMenuItem().doClick());
        popup.add(menuItem);

        menuItem = new JMenuItem("\ube4c\ub4dc...");
        menuItem.addActionListener(e -> frame.getBuildMenuItem().doClick());
        popup.add(menuItem);

        menuItem = new JMenuItem("\uc11c\ubc84 \ubc30\ud3ec...");
        menuItem.addActionListener(e -> frame.getDeployServerMenuItem().doClick());
        popup.add(menuItem);

        popup.addSeparator();

        menuItem = new JMenuItem("\uc704\uce58 \ubcc0\uacbd...");
        menuItem.addActionListener(e -> frame.getChangePackLocationMenuItem().doClick());
        popup.add(menuItem);

        menuItem = new JMenuItem("\uc81c\uac70...");
        menuItem.addActionListener(e -> frame.getRemovePackItem().doClick());
        popup.add(menuItem);

        menuItem = new JMenuItem("\uc601\uad6c \uc0ad\uc81c...");
        menuItem.addActionListener(e -> frame.getDeletePackItem().doClick());
        popup.add(menuItem);

        popup.show(component, x, y);
    }

    private void tryAddPackViaDialog() {
        BuilderConfig config = new BuilderConfig();
        addDefaultConfig(config);
        Pack pack = new Pack();
        File dir;

        do {
            if (BuilderConfigDialog.showEditor(frame, config)) {
                dir = new File(workspaceDir, config.getName());
            } else {
                return;
            }
        } while (!canAddPackDir(dir));

        pack.setLocation(dir.getAbsolutePath());

        if (pack.getConfigFile().exists()) {
            if (SwingHelper.confirmDialog(frame, "\uc791\uc5c5 \uacf5\uac04\uc5d0 \uc774\ubbf8 \ud574\ub2f9 \ubaa8\ub4dc\ud329\uacfc \ub3d9\uc77c\ud55c \uc774\ub984\uc744 \uc0ac\uc6a9\ud558\ub294 \ubaa8\ub4dc\ud329\uc774 \uc788\uc2b5\ub2c8\ub2e4. \ubcd1\ud569\ud558\uc2dc\uaca0\uc2b5\ub2c8\uae4c?", "\ucda9\ub3cc")) {
                addPackToWorkspace(pack);
            }
        } else {
            if (writeBuilderConfig(pack, config)) {
                pack.createGuideFolders();
                addPackToWorkspace(pack);
            }
        }
    }

    private void tryAddPackViaDirectory(boolean createNew) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("\ud328\ud0a4\uc9c0\uac00 \uc788\ub294 \ud3f4\ub354\ub97c \uc120\ud0dd\ud558\uc138\uc694");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setFileFilter(new PackDirectoryFilter());

        File dir = workspaceDir;

        do {
            chooser.setCurrentDirectory(dir);
            int returnVal = chooser.showOpenDialog(frame);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                dir = chooser.getSelectedFile();
            } else {
                return;
            }
        } while (!canAddPackDir(dir));

        Pack pack = new Pack();
        pack.setLocation(dir.getAbsolutePath());

        if (pack.getConfigFile().exists()) {
            if (createNew) {
                if (SwingHelper.confirmDialog(frame, "\uc774\ubbf8 \uadf8 \ub514\ub809\ud1a0\ub9ac\uc5d0 \ubaa8\ub4dc\ud329\uc774 \uc874\uc7ac\ud569\ub2c8\ub2e4. \uac00\uc838\uc624\uc2dc\uaca0\uc2b5\ub2c8\uae4c?", "\uc774\ubbf8 \uc874\uc7ac\ud568")) {
                    addPackToWorkspace(pack);
                }
            } else {
                addPackToWorkspace(pack);
            }
        } else if (createNew || SwingHelper.confirmDialog(frame, "\ubaa8\ub4dc\ud329\uc774 \uc544\ub2cc \ub514\ub809\ud1a0\ub9ac\ub97c \uc120\ud0dd\ud588\uc2b5\ub2c8\ub2e4." +
                "\ud574\ub2f9 \ud3f4\ub354\ub97c \ubaa8\ub4dc\ud329\uc73c\ub85c \ubcc0\ud658\ud558\ub294 \ub370 \ud544\uc694\ud55c \ud30c\uc77c\uc744 \uc0dd\uc131\ud558\uc2dc\uaca0\uc2b5\ub2c8\uae4c?", "\uac00\uc838\uc624\uae30 \uc624\ub958")) {

            BuilderConfig config = new BuilderConfig();
            addDefaultConfig(config);

            if (BuilderConfigDialog.showEditor(frame, config)) {
                if (writeBuilderConfig(pack, config)) {
                    pack.createGuideFolders();
                    addPackToWorkspace(pack);
                }
            }
        }
    }

    private void startTest(Pack pack, boolean online) {
        Session session;

        if (online) {
            session = LoginDialog.showLoginRequest(frame, launcher);
            if (session == null) {
                return;
            }
        } else {
            if (!isOfflineEnabled()) {
                return;
            }

            session = new OfflineSession("Player");
        }

        String version = generateVersionFromDate();

        PackBuilder builder = new PackBuilder(pack, webRoot, version, "staging.json", false, false);
        InstanceList.Enumerator enumerator = launcher.getInstances().createEnumerator();
        TestLauncher instanceLauncher = new TestLauncher(launcher, frame, pack.getCachedConfig().getName(), session);

        SettableProgress progress = new SettableProgress(builder);

        ConsoleFrame.showMessages();

        Deferred<?> deferred = Deferreds.makeDeferred(executor.submit(builder), executor)
                .thenTap(() -> progress.set("\ud14c\uc2a4\ud2b8 \ub7f0\ucc98\uc5d0 \uc778\uc2a4\ud134\ud2b8 \ubd88\ub7ec\uc624\ub294 \uc911...", -1))
                .thenRun(enumerator)
                .thenTap(() -> progress.set("\uc2e4\ud589\ud558\ub294 \uc911", -1))
                .thenApply(instanceLauncher)
                .handleAsync(result -> ConsoleFrame.hideMessages(), ex -> {}, SwingExecutor.INSTANCE);

        ProgressDialog.showProgress(frame, deferred, progress, "\ud14c\uc2a4\ud2b8 \uc778\uc2a4\ud134\ud2b8 \uc124\uc815 \uc911...", "\uc2dc\uc791\uc744 \uc704\ud55c \ud30c\uc77c \uc900\ube44 \uc911...");
        SwingHelper.addErrorDialogCallback(frame, deferred);
    }

    private void buildPack(Pack pack) {
        String initialVersion = generateVersionFromDate();
        BuildOptions options = BuildDialog.showBuildDialog(frame, initialVersion, generateManifestName(pack), distDir);

        if (options != null) {
            ConsoleFrame.showMessages();
            PackBuilder builder = new PackBuilder(pack, options.getDestDir(), options.getVersion(), options.getManifestFilename(), false, true);
            Deferred<?> deferred = Deferreds.makeDeferred(executor.submit(builder), executor)
                    .handleAsync(result -> {
                        ConsoleFrame.hideMessages();
                        SwingHelper.showMessageDialog(frame, "\ud328\ud0a4\uc9c0 \ud30c\uc77c\uc744 \uc0dd\uc131\ud588\uc2b5\ub2c8\ub2e4.", "\uc131\uacf5", null, JOptionPane.INFORMATION_MESSAGE);
                    }, ex -> {}, SwingExecutor.INSTANCE);
            ProgressDialog.showProgress(frame, deferred, builder, "\ubaa8\ub4dc\ud329 \ube4c\ub4dc \uc911...", "\ubaa8\ub4dc\ud329 \ube4c\ub4dc \uc911...");
            SwingHelper.addErrorDialogCallback(frame, deferred);
        }
    }

    private void showProblems(List<Problem> problems) {
        if (problems.isEmpty()) {
            SwingHelper.showMessageDialog(frame, "\uc7a0\uc7ac\uc801\uc778 \ubb38\uc81c\uc810\uc774 \ubc1c\uacac\ub418\uc9c0 \uc54a\uc558\uc2b5\ub2c8\ub2e4.", "\uc131\uacf5", null, JOptionPane.INFORMATION_MESSAGE);
        } else {
            ProblemViewer viewer = new ProblemViewer(frame, problems);
            viewer.setVisible(true);
        }
    }

    public String generateManifestName(Pack pack) {
        File file = pack.getConfigFile();
        if (file.exists()) {
            BuilderConfig config = Persistence.read(file, BuilderConfig.class, true);
            if (config != null) {
                String name = Strings.nullToEmpty(config.getName());
                name = name.toLowerCase();
                name = FILENAME_SANITIZE.matcher(name).replaceAll("-");
                name = name.trim();
                if (!name.isEmpty()) {
                    return name + ".json";
                }
            }
        }

        return "my_modpack.json";
    }

    public static String generateVersionFromDate() {
        Date today = Calendar.getInstance().getTime();
        return VERSION_DATE_FORMAT.format(today);
    }

}
