/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core;


/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcModelEvent {
    public enum EventType {
        KEPT_CHANNEL_ADDED, KEPT_CHANNEL_REMOVED, ACCOUNT_ADDED, ACCOUNT_REMOVED, ACCOUNT_STATE_CHANGED, CHANNEL_JOINED_CHANGED, USER_ADDED, USER_REMOVED, SERVER_CHANNEL_ADDED, SERVER_CHANNELS_ADDED, SERVER_CHANNEL_REMOVED, NEW_MESSAGE
    };

    protected final EventType eventType;
    protected final Object modelObject;

    /**
     * @param eventType
     * @param model
     */
    public IrcModelEvent(EventType eventType, Object modelObject) {
        super();
        this.modelObject = modelObject;
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Object getModelObject() {
        return modelObject;
    }

}
