/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.client.cmd;

import org.eclipse.swt.widgets.Display;
import org.l2x6.eircc.core.client.IrcClient;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.core.model.IrcLog;
import org.l2x6.eircc.core.model.IrcMessage;
import org.l2x6.eircc.ui.EirccUi;
import org.schwering.irc.lib.CTCPCommand;
import org.schwering.irc.lib.IRCCommand;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class CtcpIrcCommandMessage extends IrcCommandMessage {
    private final String protocolCommand;
    private final CTCPCommand ctcpCommand;
    private final String quotedCtcpCommand;

    /**
     * @param sourceText
     * @param ircCommand
     */
    public CtcpIrcCommandMessage(AbstractIrcChannel channel, String initialCommand, CTCPCommand ctcpCommand, String sourceText) {
        super(channel, sourceText, IRCCommand.CTCP);
        this.ctcpCommand = ctcpCommand;
        String cleanMessage = sourceText.substring(initialCommand.length() +2).trim();
        this.quotedCtcpCommand = CTCPCommand.QUOTE_CHAR + ctcpCommand.name() + " "+ cleanMessage + CTCPCommand.QUOTE_CHAR;
        this.protocolCommand = IRCCommand.PRIVMSG +" "+ channel.getName() + " :" + quotedCtcpCommand;
    }

    @Override
    public String getProtocolCommand() {
        return protocolCommand;
    }

    @Override
    public void sentSuccessfully(IrcClient client) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    IrcLog log = channel.getLog();
                    IrcMessage m = CtcpIrcCommandCallback.formatCtcpMessage(log, channel.getAccount().getMe(), ctcpCommand, quotedCtcpCommand);
                    log.appendMessage(m);
                } catch (Exception e) {
                    EirccUi.log(e);
                }
            }
        });
    }

}
