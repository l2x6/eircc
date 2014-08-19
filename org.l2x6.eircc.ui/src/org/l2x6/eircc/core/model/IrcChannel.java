/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.l2x6.eircc.core.IrcModelEvent;
import org.l2x6.eircc.core.IrcModelEvent.EventType;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcChannel extends IrcObject {
    public enum IrcChannelField implements TypedField {
        autoJoin {
            @Override
            public Object fromString(String value) {
                return Boolean.valueOf(value);
            }
        };
        public Object fromString(String value) {
            return value;
        }
    }


    /**  */
    static final String FILE_EXTENSION = ".channel.properties";;
    private final IrcAccount account;
    private boolean autoJoin = true;
    private boolean joined;
    private boolean kept;

    private IrcLog log;
    private final String name;
    private IrcUser p2pUser;

    /** Users by nick */
    private final Map<String, IrcChannelUser> users = new TreeMap<String, IrcChannelUser>();
    private IrcChannelUser[] usersArray;
    /**
     * @param account
     * @param name
     */
    public IrcChannel(IrcAccount account, String name) {
        this.name = name;
        this.account = account;
    }

    /**
     * @param result
     */
    public void addNick(String nick) {
        addNickInternal(nick);
        account.getModel().fire(new IrcModelEvent(EventType.CHANNEL_USERS_CHANGED, this));
    }

    /**
     * @param user
     */
    private void addNickInternal(String nick) {
        // if (user.getServer() != account.getServer()) {
        // throw new
        // IllegalArgumentException("Cannot add user with parent distinct from this "
        // + this.getClass().getSimpleName());
        // }
        // String nick = user.getNick();
        // if (nicks.get(nick) != null) {
        // throw new IllegalArgumentException("User with nick '" + nick +
        // "' already available under server '"
        // + account.getHost() + "' of the account '" + account.getLabel() +
        // "'.");
        // }
        users.put(nick, createUser(nick));
        usersArray = null;
    }
    /**
     * @param users2
     */
    public void addNicks(Collection<String> nicks) {
        for (String nick : nicks) {
            addNickInternal(nick);
        }
        account.getModel().fire(new IrcModelEvent(EventType.CHANNEL_USERS_CHANGED, this));
    }

    /**
     * @param oldNick
     * @param newUser
     */
    public void changeNick(String oldNick, IrcUser newUser) {
        users.remove(oldNick);
        addNickInternal(newUser.getNick());
        account.getModel().fire(new IrcModelEvent(EventType.CHANNEL_USERS_CHANGED, this));
    }

    /**
     * @return
     */
    public IrcChannelUser createUser(String nick) {
        return new IrcChannelUser(this, nick);
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
        IrcChannel other = (IrcChannel) obj;
        if (account == null) {
            if (other.account != null)
                return false;
        } else if (!account.equals(other.account))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#getAccount()
     */
    public IrcAccount getAccount() {
        return account;
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#getFields()
     */
    @Override
    public IrcChannelField[] getFields() {
        return IrcChannelField.values();
    }

    public IrcLog getLog() {
        return log;
    }

    public String getName() {
        return name;
    }

    public IrcUser getP2pUser() {
        return p2pUser;
    }

    @Override
    protected File getSaveFile(File parentDir) {
        return new File(parentDir, name + FILE_EXTENSION);
    }

    /**
     * @return
     */
    public IrcChannelUser[] getUsers() {
        if (usersArray == null) {
            Collection<IrcChannelUser> chans = users.values();
            usersArray = chans.toArray(new IrcChannelUser[chans.size()]);
        }
        return usersArray;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((account == null) ? 0 : account.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    public boolean isAutoJoin() {
        return autoJoin;
    }

    public boolean isJoined() {
        return joined;
    }

    public boolean isKept() {
        return kept;
    }

    /**
     * @param user
     * @return
     */
    public boolean isPresent(String nick) {
        return users.containsKey(nick);
    }

    public void removeNick(String nick) {
        if (users.remove(nick) != null) {
            account.getModel().fire(new IrcModelEvent(EventType.CHANNEL_USERS_CHANGED, this));
        }
    }

    public void setAutoJoin(boolean autoJoin) {
        this.autoJoin = autoJoin;
    }

    public void setJoined(boolean joined) {
        boolean oldState = this.joined;
        this.joined = joined;
        if (oldState != joined) {
            if (joined && log == null) {
                log = new IrcLog(this, System.currentTimeMillis());
            }
            account.getModel().fire(new IrcModelEvent(EventType.CHANNEL_JOINED_CHANGED, this));
        }
    }

    public void setKept(boolean kept) {
        this.kept = kept;
    }

    /**
     * @param p2p
     */
    public void setP2pUser(IrcUser p2pUser) {
        this.p2pUser = p2pUser;
    }

    @Override
    public String toString() {
        return name;
    }
}
