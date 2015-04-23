/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.l2x6.eircc.core.client.cmd.CtcpIrcCommandMessage;
import org.schwering.irc.lib.util.CTCPCommand;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcUtils {
    private static final ThreadLocal<Boolean> isShutdownThread = new ThreadLocal<Boolean>();

    public static void assertUiThread() {
        if (Display.getCurrent() == null && !Boolean.TRUE.equals(isShutdownThread.get())) {
            throw new IllegalStateException(
                    "Cannot call this method from a thread other then the SWT UI thread or shutdown thread");
        }
    }

    public static String exec(String... command) throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec(command);
        p.waitFor();
        InputStream in = null;
        StringBuilder result;
        try {
            in = p.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line = null;
            result = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                result.append(line + "\n");
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return result.toString();
    }

    /**
     * @param msg
     * @return
     */
    public static CTCPCommand getCtcpCommand(String msg) {
        if (msg == null || msg.length() < CTCPCommand.SHORTEST_COMMAND_LENGTH) {
            return null;
        }
        msg = msg.trim();
        int i = 0;
        while (i < msg.length() && !Character.isWhitespace(msg.charAt(i))) {
            i++;
        }
        String cmd = msg.substring(0, i);
        if (CtcpIrcCommandMessage.ME.equals(cmd)) {
            return CTCPCommand.ACTION;
        } else {
            return CTCPCommand.fastValueOf(cmd);
        }
    }

    public static String getRealUserName() throws IOException, InterruptedException {
        String os = System.getProperty("os.name");
        if (os != null && (os.startsWith("Windows"))) {
            /* no idea how */
            return null;
        } else {
            /* hopefully a unix-like OS */
            String result = exec("/bin/sh", "-c", "getent passwd $(whoami) | cut -d ':' -f 5 | cut -d ',' -f 1");
            return result == null ? null : result.trim();
        }
    }

    public static void markShutDownThread() {
        isShutdownThread.set(Boolean.TRUE);
    }

    public static void mkdirs(IContainer container, IProgressMonitor monitor) throws CoreException {
        switch (container.getType()) {
        case IResource.ROOT:
        case IResource.PROJECT:
            return;
        case IResource.FOLDER:
            if (!container.exists()) {
                IContainer parent = container.getParent();
                mkdirs(parent, monitor);
                ((IFolder) container).create(true, true, monitor);
            }
            break;
        default:
            break;
        }
    }

    /**
     * @param propType
     * @return
     */
    public static Class<?> toWrapperType(Class<?> propType) {
        if (propType == Integer.TYPE) {
            return Integer.class;
        }
        String name = propType.getName();
        try {
            return Class.forName("java.lang." + Character.toUpperCase(name.charAt(0)) + name.substring(1));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
