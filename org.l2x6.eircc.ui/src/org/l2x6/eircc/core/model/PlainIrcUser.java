/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.io.IOException;

import org.l2x6.eircc.core.util.IrcLogReader.IrcLogReaderException;


/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class PlainIrcUser implements IrcUserBase {

    public static PlainIrcUser parse(String input) throws IrcLogReaderException {
        if (input == null || input.isEmpty()) {
            return null;
        }

        int nickEnd = input.indexOf(0, '!');
        if (nickEnd < 0) {
            /* For backwards compatibility reasons */
            return new PlainIrcUser(input, input, "unknown");
        }

        String nick = input.subSequence(0, nickEnd).toString();

        int usernameEnd = input.indexOf(nickEnd+1, '@');
        if (usernameEnd < 0) {
            throw new IrcLogReaderException("Could not parse '+ input +'");
        }
        String username = input.subSequence(nickEnd+ 1, usernameEnd).toString();
        String host = input.subSequence(usernameEnd + 1, input.length()).toString();

        return new PlainIrcUser(host, nick, username);
    }
    private final String host;
    private final String nick;
    private final String username;

    /**
     * @param nick
     * @param username
     * @param host
     * @param realName
     */
    public PlainIrcUser(String nick, String username, String host) {
        super();
        this.username = username;
        this.host = host;
        this.nick = nick;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getNick() {
        return nick;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(length());
        try {
            append(sb);
        } catch (IOException e) {
            /* Should never happen as StringBuilder.append() does not throw it */
            throw new RuntimeException();
        }
        return sb.toString();
    }

}
