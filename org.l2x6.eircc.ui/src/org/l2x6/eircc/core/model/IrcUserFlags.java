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
public class IrcUserFlags {

    public static final IrcUserFlags EMPTY = new IrcUserFlags();

    private static final int GONE;
    private static final int HALF_OP;
    private static final int IRC_OP;
    private static final int OP;
    private static final int OWNER;
    private static final int VOICE;
    static {
        int i = 1;
        VOICE = i;
        i <<= 1;
        OP = i;
        i <<= 1;
        HALF_OP = i;
        i <<= 1;
        OWNER = i;
        i <<= 1;
        GONE = i;
        i <<= 1;
        IRC_OP = i;
    }

    public static IrcUserFlags fromNick(String rawNick) {
        return new IrcUserFlags(rawNick, true);
    }

    private final int flags;
    private final String string;

    private IrcUserFlags() {
        this.flags = 0;
        this.string = "";
    }

    /**
     * <user> <host> <server> <nick> <H|G>[*][@|+] :<hopcount> <real_name>
     * @param flags
     */
    public IrcUserFlags(String flags) {
        this(flags, false);
    }

    public IrcUserFlags(String flags, boolean fromNick) {
        super();
        this.string = flags;
        int f = 0;
        int i = 0;
        LOOP: for (; i < flags.length(); i++) {
            char ch = flags.charAt(i);
            switch (ch) {
            case '+':
                f |= VOICE;
                break;
            case '@':
                f |= OP;
                break;
            case '%':
                if (fromNick) {
                    break LOOP;
                }
                f |= HALF_OP;
                break;
            case '~':
                if (fromNick) {
                    break LOOP;
                }
                f |= OWNER;
                break;
            case 'G':
                if (fromNick) {
                    break LOOP;
                }
                f |= GONE;
                break;
            case '*':
                if (fromNick) {
                    break LOOP;
                }
                f |= IRC_OP;
                break;
            default:
                if (fromNick) {
                    break LOOP;
                }
                break;
            }
        }
        this.flags = f;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IrcUserFlags other = (IrcUserFlags) obj;
        return flags == other.flags;
    }

    @Override
    public int hashCode() {
        return flags;
    }
    public boolean hasVoice() {
        return (flags & VOICE) > 0;
    }

    public boolean isGone() {
        return (flags & GONE) > 0;
    }

    public boolean isHalfOp() {
        return (flags & HALF_OP) > 0;
    }
    public boolean isIrcOp() {
        return (flags & IRC_OP) > 0;
    }
    public boolean isOp() {
        return (flags & OP) > 0;
    }

    public boolean isOwner() {
        return (flags & OWNER) > 0;
    }

    /**
     * @return
     */
    public int length() {
        return string.length();
    }

    @Override
    public String toString() {
        return string;
    }
}