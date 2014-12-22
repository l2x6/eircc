/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.actions;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.l2x6.eircc.core.IrcController;
import org.l2x6.eircc.core.IrcException;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.core.model.IrcAccount;
import org.l2x6.eircc.core.model.IrcAccount.IrcAccountState;
import org.l2x6.eircc.core.model.IrcChannelUser;
import org.l2x6.eircc.core.model.PlainIrcChannel;
import org.l2x6.eircc.core.model.resource.IrcResourceException;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.IrcUiMessages;
import org.l2x6.eircc.ui.misc.IrcImages;
import org.l2x6.eircc.ui.misc.IrcImages.ImageKey;
import org.l2x6.eircc.ui.prefs.IrcPreferences;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcTreeActions<E> extends Action implements Listener, IrcTreeAction {

    public static IrcTreeActions<IrcAccount> createConnectAccountAction(Tree tree) {
        Predicate<? super TreeItem> predicate = treeItem -> treeItem.getData() instanceof IrcAccount
                && ((IrcAccount) treeItem.getData()).getState() != IrcAccountState.ONLINE;
        Consumer<IrcAccount> itemAction = account -> {
            try {
                EirccUi.getController().connect(account);
            } catch (IrcException e) {
                EirccUi.log(e);
            }
        };
        return new IrcTreeActions<IrcAccount>(tree, IrcUiMessages.ConnectIrcChannelAction_label, ImageKey.CONNECT,
                predicate, itemAction);
    }

    public static IrcTreeActions<IrcAccount> createDisonnectAccountAction(Tree tree) {
        Predicate<? super TreeItem> predicate = treeItem -> treeItem.getData() instanceof IrcAccount
                && ((IrcAccount) treeItem.getData()).getState() == IrcAccountState.ONLINE;
        Consumer<IrcAccount> itemAction = account -> {
            try {
                EirccUi.getController().quit(account);
            } catch (Exception e) {
                EirccUi.log(e);
            }
        };
        return new IrcTreeActions<IrcAccount>(tree, IrcUiMessages.DisconnectIrcChannelAction_label,
                ImageKey.DISCONNECT, predicate, itemAction);
    }

    public static IrcTreeActions<AbstractIrcChannel> createJoinAccountChannelAction(Tree tree) {
        Predicate<? super TreeItem> predicate = treeItem -> (treeItem.getData() instanceof AbstractIrcChannel && !((AbstractIrcChannel) treeItem
                .getData()).isJoined()) || treeItem.getData() instanceof PlainIrcChannel;
        Consumer<AbstractIrcChannel> itemAction = channel -> {
            try {
                EirccUi.getController().joinChannel(channel);
            } catch (IrcException e) {
                EirccUi.log(e);
            }
        };
        return new IrcTreeActions<AbstractIrcChannel>(tree, IrcUiMessages.JoinIrcChannelAction_label,
                ImageKey.JOIN_CHANNEL, predicate, itemAction);
    }

    public static IrcTreeActions<PlainIrcChannel> createJoinServerChannelAction(Tree tree) {
        Predicate<? super TreeItem> predicate = treeItem -> treeItem.getData() instanceof PlainIrcChannel;
        Consumer<PlainIrcChannel> itemAction = channel -> {
            try {
                EirccUi.getController().joinChannel(channel);
            } catch (IrcException | IrcResourceException e) {
                EirccUi.log(e);
            }
        };
        return new IrcTreeActions<PlainIrcChannel>(tree, IrcUiMessages.JoinIrcChannelAction_label,
                ImageKey.JOIN_CHANNEL, predicate, itemAction);
    }

    public static IrcTreeActions<AbstractIrcChannel> createLeaveChannelAction(Tree tree) {
        Predicate<? super TreeItem> predicate = treeItem -> treeItem.getData() instanceof AbstractIrcChannel
                && ((AbstractIrcChannel) treeItem.getData()).isJoined();
        Consumer<AbstractIrcChannel> itemAction = channel -> {
            try {
                EirccUi.getController().partChannel(channel);
            } catch (IrcException e) {
                EirccUi.log(e);
            }
        };
        return new IrcTreeActions<AbstractIrcChannel>(tree, IrcUiMessages.LeaveIrcChannelAction_label,
                ImageKey.LEAVE_CHANNEL, predicate, itemAction);
    }

    public static IrcTreeActions<IrcAccount> createListChannelsAction(Tree tree) {
        Predicate<? super TreeItem> predicate = treeItem -> treeItem.getData() instanceof IrcAccount;
        Consumer<IrcAccount> itemAction = account -> {
            try {
                EirccUi.getController().listChannels(account);
            } catch (IrcException e) {
                EirccUi.log(e);
            }
        };
        return new IrcTreeActions<IrcAccount>(tree, IrcUiMessages.ListChannelsAction_label, ImageKey.REFRESH,
                predicate, itemAction);
    }

    /**
     * @param tree2
     * @return
     */
    public static IrcTreeActions<?> createOpenPrivateChatAction(Tree tree) {
        Predicate<? super TreeItem> predicate = treeItem -> treeItem.getData() instanceof IrcChannelUser;
        Consumer<IrcChannelUser> itemAction = user -> {
            try {
                IrcController controller = EirccUi.getController();
                AbstractIrcChannel ch = controller.getOrCreateP2pChannel(user.getUser());
                if (!ch.isJoined()) {
                    /* this should both join and open the editor */
                    EirccUi.getController().joinChannel(ch);
                } else {
                    EirccUi.getDefault().openEditor(ch);
                }
            } catch (Exception e) {
                EirccUi.log(e);
            }
        };
        return new IrcTreeActions<IrcChannelUser>(tree, IrcUiMessages.IrcChannelOutlinePage_Open_Private_Chat,
                ImageKey.JOIN_CHANNEL, predicate, itemAction);
    }

    public static IrcTreeActions<?> createWatchChannelAction(Tree tree) {
        Predicate<? super TreeItem> predicate =
                treeItem -> treeItem.getData() instanceof AbstractIrcChannel
                && !IrcPreferences.getInstance().isWatched((AbstractIrcChannel) treeItem.getData());
        Consumer<AbstractIrcChannel> itemAction = channel -> {
            try {
                if (channel != null) {
                    IrcPreferences prefs = IrcPreferences.getInstance();
                    prefs.addWatchedChannel(channel.getName());
                }
            } catch (Exception e) {
                EirccUi.log(e);
            }
        };
        return new IrcTreeActions<AbstractIrcChannel>(tree, IrcUiMessages.IrcChannelOutlinePage_watchThisChannel, null,
                predicate, itemAction);
    }

    /**
     * @param tree2
     * @return
     */
    public static IrcTreeActions<?> createWatchUserAction(Tree tree) {
        Predicate<? super TreeItem> predicate = treeItem -> treeItem.getData() instanceof IrcChannelUser;
        Consumer<IrcChannelUser> itemAction = user -> {
            try {
                if (user != null) {
                    String patternProposal = user.getUser().getUsername();
                    IrcPreferences prefs = IrcPreferences.getInstance();
                    String pattern = prefs.showAddNickPatternDialog(patternProposal);
                    if (pattern != null) {
                        prefs.addWatchedNickPattern(pattern);
                    }
                }
            } catch (Exception e) {
                EirccUi.log(e);
            }
        };
        return new IrcTreeActions<IrcChannelUser>(tree, IrcUiMessages.IrcChannelOutlinePage_watchThisUser, null,
                predicate, itemAction);
    }

    protected final Predicate<? super TreeItem> enabledPredicate;
    private final Consumer<? super E> itemAction;

    protected final Tree tree;

    private IrcTreeActions(Tree tree, String label, ImageKey imageKey, Predicate<? super TreeItem> enabledPredicate,
            Consumer<? super E> itemAction) {
        this.tree = tree;
        setEnabled(false);
        setText(label);
        if (imageKey != null) {
            setImageDescriptor(IrcImages.getInstance().getImageDescriptor(imageKey));
        }
        this.enabledPredicate = enabledPredicate;
        this.itemAction = itemAction;
        tree.addListener(SWT.Selection, this);
    }

    public void dispose() {
        if (!tree.isDisposed()) {
            tree.removeListener(SWT.Selection, this);
        }
    }

    /**
     * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
     */
    @Override
    public void handleEvent(Event event) {
        updateEnablement();
    }

    @SuppressWarnings("unchecked")
    public void run() {
        try {
            Arrays.stream(tree.getSelection()).filter(enabledPredicate).map(treeItem -> (E) treeItem.getData())
                    .forEach(itemAction);
        } catch (Exception e) {
            EirccUi.log(e);
        }
    }

    /**
     *
     */
    public void updateEnablement() {
        boolean enabled = Arrays.stream(tree.getSelection()).anyMatch(enabledPredicate);
        setEnabled(enabled);
    }
}
