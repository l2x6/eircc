/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import org.l2x6.eircc.core.model.resource.IrcChannelResource;
import org.l2x6.eircc.core.model.resource.IrcLogResource;
import org.l2x6.eircc.core.model.resource.IrcResourceException;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class P2pIrcChannel extends AbstractIrcChannel {

    private final IrcChannelResource channelResource;
    private final IrcLog log;
    private IrcUser p2pUser;

    /**
     * @param account
     * @param p2pUser
     * @throws IrcResourceException
     */
    public P2pIrcChannel(IrcUser p2pUser) throws IrcResourceException {
        super(p2pUser.getServer().getAccount());
        this.p2pUser = p2pUser;
        this.kept = false;
        addNickInternal(p2pUser.getNick());
        IrcUser me = account.getMe();
        if (me != null) {
            addNickInternal(me.getNick());
        }
        this.channelResource = account.getAccountResource().getOrCreateChannelResource(getName());
        IrcLogResource logResource = channelResource.getActiveLogResource();
        this.log = new IrcLog(this, logResource);
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

    public IrcChannelResource getChannelResource() {
        return channelResource;
    }

    public IrcLog getLog() {
        return log;
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

}
