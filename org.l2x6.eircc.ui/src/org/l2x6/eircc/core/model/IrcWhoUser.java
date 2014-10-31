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
public class IrcWhoUser extends PlainIrcUser {

    private final IrcUserFlags flags;
    private final String realName;

    /**
     * @param nick
     * @param username
     * @param host
     * @param flags
     */
    public IrcWhoUser(String nick, String username, String host, String realName, IrcUserFlags flags) {
        super(nick, username, host);
        this.realName = realName;
        this.flags = flags;
    }

    public IrcUserFlags getFlags() {
        return flags;
    }

    public String getRealName() {
        return realName;
    }

}
