/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.io.File;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcChannelUser extends IrcObject {
    public enum IrcChannelUserFields {}
    private final IrcChannel channel;;
    private final String nick;
    /**
     * @param channel
     * @param nick
     */
    public IrcChannelUser(IrcChannel channel, String nick) {
        super();
        this.channel = channel;
        this.nick = nick;
    }
    /**
     * @see org.l2x6.eircc.core.model.IrcObject#dispose()
     */
    @Override
    public void dispose() {
    }

    public IrcChannel getChannel() {
        return channel;
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#getFields()
     */
    @Override
    public Enum<?>[] getFields() {
        return IrcChannelUserFields.values();
    }
    public String getNick() {
        return nick;
    }
    /**
     * @see org.l2x6.eircc.core.model.IrcObject#getSaveFile(java.io.File)
     */
    @Override
    protected File getSaveFile(File parentDir) {
        return null;
    }

    @Override
    public String toString() {
        return nick;
    }

}
