/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.l2x6.eircc.core.model.event.IrcModelEvent;
import org.l2x6.eircc.core.model.event.IrcModelEvent.EventType;
import org.l2x6.eircc.core.model.resource.IrcChannelResource;
import org.l2x6.eircc.core.model.resource.IrcLogResource;
import org.l2x6.eircc.core.util.TypedField;
import org.l2x6.eircc.ui.misc.Colors;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public abstract class AbstractIrcChannel extends IrcObject implements PersistentIrcObject {

    public enum IrcChannelField implements TypedField {
        autoJoin;
        private final org.l2x6.eircc.core.util.TypedField.TypedFieldData typedFieldData;

        /**
         * @param typedFieldData
         */
        private IrcChannelField() {
            this.typedFieldData = new TypedFieldData(name(), AbstractIrcChannel.class);
        }

        @Override
        public TypedFieldData getTypedFieldData() {
            return typedFieldData;
        }
    }

    protected final IrcAccount account;
    private boolean autoJoin = true;
    private boolean joined;
    protected boolean kept;
    private final Map<String, Integer> seenUsers = new HashMap<String, Integer>();

    /** Users by nick */
    private final Map<String, IrcChannelUser> users = new TreeMap<String, IrcChannelUser>();

    protected IrcChannelUser[] usersArray;

    /**
     * @param account
     */
    public AbstractIrcChannel(IrcAccount account) {
        super(account.getModel(), account.getAccountResource().getChannelsFolder().getFullPath());
        this.account = account;
        this.seenUsers.put(account.getAcceptedNick(), Colors.MY_INDEX);
    }
    /**
     * @param oldNick
     * @param newUser
     */
    public void changeNick(String oldNick, String newNick) {
        Integer i = seenUsers.get(oldNick);
        seenUsers.put(newNick, i != null ? i : Integer.valueOf(seenUsers.size()));
        IrcChannelUser channelUser = users.remove(oldNick);
        users.put(newNick, channelUser);
        usersArray = null;
        account.getModel().fire(new IrcModelEvent(EventType.CHANNEL_USERS_CHANGED, this));
    }

    /**
     * @param user
     * @return
     */
    IrcChannelUser addUserInternal(IrcUser user, IrcUserFlags flags) {
        String nick = user.getNick();
        if (seenUsers.get(nick) == null) {
            seenUsers.put(nick, seenUsers.size());
        }
        IrcChannelUser channelUser = createUser(user, flags);
        users.put(nick, channelUser);
        usersArray = null;
        return channelUser;
    }

    public void addUser(IrcUser user, IrcUserFlags flags) {
        IrcChannelUser channelUser = addUserInternal(user, flags);
        account.getModel().fire(new IrcModelEvent(EventType.CHANNEL_USER_JOINED, channelUser));
    }

    /**
     * @return
     */
    public IrcChannelUser createUser(IrcUser user, IrcUserFlags flags) {
        return new IrcChannelUser(this, user, flags);
    }

    @Override
    public void dispose() {
    }

    public IrcLog findLog(IrcLogResource logResource) {
        IrcLog log = getLog();
        if (log != null && logResource.equals(log.getLogResource())) {
            return log;
        }
        return null;
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#getAccount()
     */
    public IrcAccount getAccount() {
        return account;
    }

    public abstract IrcChannelResource getChannelResource();

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#getFields()
     */
    @Override
    public IrcChannelField[] getFields() {
        return IrcChannelField.values();
    }

    public abstract IrcLog getLog();

    /**
     * @return
     */
    public IPath getLogsFolderPath() {
        return getChannelResource().getLogsFolder().getFullPath();
    }

    /**
     * @return
     */
    public abstract String getName();

    @Override
    public IPath getPath() {
        return getChannelResource().getChannelPropertyFile().getFullPath();
    }

    /**
     * @param user
     * @return
     */
    public int getUserIndex(IrcUser user) {
        if (user == getAccount().getMe()) {
            return Colors.MY_INDEX;
        } else {
            String nick = user.getNick();
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

    /**
     * @return
     * @throws CoreException
     */
    public Collection<IFile> listSearchableLogFiles() throws CoreException {
        List<IFile> result = new ArrayList<IFile>();
        IPath logsDir = getLogsFolderPath();
        IFolder logsFolder = model.getRoot().getFolder(logsDir);
        for (IResource r : logsFolder.members()) {
            if (IrcLogResource.isLogFile(r)) {
                result.add((IFile) r);
            }
        }
        return result;
    }

    public void removeUser(String nick, String leftWithMessage) {
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

    /**
     * @param users
     */
    public void setUsers(IrcWhoUser[] users) {
        IrcServer server = account.getServer();
        for (IrcWhoUser u : users) {
            IrcUser user = server.getOrCreateUser(u.getNick(), u.getUsername(), u.getHost());
            addUserInternal(user, u.getFlags());
        }
        account.getModel().fire(new IrcModelEvent(EventType.CHANNEL_USERS_CHANGED, this));
    }

}
