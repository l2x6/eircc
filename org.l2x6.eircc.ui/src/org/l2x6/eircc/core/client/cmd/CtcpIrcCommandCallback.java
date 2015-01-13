/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.client.cmd;

import java.time.OffsetDateTime;
import java.util.Locale;

import org.eclipse.swt.widgets.Display;
import org.l2x6.eircc.core.IrcController;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.core.model.IrcAccount;
import org.l2x6.eircc.core.model.IrcLog;
import org.l2x6.eircc.core.model.IrcMessage;
import org.l2x6.eircc.core.model.IrcUser;
import org.l2x6.eircc.core.model.PlainIrcMessage.IrcMessageType;
import org.l2x6.eircc.core.model.PlainIrcUser;
import org.schwering.irc.lib.CTCPCommand;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class CtcpIrcCommandCallback implements IrcCommandCallback {
    private final IrcController controller;

    /**
     * @param controller
     */
    public CtcpIrcCommandCallback(IrcController controller) {
        super();
        this.controller = controller;
    }

    /**
     * @see org.schwering.irc.lib.IRCEventListener#onNick(org.schwering.irc.lib.IRCUser,
     *      java.lang.String)
     */
    @Override
    public void onNick(final IrcAccount account, final PlainIrcUser user, final String newNick) {
    }

    /**
     * @see org.schwering.irc.lib.IRCEventListener#onReply(int,
     *      java.lang.String, java.lang.String)
     */
    @Override
    public void onReply(final IrcAccount account, int num, String value, String msg) {
    }

    /**
     * Returns a message in a format suitable for presenting in a chat log. If
     * ctcpCommand is {@code null} returns {@code msg}.
     *
     * @return a formatted message
     */
    public static IrcMessage formatCtcpMessage(IrcLog log, IrcUser sender, CTCPCommand ctcpCommand, String msg) {
        AbstractIrcChannel channel = log.getChannel();
        String myNick = channel.getAccount().getAcceptedNick();
        if (ctcpCommand == null) {
            return new IrcMessage(log, OffsetDateTime.now(), sender, msg, myNick, channel.isP2p(),
                    IrcMessageType.CHAT, null);
        }
        switch (ctcpCommand) {
        case ACTION:
            String cleanMessage = msg.substring(ctcpCommand.name().length() + 1).trim();
            String displayMessage = "*** " + sender.getNick() + " " + cleanMessage;
            String commandSource = null;
            if (myNick.equals(sender.getNick())) {
                commandSource = "/"+ CTCPCommand.ME.toLowerCase(Locale.ENGLISH) + " "+ cleanMessage;
            }
            return new IrcMessage(log, OffsetDateTime.now(), sender, displayMessage, myNick, channel.isP2p(),
                    IrcMessageType.CHAT, commandSource);
        default:
            return new IrcMessage(log, OffsetDateTime.now(), sender, msg, myNick, channel.isP2p(),
                    IrcMessageType.CHAT, null);
        }
    }

    @Override
    public void onCtcp(AbstractIrcChannel channel, PlainIrcUser user, CTCPCommand ctcpCommand, String msg) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                IrcAccount account = channel.getAccount();
                IrcUser sender = controller.getOrCreateUser(account.getServer(), user.getNick(), user.getUsername(),
                        user.getHost());
                IrcLog log = channel.getLog();
                IrcMessage m = formatCtcpMessage(log, sender, ctcpCommand, msg);;
                log.appendMessage(m);
            }
        });
    }

}
