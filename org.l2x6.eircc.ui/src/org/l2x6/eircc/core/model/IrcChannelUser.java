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
public class IrcChannelUser extends IrcObject {

    private final AbstractIrcChannel channel;
    private String leftWithMessage;
    private final IrcNick nick;

    /**
     * @param channel
     * @param nick
     */
    public IrcChannelUser(AbstractIrcChannel channel, IrcNick nick) {
        super(channel.getModel(), channel.getParentFolderPath());
        this.channel = channel;
        this.nick = nick;
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#dispose()
     */
    @Override
    public void dispose() {
    }

    public AbstractIrcChannel getChannel() {
        return channel;
    }

    public String getCleanNick() {
        return nick.getCleanNick();
    }

    public String getLeftWithMessage() {
        return leftWithMessage;
    }

    public IrcNick getNick() {
        return nick;
    }

    public boolean isOp() {
        return nick.isOp();
    }

    public void setLeftWithMessage(String leftWithMessage) {
        this.leftWithMessage = leftWithMessage;
    }

    @Override
    public String toString() {
        return getCleanNick();
    }

}
