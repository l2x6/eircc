/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.client.cmd;

import org.l2x6.eircc.core.client.IrcClient;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.schwering.irc.lib.util.IRCCommand;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcCommandMessage {

    protected final IRCCommand ircCommand;
    protected final AbstractIrcChannel channel;

    /** The text inserted by the user. */
    protected final String sourceText;

    /**
     * @param sourceText
     * @param ircCommand
     */
    public IrcCommandMessage(AbstractIrcChannel channel, String sourceText, IRCCommand ircCommand) {
        super();
        this.channel = channel;
        this.sourceText = sourceText;
        this.ircCommand = ircCommand;
    }


    /**
     * Called from {@link IrcClient}'s executor thread.
     * @return
     */
    public String getSourceText() {
        return sourceText;
    }

    /**
     * Called on UI thread.
     *
     * @param client
     * @return
     */
    public boolean targetsClient(IrcClient client) {
        return channel == null || channel.getAccount() == client.getAccount();
    }

    /**
     * Called from {@link IrcClient}'s executor thread.
     * @param callbacks
     * @return
     */
    public IrcCommandCallbackList processCallbacks(IrcCommandCallbackList callbacks) {
        return callbacks;
    }

    /**
     * Called from {@link IrcClient}'s executor thread.
     * @return
     */
    public String getProtocolCommand() {
        return sourceText.substring(1);
    }

    /**
     * Called from {@link IrcClient}'s executor thread.
     * @param client
     */
    public void sentSuccessfully(IrcClient client) {
    }
}
