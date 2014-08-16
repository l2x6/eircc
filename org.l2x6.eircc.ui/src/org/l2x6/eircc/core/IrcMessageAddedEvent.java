/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core;

import org.l2x6.eircc.core.model.IrcChannel;
import org.l2x6.eircc.ui.IrcMessage;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcMessageAddedEvent extends IrcModelEvent {

    private final IrcMessage message;

    /**
     * @param channel
     * @param eventType
     * @param message
     */
    public IrcMessageAddedEvent(IrcChannel channel, IrcMessage message) {
        super(EventType.NEW_MESSAGE, channel);
        this.message = message;
    }

    public IrcChannel getChannel() {
        return (IrcChannel) modelObject;
    }

    public IrcMessage getMessage() {
        return message;
    }

}
