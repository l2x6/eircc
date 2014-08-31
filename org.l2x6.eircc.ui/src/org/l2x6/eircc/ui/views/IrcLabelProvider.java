/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.views;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.core.model.IrcAccountsStatistics;
import org.l2x6.eircc.core.model.IrcBase;
import org.l2x6.eircc.core.model.IrcModel;
import org.l2x6.eircc.core.model.IrcObject;
import org.l2x6.eircc.ui.IrcUiMessages;
import org.l2x6.eircc.ui.misc.IrcImages;

public class IrcLabelProvider extends LabelProvider {

    private static final IrcLabelProvider INSTANCE = new IrcLabelProvider();

    public static String getChannelJoinedLabel(AbstractIrcChannel channel) {
        return channel.isJoined() ? IrcUiMessages.Channel_Connected : IrcUiMessages.Channel_Disconnected;
    }

    public static IrcLabelProvider getInstance() {
        return INSTANCE;
    }

    /**
     *
     */
    public IrcLabelProvider() {
        super();
    }

    @Override
    public Image getImage(Object element) {
        if (element instanceof IrcObject) {
            return IrcImages.getInstance().getImage((IrcObject) element);
        }
        return null;
    }

    /**
     * @param channel
     * @return
     */
    public String getTooltipText(IrcBase object) {
        if (object == null) {
            return null;
        } else if (object instanceof IrcModel) {
            IrcModel model = (IrcModel) object;
            StringBuilder sb = new StringBuilder();
            IrcAccountsStatistics stats = model.getAccountsStatistics();
            if (stats.hasChannelsWithUnreadMessages()) {
                sb.append(IrcUiMessages.Channel_You_have_unseen_messages);
            }
            if (sb.length() > 0) {
                sb.append(" - ");
            }
            sb.append(IrcUiMessages.Eclipse_IRC_Client);
            return sb.toString();
        } else if (object instanceof AbstractIrcChannel) {
            AbstractIrcChannel channel = (AbstractIrcChannel) object;
            return channel.getName() + "@" + channel.getAccount().getLabel() + " - " + getChannelJoinedLabel(channel);
        }
        return object.toString();
    }

}