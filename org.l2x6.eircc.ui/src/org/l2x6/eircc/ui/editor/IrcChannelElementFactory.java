/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.editor;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.l2x6.eircc.core.model.IrcAccount;
import org.l2x6.eircc.core.model.IrcChannel;
import org.l2x6.eircc.core.model.IrcModel;
import org.l2x6.eircc.ui.editor.IrcChannelEditorInput.IrcChannelEditorInputField;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcChannelElementFactory implements IElementFactory {
    public static final String ID = "org.l2x6.eircc.ui.editor.IrcChannelElementFactory";
    /**
     * @see org.eclipse.ui.IElementFactory#createElement(org.eclipse.ui.IMemento)
     */
    @Override
    public IAdaptable createElement(IMemento memento) {
        String accountLabel = memento.getString(IrcChannelEditorInputField.ACCOUNT_LABEL.name());
        String channelName = memento.getString(IrcChannelEditorInputField.CHANNEL_NAME.name());
        if (accountLabel != null && channelName != null) {
            IrcAccount account = IrcModel.getInstance().getAccount(accountLabel);
            if (account != null) {
                IrcChannel channel = account.findChannel(channelName);
                if (channel != null) {
                    return new IrcChannelEditorInput(channel);
                }
            }
        }
        return null;
    }

}