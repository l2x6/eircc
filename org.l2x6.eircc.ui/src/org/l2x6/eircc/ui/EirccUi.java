/*******************************************************************************
 * Copyright (c) 2012 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.l2x6.eircc.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.l2x6.eircc.core.EirccCore;
import org.l2x6.eircc.core.IrcController;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.core.model.IrcAccount;
import org.l2x6.eircc.core.model.IrcAccount.IrcAccountState;
import org.l2x6.eircc.core.model.IrcMessage;
import org.l2x6.eircc.core.model.IrcModel;
import org.l2x6.eircc.core.model.IrcUser;
import org.l2x6.eircc.core.model.event.IrcModelEvent;
import org.l2x6.eircc.core.model.event.IrcModelEventListener;
import org.l2x6.eircc.core.model.resource.IrcResourceException;
import org.l2x6.eircc.core.util.IrcUtils;
import org.l2x6.eircc.ui.editor.IrcEditor;
import org.l2x6.eircc.ui.editor.IrcChannelEditorInput;
import org.l2x6.eircc.ui.misc.IrcImages;
import org.l2x6.eircc.ui.notify.IrcSoundNotifier;
import org.l2x6.eircc.ui.notify.IrcSystemMessagesGenerator;
import org.l2x6.eircc.ui.notify.IrcTray;
import org.l2x6.eircc.ui.views.IrcConsole;
import org.l2x6.eircc.ui.views.IrcLabelProvider;
import org.osgi.framework.BundleContext;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class EirccUi extends AbstractUIPlugin implements IrcModelEventListener, IWindowListener {

    /** The singleton */
    private static EirccUi plugin;

    /** The plugin ID */
    public static final String PLUGIN_ID = "org.l2x6.eircc.ui";

    public static final String PROJECT_NAME = "IRC";

    /**
     * Returns the singleton.
     *
     * @return the singleton
     */
    public static EirccUi getDefault() {
        return plugin;
    }

    public static Shell getShell() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
            if (windows.length > 0) {
                return windows[0].getShell();
            }
        } else {
            return window.getShell();
        }
        return null;
    }

    public static void log(String s) {
        IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, s);
        getDefault().getLog().log(status);
    }

    public static void log(Throwable e) {
        IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e);
        getDefault().getLog().log(status);
    }

    public static void warn(String s) {
        IStatus status = new Status(IStatus.WARNING, PLUGIN_ID, s);
        getDefault().getLog().log(status);
    }

    private IProject project;

    private IProject getIrcProject() throws CoreException {
        if (project == null) {
            IWorkspace ws = ResourcesPlugin.getWorkspace();
            project = ws.getRoot().getProject(PROJECT_NAME);
            if (!project.exists())
                project.create(null);
            if (!project.isOpen())
                project.open(null);
        }
        return project;
    }

    /**
     * @see org.l2x6.eircc.core.model.event.IrcModelEventListener#handle(org.l2x6.eircc.core.model.event.IrcModelEvent)
     */
    @Override
    public void handle(IrcModelEvent e) {
        switch (e.getEventType()) {
        case ACCOUNT_ADDED:
            try {
                ((IrcAccount) e.getModelObject()).save(new NullProgressMonitor());
            } catch (Exception e1) {
                log(e1);
            }
            break;
        case ACCOUNT_STATE_CHANGED:
            /* autojoin accounts after the account went online */
            try {
                IrcAccount account = (IrcAccount) e.getModelObject();
                if (account.getState() == IrcAccountState.ONLINE) {
                    IrcController controller = IrcController.getInstance();
                    for (AbstractIrcChannel ch : account.getChannels()) {
                        if (ch.isAutoJoin() && !ch.isJoined()) {
                            controller.joinChannel(ch);
                        }
                    }
                }
            } catch (Exception e1) {
                log(e1);
            }
            break;
        case CHANNEL_JOINED_CHANGED:
            try {
                AbstractIrcChannel ch = (AbstractIrcChannel) e.getModelObject();
                if (ch.isJoined()) {
                    openChannelEditor(ch);
                }
            } catch (Exception e1) {
                log(e1);
            }
            break;
        case ACCOUNT_CHANNEL_ADDED:
            try {
                AbstractIrcChannel channel = (AbstractIrcChannel) e.getModelObject();
                if (channel.isKept()) {
                    channel.save(new NullProgressMonitor());
                }
            } catch (Exception e1) {
                log(e1);
            }
            break;
        case USER_ADDED:
            try {
                IrcUser user = (IrcUser) e.getModelObject();
                user.save(new NullProgressMonitor());
            } catch (Exception e1) {
                log(e1);
            }
            break;
        case NEW_MESSAGE:
            try {
                IrcMessage m = (IrcMessage) e.getModelObject();
                m.getLog().ensureAllSaved(new NullProgressMonitor());
            } catch (Exception e1) {
                log(e1);
            }
            break;
        default:
            break;
        }
    }

    public void openChannelEditor(AbstractIrcChannel channel) throws PartInitException, IrcResourceException {
        IrcUtils.assertUiThread();
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IFile logFile = channel.getChannelResource().getActiveLogResource().getLogFile();
        IFileEditorInput input = new FileEditorInput(logFile);
        page.openEditor(input, IrcEditor.ID);
    }

    /**
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;

        IProject ircProject = getIrcProject();

        IrcModel model = IrcModel.getInstance();
        model.setTrafficLogFactory(IrcConsole.getInstance());
        model.addModelEventListener(this);
        model.load(ircProject);
        IrcController controller = IrcController.getInstance();
        for (IrcAccount account : model.getAccounts()) {
            if (account.isAutoConnect()) {
                try {
                    controller.connect(account);
                } catch (Exception e) {
                    EirccUi.log(e);
                }
            }
        }
        /* Touch IrcTray to create it */
        IrcTray.getInstance();
        IrcSoundNotifier.getInstance();

        IrcSystemMessagesGenerator.getInstance();

        PlatformUI.getWorkbench().addWindowListener(this);
    }

    /**
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        IrcUtils.markShutDownThread();
        try {
            PlatformUI.getWorkbench().removeWindowListener(this);
        } catch (Exception e) {
            log(e);
        }
        try {
            IrcSystemMessagesGenerator.getInstance().dispose();
        } catch (Exception e) {
            log(e);
        }
        try {
            IrcSoundNotifier.getInstance().dispose();
        } catch (Exception e) {
            log(e);
        }
        try {
            IrcTray.getInstance().dispose();
        } catch (Exception e) {
            log(e);
        }
        try {
            IrcLabelProvider.getInstance().dispose();
        } catch (Exception e) {
            log(e);
        }
        try {
            IrcImages.getInstance().dispose();
        } catch (Exception e) {
            log(e);
        }
        try {
            EirccCore.getInstance().dispose();
        } catch (Exception e) {
            log(e);
        }
        plugin = null;
        super.stop(context);

    }

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
                    IrcEditor channelEditor = (IrcEditor) editor;
                    channelEditor.updateReadMessages();
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

    /**
     * @return
     */
    public IDocumentProvider getDocumentProvider() {
        return null;
    }
}
