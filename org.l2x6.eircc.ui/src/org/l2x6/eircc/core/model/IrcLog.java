/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.l2x6.eircc.core.model.event.IrcModelEvent;
import org.l2x6.eircc.core.model.event.IrcModelEvent.EventType;
import org.l2x6.eircc.core.util.IrcUtils;


/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcLog extends IrcObject implements Iterable<IrcMessage> {
    public enum IrcLogField {};
    public enum LogState {
        ME_NAMED(true), NONE(false), UNREAD_MESSAGES(true);
        private final boolean hasUnreadMessages;
        private LogState(boolean hasUnreadMessages) {
            this.hasUnreadMessages = hasUnreadMessages;
        }
        public boolean hasUnreadMessages() {
            return hasUnreadMessages;
        }
    }

    private final AbstractIrcChannel channel;
    private long lastMessageTime = -1;
    private final List<IrcMessage> messages = new ArrayList<IrcMessage>();
    private long readTill = 0;
    private final long startedOn;
    private LogState state = LogState.NONE;
    /**
     * @param id
     * @param channel
     * @param startedOn
     */
    public IrcLog(AbstractIrcChannel channel, long startedOn) {
        super();
        this.channel = channel;
        this.startedOn = startedOn;
    }

    /**
     *
     */
    public void allRead() {
        readTill = System.currentTimeMillis();
        setState(LogState.NONE);
    }

    public void appendMessage(IrcMessage message) {
        messages.add(message);
        if (!message.isSystemMessage() && !message.isFromMe()) {
            lastMessageTime = message.getPostedOn();
        }
        channel.getAccount().getModel().fire(new IrcModelEvent(EventType.NEW_MESSAGE, message));
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

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#getFields()
     */
    @Override
    public IrcLogField[] getFields() {
        return IrcLogField.values();
    }

    public int getMessageCount() {
        return messages.size();
    }
    @Override
    protected File getSaveFile(File parentDir) {
        return new File(parentDir, IrcUtils.toDateTimeString(startedOn) + ".txt");
    }

    public long getStartedOn() {
        return startedOn;
    }

    public LogState getState() {
        return state;
    }

    /**
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<IrcMessage> iterator() {
        return new Iterator<IrcMessage>() {
            private final Iterator<IrcMessage> delegate = messages.iterator();
            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public IrcMessage next() {
                return delegate.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public void setState(LogState state) {
        LogState oldState = this.state;
        this.state = state;
        if (oldState != state) {
            channel.getAccount().getModel().fire(new IrcModelEvent(EventType.LOG_STATE_CHANGED, this));
        }
    }

    public void updateState() {
        boolean hasUnreadMessages = !messages.isEmpty() && lastMessageTime > readTill;
        if (hasUnreadMessages) {
            /* look if me is named */
            ListIterator<IrcMessage> it = messages.listIterator(messages.size());
            for (IrcMessage m = it.previous(); it.hasPrevious(); m = it.previous()) {
                if (m.getPostedOn() <= readTill) {
                    break;
                }
                if (m.isMeNamed()) {
                    setState(LogState.ME_NAMED);
                    return;
                }
            }
            setState(LogState.UNREAD_MESSAGES);
            return;
        }
        setState(LogState.NONE);
        return;
    }

}
