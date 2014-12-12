/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.client.cmd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.schwering.irc.lib.IRCEventListener;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcCommandCallbackList implements Iterable<IrcCommandCallback> {
    private static final IrcCommandCallbackList EMPTY = new IrcCommandCallbackList(Collections.emptyList());

    private final List<IrcCommandCallback> callbacks;
    /**
     * @param callbacks
     */
    private IrcCommandCallbackList(List<IrcCommandCallback> callbacks) {
        super();
        this.callbacks = callbacks;
    }

    public IrcCommandCallbackList add(IrcCommandCallback callback) {
        ArrayList<IrcCommandCallback> result = new ArrayList<IrcCommandCallback>(this.callbacks.size() + 1);
        result.addAll(this.callbacks);
        result.add(callback);
        return new IrcCommandCallbackList(Collections.unmodifiableList(result));
    }

    /**
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<IrcCommandCallback> iterator() {
        return callbacks.iterator();
    }

    public IrcCommandCallbackList remove(IrcCommandCallback callback) {
        ArrayList<IrcCommandCallback> result = new ArrayList<IrcCommandCallback>(this.callbacks.size() + 1);
        result.addAll(this.callbacks);
        result.remove(callback);
        return new IrcCommandCallbackList(Collections.unmodifiableList(result));
    }

    /**
     * @return
     */
    public static IrcCommandCallbackList empty() {
        return EMPTY;
    }

}
