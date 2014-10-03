/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.util;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class NickComparator {
    /**
     * Removes the longest non-alphabetic suffix.
     *
     * @param nick
     * @return
     */
    public static String getBaseNick(String nick) {
        if (nick == null) {
            return null;
        }
        if (nick.isEmpty()) {
            return nick;
        }

        int lastValidIndex = nick.length() - 1;
        while (lastValidIndex >= 0) {
            char ch = nick.charAt(lastValidIndex);
            if (Character.isAlphabetic(ch)) {
                break;
            }
            lastValidIndex--;
        }
        return nick.substring(0, lastValidIndex + 1);
    }

    /**
     * Returns {@code true} if the two nicks have the same base as delivered by
     * {@link #getBaseNick(String)}.
     *
     * @param nick1
     * @param nick2
     * @return
     */
    public static boolean isSameUser(String nick1, String nick2) {
        String base1 = getBaseNick(nick1);
        String base2 = getBaseNick(nick2);
        return base1 == base2 || (base1 != null && base1.equals(base2));
    }
}
