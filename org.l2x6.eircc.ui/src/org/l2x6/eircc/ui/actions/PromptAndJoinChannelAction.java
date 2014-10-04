/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.actions;

import java.text.MessageFormat;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.l2x6.eircc.core.IrcException;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.core.model.IrcAccount;
import org.l2x6.eircc.core.model.PlainIrcChannel;
import org.l2x6.eircc.core.model.resource.IrcResourceException;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.IrcUiMessages;
import org.l2x6.eircc.ui.misc.IrcImages;
import org.l2x6.eircc.ui.misc.IrcImages.ImageKey;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class PromptAndJoinChannelAction extends Action implements Listener, IrcTreeAction {

    private static final IInputValidator PATTERN_VALIDATOR = new IInputValidator() {
        /**
         * Validates the String. Returns null for no error, or an error message
         *
         * @param newText
         *            the String to validate
         * @return String
         */
        public String isValid(String newText) {
            if (newText.isEmpty()) {
                return IrcUiMessages.IrcNotificationsPreferencePage_cannotBeEmpty;
            }

            if (!newText.startsWith("#")) {
                return IrcUiMessages.PromptAndJoinChannelAction_shouldStartWithHash;
            }

            try {
                Pattern.compile(newText);
            } catch (PatternSyntaxException e) {
                return MessageFormat
                        .format(IrcUiMessages.IrcNotificationsPreferencePage_invalidPattern, e.getMessage());
            }
            return null;
        }
    };

    private final Tree tree;

    public PromptAndJoinChannelAction(Tree tree) {
        this.tree = tree;
        setText(IrcUiMessages.PromptAndJoinChannelAction_label);
        setImageDescriptor(IrcImages.getInstance().getImageDescriptor(ImageKey.JOIN_CHANNEL));
        setEnabled(false);
        tree.addListener(SWT.Selection, this);
    }

    public void dispose() {
        if (!tree.isDisposed()) {
            tree.removeListener(SWT.Selection, this);
        }
    }

    /**
     * @return
     */
    private IrcAccount getAccount() {
        TreeItem[] selection = tree.getSelection();
        if (selection != null && selection.length > 0) {
            TreeItem selectionItem = selection[0];
            Object data = selectionItem.getData();
            if (data instanceof IrcAccount) {
                return (IrcAccount) data;
            } else if (data instanceof AbstractIrcChannel) {
                return ((AbstractIrcChannel) data).getAccount();
            } else if (data instanceof PlainIrcChannel) {
                return ((PlainIrcChannel) data).getServer().getAccount();
            }
        }
        return null;
    }

    /**
     * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
     */
    @Override
    public void handleEvent(Event event) {
        updateEnablement();
    }

    public void run() {
        try {
            IrcAccount account = getAccount();
            if (account != null) {
                InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(),
                        IrcUiMessages.PromptAndJoinChannelAction_label, IrcUiMessages.PromptAndJoinChannelAction_label,
                        "", PATTERN_VALIDATOR);
                if (dialog.open() == Window.OK) {
                    String channelName = dialog.getValue();
                    PlainIrcChannel channel = new PlainIrcChannel(account.getServer(), channelName);
                    EirccUi.getController().joinChannel(channel);
                }
            }
        } catch (IrcException | IrcResourceException e) {
            EirccUi.log(e);
        }
    }

    /**
     *
     */
    public void updateEnablement() {
        setEnabled(getAccount() != null);
    }

}
