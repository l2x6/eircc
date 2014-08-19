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
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.swt.widgets.Display;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcUtils {
    private static final SimpleDateFormat FULL_DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    public static void assertUiThread() {
        if (Display.getCurrent() == null) {
            throw new IllegalStateException("Cannot call this method from a thread other then the SWT UI thread.");
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
    public static String getRealUserName() throws IOException, InterruptedException {
        String os = System.getProperty("os.name");
        if (os != null && (os.startsWith("Windows"))) {
            /* no idea how */
            return null;
        } else {
            /* hopefully unix-like */
            String result = exec("/bin/sh", "-c", "getent passwd $(whoami) | cut -d ':' -f 5 | cut -d ',' -f 1");
            return result == null ? null : result.trim();
        }
    }

    /**
     * @param startedOn
     * @return
     */
    public static String toDateTimeString(long unixTs) {
        assertUiThread();
        return FULL_DATE_TIME_FORMAT.format(new Date(unixTs));
    }
    public static String toTimeString(long unixTs) {
        assertUiThread();
        return TIME_FORMAT.format(new Date(unixTs));
    }

}
