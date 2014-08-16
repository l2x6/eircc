/*******************************************************************************
 * Copyright (c) 2012 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.l2x6.eircc.ui;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.l2x6.eircc.core.EirccCore;
import org.l2x6.eircc.core.IrcModelEvent;
import org.l2x6.eircc.core.IrcModelEventListener;
import org.l2x6.eircc.core.IrcUtils;
import org.l2x6.eircc.core.model.IrcAccount;
import org.l2x6.eircc.core.model.IrcChannel;
import org.l2x6.eircc.core.model.IrcModel;
import org.l2x6.eircc.ui.editor.IrcChannelEditor;
import org.l2x6.eircc.ui.editor.IrcChannelEditorInput;
import org.l2x6.eircc.ui.views.IrcLabelProvider;
import org.osgi.framework.BundleContext;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class EirccUi extends AbstractUIPlugin implements IrcModelEventListener {

    /** The singleton */
    private static EirccUi plugin;

    /** The plugin ID */
    public static final String PLUGIN_ID = "org.l2x6.eircc.ui";

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

    /**
     * @see org.l2x6.eircc.core.IrcModelEventListener#handle(org.l2x6.eircc.core.IrcModelEvent)
     */
    @Override
    public void handle(IrcModelEvent e) {
        switch (e.getEventType()) {
        case CHANNEL_JOINED_CHANGED:
            try {
                IrcChannel ch = (IrcChannel) e.getModelObject();
                if (ch.isJoined()) {
                    openChannelEditor(ch);
                }
            } catch (PartInitException e1) {
                log(e1);
            }
            break;
        case ACCOUNT_ADDED:
            try {
                ((IrcAccount)e.getModelObject()).save(getDefaultStorageRoot());
            } catch (IOException e1) {
                log(e1);
            }
            break;
        case KEPT_CHANNEL_ADDED:
            try {
                IrcChannel channel = (IrcChannel)e.getModelObject();
                File channelsDir = channel.getAccount().getChannelsDir(getDefaultStorageRoot());
                channel.save(channelsDir);
            } catch (IOException e1) {
                log(e1);
            }
            break;
        default:
            break;
        }
    }

    public void openChannelEditor(IrcChannel channel) throws PartInitException {
        IrcUtils.assertUiThread();
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        page.openEditor(new IrcChannelEditorInput(channel), IrcChannelEditor.ID);
    }

    /**
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        IrcModel model = IrcModel.getInstance();
        model.setTrafficLogFactory(IrcConsole.getInstance());
        model.addModelEventListener(this);
        model.load(getDefaultStorageRoot());
    }

    private File getDefaultStorageRoot() {
        return new File(System.getProperty("user.home"), ".eircc");
    }

    /**
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
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
}
