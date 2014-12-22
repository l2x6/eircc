/*******************************************************************************
 * Copyright (c) 2012 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.l2x6.eircc.ui;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAmount;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.l2x6.eircc.core.IrcController;
import org.l2x6.eircc.core.IrcException;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.core.model.IrcAccount;
import org.l2x6.eircc.core.model.IrcAccount.IrcAccountState;
import org.l2x6.eircc.core.model.IrcLog;
import org.l2x6.eircc.core.model.IrcMessage;
import org.l2x6.eircc.core.model.IrcModel;
import org.l2x6.eircc.core.model.IrcUser;
import org.l2x6.eircc.core.model.event.IrcModelEvent;
import org.l2x6.eircc.core.model.event.IrcModelEventListener;
import org.l2x6.eircc.core.model.resource.IrcChannelResource;
import org.l2x6.eircc.core.model.resource.IrcLogResource;
import org.l2x6.eircc.core.model.resource.IrcResourceException;
import org.l2x6.eircc.core.model.resource.IrcRootResource;
import org.l2x6.eircc.core.util.IrcUtils;
import org.l2x6.eircc.ui.editor.IrcDocumentProvider;
import org.l2x6.eircc.ui.editor.IrcEditor;
import org.l2x6.eircc.ui.misc.IrcImages;
import org.l2x6.eircc.ui.notify.IrcNotificationController;
import org.l2x6.eircc.ui.notify.IrcSystemMessagesGenerator;
import org.l2x6.eircc.ui.prefs.IrcPreferences;
import org.l2x6.eircc.ui.views.IrcConsole;
import org.l2x6.eircc.ui.views.IrcLabelProvider;
import org.osgi.framework.BundleContext;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class EirccUi extends AbstractUIPlugin implements IrcModelEventListener {

    private static final IrcController INSTANCE = new IrcController();

    /** The singleton */
    private static EirccUi plugin;

    /** The plugin ID */
    public static final String PLUGIN_ID = "org.l2x6.eircc.ui";

    public static final String PROJECT_NAME = "IRC";

    public static IrcController getController() {
        return INSTANCE;
    }

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
    private IrcNotificationController notificationController;

    private IrcModel model;

    /**
     * @return
     */
    public IDocumentProvider getDocumentProvider() {
        return null;
    }

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
                    IrcController controller = EirccUi.getController();
                    for (AbstractIrcChannel ch : account.getChannels()) {
                        if (ch.isAutoJoin() && !ch.isJoined()) {
                            System.out.println("About to join "+ ch.getName());
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
                    System.out.println("About to open editor for "+ ch.getName());
                    openEditor(ch);
                    System.out.println("Opened editor for "+ ch.getName());
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
        case MESSAGE_REPLACED:
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
    public boolean isBeingRead(AbstractIrcChannel channel) throws IrcResourceException {
        IrcEditor editor = findOpenEditor(channel);
        return editor != null && editor.isBeingRead();
    }

    public IrcEditor findOpenEditor(AbstractIrcChannel channel) throws IrcResourceException {
        IrcUtils.assertUiThread();
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IrcChannelResource channelResource = channel.getChannelResource();
        IrcLogResource logResource = channelResource.getActiveLogResource();
        IEditorInput input = logResource.getEditorInput();

        IEditorReference[] editorRefs = page.getEditorReferences();
        for (IEditorReference editorRef : editorRefs) {
            IEditorPart editor = editorRef.getEditor(true);
            if (editor instanceof IrcEditor) {
                IrcEditor ircEditor = (IrcEditor) editor;
                IEditorInput editorInput = ircEditor.getEditorInput();
                if (input.equals(editorInput)) {
                    return ircEditor;
                }
            }
        }
        return null;
    }

    public IrcEditor openEditor(AbstractIrcChannel channel) throws IrcResourceException, CoreException, IOException {
        IrcUtils.assertUiThread();
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IrcChannelResource channelResource = channel.getChannelResource();
        IrcLogResource logResource = channelResource.getActiveLogResource();
        IEditorInput input = logResource.getEditorInput();

        SortedMap<OffsetDateTime, IrcEditor> channelEditors = new TreeMap<OffsetDateTime, IrcEditor>();

        IEditorReference[] editorRefs = page.getEditorReferences();
        for (IEditorReference editorRef : editorRefs) {
            IEditorPart editor = editorRef.getEditor(true);
            if (editor instanceof IrcEditor) {
                IrcEditor ircEditor = (IrcEditor) editor;
                IEditorInput editorInput = ircEditor.getEditorInput();
                if (input.equals(editorInput)) {
                    /*
                     * The editor for the given channel is already opened, we
                     * just need to activate it
                     */
                    page.activate(editor);
                    return ircEditor;
                }
                /*
                 * we should somehow ensure that there is only one connected
                 * editor per channel
                 */
                // TODO AbstractIrcChannel editorChannel =
                // ircEditor.getChannel();

                IrcLogResource editorLogResource = ircEditor.getLastLogResource();
                if (channelResource == editorLogResource.getChannelResource()) {
                    channelEditors.put(editorLogResource.getTime(), ircEditor);
                }
            }
        }

        /*
         * take the newest open editor belonging to the given channel but make
         * sure that it is not too old, namely, that it is newer than the editor
         * look back timespan preference
         */
        if (!channelEditors.isEmpty()) {

            TemporalAmount lookBackTimeSpan = IrcPreferences.getInstance().getEditorLookBackTimeSpan();

            OffsetDateTime editorStart = channelEditors.lastKey();
            IrcEditor lastEditor = channelEditors.get(editorStart);
            OffsetDateTime editorEnd = lastEditor.getLastMessageTime();

            OffsetDateTime lookBackStart = OffsetDateTime.now().minus(lookBackTimeSpan);
            if (!lookBackStart.isAfter(editorEnd)) {
                /* lookBackStart is between editorStart and editorEnd */
                lastEditor.rotate();
                page.activate(lastEditor);
                return lastEditor;
            }
        }

        /* there was no open editor belonging to this channel */
        IEditorPart part = page.openEditor(input, IrcEditor.ID);
        return (IrcEditor) part;
    }

    /**
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;

        IProject ircProject = getIrcProject();

        this.model = new IrcModel(IrcConsole.getInstance(), IrcPreferences.getInstance());
        model.addModelEventListener(this);
        IrcRootResource rootResource = new IrcRootResource(ircProject, IrcDocumentProvider.getInstance());
        model.load(rootResource);
        IrcController controller = EirccUi.getController();
        for (IrcAccount account : model.getAccounts()) {
            if (account.isAutoConnect()) {
                try {
                    System.out.println("About to connect "+ account.getName());
                    controller.connect(account);
                } catch (Exception e) {
                    EirccUi.log(e);
                }
            }
        }
        /* Touch IrcTray to create it */
        this.notificationController = new IrcNotificationController(model);

        IrcSystemMessagesGenerator.getInstance();

    }

    /**
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        IrcUtils.markShutDownThread();
        try {
            saveAll();
        } catch (Exception e) {
            log(e);
        }
        try {
            IrcSystemMessagesGenerator.getInstance().dispose();
        } catch (Exception e) {
            log(e);
        }
        try {
            if (this.notificationController != null) {
                notificationController.dispose();
            }
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
            model.dispose();
        } catch (Exception e) {
            log(e);
        }
        plugin = null;
        super.stop(context);

    }

    /**
     * @throws CoreException
     * @throws IrcException
     *
     */
    private void saveAll() throws IrcException {

        for (IrcAccount account : model.getAccounts()) {
            for (AbstractIrcChannel channel : account.getChannels()) {
                IrcLog log = channel.getLog();
                if (log != null) {
                    log.ensureAllSaved(new NullProgressMonitor());
                }
            }
        }

    }

    public IrcModel getModel() {
        return model;
    }

    /**
     * @throws IOException
     * @throws CoreException
     * @throws IrcResourceException
     * @throws BadLocationException
     *
     */
    public void revealHottest() throws IrcResourceException, CoreException, IOException, BadLocationException {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        if (shell.getMinimized()) {
            shell.setMinimized(false);
        }
        shell.forceActive();

        IrcMessage hottestMessage = null;
        for (IrcAccount account : model.getAccounts()) {
            for (AbstractIrcChannel channel : account.getChannels()) {
                IrcLog log = channel.getLog();
                if (log != null) {
                    IrcMessage m = log.getHottestMessage();
                    if (m != null && (hottestMessage == null || hottestMessage.getNotificationLevel().getLevel() < m.getNotificationLevel().getLevel())) {
                        hottestMessage = m;
                    }
                }
            }
        }
        if (hottestMessage != null) {
            AbstractIrcChannel ch = hottestMessage.getLog().getChannel();
            IrcEditor editor = openEditor(ch);
            editor.reveal(hottestMessage);
        }
    }
}
