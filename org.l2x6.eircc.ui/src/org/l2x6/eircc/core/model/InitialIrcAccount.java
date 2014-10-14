/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import org.l2x6.eircc.core.model.resource.IrcResourceException;
import org.l2x6.eircc.core.util.TypedField;
import org.l2x6.eircc.ui.IrcUiMessages;

/**
 * An Account bean with a modifiable {@link #label}. In {@link IrcAccount} ,
 * {@link #label} is unmodifiable as {@link IrcAccount#setLabel(String)} throws
 * an {@link UnsupportedOperationException}.
 *
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class InitialIrcAccount extends IrcObject {
    public enum InitialIrcAccountField implements TypedField {
        autoConnect(IrcUiMessages.Account_Connect_Automatically) {
            @Override
            public Object fromString(String value) {
                return Boolean.valueOf(value);
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
        username(IrcUiMessages.Account_Username),//
        socksProxyHost(IrcUiMessages.Account_SOCKS_Proxy_Host), //
        socksProxyPort(IrcUiMessages.Account_SOCKS_Proxy_Port) {
            @Override
            public Object fromString(String value) {
                return value == null || value.isEmpty() ? null : Integer.valueOf(value);
            }
        } //

        ;//
        private final String label_;
        private final TypedFieldData typedFieldData;

        /**
         * @param label_
         */
        private InitialIrcAccountField(String label) {
            this.label_ = label;
            this.typedFieldData = new TypedFieldData(name(), InitialIrcAccount.class);
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

    protected boolean autoConnect = false;

    protected String host;

    private String label;
    protected String password;
    protected int port;

    protected String preferedNick;

    protected String realName;

    protected String socksProxyHost;

    protected Integer socksProxyPort;

    protected boolean ssl;

    protected String username;

    /**
     * @param model
     * @param parentFolderPath
     */
    public InitialIrcAccount(IrcModel model) {
        super(model, model.getProject().getFullPath());
    }

    protected InitialIrcAccount(IrcModel model, String label) {
        this(model);
        this.label = label;
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcBase#dispose()
     */
    @Override
    public void dispose() {
    }

    /**
     * @return
     * @throws IrcResourceException
     */
    public IrcAccount freeze() throws IrcResourceException {
        return new IrcAccount(this);
    }

    public String getHost() {
        return host;
    }

    public String getLabel() {
        return label;
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

    public String getSocksProxyHost() {
        return socksProxyHost;
    }

    public Integer getSocksProxyPort() {
        return socksProxyPort;
    }

    public String getUsername() {
        return username;
    }

    public boolean isAutoConnect() {
        return autoConnect;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setAutoConnect(boolean autoConnect) {
        this.autoConnect = autoConnect;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public void setSocksProxyHost(String sockProxyHost) {
        this.socksProxyHost = sockProxyHost;
    }

    public void setSocksProxyPort(Integer sockProxyPort) {
        this.socksProxyPort = sockProxyPort;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public void setUsername(String user) {
        this.username = user;
    }

    @Override
    public String toString() {
        return label;
    }
}
