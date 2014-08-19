/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.editor;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.l2x6.eircc.core.IrcModelEvent;
import org.l2x6.eircc.core.IrcModelEventListener;
import org.l2x6.eircc.core.model.IrcChannel;
import org.l2x6.eircc.core.model.IrcModel;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.views.IrcLabelProvider;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcChannelOutlinePage extends ContentOutlinePage implements IDoubleClickListener, IrcModelEventListener {

    private static class IrcChannelOutlineContentProvider implements ITreeContentProvider {

        private final IrcChannel channel;

        /**
         * @param channel
         */
        public IrcChannelOutlineContentProvider(IrcChannel channel) {
            super();
            this.channel = channel;
        }

        /**
         * @see org.eclipse.jface.viewers.IContentProvider#dispose()
         */
        @Override
        public void dispose() {
        }

        /**
         * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
         */
        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }

        /**
         * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
         */
        @Override
        public Object[] getElements(Object inputElement) {
            return channel.getUsers();
        }

        /**
         * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
         */
        @Override
        public Object[] getChildren(Object parentElement) {
            return null;
        }

        /**
         * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
         */
        @Override
        public Object getParent(Object element) {
            return null;
        }

        /**
         * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
         */
        @Override
        public boolean hasChildren(Object element) {
            return false;
        }

    }

    private final IrcChannel channel;

    /**
     * @param ircChannelEditor
     */
    public IrcChannelOutlinePage(IrcChannel channel) {
        this.channel = channel;
    }

    /**
     * @see org.eclipse.ui.part.IPart#createControl(Composite)
     */
    @Override
    public void createControl(Composite aParent) {
        super.createControl(aParent);

        // Init tree viewer
        TreeViewer viewer = getTreeViewer();
        viewer.setContentProvider(new IrcChannelOutlineContentProvider(channel));
        viewer.setLabelProvider(IrcLabelProvider.getInstance());
        viewer.addSelectionChangedListener(this);
        viewer.addDoubleClickListener(this);
        viewer.setInput(channel);
        IrcModel.getInstance().addModelEventListener(this);
    }

    /**
     * @see org.eclipse.jface.viewers.IDoubleClickListener#doubleClick(org.eclipse.jface.viewers.DoubleClickEvent)
     */
    @Override
    public void doubleClick(DoubleClickEvent event) {
    }

    @Override
    public void dispose() {
        try {
            IrcModel.getInstance().removeModelEventListener(this);
        } catch (Exception e) {
            EirccUi.log(e);
        }
        super.dispose();
    }

    /**
     * @see org.l2x6.eircc.core.IrcModelEventListener#handle(org.l2x6.eircc.core.IrcModelEvent)
     */
    @Override
    public void handle(IrcModelEvent e) {
        switch (e.getEventType()) {
        case CHANNEL_USERS_CHANGED:
            getTreeViewer().refresh();
            break;
        default:
            break;
        }
    }
}
