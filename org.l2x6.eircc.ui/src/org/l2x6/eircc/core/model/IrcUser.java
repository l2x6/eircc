/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

import org.l2x6.eircc.core.model.event.IrcModelEvent;
import org.l2x6.eircc.core.model.event.IrcModelEvent.EventType;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcUser extends IrcObject {
    public enum IrcUserField implements TypedField {
        host, nick, username, previousNicksString;

        @Override
        public Object fromString(String value) {
            return value;
        }
    }

    public static final String FILE_EXTENSION = ".user.properties";

    private String host;
    private final UUID id;

    private String nick;

    private final Set<String> previousNicks;
    private final IrcServer server;

    private String username;

    /**
     * @param id
     * @param nick
     * @param username
     * @param account
     * @param realName
     */
    public IrcUser(IrcServer server, UUID id) {
        this.server = server;
        this.id = id;
        this.previousNicks = new LinkedHashSet<String>();
    }

    public IrcUser(IrcServer server, UUID id, String nick, String username) {
        this(server, id);
        this.username = username;
        this.nick = nick;
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#dispose()
     */
    @Override
    public void dispose() {
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IrcUser other = (IrcUser) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
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

    public UUID getId() {
        return id;
    }

    public String getNick() {
        return nick;
    }

    /**
     * @return
     */
    public String getPreviousNick() {
        for (String previousNick : previousNicks) {
            return previousNick;
        }
        return null;
    }

    public String getPreviousNicksString() {
        if (!previousNicks.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String nick : previousNicks) {
                sb.append(nick).append(' ');
            }
            return sb.toString();
        }
        return null;
    }

    @Override
    protected File getSaveFile(File parentDir) {
        return new File(parentDir, id.toString() + FILE_EXTENSION);
    }

    public IrcServer getServer() {
        return server;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    public boolean hasOrHadNick(String nick) {
        if (nick == null) {
            return false;
        }
        return nick.equals(this.nick) || previousNicks.stream().anyMatch(previousNick -> nick.equals(previousNick));
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setNick(String nick) {
        String oldNick = this.nick;
        this.nick = nick;
        if (oldNick != null && !oldNick.equals(nick)) {
            previousNicks.add(oldNick);
            getServer().getAccount().getModel().fire(new IrcModelEvent(EventType.NICK_CHANGED, this));
        }
    }

    public void setPreviousNicksString(String nicks) {
        previousNicks.clear();
        if (nicks != null) {
            StringTokenizer st = new StringTokenizer(nicks, " ");
            while (st.hasMoreTokens()) {
                previousNicks.add(st.nextToken());
            }
        }
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return nick;
    }

}
