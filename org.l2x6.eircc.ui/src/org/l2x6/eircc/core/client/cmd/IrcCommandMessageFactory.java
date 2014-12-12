/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.client.cmd;

import java.util.Locale;
import java.util.StringTokenizer;

import org.l2x6.eircc.core.IrcException;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.core.util.IrcConstants;
import org.schwering.irc.lib.CTCPCommand;
import org.schwering.irc.lib.IRCCommand;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcCommandMessageFactory {

    public static IrcCommandMessage createCommandMessage(AbstractIrcChannel channel, String sourceText) throws IrcException {
        String initialCommand = IrcCommandMessageFactory.getInitialCommand(sourceText);
        if (initialCommand != null) {
            IRCCommand ircCommand = IRCCommand.fastValueOf(initialCommand);
            if (ircCommand != null) {
                switch (ircCommand) {
                case NICK:
                    return new NickIrcCommandMessage(channel, sourceText);
                default:
                    return new IrcCommandMessage(channel, sourceText, ircCommand);
                }
            }

            CTCPCommand ctcpCommand = CTCPCommand.fastValueOf(initialCommand);
            if (ctcpCommand != null) {
                return new CtcpIrcCommandMessage(channel, initialCommand, ctcpCommand, sourceText);
            } else {
                throw new IrcException("Unsupported command '"+ initialCommand +"'", channel);
            }
        } else {
            return null;
        }

    }

    /**
     * Looks whether the {@code message} starts with {@code '/'} followed by a
     * command name followed in {@link IRCCommand}.
     *
     * @param message
     * @return a command string without the initial {@code '/'} or {@code null}
     */
    public static String getInitialCommand(String message) {
        if (message.length() > 2 && message.charAt(0) == IrcConstants.COMMAND_MARKER) {
            String firstToken = new StringTokenizer(message, " \t\n\r").nextToken();
            firstToken = firstToken.substring(1);
            return firstToken.toUpperCase(Locale.ENGLISH);
        }
        return null;
    }
}
