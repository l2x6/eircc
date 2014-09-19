/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.l2x6.eircc.core.model.resource.IrcChannelResource;
import org.l2x6.eircc.core.model.resource.IrcLogResource;
import org.l2x6.eircc.core.model.resource.IrcResourceException;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcChannel extends AbstractIrcChannel {
    private final IrcChannelResource channelResource;
    private final String name;
    private final IrcLog log;

    /**
     * @param account
     * @param channelPropsFile
     * @throws IOException
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     * @throws CoreException
     * @throws IrcResourceException
     */
    public IrcChannel(IrcAccount account, IFile channelPropsFile) throws UnsupportedEncodingException,
            FileNotFoundException, IOException, CoreException, IrcResourceException {
        super(account);
        this.name = IrcChannelResource.getChannelNameFromChannelPropsFile(channelPropsFile);
        this.channelResource = account.getAccountResource().getChannelResource(name);
        load(channelPropsFile);
        IrcLogResource logResource = channelResource.getActiveLogResource();
        this.log = new IrcLog(this, logResource);

    }
    public IrcLog getLog() {
        return log;
    }

    /**
     * @param account
     * @param name
     * @throws IrcResourceException
     */
    public IrcChannel(IrcAccount account, String name) throws IrcResourceException {
        super(account);
        this.name = name;
        this.channelResource = account.getAccountResource().getChannelResource(name);
        this.kept = true;
        IrcLogResource logResource = channelResource.getActiveLogResource();
        this.log = new IrcLog(this, logResource);
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
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

    public IrcChannelResource getChannelResource() {
        return channelResource;
    }

    public String getName() {
        return name;
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
     * @see org.l2x6.eircc.core.model.AbstractIrcChannel#isP2p()
     */
    @Override
    public boolean isP2p() {
        return false;
    }
}
