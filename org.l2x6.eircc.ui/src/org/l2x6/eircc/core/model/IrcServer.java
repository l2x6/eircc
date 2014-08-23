/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.l2x6.eircc.core.model.event.IrcModelEvent;
import org.l2x6.eircc.core.model.event.IrcModelEvent.EventType;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcServer extends IrcObject {
    public enum IrcServerField {
    };

    private final IrcAccount account;
    private final Map<String, IrcChannel> channels = new TreeMap<String, IrcChannel>();
    private AbstractIrcChannel[] channelsArray;
    /** Users by nick */
    private final Map<UUID, IrcUser> users = new TreeMap<UUID, IrcUser>();

    /**
     * @param id
     * @param account
     */
    public IrcServer(IrcAccount account) {
        this.account = account;
    }

    public void addChannel(IrcChannel channel) {
        addChannel(channel, true);
    }

    public void addChannel(IrcChannel channel, boolean fireChannelAddedEvent) {
        if (channel.getAccount().getServer() != this) {
            throw new IllegalArgumentException("Cannot add channel with parent distinct from this "
                    + account.getClass().getSimpleName());
        }
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

    public void addChannels(IrcChannel[] addChannels) {
        for (IrcChannel channel : addChannels) {
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
    public void changeNick(String oldNick, String newNick, String username) {
        IrcUser user = findUser(oldNick);
        if (user != null) {
            user.setNick(newNick);
        } else {
            user = createUser(newNick, username);
            addUser(user);
        }
    }

    /**
     * @param nick
     * @param username
     * @param realName
     * @return
     */
    public IrcUser createUser(String nick, String username) {
        return new IrcUser(this, UUID.randomUUID(), nick, username);
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#dispose()
     */
    @Override
    public void dispose() {
    }

    /**
     * If you feel you need thi to be {@code public} you probably need to call
     * {@link IrcAccount#findChannel(String)}.
     *
     * @param channelName
     * @return
     */
    IrcChannel findChannel(String channelName) {
        return channels.get(channelName);
    }

    /**
     * Find user by nick.
     *
     * @param nick
     * @return
     */
    public IrcUser findUser(String nick) {
        return users.values().stream().findFirst().filter(user -> user.getNick().equals(nick)).orElse(null);
    }

    public IrcUser findUser(UUID id) {
        return users.get(id);
    }

    public IrcAccount getAccount() {
        return account;
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#getChannels()
     */
    public AbstractIrcChannel[] getChannels() {
        if (channelsArray == null) {
            Collection<IrcChannel> chans = channels.values();
            channelsArray = chans.toArray(new IrcChannel[chans.size()]);
        }
        return channelsArray;
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#getFields()
     */
    @Override
    public IrcServerField[] getFields() {
        return IrcServerField.values();
    }

    @Override
    protected File getSaveFile(File parentDir) {
        return new File(parentDir, account.getId().toString() + "-" + account.getLabel() + ".server.properties");
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
        IrcUser removed = users.remove(nick);
        if (removed != null) {
            account.getModel().fire(new IrcModelEvent(EventType.USER_REMOVED, removed));
            removed.dispose();
        }
    }

    /**
     * @return
     * @return
     */
    Map<UUID, IrcUser> getUsers() {
        return users;
    }

}
