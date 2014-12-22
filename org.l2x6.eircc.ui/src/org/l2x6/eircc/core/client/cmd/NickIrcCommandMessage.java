/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.client.cmd;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.l2x6.eircc.core.client.IrcClient;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.schwering.irc.lib.IRCCommand;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class NickIrcCommandMessage extends IrcCommandMessage {

    private static class Parser {
        private String newNick;
        private int offset;
        private final String sourceText;
        private boolean targetsAllConnections;
        private Duration timeout;

        /**
         *
         */
        public Parser(IRCCommand ircCommand, String sourceText) {
            super();
            String cmdString = ircCommand.name();
            this.offset = cmdString.length() + 1;
            this.sourceText = sourceText;
            wsOrAllMarker();
            nick();
            wsOrAllMarker();
            timeout();
        }

        public String getNewNick() {
            return newNick;
        }

        public Duration getTimeout() {
            return timeout;
        }

        /**
         *
         */
        private void nick() {
            int start = offset;
            LOOP: while (offset < sourceText.length()) {
                char ch = sourceText.charAt(offset);
                switch (ch) {
                case SEND_TO_ALL_CONNECTIONS_MARKER:
                case ' ':
                    break LOOP;
                }
                offset++;
            }
            if (offset > start) {
                newNick = sourceText.substring(start, offset);
            }
        }

        public boolean targetsAllConnections() {
            return targetsAllConnections;
        }

        /**
         *
         */
        private void timeout() {
            int start = offset;
            LOOP: while (offset < sourceText.length()) {
                char ch = sourceText.charAt(offset);
                if (ch >= '0' && ch <= '9') {
                    /* expected */
                } else {
                    break LOOP;
                }
                offset++;
            }
            if (offset > start) {
                String timeout = sourceText.substring(start, offset);
                start = offset;
                ChronoUnit unit = ChronoUnit.MINUTES;
                if (offset < sourceText.length()) {
                    char ch = sourceText.charAt(offset);
                    switch (ch) {
                    case 's':
                    case 'S':
                        unit = ChronoUnit.SECONDS;
                        break;
                    case 'm':
                    case 'M':
                        unit = ChronoUnit.MINUTES;
                        break;
                    case 'h':
                    case 'H':
                        unit = ChronoUnit.HOURS;
                        break;
                    case 'd':
                    case 'D':
                        unit = ChronoUnit.DAYS;
                        break;
                    }
                }

                this.timeout = timeout != null ? Duration.of(Long.parseLong(timeout), unit) : null;
            }
        }

        /**
         *
         */
        private void wsOrAllMarker() {
            while (offset < sourceText.length()) {
                char ch = sourceText.charAt(offset);
                switch (ch) {
                case SEND_TO_ALL_CONNECTIONS_MARKER:
                    targetsAllConnections = true;
                    break;
                case ' ':
                    /* ignore */
                    break;
                default:
                    return;
                }
                offset++;
            }
        }



    }

    private static final char SEND_TO_ALL_CONNECTIONS_MARKER = '*';
    private final String protocolCommand;
    private final boolean targetsAllConnections;
    private final Duration timeout;

    /**
     * @param sourceText
     * @param ircCommand
     */
    public NickIrcCommandMessage(AbstractIrcChannel channel, String sourceText) {
        super(channel, sourceText, IRCCommand.NICK);

        Parser parser = new Parser(ircCommand, sourceText);

        /* timeout */
        this.timeout = parser.getTimeout();
        this.targetsAllConnections = parser.targetsAllConnections();
        this.protocolCommand = IRCCommand.NICK.name() + " "+ parser.getNewNick();
    }

    @Override
    public String getProtocolCommand() {
        return protocolCommand;
    }

    @Override
    public IrcCommandCallbackList processCallbacks(IrcCommandCallbackList callbacks) {
        for (IrcCommandCallback callback : callbacks) {
            if (callback instanceof NickIrcCommandCallback) {
                NickIrcCommandCallback nickCallback = (NickIrcCommandCallback) callback;
                nickCallback.update(sourceText, timeout);
            }
        }
        return super.processCallbacks(callbacks);
    }

    public boolean targetsClient(IrcClient client) {
        return targetsAllConnections || super.targetsClient(client);
    }

}
