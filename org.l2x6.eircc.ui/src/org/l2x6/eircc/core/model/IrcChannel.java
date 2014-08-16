/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.io.File;

import org.l2x6.eircc.core.IrcModelEvent;
import org.l2x6.eircc.core.IrcModelEvent.EventType;


/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcChannel extends IrcObject {
    public enum IrcChannelField implements TypedField {
        autoJoin{
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
    private boolean hasUnseenMessages;
    private boolean joined;
    private boolean kept;
    private IrcLog log;
    private final String name;

    private IrcUser p2pUser;

    /**
     * @param account
     * @param name
     */
    public IrcChannel(IrcAccount account, String name) {
        this.name = name;
        this.account = account;
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


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((account == null) ? 0 : account.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }


    /**
     * @return
     */
    public boolean hasUnseenMessages() {
        return hasUnseenMessages;
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


    public void setAutoJoin(boolean autoJoin) {
        this.autoJoin = autoJoin;
    }

    public void setHasUnseenMessages(boolean hasUnseenMessages) {
        this.hasUnseenMessages = hasUnseenMessages;
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
