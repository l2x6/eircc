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
public class IrcAccountsStatistics {
    private final int channelsNamingMe;
    private final int channelsOffline;
    private final int channelsOfflineAfterError;
    private final int channelsOnline;
    private final int channelsWithUnreadMessages;

    /**
     * @param channelsOnline
     * @param channelsOffline
     * @param channelsWithUnseenMessages
     * @param channelsOfflineAfterError
     */
    public IrcAccountsStatistics(int channelsOnline, int channelsOffline, int channelsWithUnseenMessages,
            int channelsNamingMe, int channelsOfflineAfterError) {
        super();
        this.channelsOnline = channelsOnline;
        this.channelsOffline = channelsOffline;
        this.channelsWithUnreadMessages = channelsWithUnseenMessages;
        this.channelsNamingMe = channelsNamingMe;
        this.channelsOfflineAfterError = channelsOfflineAfterError;
    }

    public int getChannelsNamingMe() {
        return channelsNamingMe;
    }

    public int getChannelsOffline() {
        return channelsOffline;
    }

    public int getChannelsOfflineAfterError() {
        return channelsOfflineAfterError;
    }

    public int getChannelsOnline() {
        return channelsOnline;
    }

    public int getChannelsWithUnreadMessages() {
        return channelsWithUnreadMessages;
    }

    public boolean hasChannelsNamingMe() {
        return channelsNamingMe > 0;
    }

    public boolean hasChannelsOffline() {
        return channelsOffline > 0;
    }

    public boolean hasChannelsOfflineAfterError() {
        return channelsOfflineAfterError > 0;
    }

    public boolean hasChannelsOnline() {
        return channelsOnline > 0;
    }

    public boolean hasChannelsWithUnreadMessages() {
        return channelsWithUnreadMessages > 0;
    }

}
