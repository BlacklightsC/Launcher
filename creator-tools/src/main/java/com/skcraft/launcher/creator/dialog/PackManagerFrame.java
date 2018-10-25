/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.creator.dialog;

import com.jidesoft.swing.SearchableUtils;
import com.jidesoft.swing.TableSearchable;
import com.skcraft.launcher.creator.Creator;
import com.skcraft.launcher.swing.DefaultTable;
import com.skcraft.launcher.swing.SwingHelper;
import com.skcraft.launcher.swing.TableColumnAdjuster;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class PackManagerFrame extends JFrame {

    @Getter private final JButton newPackButton = new JButton("\uc0c8 \ud328\ud0a4\uc9c0", SwingHelper.createIcon(Creator.class, "new.png"));
    @Getter private final JButton importButton = new JButton("\uae30\uc874 \ud328\ud0a4\uc9c0", SwingHelper.createIcon(Creator.class, "import.png"));
    @Getter private final JButton editConfigButton = new JButton("\ud3b8\uc9d1", SwingHelper.createIcon(Creator.class, "edit.png"));
    @Getter private final JButton openFolderButton = new JButton("\uc5f4\uae30", SwingHelper.createIcon(Creator.class, "open_folder.png"));
    @Getter private final JButton checkProblemsButton = new JButton("\uac80\uc99d", SwingHelper.createIcon(Creator.class, "check.png"));
    @Getter private final JButton testButton = new JButton("\ud14c\uc2a4\ud2b8", SwingHelper.createIcon(Creator.class, "test.png"));
    @Getter private final JButton buildButton = new JButton("\ube4c\ub4dc", SwingHelper.createIcon(Creator.class, "build.png"));

    @Getter private final JMenuItem newPackMenuItem = new JMenuItem("\uc0c8 \ud328\ud0a4\uc9c0 \ub9cc\ub4e4\uae30...");
    @Getter private final JMenuItem newPackAtLocationMenuItem = new JMenuItem("\ub2e4\ub978 \uc704\uce58\ub85c \uc0c8 \ud328\ud0a4\uc9c0 \ub9cc\ub4e4\uae30...");
    @Getter private final JMenuItem importPackMenuItem = new JMenuItem("\uae30\uc874 \ud328\ud0a4\uc9c0 \ucd94\uac00\ud558\uae30...");
    @Getter private final JMenuItem changePackLocationMenuItem = new JMenuItem("\ud328\ud0a4\uc9c0 \uc704\uce58 \ubcc0\uacbd...");
    @Getter private final JMenuItem refreshMenuItem = new JMenuItem("\uc791\uc5c5 \uacf5\uac04 \ub2e4\uc2dc \ubd88\ub7ec\uc624\uae30");
    @Getter private final JMenuItem removePackItem = new JMenuItem("\ud328\ud0a4\uc9c0 \uc81c\uac70...");
    @Getter private final JMenuItem deletePackItem = new JMenuItem("\ud328\ud0a4\uc9c0 \uc601\uad6c \uc0ad\uc81c...");
    @Getter private final JMenuItem quitMenuItem = new JMenuItem("\uc885\ub8cc");
    @Getter private final JMenuItem editConfigMenuItem = new JMenuItem("modpack.json \ud3b8\uc9d1...");
    @Getter private final JMenuItem openFolderMenuItem = new JMenuItem("\ub514\ub809\ud1a0\ub9ac \uc5f4\uae30");
    @Getter private final JMenuItem checkProblemsMenuItem = new JMenuItem("\ubb38\uc81c \uac80\uc0c9...");
    @Getter private final JMenuItem testMenuItem = new JMenuItem("\ud14c\uc2a4\ud2b8");
    @Getter private final JMenuItem testOnlineMenuItem = new JMenuItem("\uc628\ub77c\uc778\uc73c\ub85c \ud14c\uc2a4\ud2b8");
    @Getter private final JMenuItem optionsMenuItem = new JMenuItem("\ud14c\uc2a4\ud2b8 \ub7f0\ucc98 \uc124\uc815...");
    @Getter private final JMenuItem clearInstanceMenuItem = new JMenuItem("\ud14c\uc2a4\ud2b8 \ub7f0\ucc98 \uc778\uc2a4\ud134\uc2a4 \uc0ad\uc81c");
    @Getter private final JMenuItem clearWebRootMenuItem = new JMenuItem("\ube48 \ud14c\uc2a4\ud2b8 \uc6f9 \uc11c\ubc84");
    @Getter private final JMenuItem buildMenuItem = new JMenuItem("\ud328\ud0a4\uc9c0 \ube4c\ub4dc...");
    @Getter private final JMenuItem deployServerMenuItem = new JMenuItem("\uc11c\ubc84 \ubc30\ud3ec...");
    @Getter private final JMenuItem generatePackagesMenuItem = new JMenuItem("packages.json \uc0dd\uc131...");
    @Getter private final JMenuItem openOutputFolderMenuItem = new JMenuItem("\uc5c5\ub85c\ub4dc \ud3f4\ub354 \uc5f4\uae30");
    @Getter private final JMenuItem versionCheckMenuItem = new JMenuItem("\ubaa8\ub4dc \uc5c5\ub370\uc774\ud2b8 \ud655\uc778");
    @Getter private final JMenuItem openWorkspaceFolderMenuItem = new JMenuItem("\uc791\uc5c5 \uacf5\uac04 \ud3f4\ub354 \uc5f4\uae30");
    @Getter private final JMenuItem openLauncherFolderMenuItem = new JMenuItem("\ud14c\uc2a4\ud2b8 \ub7f0\ucc98 \ud3f4\ub354 \uc5f4\uae30");
    @Getter private final JMenuItem openWebRootMenuItem = new JMenuItem("\ud14c\uc2a4\ud2b8 \uc6f9 \uc11c\ubc84 \ud3f4\ub354 \uc5f4\uae30");
    @Getter private final JMenuItem openConsoleMenuItem = new JMenuItem("\ucf58\uc194 \ucc3d \uc5f4\uae30");
    @Getter private final JMenuItem docsMenuItem = new JMenuItem("\ub3c4\uc6c0\ub9d0");
    @Getter private final JMenuItem aboutMenuItem = new JMenuItem("\uc815\ubcf4");

    @Getter private final JTable packTable = new DefaultTable();

    public PackManagerFrame() {
        super("\ubaa8\ub4dc\ud329 \ud3b8\uc9d1\uae30");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        initComponents();
        initMenu();
        pack();
        setLocationRelativeTo(null);

        SwingHelper.setFrameIcon(this, Creator.class, "icon.png");
    }

    private void initComponents() {
        TableColumnAdjuster adjuster = new TableColumnAdjuster(packTable);
        adjuster.adjustColumns();
        adjuster.setDynamicAdjustment(true);

        packTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        packTable.setAutoCreateRowSorter(true);

        JPanel container = new JPanel();
        container.setLayout(new MigLayout("fill, wrap 1"));

        container.add(createToolbar(), "dock north");
        container.add(SwingHelper.wrapScrollPane(packTable), "grow, span, w null:800:null");

        TableSearchable tableSearchable = SearchableUtils.installSearchable(packTable);
        tableSearchable.setMainIndex(-1);

        add(container, BorderLayout.CENTER);
    }

    private JToolBar createToolbar() {
        JToolBar toolBar = new JToolBar("\ub3c4\uad6c \uc0c1\uc790");

        toolBar.setFloatable(false);

        toolBar.add(newPackButton);
        toolBar.add(importButton);
        toolBar.addSeparator();
        toolBar.add(editConfigButton);
        toolBar.add(openFolderButton);
        toolBar.add(checkProblemsButton);
        toolBar.addSeparator();
        toolBar.add(testButton);
        toolBar.add(buildButton);

        return toolBar;
    }

    private void initMenu() {
        int ctrlKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        newPackMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ctrlKeyMask));
        newPackAtLocationMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ctrlKeyMask | Event.SHIFT_MASK));
        editConfigMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ctrlKeyMask));
        openFolderMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ctrlKeyMask | Event.SHIFT_MASK));
        testMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        testOnlineMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
        buildMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F10, Event.SHIFT_MASK));
        deployServerMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F9, Event.SHIFT_MASK));
        docsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));

        JMenuBar menuBar;
        JMenu menu;

        menuBar = new JMenuBar();
        menuBar.setBorder(BorderFactory.createEmptyBorder());

        Insets menuInset = new Insets(2, 2, 2, 2);

        menu = new JMenu("\ud30c\uc77c(F)");
        menu.setMargin(menuInset);
        menu.setMnemonic('f');
        menuBar.add(menu);
        menu.add(newPackMenuItem);
        menu.add(newPackAtLocationMenuItem);
        menu.add(importPackMenuItem);
        menu.addSeparator();
        menu.add(changePackLocationMenuItem);
        menu.add(removePackItem);
        menu.add(deletePackItem);
        menu.addSeparator();
        menu.add(refreshMenuItem);
        menu.addSeparator();
        menu.add(quitMenuItem);

        menu = new JMenu("\ud3b8\uc9d1(E)");
        menu.setMargin(menuInset);
        menu.setMnemonic('e');
        menuBar.add(menu);
        menu.add(editConfigMenuItem);
        menu.add(openFolderMenuItem);
        menu.addSeparator();
        menu.add(checkProblemsMenuItem);

        menu = new JMenu("\ud14c\uc2a4\ud2b8(S)");
        menu.setMargin(menuInset);
        menu.setMnemonic('s');
        menuBar.add(menu);
        menu.add(testMenuItem);
        menu.add(testOnlineMenuItem);
        menu.addSeparator();
        menu.add(optionsMenuItem);
        menu.addSeparator();
        menu.add(clearInstanceMenuItem);
        menu.add(clearWebRootMenuItem);

        menu = new JMenu("\ube4c\ub4dc(B)");
        menu.setMargin(menuInset);
        menu.setMnemonic('b');
        menuBar.add(menu);
        menu.add(buildMenuItem);
        menu.add(deployServerMenuItem);
        menu.addSeparator();
        menu.add(generatePackagesMenuItem);
        menu.addSeparator();
        menu.add(openOutputFolderMenuItem);

        menu = new JMenu("\ub3c4\uad6c(T)");
        menu.setMargin(menuInset);
        menu.setMnemonic('t');
        menuBar.add(menu);
        menu.add(versionCheckMenuItem);
        menu.addSeparator();
        menu.add(openWorkspaceFolderMenuItem);
        menu.add(openLauncherFolderMenuItem);
        menu.add(openWebRootMenuItem);
        menu.addSeparator();
        menu.add(openConsoleMenuItem);

        menu = new JMenu("\ub3c4\uc6c0\ub9d0(H)");
        menu.setMargin(menuInset);
        menu.setMnemonic('h');
        menuBar.add(menu);
        menu.add(docsMenuItem);
        menu.addSeparator();
        menu.add(aboutMenuItem);

        setJMenuBar(menuBar);
    }

}
