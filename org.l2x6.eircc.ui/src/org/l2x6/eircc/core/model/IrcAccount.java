/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.l2x6.eircc.core.IrcException;
import org.l2x6.eircc.core.model.PlainIrcMessage.IrcMessageType;
import org.l2x6.eircc.core.model.event.IrcModelEvent;
import org.l2x6.eircc.core.model.event.IrcModelEvent.EventType;
import org.l2x6.eircc.core.model.resource.IrcAccountResource;
import org.l2x6.eircc.core.model.resource.IrcChannelResource;
import org.l2x6.eircc.core.model.resource.IrcLogResource;
import org.l2x6.eircc.core.model.resource.IrcResourceException;
import org.l2x6.eircc.core.util.TypedField;
import org.l2x6.eircc.ui.IrcUiMessages;
import org.schwering.irc.lib.IRCExceptionHandler;
import org.schwering.irc.lib.IRCTrafficLogger;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcAccount extends InitialIrcAccount implements PersistentIrcObject {
    public enum IrcAccountField implements TypedField {
        autoConnect(IrcUiMessages.Account_Connect_Automatically) {
            @Override
            public Object fromString(String value) {
                return Boolean.valueOf(value);
            }
        }, //
        createdOn(IrcUiMessages.Account_Created_on) {
            @Override
            public Object fromString(String value) {
                return Long.valueOf(value);
            }
        }, //
        host(IrcUiMessages.Account_Host), //
        name(IrcUiMessages.Account_Name), //
        password(IrcUiMessages.Account_Password), //
        port(IrcUiMessages.Account_Port) {
            @Override
            public Object fromString(String value) {
                return Integer.valueOf(value);
            }
        }, //
        preferedNick(IrcUiMessages.Account_Nick), //
        socksProxyHost(IrcUiMessages.Account_SOCKS_Proxy_Host), //
        socksProxyPort(IrcUiMessages.Account_SOCKS_Proxy_Port) {
            @Override
            public Object fromString(String value) {
                return value == null || value.isEmpty() ? null : Integer.valueOf(value);
            }
        }, //
        ssl(IrcUiMessages.Account_Use_SSL) {
            @Override
            public Object fromString(String value) {
                return Boolean.valueOf(value);
            }
        }, //
        username(IrcUiMessages.Account_Username)//
        ;//
        private final String label_;
        private final TypedFieldData typedFieldData;

        /**
         * @param label_
         */
        private IrcAccountField(String label) {
            this.label_ = label;
            this.typedFieldData = new TypedFieldData(name(), IrcAccount.class);
        }

        public Object fromString(String value) {
            return value;
        }

        public String getLabel() {
            return label_;
        }

        /**
         * @see org.l2x6.eircc.core.util.TypedField#getTypedFieldData()
         */
        @Override
        public TypedFieldData getTypedFieldData() {
            return typedFieldData;
        }
    }

    public enum IrcAccountState {
        OFFLINE, OFFLINE_AFTER_ERROR, ONLINE
    }

    private static class NickTimeoutMessageReplacer implements IrcMessageReplacer {
        private boolean depleted = false;
        private final String expectedText;

        /**
         * @param expectedMessage
         */
        public NickTimeoutMessageReplacer(String expectedMessage) {
            super();
            this.expectedText = expectedMessage;
        }

        /**
         * @see org.l2x6.eircc.core.model.IrcMessageReplacer#createNewMessage(org.l2x6.eircc.core.model.IrcLog)
         */
        @Override
        public IrcMessage createNewMessage(IrcLog log) {
            return new IrcMessage(log, OffsetDateTime.now(), null, expectedText, log.getChannel().isP2p(),
                    IrcMessageType.NOTIFICATION);
        }

        /**
         * @see org.l2x6.eircc.core.model.IrcMessageReplacer#createReplacementMessage(org.l2x6.eircc.core.model.IrcMessage)
         */
        @Override
        public IrcMessage createReplacementMessage(IrcMessage replacedMessage) {
            return new IrcMessage(replacedMessage.getLog(), OffsetDateTime.now(), replacedMessage.getUser(),
                    replacedMessage.getText(), replacedMessage.getMyNick(), replacedMessage.getLog().getChannel()
                            .isP2p(), replacedMessage.getType(), replacedMessage.getRawInput());
        }

        @Override
        public IrcMessageMatcherState match(IrcMessage m) {
            if (depleted) {
                return IrcMessageMatcherState.STOP;
            }
            if (m.getType() == IrcMessageType.NOTIFICATION && expectedText.equals(m.getText())) {
                depleted = true;
                return IrcMessageMatcherState.MATCH;
            }
            return IrcMessageMatcherState.STOP;
        }

    }

    private final IrcAccountResource accountResource;
    /** Kept or joined channels */
    private final List<AbstractIrcChannel> channels = new ArrayList<AbstractIrcChannel>();

    private long createdOn;

    private IRCExceptionHandler exceptionHandler;

    private AbstractIrcChannel[] keptChannelsArray;
    private IrcException lastException;

    private IrcUser me;
    private final IrcServer server;

    private IrcAccountState state = IrcAccountState.OFFLINE;
    private IRCTrafficLogger trafficLogger;

    public IrcAccount(InitialIrcAccount src) throws IrcResourceException {
        this(src.getModel(), src.getLabel(), System.currentTimeMillis());
        this.autoConnect = src.autoConnect;
        this.host = src.host;
        this.password = src.password;
        this.port = src.port;
        this.preferedNick = src.preferedNick;
        this.realName = src.realName;
        this.ssl = src.ssl;
        this.username = src.username;
        this.socksProxyHost = src.socksProxyHost;
        this.socksProxyPort = src.socksProxyPort;
    }

    /**
     * @param model
     * @param f
     * @param monitor
     * @throws IOException
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     * @throws CoreException
     * @throws IrcResourceException
     */
    public IrcAccount(IrcModel model, IFile f) throws UnsupportedEncodingException, FileNotFoundException, IOException,
            CoreException, IrcResourceException {
        super(model, IrcAccountResource.getAccountName(f));
        this.accountResource = model.getRootResource().getOrCreateAccountResource(getLabel());
        this.server = new IrcServer(this);
        load(f);

        IWorkspaceRoot root = model.getRoot();
        IPath channelsFolderPath = accountResource.getChannelsFolder().getFullPath();
        if (root.exists(channelsFolderPath)) {
            IFolder channelsFolder = root.getFolder(channelsFolderPath);
            for (IResource m : channelsFolder.members()) {
                if (IrcChannelResource.isChannelFile(m) && m.exists()) {
                    IrcChannel channel = new IrcChannel(this, (IFile) m);
                    channels.add(channel);
                }
            }
        }
        IPath usersFolderPath = accountResource.getUsersFolder().getFullPath();
        if (root.exists(usersFolderPath)) {
            IFolder usersFolder = root.getFolder(usersFolderPath);
            for (IResource m : usersFolder.members()) {
                if (IrcUser.isUserFile(m)) {
                    IrcUser user = new IrcUser(server, (IFile) m);
                    server.getUsers().put(user.getId(), user);
                }
            }
        }
    }

    /**
     * @param model
     * @param label
     * @throws IrcResourceException
     */
    public IrcAccount(IrcModel model, String label, long createdOn) throws IrcResourceException {
        super(model, label);
        this.accountResource = model.getRootResource().getOrCreateAccountResource(label);
        this.createdOn = createdOn;
        this.server = new IrcServer(this);
    }

    public void addChannel(AbstractIrcChannel channel) {
        if (channel.getAccount() != this) {
            throw new IllegalArgumentException("Cannot add channel with parent distinct from this "
                    + this.getClass().getSimpleName());
        }
        String channelName = channel.getName();
        AbstractIrcChannel availableChannel = channel.isP2p() ? findP2pChannel(((P2pIrcChannel) channel).getP2pUser())
                : findOwnChannel(channelName);
        if (availableChannel != null) {
            throw new IllegalArgumentException("Channel with name '" + channelName
                    + "' already available under account '" + this.getLabel() + "'");
        }
        channels.add(channel);
        keptChannelsArray = null;
        model.fire(new IrcModelEvent(EventType.ACCOUNT_CHANNEL_ADDED, channel));
    }

    public IrcChannel createChannel(String name) throws IrcResourceException {
        return new IrcChannel(this, name);
    }

    public P2pIrcChannel createP2pChannel(IrcUser p2pUser) throws IrcResourceException {
        return new P2pIrcChannel(p2pUser);
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#dispose()
     */
    @Override
    public void dispose() {
        for (AbstractIrcChannel channel : channels) {
            channel.dispose();
        }
        keptChannelsArray = null;
        super.dispose();
    }

    /**
     * @param result
     */
    public void ensureChannelListed(AbstractIrcChannel channel) {
        String channelName = channel.getName();
        AbstractIrcChannel availableChannel = channel.isP2p() ? findP2pChannel(((P2pIrcChannel) channel).getP2pUser())
                : findOwnChannel(channelName);
        if (availableChannel == channel) {
            return;
        } else if (availableChannel != null) {
            throw new IllegalArgumentException("A distinct channel instance with the same name '" + channelName
                    + "' already available under account '" + this.getLabel() + "'");
        }
        addChannel(channel);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IrcAccount other = (IrcAccount) obj;
        String label = getLabel();
        if (label == null) {
            if (other.getLabel() != null)
                return false;
        } else if (!label.equals(other.getLabel()))
            return false;
        return true;
    }

    public IrcChannel findChannel(String channelName) {
        for (AbstractIrcChannel channel : channels) {
            if (channel instanceof IrcChannel && channel.getName().equals(channelName)) {
                return (IrcChannel) channel;
            }
        }
        return null;
    }

    private IrcChannel findOwnChannel(String channelName) {
        for (AbstractIrcChannel channel : channels) {
            if (channel instanceof IrcChannel && channel.getName().equals(channelName)) {
                return (IrcChannel) channel;
            }
        }
        return null;
    }

    public P2pIrcChannel findP2pChannel(IrcUser p2pUser) {
        for (AbstractIrcChannel channel : channels) {
            if (channel instanceof P2pIrcChannel && ((P2pIrcChannel) channel).getP2pUser().equals(p2pUser)) {
                return (P2pIrcChannel) channel;
            }
        }
        return null;
    }

    public void fireNickTimeoutNotification() {
        getModel().fire(new IrcModelEvent(EventType.NICK_TIMEOUT, this));
        String myNick = getAcceptedNick();
        String text = MessageFormat.format(IrcUiMessages.Message_Still_nick, myNick);
        for (AbstractIrcChannel channel : channels) {
            IrcLog log = channel.getLog();
            NickTimeoutMessageReplacer replacer = new NickTimeoutMessageReplacer(text);
            log.replaceOrAppendMessage(replacer, true);
        }

    }

    public String getAcceptedNick() {
        return me != null ? me.getNick() : null;
    }

    public IrcAccountResource getAccountResource() {
        return accountResource;
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#getChannels()
     */
    public AbstractIrcChannel[] getChannels() {
        if (keptChannelsArray == null) {
            keptChannelsArray = channels.toArray(new AbstractIrcChannel[channels.size()]);
        }
        return keptChannelsArray;
    }

    public long getCreatedOn() {
        return createdOn;
    }

    public IRCExceptionHandler getExceptionHandler() {
        if (exceptionHandler == null) {
            exceptionHandler = model.createExceptionHandler(this);
        }
        return exceptionHandler;
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#getFields()
     */
    @Override
    public IrcAccountField[] getFields() {
        return IrcAccountField.values();
    }

    public IrcException getLastException() {
        return lastException;
    }

    /**
     * @return
     */
    public IrcUser getMe() {
        return me;
    }

    /**
     * @return
     */
    public IrcModel getModel() {
        return model;
    }

    /**
     * @param logResource
     * @return
     * @throws IrcResourceException
     */
    public AbstractIrcChannel getOrCreateChannel(IrcLogResource logResource) throws IrcResourceException {
        for (AbstractIrcChannel channel : channels) {
            IrcChannelResource channelResource = channel.getChannelResource();
            if (channelResource.getLogResource(logResource.getTime()) == logResource) {
                return channel;
            }
        }
        IrcChannelResource channelResource = logResource.getChannelResource();
        final AbstractIrcChannel result;
        if (channelResource.isP2p()) {
            IrcUser user = server.findUser(channelResource.getChannelName());
            result = createP2pChannel(user);
            addChannel(result);
        } else {
            result = createChannel(channelResource.getChannelName());
            ensureChannelListed(result);
        }
        return result;
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#getPath()
     */
    @Override
    public IPath getPath() {
        return accountResource.getAccountPropertyFile().getFullPath();
    }

    /**
     * @return
     */
    public String getQuitMessage() {
        return IrcUiMessages.Account_Quit_Message;
    }

    /**
     * @return
     */
    public Collection<AbstractIrcChannel> getSearchableChannels() {
        return new ArrayList<AbstractIrcChannel>(channels);
    }

    public IrcServer getServer() {
        return server;
    }

    public IrcAccountState getState() {
        return state;
    }

    /**
     * @return
     */
    public IRCTrafficLogger getTraffciLogger() {
        if (trafficLogger == null) {
            trafficLogger = model.createTrafficLogger(this);
        }
        return trafficLogger;
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#hasAccounts()
     */
    public boolean hasChannels() {
        return !channels.isEmpty();
    }

    @Override
    public int hashCode() {
        String label = getLabel();
        return label == null ? 0 : label.hashCode();
    }

    public void removeChannel(AbstractIrcChannel channel) {
        channels.remove(channel);
        keptChannelsArray = null;
        model.fire(new IrcModelEvent(EventType.ACCOUNT_CHANNEL_REMOVED, channel));
    }

    @Override
    public void save(IProgressMonitor monitor) throws CoreException {
        PersistentIrcObject.super.save(monitor);
        if (!channels.isEmpty()) {
            for (AbstractIrcChannel channel : channels) {
                if (channel.isKept()) {
                    channel.save(monitor);
                }
            }
        }
        Map<UUID, IrcUser> users = server.getUsers();
        if (!users.isEmpty()) {
            for (IrcUser user : users.values()) {
                user.save(monitor);
            }
        }
    }

    public void setCreatedOn(long createdOn) {
        this.createdOn = createdOn;
    }

    @Override
    public void setLabel(String label) {
        throw new UnsupportedOperationException("label is immutable in " + this.getClass().getName());
    }

    /**
     * @param me
     */
    public void setMe(IrcUser me) {
        this.me = me;
        for (AbstractIrcChannel channel : channels) {
            if (channel.isP2p() && !channel.isPresent(me.getNick())) {
                channel.addUser(me, IrcUserFlags.EMPTY);
            }
        }
    }

    public void setOffline(IrcException e) {
        this.lastException = e;
        setState(IrcAccountState.OFFLINE_AFTER_ERROR);
    }

    public void setState(IrcAccountState state) {
        IrcAccountState oldState = this.state;
        this.state = state;
        if (state != IrcAccountState.OFFLINE_AFTER_ERROR) {
            this.lastException = null;
        }
        if (oldState != state) {
            model.fire(new IrcModelEvent(EventType.ACCOUNT_STATE_CHANGED, this));

            /* and leave all channels if necessary */
            switch (state) {
            case OFFLINE:
            case OFFLINE_AFTER_ERROR:
                for (AbstractIrcChannel channel : channels) {
                    channel.setJoined(false);
                }
                break;
            default:
                break;
            }
        }
    }

}
