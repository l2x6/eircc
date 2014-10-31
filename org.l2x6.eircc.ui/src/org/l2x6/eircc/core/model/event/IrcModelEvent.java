/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model.event;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcModelEvent {
    public enum EventType {
        ACCOUNT_ADDED, ACCOUNT_CHANNEL_ADDED, ACCOUNT_CHANNEL_REMOVED, ACCOUNT_REMOVED, ACCOUNT_STATE_CHANGED, CHANNEL_JOINED_CHANGED, CHANNEL_USER_JOINED, CHANNEL_USER_LEFT, LOG_STATE_CHANGED, NEW_MESSAGE, NICK_CHANGED, SERVER_CHANNEL_ADDED, SERVER_CHANNEL_REMOVED, SERVER_CHANNELS_ADDED, USER_ADDED, USER_REMOVED, CHANNEL_USERS_CHANGED
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
