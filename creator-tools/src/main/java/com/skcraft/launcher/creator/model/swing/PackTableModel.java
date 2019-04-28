/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.creator.model.swing;

import com.skcraft.launcher.builder.BuilderConfig;
import com.skcraft.launcher.creator.Creator;
import com.skcraft.launcher.creator.model.creator.Pack;
import com.skcraft.launcher.swing.SwingHelper;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.List;

public class PackTableModel extends AbstractTableModel {

    private final Icon instanceIcon;
    private final Icon warningIcon;
    private final List<Pack> packs;

    public PackTableModel(List<Pack> packs) {
        this.packs = packs;

        instanceIcon = SwingHelper.createIcon(Creator.class, "pack_icon.png");
        warningIcon = SwingHelper.createIcon(Creator.class, "warning_icon.png");
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "";
            case 1:
                return "\uc774\ub984";
            case 2:
                return "\ud45c\uc81c";
            case 3:
                return "\uac8c\uc784 \ubc84\uc804";
            case 4:
                return "\uc704\uce58";
            default:
                return null;
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return Icon.class;
            default:
                return String.class;
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public int getRowCount() {
        return packs.size();
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Pack pack = packs.get(rowIndex);

        BuilderConfig config = pack.getCachedConfig();

        switch (columnIndex) {
            case 0:
                return config != null ? instanceIcon : warningIcon;
            case 1:
                return config != null ? config.getName() : "<\uc774\ub3d9\ub418\uac70\ub098 \uc0ad\uc81c\ub428>";
            case 2:
                return config != null ? config.getTitle() : "?";
            case 3:
                return config != null ? config.getGameVersion() : "?";
            case 4:
                return pack.getLocation();
            default:
                return null;
        }
    }

}
