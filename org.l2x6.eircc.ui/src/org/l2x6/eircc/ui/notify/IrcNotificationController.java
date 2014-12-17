/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.notify;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.core.model.IrcLog;
import org.l2x6.eircc.core.model.IrcMessage;
import org.l2x6.eircc.core.model.IrcModel;
import org.l2x6.eircc.core.model.event.IrcModelEvent;
import org.l2x6.eircc.core.model.event.IrcModelEventListener;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.editor.IrcEditor;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcNotificationController implements IrcModelEventListener {

    private final IWindowListener notificationsCleaner = new IWindowListener() {

        /**
         * @see org.eclipse.ui.IWindowListener#windowActivated(org.eclipse.ui.IWorkbenchWindow)
         */
        @Override
        public void windowActivated(IWorkbenchWindow window) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                IEditorReference[] editorRefs = page.getEditorReferences();
                for (IEditorReference ref : editorRefs) {
                    IEditorPart editor = ref.getEditor(false);
                    if (editor instanceof IrcEditor) {
                        IrcEditor ircEditor = (IrcEditor) editor;
                        if (ircEditor.isBeingRead()) {
                            AbstractIrcChannel channel = ircEditor.getChannel();
                            if (channel != null) {
                                IrcLog log = channel.getLog();
                                if (log != null) {
                                    log.allRead();
                                }
                            }
                            ircEditor.updateTitle();
                        }
                    }
                }
            }
        }

        /**
         * @see org.eclipse.ui.IWindowListener#windowClosed(org.eclipse.ui.IWorkbenchWindow)
         */
        @Override
        public void windowClosed(IWorkbenchWindow window) {
        }

        /**
         * @see org.eclipse.ui.IWindowListener#windowDeactivated(org.eclipse.ui.IWorkbenchWindow)
         */
        @Override
        public void windowDeactivated(IWorkbenchWindow window) {
        }

        /**
         * @see org.eclipse.ui.IWindowListener#windowOpened(org.eclipse.ui.IWorkbenchWindow)
         */
        @Override
        public void windowOpened(IWorkbenchWindow window) {
            windowActivated(window);
        }

    };

    private final IrcSoundNotifier soundNotifier;
    private final IrcModel model;

    private final IrcTray tray;

    /**
     * @param model
     *
     */
    public IrcNotificationController(IrcModel model) {
        super();
        this.model = model;
        this.soundNotifier = new IrcSoundNotifier();
        this.tray = new IrcTray();
        this.model.addModelEventListener(this);
        PlatformUI.getWorkbench().addWindowListener(notificationsCleaner);
    }

    public void dispose() {
        PlatformUI.getWorkbench().removeWindowListener(notificationsCleaner);
        model.removeModelEventListener(this);
        tray.dispose();
    }

    /**
     * @see org.l2x6.eircc.core.model.event.IrcModelEventListener#handle(org.l2x6.eircc.core.model.event.IrcModelEvent)
     */
    @Override
    public void handle(IrcModelEvent e) {
        soundNotifier.handle(e);
        try {
            switch (e.getEventType()) {
            case ACCOUNT_STATE_CHANGED:
            case LOG_STATE_CHANGED:
                tray.update();
                break;
            default:
                break;
            }
        } catch (Exception e1) {
            EirccUi.log(e1);
        }
    }

}
