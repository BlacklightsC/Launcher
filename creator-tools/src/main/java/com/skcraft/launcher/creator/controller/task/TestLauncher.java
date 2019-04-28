/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.creator.controller.task;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.skcraft.concurrency.ProgressObservable;
import com.skcraft.launcher.Instance;
import com.skcraft.launcher.InstanceList;
import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.auth.Session;
import com.skcraft.launcher.launch.LaunchOptions;
import com.skcraft.launcher.launch.LaunchOptions.UpdatePolicy;
import com.skcraft.launcher.swing.SwingHelper;

import java.awt.*;
import java.util.List;

public class TestLauncher implements Function<InstanceList, Instance>, ProgressObservable {

    private final Launcher launcher;
    private final Window window;
    private final String id;
    private final Session session;

    public TestLauncher(Launcher launcher, Window window, String id, Session session) {
        this.launcher = launcher;
        this.window = window;
        this.id = id;
        this.session = session;
    }

    private Optional<Instance> findInstance(List<Instance> instances) {
        for (Instance instance : instances) {
            if (instance.getName().equals(id)) {
                return Optional.fromNullable(instance);
            }
        }

        return Optional.absent();
    }

    @Override
    public Instance apply(InstanceList instanceList) {
        Optional<Instance> optional = findInstance(instanceList.getInstances());

        if (optional.isPresent()) {
            LaunchOptions options = new LaunchOptions.Builder()
                    .setInstance(optional.get())
                    .setUpdatePolicy(UpdatePolicy.ALWAYS_UPDATE)
                    .setWindow(window)
                    .setSession(session)
                    .build();

            launcher.getLaunchSupervisor().launch(options);

            return optional.get();
        } else {
            SwingHelper.showErrorDialog(window,
                    "\ud544\uc694\ud55c \ud30c\uc77c\uc744 \uc0dd\uc131\ud55c \ud6c4 \ub7f0\ucc98\uc5d0\uc11c \ubaa8\ub4dc\ud329\uc744 \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4. \uc2e4\ud589 \ub3c4\uc911 modpack.json\uc774 \ubcc0\uacbd\ub41c \uac83 \uac19\uc2b5\ub2c8\ub2e4.", "\uc2e4\ud589 \uc624\ub958");

            return null;
        }
    }

    @Override
    public double getProgress() {
        return -1;
    }

    @Override
    public String getStatus() {
        return "\uac8c\uc784\uc744 \uc2e4\ud589\ud558\ub294 \uc911...";
    }

}
