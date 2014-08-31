/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.UnsupportedEncodingException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.l2x6.eircc.core.model.event.IrcModelEvent;
import org.l2x6.eircc.core.model.event.IrcModelEvent.EventType;
import org.l2x6.eircc.core.util.IrcUtils;
import org.l2x6.eircc.core.util.TypedField;
import org.l2x6.eircc.ui.EirccUi;

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

    private static final String FILE_EXTENSION = ".irc.log";
    private static final int NOTHING_SAVED = -1;

    /**
     * @param f
     * @return
     */
    public static boolean isLogFile(IResource f) {
        return f.getType() == IResource.FILE && f.getName().endsWith(FILE_EXTENSION);
    }

    private final AbstractIrcChannel channel;
    private int lastNonSystemMessageIndex = -1;
    /** The user has read all messages till (and including) this instant */
    private int lastReadIndex = -1;
    /** The time of the last non-system message that arrived */
    // private Instant lastMessageTime = Instant.MIN;
    private int lastSavedMessageIndex = NOTHING_SAVED;
    private final List<IrcMessage> messages = new ArrayList<IrcMessage>();
    private IPath path;
    // private Instant readTill = Instant.MIN;
    private final OffsetDateTime startedOn;

    private LogState state = LogState.NONE;

    public IrcLog(AbstractIrcChannel channel, File logFile) throws UnsupportedEncodingException, FileNotFoundException,
            IOException {
        super(channel.getModel(), channel.getLogsFolderPath());
        this.channel = channel;
        String fileName = logFile.getName();
        String startedOnString = fileName.substring(0, fileName.length() - FILE_EXTENSION.length());
        this.startedOn = OffsetDateTime.parse(startedOnString);

        try (PushbackReader in = new PushbackReader(new InputStreamReader(new FileInputStream(logFile), "utf-8"), 1)) {
            int ch;
            while ((ch = in.read()) >= 0) {
                in.unread(ch);
                try {
                    IrcMessage m = new IrcMessage(this, in);
                    messages.add(m);
                } catch (Exception e) {
                    EirccUi.log(e);
                }
            }
        }
    }

    /**
     * @param id
     * @param channel
     * @param startedOn
     */
    public IrcLog(AbstractIrcChannel channel, OffsetDateTime startedOn) {
        super(channel.getModel(), channel.getLogsFolderPath());
        this.channel = channel;
        this.startedOn = startedOn.truncatedTo(ChronoUnit.SECONDS);
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
        IPath path = getPath();
        IFile file = model.getRoot().getFile(path);

        ITextFileBufferManager fbm = FileBuffers.getTextFileBufferManager();
        if (!file.exists()) {
            IrcUtils.mkdirs(file.getParent(), monitor);
            file.create(new ByteArrayInputStream(new byte[0]), true, monitor);
            fbm.connect(path, LocationKind.IFILE, monitor);
        }

        ITextFileBuffer buffer = fbm.getTextFileBuffer(file.getFullPath(),
                LocationKind.NORMALIZE);
        monitor.beginTask("", 2); //$NON-NLS-1$

        IDocument document = buffer.getDocument();

        if (lastSavedMessageIndex == NOTHING_SAVED) {
            document.set("");
        }

        // boolean newFile = lastSavedMessageIndex < 0 || !path.exists();
        /* append unsaved messages */
        for (int i = lastSavedMessageIndex + 1; i < messages.size(); i++) {
            IrcMessage m = messages.get(i);
            m.write(document);
            // out.flush();
        }

        buffer.commit(new SubProgressMonitor(monitor, 1), false);
        file.refreshLocal(IResource.DEPTH_ONE, new SubProgressMonitor(monitor, 1));
        monitor.done();

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
    public IPath getPath() {
        if (path == null) {
            path = parentFolderPath.append(startedOn.toString() + FILE_EXTENSION);
        }
        return path;
    }

    public OffsetDateTime getStartedOn() {
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

    /**
     * Saves all overwriting the previous file.
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
                IrcMessage m = it.previous();
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
