/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.creator.controller.task;

import com.beust.jcommander.internal.Lists;
import com.skcraft.concurrency.ProgressObservable;
import com.skcraft.launcher.builder.BuilderOptions;
import com.skcraft.launcher.creator.model.creator.Pack;
import com.skcraft.launcher.creator.model.creator.Problem;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

public class ProblemChecker implements Callable<List<Problem>>, ProgressObservable {

    private final Pack pack;

    public ProblemChecker(Pack pack) {
        this.pack = pack;
    }

    @Override
    public List<Problem> call() throws Exception {
        List<Problem> problems = Lists.newArrayList();

        File packDir = pack.getDirectory();
        File srcDir = pack.getSourceDir();

        File loadersDir = new File(packDir, BuilderOptions.DEFAULT_LOADERS_DIRNAME);
        File modsDir = new File(srcDir, "mods");
        boolean hasLoaders = hasFiles(loadersDir);
        boolean hasMods = hasFiles(modsDir);

        String[] files;

        if (new File(packDir, "_CLIENT").exists()) {
            problems.add(new Problem("\uc678\ubd80\uc5d0 _CLIENT\uac00 \uc874\uc7ac", "src \ud3f4\ub354 \uc678\ubd80\uc5d0 \uc788\ub294 _CLIENT \ud3f4\ub354\uac00 \uc788\uc2b5\ub2c8\ub2e4. " +
                    "src \ud3f4\ub354\uc5d0 \uc788\ub294 \ud30c\uc77c\ub4e4\ub9cc \ubaa8\ub4dc\ud329\uc5d0 \uc2e4\uc81c\ub85c \ub098\ud0c0\ub0a0 \uac83\uc774\ubbc0\ub85c _CLIENT \ud3f4\ub354\ub97c src \ud3f4\ub354 \ub0b4\ubd80\ub85c \ub123\uc5b4\uc57c\ud569\ub2c8\ub2e4."));
        }

        if (new File(packDir, "_SERVER").exists()) {
            problems.add(new Problem("\uc678\ubd80\uc5d0 _SERVER\uac00 \uc874\uc7ac", "src \ud3f4\ub354 \uc678\ubd80\uc5d0 \uc788\ub294 _SERVER \ud3f4\ub354\uac00 \uc788\uc2b5\ub2c8\ub2e4. " +
                    "src \ud3f4\ub354\uc5d0 \uc788\ub294 \ud30c\uc77c\ub4e4\ub9cc \ubaa8\ub4dc\ud329\uc5d0 \uc2e4\uc81c\ub85c \ub098\ud0c0\ub0a0 \uac83\uc774\ubbc0\ub85c _SERVER \ud3f4\ub354\ub97c src \ud3f4\ub354 \ub0b4\ubd80\ub85c \ub123\uc5b4\uc57c\ud569\ub2c8\ub2e4."));
        }

        if (new File(packDir, "mods").exists()) {
            problems.add(new Problem("\uc678\ubd80\uc5d0 mods\uac00 \uc874\uc7ac", "src \ud3f4\ub354 \uc678\ubd80\uc5d0 \uc788\ub294 mods \ud3f4\ub354\uac00 \uc788\uc2b5\ub2c8\ub2e4. " +
                    "src \ud3f4\ub354\uc5d0 \uc788\ub294 \ud30c\uc77c\ub4e4\ub9cc \ubaa8\ub4dc\ud329\uc5d0 \uc2e4\uc81c\ub85c \ub098\ud0c0\ub0a9\ub2c8\ub2e4."));
        }

        if (new File(packDir, "config").exists()) {
            problems.add(new Problem("\uc678\ubd80\uc5d0 config\uac00 \uc874\uc7ac", "src \ud3f4\ub354 \uc678\ubd80\uc5d0 \uc788\ub294 config \ud3f4\ub354\uac00 \uc788\uc2b5\ub2c8\ub2e4. " +
                    "src \ud3f4\ub354\uc5d0 \uc788\ub294 \ud30c\uc77c\ub4e4\ub9cc \ubaa8\ub4dc\ud329\uc5d0 \uc2e4\uc81c\ub85c \ub098\ud0c0\ub0a9\ub2c8\ub2e4."));
        }

        if (new File(packDir, "version.json").exists()) {
            problems.add(new Problem("\uc774\uc804 \ubc84\uc804\uc758 version.json", "\ud504\ub85c\uc81d\ud2b8 \ub514\ub809\ud1a0\ub9ac\uc5d0 version.json \ud30c\uc77c\uc774 \uc788\uc2b5\ub2c8\ub2e4. " +
                    "\uc774\uc804 \ubc84\uc804\uc758 \uc2e4\ud589 \ud504\ub85c\uadf8\ub7a8\uc5d0\uc11c \ubaa8\ub4dc\ud329\uc744 \uc5c5\uadf8\ub808\uc774\ub4dc\ud558\ub294 \uacbd\uc6b0 \ubaa8\ub4dc\ud329\uc744 \uc791\uc131\ud558\ub294 \ub370\uc5d0 \uc788\uc5b4 " +
                    "\ub354 \uc774\uc0c1 \ud544\uc694\ud558\uc9c0 \uc54a\uc73c\ubbc0\ub85c, version.json\uc744 \uc0ad\uc81c\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4. \uc758\ub3c4\uc801\uc73c\ub85c Minecraft \ubc84\uc804 " +
                    "\ub9e4\ub2c8\ud398\uc2a4\ud2b8\ub97c \uc7ac\uc815\uc758 \ud558\ub294 \uacbd\uc6b0, \uc774 \uacbd\uace0\ub97c \ubb34\uc2dc\ud558\uc138\uc694."));
        }

        if (hasMods && !hasLoaders) {
            problems.add(new Problem("\ubaa8\ub4dc\ub85c\ub354\uac00 \uc5c6\uc74c", "mods \ud3f4\ub354\uac00 \uc874\uc7ac\ud558\uc9c0\ub9cc, loaders \ud3f4\ub354 \uc18d\uc5d0 \ubaa8\ub4dc\ub85c\ub354\uac00 \uc874\uc7ac\ud558\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4."));
        }

        return problems;
    }

    @Override
    public double getProgress() {
        return -1;
    }

    @Override
    public String getStatus() {
        return "\ubb38\uc81c \uac80\uc0c9 \uc911...";
    }

    private static boolean hasFiles(File dir) {
        String[] contents = dir.list();
        return contents != null && contents.length > 0;
    }

}
