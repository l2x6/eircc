/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.editor;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.SortedMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.core.model.IrcLog;
import org.l2x6.eircc.core.model.IrcMessage;
import org.l2x6.eircc.core.model.IrcModel;
import org.l2x6.eircc.core.model.PlainIrcMessage;
import org.l2x6.eircc.core.model.event.IrcModelEvent;
import org.l2x6.eircc.core.model.event.IrcModelEventListener;
import org.l2x6.eircc.core.model.resource.IrcAccountResource;
import org.l2x6.eircc.core.model.resource.IrcChannelResource;
import org.l2x6.eircc.core.model.resource.IrcLogResource;
import org.l2x6.eircc.core.model.resource.IrcResourceException;
import org.l2x6.eircc.core.util.IrcLogReader;
import org.l2x6.eircc.core.util.IrcLogReader.IrcLogReaderException;
import org.l2x6.eircc.core.util.IrcUtils;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.IrcUiMessages;
import org.l2x6.eircc.ui.editor.IrcDefaultMessageFormatter.TimeStyle;
import org.l2x6.eircc.ui.misc.IrcImages;
import org.l2x6.eircc.ui.misc.IrcImages.ImageKey;
import org.l2x6.eircc.ui.misc.StyledWrapper;
import org.l2x6.eircc.ui.misc.StyledWrapper.StylesCollector;
import org.l2x6.eircc.ui.misc.StyledWrapper.TextViewerWrapper;
import org.l2x6.eircc.ui.prefs.IrcPreferences;
import org.l2x6.eircc.ui.search.IrcMatch;
import org.l2x6.eircc.ui.views.IrcLabelProvider;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcEditor extends AbstractIrcEditor implements IrcModelEventListener {

    private static class IrcLogEntry {
        private final int lineIndex;
        private final IrcLogResource logResource;
        /**
         * @param logResource
         * @param start
         */
        public IrcLogEntry(IrcLogResource logResource, int lineIndex) {
            super();
            this.logResource = logResource;
            this.lineIndex = lineIndex;
        }
        public int getLineIndex() {
            return lineIndex;
        }
        public IrcLogResource getLogResource() {
            return logResource;
        }
    }

    private static final String HISTORY_VIEWER_KEY = "org.l2x6.eircc.ui.editor.IrcEditor.historyViewer";

    public static final String ID = "org.l2x6.eircc.ui.editor.IrcEditor";

    private static final DateTimeFormatter TITLE_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(':').appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .toFormatter();

    private SashForm accountsDetailsSplitter;

    private AbstractIrcChannel channel;
    private boolean historyViewer = true;
    private IrcInputField inputViewer;
    private OffsetDateTime lastMessageTime;


    private List<IrcLogEntry> logResources = new ArrayList<IrcLogEntry>();
    private IrcChannelOutlinePage outlinePage;

    private IPartListener2 readMessagesUpdater = new IPartListener2() {

        /**
         * @see org.eclipse.ui.IPartListener2#partActivated(org.eclipse.ui.IWorkbenchPartReference)
         */
        @Override
        public void partActivated(IWorkbenchPartReference partRef) {
            updateReadMessages(partRef);
        }

        /**
         * @see org.eclipse.ui.IPartListener2#partBroughtToTop(org.eclipse.ui.IWorkbenchPartReference)
         */
        @Override
        public void partBroughtToTop(IWorkbenchPartReference partRef) {
            updateReadMessages(partRef);
        }

        /**
         * @see org.eclipse.ui.IPartListener2#partClosed(org.eclipse.ui.IWorkbenchPartReference)
         */
        @Override
        public void partClosed(IWorkbenchPartReference partRef) {
        }

        /**
         * @see org.eclipse.ui.IPartListener2#partDeactivated(org.eclipse.ui.IWorkbenchPartReference)
         */
        @Override
        public void partDeactivated(IWorkbenchPartReference partRef) {
        }

        /**
         * @see org.eclipse.ui.IPartListener2#partHidden(org.eclipse.ui.IWorkbenchPartReference)
         */
        @Override
        public void partHidden(IWorkbenchPartReference partRef) {
        }

        /**
         * @see org.eclipse.ui.IPartListener2#partInputChanged(org.eclipse.ui.IWorkbenchPartReference)
         */
        @Override
        public void partInputChanged(IWorkbenchPartReference partRef) {
        }

        /**
         * @see org.eclipse.ui.IPartListener2#partOpened(org.eclipse.ui.IWorkbenchPartReference)
         */
        @Override
        public void partOpened(IWorkbenchPartReference partRef) {
            updateReadMessages(partRef);
        }

        /**
         * @see org.eclipse.ui.IPartListener2#partVisible(org.eclipse.ui.IWorkbenchPartReference)
         */
        @Override
        public void partVisible(IWorkbenchPartReference partRef) {
            updateReadMessages(partRef);
        }

    };
    private String tooltip;
    /**
     *
     */
    public IrcEditor() {
        super();
        setSourceViewerConfiguration(new IrcLogEditorConfiguration(getPreferenceStore()));
    }
    private void addLogResource(IrcLogResource logResource) {
        int lineIndex = 0;
        if (!logResources.isEmpty()) {

            if (logViewer != null) {
                IDocument doc = logViewer.getDocument();
                if (doc != null) {
                    try {
                        lineIndex = doc.getLineOfOffset(doc.getLength());
                    } catch (BadLocationException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        IrcLogEntry entry = new IrcLogEntry(logResource, lineIndex);
        logResources.add(entry);
    }
    private void adjustUi() {
        if (isHistoryViewer()) {
            accountsDetailsSplitter.setMaximizedControl(getSourceViewer().getControl());
        } else {
            accountsDetailsSplitter.setMaximizedControl(null);
            accountsDetailsSplitter.setWeights(new int[] { 80, 20 });
        }
        if (outlinePage != null) {
            outlinePage.updateInput();
        }
    }

    /**
     * @param m
     */
    private void appendMessage(StyledWrapper wrapper, PlainIrcMessage m) {
        IrcPreferences prefs = IrcPreferences.getInstance();
        IrcDefaultMessageFormatter formatter = prefs.getFormatter(m);
        formatter.format(wrapper, m, TimeStyle.TIME);
        lastMessageTime = m.getArrivedAt();
    }

    /**
     * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(Composite parent) {
        try {
            accountsDetailsSplitter = new SashForm(parent, SWT.VERTICAL);

            super.createPartControl(accountsDetailsSplitter);

            inputViewer = new IrcInputField(accountsDetailsSplitter, this);

            adjustUi();
            reload();

            if (!isHistoryViewer()) {
                logViewer.scrollToBottom();
                AbstractIrcChannel channel = getChannel();
                channel.getLog().allRead();
            }
        } catch (IOException | CoreException e) {
            EirccUi.log(e);
        }
    }

    @Override
    public void dispose() {
        try {
            getSite().getPage().addPartListener(readMessagesUpdater);
        } catch (Exception e1) {
            EirccUi.log(e1);
        }
        try {
            EirccUi.getDefault().getModel().removeModelEventListener(this);
        } catch (Exception e1) {
            EirccUi.log(e1);
        }
        try {
            AbstractIrcChannel channel = getChannel();
            if (channel != null) {
                EirccUi.getController().partChannel(channel);
            }
        } catch (Exception e) {
            EirccUi.log(e);
        }
        super.dispose();
    }

    protected void doRestoreState(IMemento memento) {

    }

    /**
     * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#doSaveAs()
     */
    @Override
    public void doSaveAs() {
    }

    private IrcLogEntry findEntry(IFile logFile) {
        for (IrcLogEntry entry : logResources) {
            if (logFile.equals(entry.getLogResource().getLogFile())) {
                return entry;
            }
        }
        return null;
    }

    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class cl) {
        Object adapter;
        if (cl.equals(IContentOutlinePage.class)) {
            if (outlinePage == null) {
                outlinePage = new IrcChannelOutlinePage(this);
            }
            adapter = outlinePage;
        } else {
            adapter = super.getAdapter(cl);
        }
        return adapter;
    }

    public AbstractIrcChannel getChannel() {
        return channel;
    }

    /**
     * @return
     */
    private IrcLogResource getFirstLogResource() {
        if (logResources.isEmpty()) {
            return null;
        } else {
            return logResources.get(0).getLogResource();
        }
    }

    public IrcLogResource getLastLogResource() {
        if (logResources.isEmpty()) {
            return null;
        } else {
            return logResources.get(logResources.size() - 1).getLogResource();
        }
    }

    public OffsetDateTime getLastMessageTime() {
        return lastMessageTime;
    }

    @Override
    public float getOrderingKey() {
        /*
         * It is important that this listener gets called before
         * IrcSoundNotifier
         */
        return -1f;
    }

    @Override
    public String getTitleToolTip() {
        return tooltip == null ? super.getTitleToolTip() : tooltip;
    }

    /**
     * @see org.l2x6.eircc.core.model.event.IrcModelEventListener#handle(org.l2x6.eircc.core.model.event.IrcModelEvent)
     */
    @Override
    public void handle(IrcModelEvent e) {
        IrcUtils.assertUiThread();
        try {
            switch (e.getEventType()) {
            case LOG_STATE_CHANGED:
                IrcLog log = (IrcLog) e.getModelObject();
                if (log.getLogResource() == getLastLogResource()) {
                    updateTitle();
                }
                break;
            case CHANNEL_JOINED_CHANGED:
                AbstractIrcChannel ch = (AbstractIrcChannel) e.getModelObject();
                if (ch == getChannel()) {
                    updateTitle();
                }
                break;
            case NEW_MESSAGE:
                IrcMessage m = (IrcMessage) e.getModelObject();
                if (m.getLog().getChannel() == getChannel()) {
                    appendMessage(new TextViewerWrapper(logViewer), m);
                    logViewer.scrollToBottom();
                    updateReadMessages();
                }
                break;
            case MESSAGE_REPLACED:
                IrcMessage replacementMessage = (IrcMessage) e.getModelObject();
                IrcLog l = replacementMessage.getLog();
                if (l.getChannel() == getChannel()) {
                    IDocument doc = logViewer.getDocument();
                    int replacementLineIndex = replacementMessage.getLineIndex();
                    int replacementStart = doc.getLineOffset(replacementLineIndex);
                    if (replacementStart > 0 && doc.getChar(replacementStart - 1) == '\n') {
                        replacementStart--;
                    }
                    doc.replace(replacementStart, doc.getLength() - replacementStart, "");
                    boolean replacing = false;
                    for (IrcMessage logMessage : l) {
                        if (logMessage == replacementMessage) {
                            replacing = true;
                        }
                        if (replacing) {
                            appendMessage(new TextViewerWrapper(logViewer), logMessage);
                        }
                    }
                    logViewer.scrollToBottom();
                    updateReadMessages();
                }
                break;
            default:
                break;
            }
        } catch (BadLocationException e1) {
            EirccUi.log(e1);
        }
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        if (input instanceof IFileEditorInput) {
            setSite(site);
            setInput(input);
            IFileEditorInput fileInput = (IFileEditorInput) input;
            if (!fileInput.exists()) {
                throw new PartInitException(MessageFormat.format(IrcUiMessages.IrcEditor_File_x_does_not_exist,
                        fileInput.getFile().getFullPath().toString()));
            }
            IFile logFile = fileInput.getFile();
            IrcModel model = EirccUi.getDefault().getModel();
            try {
                IrcLogResource logResource = model.getRootResource().getLogResource(logFile);
                lastMessageTime = logResource.getTime();
                addLogResource(logResource);
                updateMode();
                if (isHistoryViewer()) {
                    EirccUi.getDefault().getModel().removeModelEventListener(this);
                    site.getPage().removePartListener(readMessagesUpdater);
                } else {
                    updateReadMessages();
                    EirccUi.getDefault().getModel().addModelEventListener(this);
                    site.getPage().addPartListener(readMessagesUpdater);
                }

                updateTitle();
            } catch (IrcResourceException e) {
                throw new PartInitException("Cannot initialize IRC Editor", e);
            }

        } else {
            throw new PartInitException("IPathEditorInput expected.");
        }
    }

    public boolean isBeingRead() {
        Shell myShell = getEditorSite().getShell();
        boolean windowActive = myShell.getDisplay().getActiveShell() == myShell;
        return windowActive && logViewer != null && logViewer.isVisible();
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#isDirty()
     */
    @Override
    public boolean isDirty() {
        return false;
    }

    public boolean isHistoryViewer() {
        return historyViewer;
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
     */
    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    /**
     * @param logResource
     * @throws CoreException
     * @throws IOException
     */
    private void load(IrcLogResource logResource) throws CoreException, IOException {
        if (!logViewer.isEmpty()) {
            logViewer.addHorizontalLine();
        }

        System.out.println("loading "+ logResource.getLogFile() + " in "+ getEditorInput());

        AbstractIrcChannel ch = getChannel();
        if (ch != null) {
            IrcLog log = ch.findLog(logResource);
            if (log != null) {
                Iterator<? extends PlainIrcMessage> it = log.iterator();
                StylesCollector collector = new StylesCollector(logViewer);
                while (it.hasNext()) {
                    PlainIrcMessage m = it.next();
                    appendMessage(collector, m);
                }
                collector.apply();
                addLogResource(logResource);
                return;
            }
        }

        /* ch.findLog returned null */
        Object lock = logResource.getLockObject();
        synchronized (lock) {
            IrcLogReader reader = null;
            StylesCollector collector = new StylesCollector(logViewer);
            try {
                IDocument document = logResource.getDocument();
                reader = new IrcLogReader(document, logResource.getLogFile().toString(), logResource.getChannelResource()
                        .isP2p());
                while (reader.hasNext()) {
                    PlainIrcMessage m = reader.next();
                    appendMessage(collector, m);
                }
            } catch (IrcLogReaderException e) {
                EirccUi.log(e);
            } finally {
                collector.apply();
                if (reader != null) {
                    reader.close();
                }
            }
        }
        addLogResource(logResource);
    }

    private void reload() throws IOException, CoreException {
        logViewer.clear();
        List<IrcLogEntry> logResourcesCopy = new ArrayList<IrcLogEntry>(logResources);
        logResources.clear();
        for (IrcLogEntry ircLogResource : logResourcesCopy) {
            load(ircLogResource.getLogResource());
        }

        updateTitle();
    }

    @Override
    public void restoreState(IMemento memento) {
        super.restoreState(memento);
        Boolean b = memento.getBoolean(HISTORY_VIEWER_KEY);
        this.historyViewer = b != null ? b.booleanValue() : false;
    }

    private void reveal(IrcLogEntry entry, PlainIrcMessage message, int offsetInMessageText, int length) throws BadLocationException {
        int lineOffset = logViewer.getDocument().getLineOffset(entry.getLineIndex() + message.getLineIndex());
        String nick = message.getNick();
        int nickLength = nick != null ? nick.length() + 2 : 0;
        int relativeTextOfset = IrcDefaultMessageFormatter.TimeStyle.TIME.getCharacterLength() + 1 + nickLength
                + offsetInMessageText;
        selectAndReveal(lineOffset + relativeTextOfset, length);
    }

    /**
     * @param ircMatch
     * @throws BadLocationException
     */
    public void reveal(IrcMatch ircMatch) throws BadLocationException {
        IrcLogEntry entry = findEntry(ircMatch.getFile());
        PlainIrcMessage message = ircMatch.getMessageMatches().getMessage();
        reveal(entry, message, ircMatch.getOffsetInMessageText(), ircMatch.getLength());
    }

    public void reveal(IrcMessage message) throws BadLocationException {
        IrcLogEntry entry = findEntry(message.getLog().getLogResource().getLogFile());
        reveal(entry, message, 0, message.getText().length());
    }

    /**
     * Updates the input to point to the last log of the present channel.
     *
     * @throws IrcResourceException
     * @throws IOException
     * @throws CoreException
     */
    public void rotate() throws IrcResourceException, CoreException, IOException {
        IrcLogResource logResource = getLastLogResource();
        if (logResource != null && !logResource.isLast()) {

            IrcChannelResource channelResouce = logResource.getChannelResource();
            channelResouce.refresh();
            SortedMap<OffsetDateTime, IrcLogResource> tailMap = channelResouce.getLogResources().tailMap(
                    logResource.getTime());
            Iterator<IrcLogResource> tailIt = tailMap.values().iterator();

            if (tailIt.hasNext()) {
                IrcLogResource lr = tailIt.next();
                if (lr == logResource) {
                    /* already loaded - ignore */
                } else {
                    load(lr);
                }
                while (tailIt.hasNext()) {
                    lr = tailIt.next();
                    load(lr);
                }
            }

            IEditorInput newInput = logResource.getChannelResource().getActiveLogResource().getEditorInput();
            init(getEditorSite(), newInput);
            adjustUi();
        }
    }

    @Override
    public void saveState(IMemento memento) {
        memento.putBoolean(HISTORY_VIEWER_KEY, Boolean.valueOf(this.historyViewer));
        super.saveState(memento);
    }

    /**
     * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
     */
    @Override
    public void setFocus() {
        if (inputViewer != null && inputViewer.isVisible()) {
            inputViewer.setFocus();
        } else {
            super.setFocus();
        }
    }

    private void updateMode() {
        IrcLogResource logResource = getLastLogResource();
        boolean isHistory = !logResource.isLast();
        this.historyViewer = isHistory;
        if (isHistory) {
            this.channel = null;
        } else {
            IrcChannelResource channelResource = logResource.getChannelResource();
            IrcAccountResource accountResource = channelResource.getAccountResource();
            try {
                this.channel = EirccUi.getDefault().getModel().getAccount(accountResource.getAccountName())
                        .getOrCreateChannel(logResource);
            } catch (IrcResourceException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     *
     */
    public void updateReadMessages() {
        AbstractIrcChannel channel = getChannel();
        if (channel != null) {
            IrcLog log = channel.getLog();
            if (log != null) {
                if (isBeingRead()) {
                    log.allRead();
                } else {
                    /* let us update the channel state */
                    log.updateNotificationLevel();
                }
            }
            updateTitle();
        }
    }

    /**
     * @param partRef
     */
    private void updateReadMessages(IWorkbenchPartReference partRef) {
        if (partRef.getPart(false) == this) {
            updateReadMessages();
        }
    }

    /**
    *
    */
    public void updateTitle() {
        IrcLogResource lastLogResource = getLastLogResource();
        if (lastLogResource != null) {
            String channelName = lastLogResource.getChannelResource().getChannelName();
            if (isHistoryViewer()) {
                IrcLogResource firstLogResource = getFirstLogResource();
                OffsetDateTime firstLogStart = firstLogResource.getTime();
                String t = firstLogStart.format(DateTimeFormatter.ISO_LOCAL_DATE) + ' '
                        + firstLogStart.format(TITLE_DATE_FORMATTER) + ' ' + channelName;
                setPartName(t);

                if (lastMessageTime == null) {
                    tooltip = t;
                } else {
                    String start = firstLogStart.format(DateTimeFormatter.ISO_LOCAL_DATE);
                    String end = lastMessageTime.format(DateTimeFormatter.ISO_LOCAL_DATE);
                    if (start.equals(end)) {
                        tooltip = start + ' ' + firstLogStart.format(TITLE_DATE_FORMATTER) + " - "
                                + lastMessageTime.format(TITLE_DATE_FORMATTER) + ' ' + channelName;
                    } else {
                        tooltip = start + ' ' + firstLogStart.format(TITLE_DATE_FORMATTER) + " - " + end + ' '
                                + lastMessageTime.format(TITLE_DATE_FORMATTER) + ' ' + channelName;
                    }
                }
                setTitleImage(IrcImages.getInstance().getImage(ImageKey.CHANNEL_HISTORY));
            } else {
                AbstractIrcChannel channel = getChannel();
                setPartName(channelName);
                tooltip = IrcLabelProvider.getInstance().getTooltipText(channel);
                setTitleImage(IrcImages.getInstance().getImage(channel));
            }
        }
    }

    /**
     *
     */
    public void fillAndRotate() {
        /* Make sure we show the preferred span of the history up to the
         * newest session log */
        int desiredMessageCount = IrcPreferences.getInstance().getEditorLookBackMessageSpan();
        IrcLogResource logResource = getLastLogResource();
        if (logResource != null && !logResource.isLast()) {
            IrcChannelResource channelResouce = logResource.getChannelResource();
            channelResouce.refresh();
            SortedMap<OffsetDateTime, IrcLogResource> logs = channelResouce.getLogResources();
            Iterator<Entry<OffsetDateTime, IrcLogResource>> it = ((NavigableSet<Map.Entry<OffsetDateTime, IrcLogResource>>)logs.entrySet()).descendingIterator();
            while (it.hasNext()) {
                IrcLogResource r = it.next().getValue();
                r.getLogFile().get
            }
        }

        rotate();

    }

}
