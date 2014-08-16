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

import org.l2x6.eircc.core.IrcMessageAddedEvent;
import org.l2x6.eircc.core.IrcUtils;
import org.l2x6.eircc.ui.IrcMessage;


/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcLog extends IrcObject implements Iterable<IrcMessage> {
    public enum IrcLogField {};
    private final IrcChannel channel;
    private final List<IrcMessage> messages = new ArrayList<IrcMessage>();
    private long seenTill;
    private final long startedOn;
    /**
     * @param id
     * @param channel
     * @param startedOn
     */
    public IrcLog(IrcChannel channel, long startedOn) {
        super();
        this.channel = channel;
        this.startedOn = startedOn;
    }

    public void appendMessage(IrcMessage message) {
        messages.add(message);
        channel.getAccount().getModel().fire(new IrcMessageAddedEvent(channel, message));
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
    public long getSeenTill() {
        return seenTill;
    }

    public long getStartedOn() {
        return startedOn;
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

}
