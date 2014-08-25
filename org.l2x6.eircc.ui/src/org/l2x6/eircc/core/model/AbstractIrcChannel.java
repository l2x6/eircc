/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.l2x6.eircc.core.model.event.IrcModelEvent;
import org.l2x6.eircc.core.model.event.IrcModelEvent.EventType;
import org.l2x6.eircc.ui.misc.Colors;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public abstract class AbstractIrcChannel extends IrcObject {

    public enum IrcChannelField implements TypedField {
        autoJoin {
            @Override
            public Object fromString(String value) {
                return Boolean.valueOf(value);
            }
        };
        public Object fromString(String value) {
            return value;
        }
    }

    /**  */
    protected static final String FILE_EXTENSION = ".channel.properties";

    /**
     * @param f
     * @return
     */
    public static boolean isChannelFile(File f) {
        return f.isFile() && f.getName().endsWith(AbstractIrcChannel.FILE_EXTENSION);
    }

    protected final IrcAccount account;
    private boolean autoJoin = true;
    private boolean joined;
    protected boolean kept;
    private IrcLog log;
    private File logsDirectory;
    private final Map<String, Integer> seenUsers = new HashMap<String, Integer>();
    /** Users by nick */
    protected final Map<String, IrcChannelUser> users = new TreeMap<String, IrcChannelUser>();
    protected IrcChannelUser[] usersArray;

    /**
     * @param account
     */
    public AbstractIrcChannel(IrcAccount account) {
        super(account.getChannelsDirectory());
        this.account = account;
        this.seenUsers.put(account.getAcceptedNick(), Colors.MY_INDEX);
    }

    /**
     * @param result
     */
    public void addNick(String nick) {
        if (seenUsers.get(nick) == null) {
            seenUsers.put(nick, seenUsers.size());
        }
        addNickInternal(nick);
        account.getModel().fire(new IrcModelEvent(EventType.CHANNEL_USER_JOINED, new IrcChannelUser(this, nick)));
    }

    /**
     * @param user
     */
    void addNickInternal(String nick) {
        // if (user.getServer() != account.getServer()) {
        // throw new
        // IllegalArgumentException("Cannot add user with parent distinct from this "
        // + this.getClass().getSimpleName());
        // }
        // String nick = user.getNick();
        // if (nicks.get(nick) != null) {
        // throw new IllegalArgumentException("User with nick '" + nick +
        // "' already available under server '"
        // + account.getHost() + "' of the account '" + account.getLabel() +
        // "'.");
        // }
        users.put(nick, createUser(nick));
        usersArray = null;
    }

    /**
     * @param users2
     */
    public void addNicks(Collection<String> nicks) {
        for (String nick : nicks) {
            addNickInternal(nick);
            if (seenUsers.get(nick) == null) {
                seenUsers.put(nick, seenUsers.size());
            }
        }
        account.getModel().fire(new IrcModelEvent(EventType.CHANNEL_USERS_CHANGED, this));
    }

    /**
     * @param oldNick
     * @param newUser
     */
    public void changeNick(String oldNick, String newNick) {
        Integer i = seenUsers.get(oldNick);
        users.remove(oldNick);
        addNickInternal(newNick);
        seenUsers.put(newNick, i != null ? i : Integer.valueOf(seenUsers.size()));
        account.getModel().fire(new IrcModelEvent(EventType.CHANNEL_USERS_CHANGED, this));
    }

    /**
     * @return
     */
    public IrcChannelUser createUser(String nick) {
        return new IrcChannelUser(this, nick);
    }

    @Override
    public void dispose() {
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#getAccount()
     */
    public IrcAccount getAccount() {
        return account;
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#getFields()
     */
    @Override
    public IrcChannelField[] getFields() {
        return IrcChannelField.values();
    }

    public IrcLog getLog() {
        return log;
    }

    /**
     * @return
     */
    public File getLogsDirectory() {
        if (this.logsDirectory == null) {
            this.logsDirectory = new File(saveDirectory, getName() + "-logs");
        }
        return this.logsDirectory;
    }

    /**
     * @return
     */
    public abstract String getName();

    @Override
    protected File getSaveFile() {
        return new File(saveDirectory, getName() + FILE_EXTENSION);
    }

    /**
     * @param user
     * @return
     */
    public int getUserIndex(String nick) {
        if (nick.equals(getAccount().getAcceptedNick())) {
            return Colors.MY_INDEX;
        } else {
            Integer result = seenUsers.get(nick);
            if (result == null) {
                result = seenUsers.size();
                seenUsers.put(nick, result);
            }
            return result.intValue();
        }
    }

    /**
     * @return
     */
    public IrcChannelUser[] getUsers() {
        if (usersArray == null) {
            Collection<IrcChannelUser> chans = users.values();
            usersArray = chans.toArray(new IrcChannelUser[chans.size()]);
        }
        return usersArray;
    }

    public boolean isAutoJoin() {
        return autoJoin;
    }

    public boolean isJoined() {
        return joined;
    }

    public boolean isKept() {
        return kept;
    }

    /**
     * @return
     */
    public abstract boolean isP2p();

    /**
     * @param user
     * @return
     */
    public boolean isPresent(String nick) {
        return users.containsKey(nick);
    }

    public void removeNick(String nick, String leftWithMessage) {
        IrcChannelUser removed = users.remove(nick);
        if (removed != null) {
            removed.setLeftWithMessage(leftWithMessage);
            account.getModel().fire(new IrcModelEvent(EventType.CHANNEL_USER_LEFT, removed));
        }
    }

    public void setAutoJoin(boolean autoJoin) {
        this.autoJoin = autoJoin;
    }

    public void setJoined(boolean joined) {
        boolean oldState = this.joined;
        this.joined = joined;
        if (oldState != joined) {
            if (joined && log == null) {
                log = new IrcLog(this, OffsetDateTime.now());
            }
            account.getModel().fire(new IrcModelEvent(EventType.CHANNEL_JOINED_CHANGED, this));
        }
    }

    public void setKept(boolean kept) {
        this.kept = kept;
    }

    @Override
    public String toString() {
        return getName();
    }

}
