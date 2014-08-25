/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.notify;

import java.text.MessageFormat;
import java.time.ZonedDateTime;

import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.core.model.IrcAccount;
import org.l2x6.eircc.core.model.IrcChannelUser;
import org.l2x6.eircc.core.model.IrcLog;
import org.l2x6.eircc.core.model.IrcMessage;
import org.l2x6.eircc.core.model.IrcModel;
import org.l2x6.eircc.core.model.IrcUser;
import org.l2x6.eircc.core.model.event.IrcModelEvent;
import org.l2x6.eircc.core.model.event.IrcModelEventListener;
import org.l2x6.eircc.ui.IrcUiMessages;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcSystemMessagesGenerator implements IrcModelEventListener {
    private static final IrcSystemMessagesGenerator INSTANCE = new IrcSystemMessagesGenerator();

    public static IrcSystemMessagesGenerator getInstance() {
        return INSTANCE;
    }

    /**
     *
     */
    public IrcSystemMessagesGenerator() {
        super();
        IrcModel.getInstance().addModelEventListener(this);
    }

    /**
     *
     */
    private void channelJoinedChanged(AbstractIrcChannel channel) {
        IrcLog log = channel.getLog();
        if (log != null) {
            String text;
            if (!channel.isJoined()) {
                text = IrcUiMessages.Message_You_left;
            } else {
                text = MessageFormat.format(IrcUiMessages.Message_You_joined_as_nick, channel.getAccount().getMe()
                        .getNick());
            }
            IrcMessage m = new IrcMessage(log, ZonedDateTime.now(), text);
            channel.getLog().appendMessage(m);
        }
    }

    /**
     * @param modelObject
     */
    private void channelUserJoined(IrcChannelUser user) {
        IrcLog log = user.getChannel().getLog();
        if (log != null) {
            String text = MessageFormat.format(IrcUiMessages.Message_x_joined, user.getNick());
            IrcMessage m = new IrcMessage(log, ZonedDateTime.now(), text);
            log.appendMessage(m);
        }
    }

    /**
     * @param modelObject
     */
    private void channelUserLeft(IrcChannelUser user) {
        IrcLog log = user.getChannel().getLog();
        if (log != null) {
            String msg = user.getLeftWithMessage();
            String text;
            if (msg != null && msg.length() > 0) {
                text = MessageFormat.format(IrcUiMessages.Message_x_left_with_message, user.getNick(), msg);
            } else {
                text = MessageFormat.format(IrcUiMessages.Message_x_left, user.getNick());
            }
            IrcMessage m = new IrcMessage(log, ZonedDateTime.now(), text);
            log.appendMessage(m);

        }
    }

    public void dispose() {
        IrcModel.getInstance().removeModelEventListener(this);
    }

    /**
     * @see org.l2x6.eircc.core.model.event.IrcModelEventListener#handle(org.l2x6.eircc.core.model.event.IrcModelEvent)
     */
    @Override
    public void handle(IrcModelEvent e) {
        ;
        switch (e.getEventType()) {
        case CHANNEL_JOINED_CHANGED:
            channelJoinedChanged((AbstractIrcChannel) e.getModelObject());
            break;
        case CHANNEL_USER_JOINED:
            channelUserJoined((IrcChannelUser) e.getModelObject());
            break;
        case CHANNEL_USER_LEFT:
            channelUserLeft((IrcChannelUser) e.getModelObject());
            break;
        case NICK_CHANGED:
            nickChanged((IrcUser) e.getModelObject());
            break;
        default:
            break;
        }

    }

    /**
     * @param modelObject
     */
    private void nickChanged(IrcUser user) {
        IrcAccount account = user.getServer().getAccount();
        String text;
        String oldNick = user.getPreviousNick();
        if (account.getMe() == user) {
            text = MessageFormat.format(IrcUiMessages.Message_You_are_known_as_x, user.getNick());
        } else {
            text = MessageFormat.format(IrcUiMessages.Message_x_is_known_as_y, oldNick, user.getNick());
        }
        ZonedDateTime now = ZonedDateTime.now();
        for (AbstractIrcChannel channel : account.getChannels()) {
            if (channel.isJoined() && channel.isPresent(oldNick)) {
                channel.changeNick(oldNick, user.getNick());
                IrcLog log = channel.getLog();
                IrcMessage m = new IrcMessage(log, now, null, text);
                log.appendMessage(m);
            }
        }
    }

}
