/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;


/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class P2pIrcChannel extends AbstractIrcChannel {

    private IrcUser p2pUser;

    /**
     * @param account
     * @param p2pUser
     */
    public P2pIrcChannel(IrcUser p2pUser) {
        super(p2pUser.getServer().getAccount());
        this.p2pUser = p2pUser;
        this.kept = false;
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
        P2pIrcChannel other = (P2pIrcChannel) obj;
        if (account == null) {
            if (other.account != null)
                return false;
        } else if (!account.equals(other.account))
            return false;
        if (p2pUser == null) {
            if (other.p2pUser != null)
                return false;
        } else if (!p2pUser.equals(other.p2pUser))
            return false;
        return true;
    }

    /**
     * @see org.l2x6.eircc.core.model.AbstractIrcChannel#getName()
     */
    @Override
    public String getName() {
        return p2pUser.getNick();
    }

    public IrcUser getP2pUser() {
        return p2pUser;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((account == null) ? 0 : account.hashCode());
        result = prime * result + ((p2pUser == null) ? 0 : p2pUser.hashCode());
        return result;
    }

    /**
     * @see org.l2x6.eircc.core.model.AbstractIrcChannel#isP2p()
     */
    @Override
    public boolean isP2p() {
        return true;
    }

    /**
     * @param p2p
     */
    public void setP2pUser(IrcUser p2pUser) {
        // IrcUser oldUser = this.p2pUser;
        this.p2pUser = p2pUser;
        // if (!oldUser.equals(p2pUser)) {
        // account.getModel().fire(new
        // IrcModelEvent(EventType.CHANNEL_P2P_USER_CHANGED, this));
        // }
    }


}
