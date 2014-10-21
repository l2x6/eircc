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
public enum IrcNotificationLevel {
    /* Keep the items a ordered by level */
    NO_NOTIFICATION(0, false), UNREAD_MESSAGES(1, true), UNREAD_MESSAGES_FROM_A_TRACKED_USER(2, true), ME_NAMED(3, true);
    private final boolean hasUnreadMessages;
    private final int level;

    static {
        for (IrcNotificationLevel value : values()) {
            if (value.ordinal() != value.getLevel()) {
                throw new AssertionError(value.getClass().getSimpleName() +"."+ value.name() +" has a broken ordering.");
            }
        }
    }

    private IrcNotificationLevel(int level, boolean hasUnreadMessages) {
        this.level = level;
        this.hasUnreadMessages = hasUnreadMessages;
    }

    public int getLevel() {
        return level;
    }

    public boolean hasUnreadMessages() {
        return hasUnreadMessages;
    }

}