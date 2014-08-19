/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.io.File;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.l2x6.eircc.core.IrcModelEvent;
import org.l2x6.eircc.core.IrcModelEvent.EventType;
import org.l2x6.eircc.ui.IrcUiMessages;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcServer extends IrcObject {
    public enum IrcServerField {
    };

    private final IrcAccount account;
    private final Map<String, IrcChannel> channels = new TreeMap<String, IrcChannel>();
    private IrcChannel[] channelsArray;
    /** Users by nick */
    private final Map<String, IrcUser> users = new TreeMap<String, IrcUser>();

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
        String nick = user.getNick();
        if (users.get(nick) != null) {
            throw new IllegalArgumentException("User with nick '" + nick + "' already available under server '"
                    + account.getHost() + "' of the account '" + account.getLabel() + "'.");
        }
        users.put(nick, user);
        account.getModel().fire(new IrcModelEvent(EventType.USER_ADDED, user));
    }

    /**
     * @param oldNick
     * @param newNick
     * @param username2
     */
    public void changeNick(String oldNick, String newNick, String username) {
        removeUser(oldNick);
        IrcUser newUser = createUser(newNick, username);
        addUser(newUser);

        String text = MessageFormat.format(IrcUiMessages.Message_x_is_known_as_y, oldNick, newNick);
        long now = System.currentTimeMillis();

        for (IrcChannel channel : account.getChannels()) {
            if (channel.isJoined() && channel.isPresent(oldNick)) {
                channel.changeNick(oldNick, newUser);
                IrcLog log = channel.getLog();
                IrcMessage m = new IrcMessage(log, now, null, text);
                log.appendMessage(m);
            }
        }
    }

    /**
     * @param nick
     * @param username
     * @param realName
     * @return
     */
    public IrcUser createUser(String nick, String username) {
        return new IrcUser(this, nick, username);
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
        return users.get(nick);
    }

    public IrcAccount getAccount() {
        return account;
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#getChannels()
     */
    public IrcChannel[] getChannels() {
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

    public void removeChannel(IrcChannel channel) {
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

}
