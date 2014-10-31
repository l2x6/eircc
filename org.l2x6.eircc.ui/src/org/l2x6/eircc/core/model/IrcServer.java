/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import org.l2x6.eircc.core.model.event.IrcModelEvent;
import org.l2x6.eircc.core.model.event.IrcModelEvent.EventType;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcServer extends IrcObject {

    private final IrcAccount account;
    private final SortedMap<String, PlainIrcChannel> channels = new TreeMap<String, PlainIrcChannel>();
    private PlainIrcChannel[] channelsArray;
    /** Users by id */
    private final Map<UUID, IrcUser> users = new TreeMap<UUID, IrcUser>();

    /**
     * @param id
     * @param account
     */
    public IrcServer(IrcAccount account) {
        super(account.getModel(), account.getParentFolderPath());
        this.account = account;
    }

    public void addChannel(PlainIrcChannel channel, boolean fireChannelAddedEvent) {
        String channelName = channel.getName();
        if (channels.get(channelName) != null) {
            throw new IllegalArgumentException("Channel with name '" + channelName
                    + "' already available under server '" + account.getHost() + "' account ' of account "
                    + account.getLabel() + "'");
        }
        channels.put(channelName, channel);
        channelsArray = null;
        if (fireChannelAddedEvent) {
            account.getModel().fire(new IrcModelEvent(EventType.SERVER_CHANNEL_ADDED, channel));
        }
    }

    public void addChannels(PlainIrcChannel[] addChannels) {
        for (PlainIrcChannel channel : addChannels) {
            addChannel(channel, false);
        }
        account.getModel().fire(new IrcModelEvent(EventType.SERVER_CHANNELS_ADDED, addChannels));
    }

    /**
     * @param result
     */
    public void addUser(IrcUser user) {
        if (user.getServer() != this) {
            throw new IllegalArgumentException("Cannot add user with parent distinct from this "
                    + this.getClass().getSimpleName());
        }
        UUID id = user.getId();
        if (users.get(id) != null) {
            throw new IllegalArgumentException("User with ID '" + id + "' already available under server '"
                    + account.getHost() + "' of the account '" + account.getLabel() + "'.");
        }
        users.put(id, user);
        account.getModel().fire(new IrcModelEvent(EventType.USER_ADDED, user));
    }

    /**
     * @param oldNick
     * @param newNick
     * @param username2
     */
    public void changeNick(IrcUserBase plainUser, String newNick) {
        IrcUser user = findUser(plainUser.getNick());
        if (user != null) {
            user.setNick(newNick);
        } else {
            user = createUser(newNick, plainUser.getUsername(), plainUser.getHost());
            addUser(user);
        }
    }

    /**
     * @param channelName
     * @return
     */
    public PlainIrcChannel createChannel(String channelName) {
        return new PlainIrcChannel(this, channelName);
    }

    /**
     * @param nick
     * @param username
     * @param host
     * @param realName
     * @return
     */
    public IrcUser createUser(String nick, String username, String host) {
        return new IrcUser(this, UUID.randomUUID(), nick, username, host);
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#dispose()
     */
    @Override
    public void dispose() {
    }

    /**
     * Find user by nick.
     *
     * @param nick
     * @return
     */
    public IrcUser findUser(String nick) {
        for (IrcUser user : users.values()) {
            if (user.getNick().equals(nick)) {
                return user;
            }
        }
        return null;
    }

    public IrcUser findUser(UUID id) {
        return users.get(id);
    }

    public IrcAccount getAccount() {
        return account;
    }

    public PlainIrcChannel[] getChannels() {
        if (channelsArray == null) {
            Collection<PlainIrcChannel> chans = channels.values();
            channelsArray = chans.toArray(new PlainIrcChannel[chans.size()]);
        }
        return channelsArray;
    }

    /**
     * @param nick
     * @param username
     * @param host
     * @return
     */
    public IrcUser getOrCreateUser(String nick, String username, String host) {
        IrcUser result = findUser(nick);
        if (result == null) {
            result = createUser(nick, username, host);
            addUser(result);
        }
        return result;
    }

    /**
     * @return
     * @return
     */
    Map<UUID, IrcUser> getUsers() {
        return users;
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#hasAccounts()
     */
    public boolean hasChannels() {
        return !channels.isEmpty();
    }

    public void removeChannel(AbstractIrcChannel channel) {
        channels.remove(channel);
        channelsArray = null;
        account.getModel().fire(new IrcModelEvent(EventType.SERVER_CHANNEL_REMOVED, channel));
    }

    public void removeUser(String nick) {
        IrcUser removed = findUser(nick);
        if (removed != null) {
            users.remove(removed.getId());
            account.getModel().fire(new IrcModelEvent(EventType.USER_REMOVED, removed));
            removed.dispose();
        }
    }

}
