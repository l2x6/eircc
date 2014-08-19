/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.views;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.handlers.CollapseAllHandler;
import org.eclipse.ui.handlers.ExpandAllHandler;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.l2x6.eircc.core.IrcController;
import org.l2x6.eircc.core.IrcModelEvent;
import org.l2x6.eircc.core.IrcModelEventListener;
import org.l2x6.eircc.core.model.IrcAccount;
import org.l2x6.eircc.core.model.IrcChannel;
import org.l2x6.eircc.core.model.IrcModel;
import org.l2x6.eircc.ui.ContextMenuConstants;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.IrcUiMessages;
import org.l2x6.eircc.ui.actions.AddIrcAccountAction;
import org.l2x6.eircc.ui.actions.JoinIrcChannelAction;
import org.l2x6.eircc.ui.actions.LeaveIrcChannelAction;
import org.l2x6.eircc.ui.actions.ListIrcChannelsAction;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcAccountsView extends ViewPart implements IrcModelEventListener {
    public static final String ID = "org.l2x6.eircc.ui.views.IrcAccountsView";
    private SashForm accountsDetailsSplitter;
    private Listener accountsTreeSelectionListener;
    private TreeViewer accountsTreeViewer;
    private AddIrcAccountAction addIrcAccountAction;

    private CollapseAllHandler collapseAllAccounts;
    private Label emptyLabel;

    private ExpandAllHandler expandAllAccounts;

    private JoinIrcChannelAction joinIrcChannelAction;

    private LeaveIrcChannelAction leaveIrcChannelAction;
    private ListIrcChannelsAction listChannelsAction;
    private PageBook pagebook;
    private CLabel serverChannelsLabel;
    private TreeViewer serverChannelsTreeViewer;
    // TODO private TreeViewer serverUsersTreeViewer;
    private ViewForm serverChannelsViewForm;
    private MouseListener treeMouseListener;

    @Override
    public void createPartControl(Composite container) {

        pagebook = new PageBook(container, SWT.NONE);

        emptyLabel = new Label(pagebook, SWT.TOP + SWT.LEFT + SWT.WRAP);
        emptyLabel.setText(IrcUiMessages.IrcServersView_empty);

        accountsDetailsSplitter = new SashForm(pagebook, SWT.VERTICAL);
        // accountsDetailsSplitter.setVisible(false);

        /* accounts tree */
        accountsTreeViewer = new TreeViewer(accountsDetailsSplitter, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        accountsTreeViewer.setLabelProvider(IrcLabelProvider.getInstance());
        accountsTreeViewer.setContentProvider(new IrcAccountsTreeContentProvider());

        /* the bottom part */

        /* server channels */
        serverChannelsViewForm = new ViewForm(accountsDetailsSplitter, SWT.NONE);
        accountsDetailsSplitter.setWeights(new int[] { 35, 65 });

        serverChannelsTreeViewer = new TreeViewer(serverChannelsViewForm, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        serverChannelsTreeViewer.setLabelProvider(IrcLabelProvider.getInstance());
        serverChannelsTreeViewer.setContentProvider(new IrcServerChannelsTreeContentProvider());

        serverChannelsViewForm.setContent(serverChannelsTreeViewer.getControl());

        serverChannelsLabel = new CLabel(serverChannelsViewForm, SWT.NONE);
        serverChannelsViewForm.setTopLeft(serverChannelsLabel);
        serverChannelsLabel.setText(IrcUiMessages.IrcAccountsView_serverChannelsLabel_text);

        ToolBar serverChannelsToolbar = new ToolBar(serverChannelsViewForm, SWT.FLAT | SWT.WRAP);
        serverChannelsViewForm.setTopCenter(serverChannelsToolbar);

        /* server users */
        // TODO server users pane

        /* connect the model */
        IrcModel ircModel = IrcModel.getInstance();
        ircModel.addModelEventListener(this);
        accountsTreeViewer.setInput(ircModel);
        setEmptyLabelVisible(!ircModel.hasAccounts());

        /* actions, tolbars and menus */

        /* acounts related actions */
        Tree accountsTree = accountsTreeViewer.getTree();
        accountsTree.addListener(SWT.Selection, getAccountsTreeSelectionListener());
        accountsTree.addMouseListener(getTreeMouseListener());
        addIrcAccountAction = new AddIrcAccountAction();
        listChannelsAction = new ListIrcChannelsAction(accountsTree);

        IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
        tbm.add(new Separator(ContextMenuConstants.GROUP_IRC_ACCOUNTS));
        tbm.appendToGroup(ContextMenuConstants.GROUP_IRC_ACCOUNTS, addIrcAccountAction);
        getViewSite().getActionBars().updateActionBars();

        /* server channels related actions */
        Tree serverChannelsTree = serverChannelsTreeViewer.getTree();
        serverChannelsTree.addMouseListener(getTreeMouseListener());

        joinIrcChannelAction = new JoinIrcChannelAction(serverChannelsTree);
        leaveIrcChannelAction = new LeaveIrcChannelAction(serverChannelsTree);

        ToolBarManager serverChannelsTbm = new ToolBarManager(serverChannelsToolbar);
        serverChannelsTbm.add(new Separator(ContextMenuConstants.GROUP_IRC_SERVER_CHANNELS));
        serverChannelsTbm.appendToGroup(ContextMenuConstants.GROUP_IRC_SERVER_CHANNELS, listChannelsAction);
        serverChannelsTbm.appendToGroup(ContextMenuConstants.GROUP_IRC_SERVER_CHANNELS, joinIrcChannelAction);
        serverChannelsTbm.appendToGroup(ContextMenuConstants.GROUP_IRC_SERVER_CHANNELS, leaveIrcChannelAction);
        serverChannelsTbm.update(false);

        IWorkbenchPartSite site = getSite();
        if (site != null) {
            IHandlerService handlerService = (IHandlerService) site.getService(IHandlerService.class);
            if (handlerService != null) {
                collapseAllAccounts = new CollapseAllHandler(accountsTreeViewer);
                handlerService.activateHandler(CollapseAllHandler.COMMAND_ID, collapseAllAccounts);
                expandAllAccounts = new ExpandAllHandler(accountsTreeViewer);
                handlerService.activateHandler(ExpandAllHandler.COMMAND_ID, expandAllAccounts);
            }
        }
    }

    @Override
    public void dispose() {
        collapseAllAccounts.dispose();
        expandAllAccounts.dispose();
        Tree accountsTree = accountsTreeViewer.getTree();
        if (!accountsTree.isDisposed()) {
            accountsTree.removeListener(SWT.Selection, getAccountsTreeSelectionListener());
            accountsTree.removeMouseListener(getTreeMouseListener());
        }
        Tree serverChannelsTree = accountsTreeViewer.getTree();
        if (!serverChannelsTree.isDisposed()) {
            serverChannelsTree.removeMouseListener(getTreeMouseListener());
        }
        IrcModel.getInstance().removeModelEventListener(this);
        listChannelsAction.dispose();
        joinIrcChannelAction.dispose();
        leaveIrcChannelAction.dispose();
        super.dispose();
    }

    private Listener getAccountsTreeSelectionListener() {
        if (accountsTreeSelectionListener == null) {
            accountsTreeSelectionListener = new Listener() {
                @Override
                public void handleEvent(Event event) {
                    if (event.widget instanceof Tree) {
                        Tree tree = (Tree) event.widget;
                        TreeItem[] selection = tree.getSelection();
                        if (selection.length == 1 && selection[0].getData() instanceof IrcAccount) {
                            IrcAccount account = (IrcAccount) selection[0].getData();
                            serverChannelsTreeViewer.setInput(account.getServer());
                        } else {
                            serverChannelsTreeViewer.setInput(null);
                        }
                    }
                }
            };
        }
        return accountsTreeSelectionListener;
    }

    private MouseListener getTreeMouseListener() {
        if (treeMouseListener == null) {
            treeMouseListener = new MouseListener() {
                @Override
                public void mouseDoubleClick(MouseEvent e) {
                    try {
                        if (e.widget instanceof Tree) {
                            Tree tree = (Tree) e.widget;
                            TreeItem[] selection = tree.getSelection();
                            if (selection.length == 1 && selection[0].getData() instanceof IrcChannel) {
                                IrcChannel ch = (IrcChannel) selection[0].getData();
                                if (!ch.isJoined()) {
                                    /* this should both join and open the editor */
                                    IrcController.getInstance().joinChannel(ch);
                                } else {
                                    EirccUi.getDefault().openChannelEditor(ch);
                                }
                            }
                        }
                    } catch (Exception e1) {
                        EirccUi.log(e1);
                    }
                }

                @Override
                public void mouseDown(MouseEvent e) {
                }

                @Override
                public void mouseUp(MouseEvent e) {
                }
            };
        }
        return treeMouseListener;
    }

    /**
     * @see org.l2x6.eircc.core.IrcModelEventListener#handle(org.l2x6.eircc.core.IrcModelEvent)
     */
    @Override
    public void handle(IrcModelEvent e) {
        switch (e.getEventType()) {
        case ACCOUNT_ADDED:
            accountsTreeViewer.refresh();
            setEmptyLabelVisible(false);
            break;
        case ACCOUNT_REMOVED:
            accountsTreeViewer.refresh();
            if (!IrcModel.getInstance().hasAccounts()) {
                /* removed the last account */
                setEmptyLabelVisible(true);
            }
            break;
        case KEPT_CHANNEL_ADDED:
            accountsTreeViewer.refresh();
            accountsTreeViewer.expandAll();
            break;
        case KEPT_CHANNEL_REMOVED:
        case ACCOUNT_STATE_CHANGED:
            accountsTreeViewer.refresh();
            IrcAccount account = (IrcAccount) e.getModelObject();
            accountsTreeViewer.setExpandedState(account, true);
            break;
        case CHANNEL_JOINED_CHANGED:
            accountsTreeViewer.refresh();
            serverChannelsTreeViewer.refresh();
            joinIrcChannelAction.updateEnablement();
            leaveIrcChannelAction.updateEnablement();
            break;
        case SERVER_CHANNEL_ADDED:
        case SERVER_CHANNELS_ADDED:
        case SERVER_CHANNEL_REMOVED:
            serverChannelsTreeViewer.refresh();
            break;
        case CHANNEL_USERS_CHANGED:
        case USER_ADDED:
        case USER_REMOVED:
        case NEW_MESSAGE:
            /* ignore */
            break;
        default:
            break;
        }
    }

    private void setEmptyLabelVisible(boolean visible) {
        if (visible) {
            pagebook.showPage(emptyLabel);
        } else {
            pagebook.showPage(accountsDetailsSplitter);
        }
    }

    @Override
    public void setFocus() {
        accountsTreeViewer.getTree().setFocus();
    }

}
