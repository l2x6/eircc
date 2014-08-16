/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.io.File;


/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcUser extends IrcObject {
    public enum IrcUserField {};
    private String host;

    private final String nick;

    private final IrcServer server;
    private final String username;
    /**
     * @param id
     * @param nick
     * @param username
     * @param account
     * @param realName
     */
    public IrcUser(IrcServer server, String nick, String username) {
        super();
        this.server = server;
        this.username = username;
        this.nick = nick;
    }
    /**
     * @see org.l2x6.eircc.core.model.IrcObject#dispose()
     */
    @Override
    public void dispose() {
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#getFields()
     */
    @Override
    public IrcUserField[] getFields() {
        return IrcUserField.values();
    }

    public String getHost() {
        return host;
    }

    public String getNick() {
        return nick;
    }

    @Override
    protected File getSaveFile(File parentDir) {
        return new File(parentDir, username + ".user.properties");
    }

    public IrcServer getServer() {
        return server;
    }
    public String getUsername() {
        return username;
    }

    public void setHost(String host) {
        this.host = host;
    }

}
