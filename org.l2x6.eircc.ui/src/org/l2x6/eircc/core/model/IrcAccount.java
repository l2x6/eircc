/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.l2x6.eircc.core.IrcException;
import org.l2x6.eircc.core.model.event.IrcModelEvent;
import org.l2x6.eircc.core.model.event.IrcModelEvent.EventType;
import org.l2x6.eircc.ui.IrcUiMessages;
import org.schwering.irc.lib.TrafficLogger;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcAccount extends IrcObject {
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
        label(IrcUiMessages.Account_Label), //
        name(IrcUiMessages.Account_Name), //
        password(IrcUiMessages.Account_Password), //
        port(IrcUiMessages.Account_Port) {
            @Override
            public Object fromString(String value) {
                return Integer.valueOf(value);
            }
        }, //
        preferedNick(IrcUiMessages.Account_Nick), //
        ssl(IrcUiMessages.Account_Use_SSL) {
            @Override
            public Object fromString(String value) {
                return Boolean.valueOf(value);
            }
        }, //
        username(IrcUiMessages.Account_Username)//
        ;//
        private final String label_;

        /**
         * @param label_
         */
        private IrcAccountField(String label) {
            this.label_ = label;
        }

        public Object fromString(String value) {
            return value;
        }

        public String getLabel() {
            return label_;
        }
    }

    public enum IrcAccountState {
        OFFLINE, OFFLINE_AFTER_ERROR, ONLINE
    }

    static final String FILE_EXTENSION = ".account.properties";

    /**
     * @param f
     * @return
     */
    public static boolean isAccountFile(File f) {
        return f.isFile() && f.getName().endsWith(IrcAccount.FILE_EXTENSION);
    }

    private boolean autoConnect = false;
    /** Kept or joined channels */
    private final List<AbstractIrcChannel> channels = new ArrayList<AbstractIrcChannel>();

    private final File channelsDirectory;
    private long createdOn;

    private String host;
    protected final UUID id;
    private AbstractIrcChannel[] keptChannelsArray;
    private String label;
    private IrcException lastException;
    private IrcUser me;
    private final IrcModel model;
    private String password;
    private int port;
    private String preferedNick;
    private String realName;
    private final IrcServer server;
    private boolean ssl;

    private IrcAccountState state = IrcAccountState.OFFLINE;

    private TrafficLogger trafficLogger;

    private String username;

    private final File usersDirectory;

    /**
     * @param model
     * @param f
     * @throws IOException
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public IrcAccount(IrcModel model, File f) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        super(model.getSaveDirectory());
        this.model = model;
        this.server = new IrcServer(this);

        String fileName = f.getName();
        String bareName = fileName.substring(0, fileName.length() - IrcAccount.FILE_EXTENSION.length());
        int minusPos = bareName.lastIndexOf('-');
        if (minusPos >= 0) {
            String uuidString = bareName.substring(0, minusPos);
            this.id = UUID.fromString(uuidString);
            this.channelsDirectory = new File(saveDirectory, id.toString() + "-channels");
            this.usersDirectory = new File(saveDirectory, id.toString() + "-users");
            this.label = bareName.substring(minusPos + 1);
            load(f);

            if (channelsDirectory.exists()) {
                for (File channelPropsFile : channelsDirectory.listFiles()) {
                    if (AbstractIrcChannel.isChannelFile(channelPropsFile)) {
                        IrcChannel channel = new IrcChannel(this, channelPropsFile);
                        channels.add(channel);
                    }
                }
            }
            if (usersDirectory.exists()) {
                for (File userPropsFile : usersDirectory.listFiles()) {
                    if (IrcUser.isUserFile(userPropsFile)) {
                        IrcUser user = new IrcUser(server, userPropsFile);
                        server.getUsers().put(user.getId(), user);
                    }
                }
            }

        } else {
            throw new IllegalStateException(f.getAbsolutePath() + "should contain '-' in the file name.");
        }
    }

    public IrcAccount(IrcModel model, UUID id, String label) {
        this(model, id, label, -1);
    }

    /**
     * @param model
     * @param label
     */
    public IrcAccount(IrcModel model, UUID id, String label, long createdOn) {
        super(model.getSaveDirectory());
        this.channelsDirectory = new File(saveDirectory, id.toString() + "-channels");
        this.usersDirectory = new File(saveDirectory, id.toString() + "-users");
        this.id = id;
        this.model = model;
        this.label = label;
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
                    + "' already available under account '" + this.label + "'");
        }
        channels.add(channel);
        keptChannelsArray = null;
        model.fire(new IrcModelEvent(EventType.ACCOUNT_CHANNEL_ADDED, channel));
    }

    public IrcChannel createChannel(String name) {
        return new IrcChannel(this, name);
    }

    public P2pIrcChannel createP2pChannel(IrcUser p2pUser) {
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
                    + "' already available under account '" + this.label + "'");
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
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    public IrcChannel findChannel(String channelName) {
        for (AbstractIrcChannel channel : channels) {
            if (channel instanceof IrcChannel && channel.getName().equals(channelName)) {
                return (IrcChannel) channel;
            }
        }
        return server.findChannel(channelName);
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

    public String getAcceptedNick() {
        return me != null ? me.getNick() : null;
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

    protected File getChannelsDirectory() {
        return channelsDirectory;
    }

    public long getCreatedOn() {
        return createdOn;
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#getFields()
     */
    @Override
    public IrcAccountField[] getFields() {
        return IrcAccountField.values();
    }

    public String getHost() {
        return host;
    }

    public UUID getId() {
        return id;
    }

    public String getLabel() {
        return label;
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

    public String getName() {
        return realName;
    }

    public String getPassword() {
        return password;
    }

    public int getPort() {
        return port;
    }

    public String getPreferedNick() {
        return preferedNick;
    }

    public String getPreferedNickOrUser() {
        return preferedNick != null ? preferedNick : username;
    }

    /**
     * @return
     */
    public String getQuitMessage() {
        return IrcUiMessages.Account_Quit_Message;
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#getSaveFile()
     */
    @Override
    protected File getSaveFile() {
        return new File(saveDirectory, id.toString() + "-" + label + FILE_EXTENSION);
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
    public TrafficLogger getTraffciLogger() {
        if (trafficLogger == null) {
            trafficLogger = model.createTrafficLogger(this);
        }
        return trafficLogger;
    }

    public String getUsername() {
        return username;
    }

    protected File getUsersDirectory() {
        return usersDirectory;
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#hasAccounts()
     */
    public boolean hasChannels() {
        return !channels.isEmpty();
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    public boolean isAutoConnect() {
        return autoConnect;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void removeChannel(AbstractIrcChannel channel) {
        channels.remove(channel);
        keptChannelsArray = null;
        model.fire(new IrcModelEvent(EventType.ACCOUNT_CHANNEL_REMOVED, channel));
    }

    @Override
    public void save() throws UnsupportedEncodingException, FileNotFoundException, IOException {
        super.save();
        if (!channels.isEmpty()) {
            for (AbstractIrcChannel channel : channels) {
                if (channel.isKept()) {
                    channel.save();
                }
            }
        }
        Map<UUID, IrcUser> users = server.getUsers();
        if (!users.isEmpty()) {
            for (IrcUser user : users.values()) {
                user.save();
            }
        }
    }

    public void setAutoConnect(boolean autoConnect) {
        this.autoConnect = autoConnect;
    }

    public void setCreatedOn(long createdOn) {
        this.createdOn = createdOn;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @param me
     */
    public void setMe(IrcUser me) {
        this.me = me;
        for (AbstractIrcChannel channel : channels) {
            if (channel.isP2p() && !channel.isPresent(me.getNick())) {
                channel.addNick(me.getNick());
            }
        }
    }

    public void setName(String name) {
        this.realName = name;
    }

    public void setOffline(IrcException e) {
        this.lastException = e;
        setState(IrcAccountState.OFFLINE_AFTER_ERROR);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setPreferedNick(String nick) {
        this.preferedNick = nick;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
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
                for (AbstractIrcChannel channel : keptChannelsArray) {
                    channel.setJoined(false);
                }
                break;
            default:
                break;
            }
        }
    }

    public void setUsername(String user) {
        this.username = user;
    }

    @Override
    public String toString() {
        return label;
    }

}
