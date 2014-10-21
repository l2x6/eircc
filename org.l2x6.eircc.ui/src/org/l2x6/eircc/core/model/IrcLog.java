/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.l2x6.eircc.core.model.PlainIrcMessage.IrcMessageType;
import org.l2x6.eircc.core.model.event.IrcModelEvent;
import org.l2x6.eircc.core.model.event.IrcModelEvent.EventType;
import org.l2x6.eircc.core.model.resource.IrcLogResource;
import org.l2x6.eircc.core.util.IrcLogReader;
import org.l2x6.eircc.core.util.IrcLogReader.IrcLogReaderException;
import org.l2x6.eircc.ui.EirccUi;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcLog extends IrcObject implements Iterable<IrcMessage> {

    public static final int NOTHING_SAVED = -1;
    private final AbstractIrcChannel channel;
    private int charLength = 0;
    private int lastChatMessageIndex = -1;
    /** The user has read all messages till (and including) this instant */
    private int lastReadIndex = -1;
    /** The time of the last non-system message that arrived */
    // private Instant lastMessageTime = Instant.MIN;
    private int lastSavedMessageIndex = IrcLog.NOTHING_SAVED;
    private int lineIndex = 0;

    private final IrcLogResource logResource;
    private final List<IrcMessage> messages = new ArrayList<IrcMessage>();

    private IrcNotificationLevel notificationLevel = IrcNotificationLevel.NO_NOTIFICATION;
    /**
     * @param id
     * @param channel
     * @param startedOn
     */
    public IrcLog(AbstractIrcChannel channel, IrcLogResource logResource) {
        super(channel.getModel(), channel.getLogsFolderPath());
        this.channel = channel;
        this.logResource = logResource;

        load();
    }

    /**
     *
     */
    public void allRead() {
        lastReadIndex = messages.size() - 1;
        setNotificationLevel(IrcNotificationLevel.NO_NOTIFICATION);
    }

    public void appendErrorMessage(String text) {
        IrcMessage m = new IrcMessage(this, OffsetDateTime.now(), null, text, channel.isP2p(), IrcMessageType.ERROR);
        appendMessage(m);
    }

    public void appendMessage(IrcMessage message) {
        messages.add(message);
        if (message.getType() == IrcMessageType.CHAT && !message.isFromMe()) {
            lastChatMessageIndex = messages.size() - 1;
        }
        channel.getAccount().getModel().fire(new IrcModelEvent(EventType.NEW_MESSAGE, message));
        charLength += message.getRecordLenght();
        lineIndex += message.getLineCount();
    }

    public void appendSystemMessage(String text) {
        IrcMessage m = new IrcMessage(this, OffsetDateTime.now(), null, text, channel.isP2p(), IrcMessageType.SYSTEM);
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
        IFileEditorInput editorInput = logResource.getEditorInput();
        IDocumentProvider documentProvider = getModel().getRootResource().getDocumentProvider();
        IDocument document = documentProvider.getDocument(editorInput);

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
        documentProvider.saveDocument(monitor, editorInput, document, true);
        lastSavedMessageIndex = messages.size() - 1;
        monitor.done();
    }

    public AbstractIrcChannel getChannel() {
        return channel;
    }

    int getCharLength() {
        return charLength;
    }

    int getLineIndex() {
        return lineIndex;
    }

    public IrcLogResource getLogResource() {
        return logResource;
    }

    public int getMessageCount() {
        return messages.size();
    }

    public IrcNotificationLevel getNotificationLevel() {
        return notificationLevel;
    }

    public OffsetDateTime getStartedOn() {
        return logResource.getTime();
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
     *
     */
    private void load() {
        IFileEditorInput editorInput = logResource.getEditorInput();
        IDocumentProvider documentProvider = getModel().getRootResource().getDocumentProvider();
        IDocument document = documentProvider.getDocument(editorInput);
        if (document.getLength() > 0) {
            IrcLogReader reader = null;
            try {
                reader = new IrcLogReader(document, editorInput.getFile().toString(), logResource.getChannelResource()
                        .isP2p());
                while (reader.hasNext()) {
                    PlainIrcMessage message = reader.next();
                    appendMessage(message.toIrcMessage(this));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (IrcLogReaderException e) {
                EirccUi.log(e);
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    EirccUi.log(e);
                }
            }
        }
        allRead();
        lastSavedMessageIndex = messages.size() - 1;
    }

    public void setNotificationLevel(IrcNotificationLevel state) {
        IrcNotificationLevel oldState = this.notificationLevel;
        this.notificationLevel = state;
        if (oldState != state) {
            channel.getAccount().getModel().fire(new IrcModelEvent(EventType.LOG_STATE_CHANGED, this));
        }
    }

    public void updateNotificationLevel() {
        boolean hasUnreadMessages = !messages.isEmpty() && lastReadIndex < lastChatMessageIndex;
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
                    setNotificationLevel(IrcNotificationLevel.ME_NAMED);
                    return;
                }
            }
            setNotificationLevel(IrcNotificationLevel.UNREAD_MESSAGES);
            return;
        }
        setNotificationLevel(IrcNotificationLevel.NO_NOTIFICATION);
        return;
    }

}
