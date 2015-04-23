/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.client.cmd;

import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.core.model.IrcAccount;
import org.l2x6.eircc.core.model.PlainIrcUser;
import org.schwering.irc.lib.util.CTCPCommand;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public interface IrcCommandCallback {
    public void onCtcp(AbstractIrcChannel channel, PlainIrcUser plainUser, CTCPCommand ctcpCommand, String msg);
    public void onNick(IrcAccount account, PlainIrcUser user, String newNick);
    public void onReply(IrcAccount account, int num, String value, String msg);
}
