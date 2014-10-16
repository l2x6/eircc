/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcNick {
    public static IrcNick parse(String rawNick) {
        boolean isOp = false;
        boolean hasVoice = false;
        int i = 0;
        LOOP: while (i < rawNick.length()) {
            char ch = rawNick.charAt(i);
            switch (ch) {
            case '@':
                isOp = true;
                break;
            case '+':
                hasVoice = true;
                break;
            default:
                break LOOP;
            }
            i++;
        }
        return new IrcNick(rawNick.substring(i), isOp, hasVoice);
    }

    private final String cleanNick;
    private final boolean op;
    private final boolean voice;

    /**
     * @param cleanNick
     * @param op
     * @param voice
     */
    public IrcNick(String cleanNick, boolean op, boolean voice) {
        super();
        this.cleanNick = cleanNick;
        this.op = op;
        this.voice = voice;
    }

    public String getCleanNick() {
        return cleanNick;
    }

    public boolean hasVoice() {
        return voice;
    }

    public boolean isOp() {
        return op;
    }
}
