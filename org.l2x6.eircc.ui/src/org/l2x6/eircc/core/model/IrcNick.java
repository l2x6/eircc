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
        IrcUserFlags flags = IrcUserFlags.fromNick(rawNick);
        String cleanNick = rawNick.substring(flags.length());
        return new IrcNick(cleanNick, flags);
    }

    private final String cleanNick;
    private final IrcUserFlags flags;

    /**
     * @param cleanNick
     * @param op
     * @param voice
     */
    public IrcNick(String cleanNick, IrcUserFlags flags) {
        super();
        this.cleanNick = cleanNick;
        this.flags = flags;
    }

    public String getCleanNick() {
        return cleanNick;
    }

    public IrcUserFlags getFlags() {
        return flags;
    }
}
