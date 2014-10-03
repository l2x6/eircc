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
public class PlainIrcChannel extends IrcBase implements Comparable<PlainIrcChannel> {
    private final String name;
    private final IrcServer server;

    /**
     * @param name
     */
    public PlainIrcChannel(IrcServer server, String name) {
        super();
        this.server = server;
        if (name == null) {
            throw new IllegalArgumentException(this.getClass().getName() + ".name cannot be null.");
        }
        this.name = name;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(PlainIrcChannel other) {
        return this.name.compareTo(other.name);
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcBase#dispose()
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
        PlainIrcChannel other = (PlainIrcChannel) obj;
        return this.name.equals(other.name);
    }

    public String getName() {
        return name;
    }

    public IrcServer getServer() {
        return server;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

}
