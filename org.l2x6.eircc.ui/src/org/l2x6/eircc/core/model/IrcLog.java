/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.l2x6.eircc.core.model.event.IrcModelEvent;
import org.l2x6.eircc.core.model.event.IrcModelEvent.EventType;
import org.l2x6.eircc.core.model.resource.IrcLogResource;
import org.l2x6.eircc.core.util.TypedField;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcLog extends IrcObject implements Iterable<IrcMessage>, PersistentIrcObject {

    public enum IrcLogField implements TypedField {
        ;
        private final TypedFieldData typedFieldData;

        /**
         * @param typedFieldData
         */
        private IrcLogField() {
            this.typedFieldData = new TypedFieldData(name(), IrcLog.class);
        }

        /**
         * @see org.l2x6.eircc.core.util.TypedField#getTypedFieldData()
         */
        @Override
        public TypedFieldData getTypedFieldData() {
            return typedFieldData;
        }
    };

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

    public static final int NOTHING_SAVED = -1;
    private final AbstractIrcChannel channel;
    private int charLength = 0;
    private int lastNonSystemMessageIndex = -1;
    /** The user has read all messages till (and including) this instant */
    private int lastReadIndex = -1;
    /** The time of the last non-system message that arrived */
    // private Instant lastMessageTime = Instant.MIN;
    private int lastSavedMessageIndex = IrcLog.NOTHING_SAVED;
    private int lineIndex = 0;

    private final IrcLogResource logResource;
    private final List<IrcMessage> messages = new ArrayList<IrcMessage>();
    private LogState state = LogState.NONE;

    /**
     * @param id
     * @param channel
     * @param startedOn
     */
    public IrcLog(AbstractIrcChannel channel, IrcLogResource logResource) {
        super(channel.getModel(), channel.getLogsFolderPath());
        this.channel = channel;
        this.logResource = logResource;
    }

    /**
     *
     */
    public void allRead() {
        lastReadIndex = messages.size() - 1;
        setState(LogState.NONE);
    }

    public void appendMessage(IrcMessage message) {
        messages.add(message);
        if (!message.isSystemMessage() && !message.isFromMe()) {
            lastNonSystemMessageIndex = messages.size() - 1;
        }
        channel.getAccount().getModel().fire(new IrcModelEvent(EventType.NEW_MESSAGE, message));
        charLength += message.getRecordLenght();
        lineIndex += message.getLineCount();
    }

    public void appendSystemMessage(String text) {
        IrcMessage m = new IrcMessage(this, OffsetDateTime.now(), text, channel.isP2p());
        appendMessage(m);
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#dispose()
     */
    @Override
    public void dispose() {
    }

    public void ensureAllSaved(IProgressMonitor monitor) throws CoreException {
        if (lastSavedMessageIndex == messages.size() - 1) {
            return;
        }

        IDocument document = logResource.getDocument(monitor);
        monitor.beginTask("", 2); //$NON-NLS-1$

        if (lastSavedMessageIndex == IrcLog.NOTHING_SAVED) {
            document.set("");
        }

        // boolean newFile = lastSavedMessageIndex < 0 || !path.exists();
        /* append unsaved messages */
        for (int i = lastSavedMessageIndex + 1; i < messages.size(); i++) {
            PlainIrcMessage m = messages.get(i);
            m.write(document);
            // out.flush();
        }
        logResource.commitBuffer(monitor);
        monitor.done();

    }

    public AbstractIrcChannel getChannel() {
        return channel;
    }

    int getCharLength() {
        return charLength;
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#getFields()
     */
    @Override
    public IrcLogField[] getFields() {
        return IrcLogField.values();
    }

    int getLineIndex() {
        return lineIndex;
    }

    public int getMessageCount() {
        return messages.size();
    }

    @Override
    public IPath getPath() {
        return logResource.getLogFile().getFullPath();
    }

    public OffsetDateTime getStartedOn() {
        return logResource.getTime();
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

    /**
     * Saves all overwriting the previous file.
     *
     * @see #ensureAllSaved(IProgressMonitor)
     * @see org.l2x6.eircc.core.model.PersistentIrcObject#save(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void save(IProgressMonitor monitor) throws CoreException {
        lastSavedMessageIndex = -1;
        ensureAllSaved(monitor);
    }

    public void setState(LogState state) {
        LogState oldState = this.state;
        this.state = state;
        if (oldState != state) {
            channel.getAccount().getModel().fire(new IrcModelEvent(EventType.LOG_STATE_CHANGED, this));
        }
    }

    public void updateState() {
        boolean hasUnreadMessages = !messages.isEmpty() && lastReadIndex < lastNonSystemMessageIndex;
        if (hasUnreadMessages) {
            /* look if me is named */
            ListIterator<IrcMessage> it = messages.listIterator(messages.size());
            while (it.hasPrevious()) {
                int i = it.previousIndex();
                if (lastReadIndex >= i) {
                    break;
                }
                PlainIrcMessage m = it.previous();
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
