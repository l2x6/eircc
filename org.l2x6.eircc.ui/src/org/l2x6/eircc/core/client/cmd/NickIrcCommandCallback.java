/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.client.cmd;

import java.text.MessageFormat;

import org.eclipse.swt.widgets.Display;
import org.l2x6.eircc.core.IrcController;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.core.model.IrcAccount;
import org.l2x6.eircc.core.model.IrcLog;
import org.l2x6.eircc.core.model.PlainIrcUser;
import org.l2x6.eircc.ui.IrcUiMessages;
import org.schwering.irc.lib.CTCPCommand;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class NickIrcCommandCallback implements IrcCommandCallback {
    private String sourceText;
    private final IrcController controller;

    /**
     * @param controller
     */
    public NickIrcCommandCallback(IrcController controller) {
        super();
        this.controller = controller;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    /**
     * @see org.schwering.irc.lib.IRCEventListener#onNick(org.schwering.irc.lib.IRCUser, java.lang.String)
     */
    @Override
    public void onNick(final IrcAccount account, final PlainIrcUser user, final String newNick) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                controller.changeNick(account.getServer(), user, newNick);
                String text;
                String useSrc = null;
                String oldNick = user.getNick();
                if (newNick.equals(account.getAcceptedNick())) {
                    text = MessageFormat.format(IrcUiMessages.Message_You_are_known_as_x, newNick);
                    useSrc = sourceText;
                    sourceText = null;
                } else {
                    text = MessageFormat.format(IrcUiMessages.Message_x_is_known_as_y, oldNick, newNick);
                }
                for (AbstractIrcChannel channel : account.getChannels()) {
                    if (channel.isJoined() && channel.isPresent(oldNick)) {
                        channel.changeNick(oldNick, newNick);
                        IrcLog log = channel.getLog();
                        log.appendSystemMessage(text, useSrc);
                    }
                }
            }
        });
    }

    @Override
    public void onReply(final IrcAccount account, int num, String value, String msg) {
    }

    @Override
    public void onCtcp(AbstractIrcChannel channel, PlainIrcUser plainUser, CTCPCommand ctcpCommand, String msg) {
    }

}
