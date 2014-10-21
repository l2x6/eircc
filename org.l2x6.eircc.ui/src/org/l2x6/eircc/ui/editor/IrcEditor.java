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
import java.util.SortedMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.l2x6.eircc.core.IrcException;
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
import org.l2x6.eircc.core.util.IrcUtils;
import org.l2x6.eircc.core.util.IrcLogReader.IrcLogReaderException;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.IrcUiMessages;
import org.l2x6.eircc.ui.misc.IrcImages;
import org.l2x6.eircc.ui.misc.IrcImages.ImageKey;
import org.l2x6.eircc.ui.prefs.IrcPreferences;
import org.l2x6.eircc.ui.search.IrcMatch;
import org.l2x6.eircc.ui.views.IrcLabelProvider;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcEditor extends AbstractIrcEditor implements IrcModelEventListener {

    public static final String ID = "org.l2x6.eircc.ui.editor.IrcEditor";
    private static final DateTimeFormatter TITLE_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(':').appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .toFormatter();
    private SashForm accountsDetailsSplitter;
    private AbstractIrcChannel channel;

    private ContentAssistant contentAssistant;

    private boolean historyViewer = true;
    private TextViewer inputViewer;
    private VerifyKeyListener inputWidgetListenet = new VerifyKeyListener() {

        @Override
        public void verifyKey(VerifyEvent e) {
            switch (e.keyCode) {
            case SWT.CR:
            case SWT.LF:
            case SWT.KEYPAD_CR:
                if (e.stateMask == 0) {
                    try {
                        sendMessage();
                        e.doit = false;
                    } catch (Exception e1) {
                        EirccUi.log(e1);
                    }
                }
                break;
            case SWT.SPACE:
                if ((e.stateMask & SWT.CTRL) == SWT.CTRL) {
                    try {
                        contentAssistant.showPossibleCompletions();
                    } catch (Exception e1) {
                        EirccUi.log(e1);
                    }
                }
                break;
            default:
                break;
            }

        }
    };
    private OffsetDateTime lastMessageTime;
    private List<IrcLogResource> logResources = new ArrayList<IrcLogResource>();
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
     * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(Composite parent) {
        try {
            accountsDetailsSplitter = new SashForm(parent, SWT.VERTICAL);

            super.createPartControl(accountsDetailsSplitter);

            inputViewer = new TextViewer(accountsDetailsSplitter, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL);
            inputViewer.setDocument(new Document());
            inputViewer.appendVerifyKeyListener(inputWidgetListenet);

            contentAssistant = new ContentAssistant();
            contentAssistant.setContentAssistProcessor(new IrcContentAssistProcessor(this),
                    IDocument.DEFAULT_CONTENT_TYPE);
            IrcPreferences prefs = IrcPreferences.getInstance();
            contentAssistant.enablePrefixCompletion(prefs.getEditorAutoPrefixCompletion());
            contentAssistant.enableAutoInsert(prefs.getEditorAutoInsert());
            contentAssistant.install(inputViewer);

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
            IrcModel.getInstance().removeModelEventListener(this);
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
            return logResources.get(0);
        }
    }

    public IrcLogResource getLastLogResource() {
        if (logResources.isEmpty()) {
            return null;
        } else {
            return logResources.get(logResources.size() - 1);
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
                logViewer.appendMessage(m);
                logViewer.scrollToBottom();
                updateReadMessages();
            }
            break;
        default:
            break;
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
            IrcModel model = IrcModel.getInstance();
            try {
                IrcLogResource logResource = model.getRootResource().getLogResource(logFile);
                lastMessageTime = logResource.getTime();
                logResources.add(logResource);
                updateMode();
                if (isHistoryViewer()) {
                    IrcModel.getInstance().removeModelEventListener(this);
                    site.getPage().removePartListener(readMessagesUpdater);
                } else {
                    updateReadMessages();
                    IrcModel.getInstance().addModelEventListener(this);
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

        IrcLogReader reader = null;
        IEditorInput input = logResource.getEditorInput();
        IDocumentProvider provider = getDocumentProvider();
        try {
            provider.connect(input);
            IDocument document = provider.getDocument(input);
            reader = new IrcLogReader(document, logResource.getLogFile().toString(), logResource.getChannelResource()
                    .isP2p());
            while (reader.hasNext()) {
                PlainIrcMessage m = reader.next();
                logViewer.appendMessage(m);
                lastMessageTime = m.getArrivedAt();
            }
        } catch (IrcLogReaderException e) {
            EirccUi.log(e);
        } finally {
            if (reader != null) {
                reader.close();
            }
            provider.disconnect(input);
        }

        logResources.add(logResource);
    }

    private void reload() throws IOException, CoreException {
        logViewer.clear();
        List<IrcLogResource> logResourcesCopy = new ArrayList<IrcLogResource>(logResources);
        logResources.clear();
        for (IrcLogResource ircLogResource : logResourcesCopy) {
            load(ircLogResource);
        }

        updateTitle();
    }

    /**
     * @param ircMatch
     * @throws BadLocationException
     */
    public void reveal(IrcMatch ircMatch) throws BadLocationException {
        PlainIrcMessage message = ircMatch.getMessageMatches().getMessage();
        int lineOffset = logViewer.getDocument().getLineOffset(message.getLineIndex());
        String nick = message.getNick();
        int nickLength = nick != null ? nick.length() + 2 : 0;
        int relativeTextOfset = IrcDefaultMessageFormatter.TimeStyle.TIME.getCharacterLength() + 1 + nickLength
                + ircMatch.getOffsetInMessageText();
        selectAndReveal(lineOffset + relativeTextOfset, ircMatch.getLength());
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

    private void sendMessage() throws IrcException {
        String text = inputViewer.getDocument().get();
        if (text.length() > 0) {
            /* remove the trailing whitespace */
            int end = text.length() - 1;
            for (; end >= 0; end--) {
                if (!Character.isWhitespace(text.charAt(end))) {
                    break;
                }
            }
            end++;

            if (end < text.length()) {
                text = text.substring(0, end);
            }
            if (text.length() > 0) {
                AbstractIrcChannel channel = getChannel();
                EirccUi.getController().postMessage(channel, text);
                inputViewer.getDocument().set("");
            }
        }
    }

    /**
     * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
     */
    @Override
    public void setFocus() {
        if (inputViewer != null && inputViewer.getControl().isVisible()) {
            inputViewer.getControl().setFocus();
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
                this.channel = IrcModel.getInstance().getAccount(accountResource.getAccountName())
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

}
