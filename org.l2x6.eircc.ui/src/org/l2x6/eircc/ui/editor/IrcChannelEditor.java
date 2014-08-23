/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.l2x6.eircc.core.IrcController;
import org.l2x6.eircc.core.model.IrcChannel;
import org.l2x6.eircc.core.model.IrcLog;
import org.l2x6.eircc.core.model.IrcMessage;
import org.l2x6.eircc.core.model.IrcModel;
import org.l2x6.eircc.core.model.event.IrcModelEvent;
import org.l2x6.eircc.core.model.event.IrcModelEventListener;
import org.l2x6.eircc.core.util.IrcUtils;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.IrcImages;
import org.l2x6.eircc.ui.views.IrcLabelProvider;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcChannelEditor extends EditorPart implements IrcModelEventListener {

    public static final String ID = "org.l2x6.eircc.ui.editor.IrcChannelEditor";
    private SashForm accountsDetailsSplitter;
    private IrcChannel channel;
    private StyledText historyWidget;
    private StyledText inputWidget;

    private KeyListener inputWidgetListenet = new KeyListener() {

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.stateMask == 0 && (e.keyCode == SWT.CR || e.keyCode == SWT.LF || e.keyCode == SWT.KEYPAD_CR)) {
                try {
                    String text = inputWidget.getText();
                    if (text.length() > 0) {
                        int i = text.length() - 1;
                        for (; i >= 0; i--) {
                            if (!Character.isWhitespace(text.charAt(i))) {
                                break;
                            }
                        }
                        i++;
                        if (i < text.length()) {
                            text = text.substring(0, i);
                        }
                        if (text.length() > 0) {
                            IrcController.getInstance().postMessage(channel, text);
                            inputWidget.setText("");
                        }
                    }
                } catch (Exception e1) {
                    EirccUi.log(e1);
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }
    };
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

    /**
     * @param m
     */
    private void append(IrcMessage m) {
        if (historyWidget.getCharCount() > 0) {
            historyWidget.append("\n");
        }
        historyWidget.append(m.toString());
        historyWidget.setTopIndex(historyWidget.getLineCount() - 1);
    }

    /**
     * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(Composite parent) {

        accountsDetailsSplitter = new SashForm(parent, SWT.VERTICAL);
        historyWidget = new StyledText(accountsDetailsSplitter, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.H_SCROLL
                | SWT.V_SCROLL);
        inputWidget = new StyledText(accountsDetailsSplitter, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL);
        inputWidget.addKeyListener(inputWidgetListenet);
        accountsDetailsSplitter.setWeights(new int[] { 80, 20 });

        if (channel.getLog() != null) {
            initHistory(channel.getLog());
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
            IrcController.getInstance().partChannel(channel);
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
                outlinePage = new IrcChannelOutlinePage(channel);
            }
            adapter = outlinePage;
        } else {
            adapter = super.getAdapter(cl);
        }
        return adapter;
    }

    /**
     * @see org.l2x6.eircc.core.model.event.IrcModelEventListener#handle(org.l2x6.eircc.core.model.event.IrcModelEvent)
     */
    @Override
    public void handle(IrcModelEvent e) {
        IrcUtils.assertUiThread();
        switch (e.getEventType()) {
        case CHANNEL_JOINED_CHANGED:
            IrcChannel ch = (IrcChannel) e.getModelObject();
            if (ch == channel) {
                updateTitle();
            }
            break;
        case NEW_MESSAGE:
            IrcMessage m = (IrcMessage) e.getModelObject();
            if (m.getLog().getChannel() == channel) {
                append(m);
                updateReadMessages();
            }
            break;
        default:
            break;
        }
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite,
     *      org.eclipse.ui.IEditorInput)
     */
    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        if (input instanceof IrcChannelEditorInput) {
            setInput(input);
            setSite(site);
            this.channel = ((IrcChannelEditorInput) input).getChannel();
            updateReadMessages();
            IrcModel.getInstance().addModelEventListener(this);
            site.getPage().addPartListener(readMessagesUpdater);
        } else {
            throw new PartInitException("Expected an " + IrcChannelEditorInput.class.getSimpleName() + " but got a "
                    + input.getClass().getName());
        }
    }

    /**
     * @param log
     */
    private void initHistory(IrcLog log) {
        for (IrcMessage m : log) {
            append(m);
        }
        log.allRead();
    }

    private boolean isBeingRead() {
        Shell myShell = getEditorSite().getShell();
        boolean windowActive = myShell.getDisplay().getActiveShell() == myShell;
        return windowActive && historyWidget.isVisible();
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#isDirty()
     */
    @Override
    public boolean isDirty() {
        return false;
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
     */
    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    /**
     * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
     */
    @Override
    public void setFocus() {
        inputWidget.setFocus();
    }

    /**
     *
     */
    public void updateReadMessages() {
        if (channel != null && channel.getLog() != null) {
            if (isBeingRead()) {
                channel.getLog().allRead();
            } else {
                /* let us update the channel state */
                channel.getLog().updateState();
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
    private void updateTitle() {
        setPartName(channel.getName());
        setTitleToolTip(IrcLabelProvider.getInstance().getTooltipText(channel));
        setTitleImage(IrcImages.getInstance().getImage(channel));
    }

}
