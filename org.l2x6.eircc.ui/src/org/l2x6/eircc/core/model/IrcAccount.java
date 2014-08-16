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
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.l2x6.eircc.core.IrcModelEvent;
import org.l2x6.eircc.core.IrcModelEvent.EventType;
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
        },//
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

    private boolean autoConnect = false;

    /** Kept or joined channels */
    private final Map<String, IrcChannel> channels = new TreeMap<String, IrcChannel>();
    private long createdOn;

    private String host;
    protected final UUID id;

    private IrcChannel[] keptChannelsArray;
    private String label;
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

    public IrcAccount(IrcModel model, UUID id, String label) {
        this(model, id, label, -1);
    }
    /**
     * @param model
     * @param label
     */
    public IrcAccount(IrcModel model, UUID id, String label, long createdOn) {
        super();
        this.id = id;
        this.model = model;
        this.label = label;
        this.createdOn = createdOn;
        this.server = new IrcServer(this);
    }

    public void addChannel(IrcChannel channel) {
        if (channel.getAccount() != this) {
            throw new IllegalArgumentException("Cannot add channel with parent distinct from this "
                    + this.getClass().getSimpleName());
        }
        String channelName = channel.getName();
        if (channels.get(channelName) != null) {
            throw new IllegalArgumentException("Channel with name '" + channelName
                    + "' already available under account '" + this.label + "'");
        }
        channels.put(channelName, channel);
        keptChannelsArray = null;
        model.fire(new IrcModelEvent(EventType.KEPT_CHANNEL_ADDED, channel));
    }

    public IrcChannel createChannel(String name) {
        return new IrcChannel(this, name);
    }
    /**
     * @see org.l2x6.eircc.core.model.IrcObject#dispose()
     */
    @Override
    public void dispose() {
        for (IrcChannel channel : channels.values()) {
            channel.dispose();
        }
        keptChannelsArray = null;
    }

    /**
     * @param result
     */
    public void ensureChannelKept(IrcChannel channel) {
        String channelName = channel.getName();
        IrcChannel availableChannel = channels.get(channelName);
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
        IrcChannel result = channels.get(channelName);
        if (result == null) {
            result = server.findChannel(channelName);
        }
        return result;
    }

    public String getAcceptedNick() {
        return me.getNick();
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#getChannels()
     */
    public IrcChannel[] getChannels() {
        if (keptChannelsArray == null) {
            Collection<IrcChannel> chans = channels.values();
            keptChannelsArray = chans.toArray(new IrcChannel[chans.size()]);
        }
        return keptChannelsArray;
    }

    /**
     * @param parentDir
     * @return
     */
    public File getChannelsDir(File parentDir) {
        return new File(parentDir, id.toString() +"-channels");
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
     * @see org.l2x6.eircc.core.model.IrcObject#getSaveFile(java.io.File)
     */
    @Override
    protected File getSaveFile(File parentDir) {
        return new File(parentDir, id.toString() + "-"+ label + FILE_EXTENSION);
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

    @Override
    public void load(File accountPropsFile) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        super.load(accountPropsFile);
        File channelsDir = getChannelsDir(accountPropsFile.getParentFile());
        if (channelsDir.exists()) {
            for (File channelPropsFile : channelsDir.listFiles()) {
                String fileName = channelPropsFile.getName();
                if (channelPropsFile.isFile() && fileName.endsWith(IrcChannel.FILE_EXTENSION)) {
                    String chName = fileName.substring(0, fileName.length() - IrcChannel.FILE_EXTENSION.length());
                        IrcChannel channel = new IrcChannel(this, chName);
                        channel.load(channelPropsFile);
                        channels.put(channel.getName(), channel);
                }
            }
        }
    }

    public void removeChannel(IrcChannel channel) {
        channels.remove(channel);
        keptChannelsArray = null;
        model.fire(new IrcModelEvent(EventType.KEPT_CHANNEL_REMOVED, channel));
    }

    @Override
    public void save(File parentDir) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        super.save(parentDir);
        if (!channels.isEmpty()) {
            File channelsDir = getChannelsDir(parentDir);
            for (IrcChannel channel : channels.values()) {
                channel.save(channelsDir);
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
    }

    public void setName(String name) {
        this.realName = name;
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
        if (oldState != state) {
            model.fire(new IrcModelEvent(EventType.ACCOUNT_STATE_CHANGED, this));
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
