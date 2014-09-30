/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.editor;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.core.model.IrcChannelUser;
import org.l2x6.eircc.core.model.IrcModel;
import org.l2x6.eircc.core.model.event.IrcModelEvent;
import org.l2x6.eircc.core.model.event.IrcModelEventListener;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.actions.IrcTreeAction;
import org.l2x6.eircc.ui.prefs.IrcPreferences;
import org.l2x6.eircc.ui.views.IrcLabelProvider;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcChannelOutlinePage extends ContentOutlinePage implements IDoubleClickListener, IrcModelEventListener {

    private static class IrcChannelOutlineContentProvider implements ITreeContentProvider {

        private final IrcEditor editor;

        /**
         * @param channel
         */
        public IrcChannelOutlineContentProvider(IrcEditor editor) {
            super();
            this.editor = editor;
        }

        /**
         * @see org.eclipse.jface.viewers.IContentProvider#dispose()
         */
        @Override
        public void dispose() {
        }

        /**
         * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
         */
        @Override
        public Object[] getChildren(Object parentElement) {
            return null;
        }

        /**
         * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
         */
        @Override
        public Object[] getElements(Object inputElement) {
            AbstractIrcChannel channel = editor.getChannel();
            if (channel != null) {
                return channel.getUsers();
            }
            return new Object[0];
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

        /**
         * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
         *      java.lang.Object, java.lang.Object)
         */
        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }

    }

    private static class UsersLabelProvider extends IrcLabelProvider implements IStyledLabelProvider {
        private final IrcEditor editor;

        /**
         * @param channel
         */
        public UsersLabelProvider(IrcEditor editor) {
            super();
            this.editor = editor;
        }

        /**
         * @see org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider#getStyledText(java.lang.Object)
         */
        @Override
        public StyledString getStyledText(Object element) {
            if (element instanceof IrcChannelUser) {
                AbstractIrcChannel channel = editor.getChannel();
                if (channel != null) {
                    int index = channel.getUserIndex(((IrcChannelUser) element).getNick());
                    return new StyledString(super.getText(element), IrcPreferences.getInstance().getUserStyler(index));
                }
            }
            return new StyledString(super.getText(element));
        }

    }

    private final IrcEditor editor;
    private IrcTreeAction<?> openPrivateChatAction;

    /**
     * @param ircChannelEditor
     */
    public IrcChannelOutlinePage(IrcEditor editor) {
        this.editor = editor;
    }

    /**
     * @see org.eclipse.ui.part.IPart#createControl(Composite)
     */
    @Override
    public void createControl(Composite aParent) {
        super.createControl(aParent);

        // Init tree viewer
        TreeViewer viewer = getTreeViewer();
        viewer.setContentProvider(new IrcChannelOutlineContentProvider(editor));
        viewer.setLabelProvider(new DelegatingStyledCellLabelProvider(new UsersLabelProvider(editor)));
        viewer.addSelectionChangedListener(this);
        viewer.addDoubleClickListener(this);
        updateInput();
        IrcModel.getInstance().addModelEventListener(this);

        Tree tree = viewer.getTree();
        openPrivateChatAction = IrcTreeAction.createOpenPrivateChatAction(tree);
        MenuManager accountsMenuManager = new MenuManager("#PopupMenu");
        Menu accountsMenu = accountsMenuManager.createContextMenu(tree);
        accountsMenuManager.add(openPrivateChatAction);
        tree.setMenu(accountsMenu);
        getSite().registerContextMenu(this.getClass().getName(), accountsMenuManager, viewer);

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
     * @see org.eclipse.jface.viewers.IDoubleClickListener#doubleClick(org.eclipse.jface.viewers.DoubleClickEvent)
     */
    @Override
    public void doubleClick(DoubleClickEvent event) {
        openPrivateChatAction.run();
    }

    /**
     * @see org.l2x6.eircc.core.model.event.IrcModelEventListener#handle(org.l2x6.eircc.core.model.event.IrcModelEvent)
     */
    @Override
    public void handle(IrcModelEvent e) {
        switch (e.getEventType()) {
        case CHANNEL_USERS_CHANGED:
        case CHANNEL_USER_JOINED:
        case CHANNEL_USER_LEFT:
            getTreeViewer().refresh();
            break;
        default:
            break;
        }
    }

    /**
     *
     */
    public void updateInput() {
        AbstractIrcChannel channel = editor.getChannel();
        getTreeViewer().setInput(channel);
    }
}
