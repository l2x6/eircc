/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.actions;

import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.l2x6.eircc.core.IrcController;
import org.l2x6.eircc.core.model.IrcChannel;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.IrcImages;
import org.l2x6.eircc.ui.IrcImages.ImageKey;
import org.l2x6.eircc.ui.IrcUiMessages;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class LeaveIrcChannelAction extends AbstractTreeAction {

    public LeaveIrcChannelAction(Tree tree) {
        super(tree);
        setText(IrcUiMessages.LeaveIrcChannelAction_label);
        setImageDescriptor(IrcImages.getInstance().getImageDescriptor(ImageKey.DISCONNECT));
    }

    public void run() {
        try {
            for (TreeItem treeItem : tree.getSelection()) {
                if (treeItem.getData() instanceof IrcChannel) {
                    IrcChannel channel = (IrcChannel) treeItem.getData();
                    if (channel.isJoined()) {
                        IrcController.getInstance().partChannel(channel);
                    }
                }
            }
        } catch (Exception e) {
            EirccUi.log(e);
        }
    }

    /**
     * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
     */
    @Override
    public void updateEnablement() {
        for (TreeItem treeItem : tree.getSelection()) {
            if (treeItem.getData() instanceof IrcChannel && ((IrcChannel) treeItem.getData()).isJoined()) {
                setEnabled(true);
                return;
            }
        }
        setEnabled(false);
    }

}
