/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.editor;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.l2x6.eircc.core.IrcController;
import org.l2x6.eircc.core.IrcMessageAddedEvent;
import org.l2x6.eircc.core.IrcModelEvent;
import org.l2x6.eircc.core.IrcModelEventListener;
import org.l2x6.eircc.core.IrcUtils;
import org.l2x6.eircc.core.model.IrcChannel;
import org.l2x6.eircc.core.model.IrcLog;
import org.l2x6.eircc.core.model.IrcModel;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.IrcMessage;

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
                            inputWidget.setEditable(false);
                        }
                    }
                } catch (IOException e1) {
                    EirccUi.log(e1);
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }
    };

    /**
     * @param m
     */
    private void append(IrcMessage m) {
        historyWidget.append("\n" + IrcUtils.toTimeString(m.getPostedOn()) + " " + m.getUser().getNick() + ": "
                + m.getText());
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

        initHistory(channel.getLog());
    }

    @Override
    public void dispose() {
        IrcModel.getInstance().removeModelEventListener(this);
        super.dispose();
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void doSave(IProgressMonitor monitor) {
        // TODO Auto-generated method stub
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#doSaveAs()
     */
    @Override
    public void doSaveAs() {
        // TODO Auto-generated method stub
    }

    /**
     * @see org.l2x6.eircc.core.IrcModelEventListener#handle(org.l2x6.eircc.core.IrcModelEvent)
     */
    @Override
    public void handle(IrcModelEvent e) {
        IrcUtils.assertUiThread();
        switch (e.getEventType()) {
        case NEW_MESSAGE:
            IrcMessageAddedEvent mae = (IrcMessageAddedEvent) e;
            if (mae.getChannel() == channel) {
                IrcMessage m = mae.getMessage();
                append(m);
                if (m.getUser() == channel.getAccount().getMe()) {
                    inputWidget.setText("");
                    inputWidget.setEditable(true);
                }
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
            setPartName(channel.getName());
            IrcModel.getInstance().addModelEventListener(this);
        } else {
            throw new PartInitException("Expected an " + IrcChannelEditorInput.class.getSimpleName() + " but got a "
                    + input.getClass().getName());
        }
    }

    /**
     * @param log
     */
    private void initHistory(IrcLog log) {
        historyWidget.append("You joined on " + IrcUtils.toDateTimeString(log.getStartedOn()));
        for (IrcMessage m : log) {
            append(m);
        }
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

}
