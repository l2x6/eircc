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
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.UUID;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.l2x6.eircc.core.model.event.IrcModelEvent;
import org.l2x6.eircc.core.model.event.IrcModelEvent.EventType;
import org.l2x6.eircc.core.util.TypedField;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcUser extends IrcObject implements PersistentIrcObject {
    public static class IrcHistoricUser extends IrcUser {
        /**
         * @param server
         * @param id
         * @param nick
         * @param username
         */
        public IrcHistoricUser(String nick) {
            super(null, null, nick, null);
        }

    }

    public enum IrcUserField implements TypedField {
        host, nick, previousNicksString, username;

        private final TypedFieldData typedFieldData;

        /**
         * @param typedFieldData
         */
        private IrcUserField() {
            this.typedFieldData = new TypedFieldData(name(), IrcUser.class);
        }

        @Override
        public TypedFieldData getTypedFieldData() {
            return typedFieldData;
        }
    }

    public static final String FILE_EXTENSION = ".user.properties";

    /**
     * @return
     */
    public static boolean isUserFile(IResource f) {
        return f.getType() == IResource.FILE && f.getName().endsWith(IrcUser.FILE_EXTENSION);
    }

    private String host;

    private final UUID id;

    private String nick;
    private IPath path;

    private final List<String> previousNicks;

    private final IrcServer server;

    private String username;

    /**
     * @param server2
     * @param userPropsFile
     * @throws IOException
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     * @throws CoreException
     */
    public IrcUser(IrcServer server, IFile userPropsFile) throws UnsupportedEncodingException, FileNotFoundException,
            IOException, CoreException {
        super(server.getAccount().getModel(), server.getAccount().getUsersFolderPath());
        this.server = server;
        String fName = userPropsFile.getName();
        String uid = fName.substring(0, fName.length() - IrcUser.FILE_EXTENSION.length());
        this.id = UUID.fromString(uid);
        this.previousNicks = new ArrayList<String>();
        load(userPropsFile);
    }

    /**
     * @param id
     * @param nick
     * @param username
     * @param account
     * @param realName
     */
    public IrcUser(IrcServer server, UUID id) {
        super(server.getAccount().getModel(), server.getAccount().getUsersFolderPath());
        this.server = server;
        this.id = id;
        this.previousNicks = new ArrayList<String>();
    }

    public IrcUser(IrcServer server, UUID id, String nick, String username) {
        this(server, id);
        this.username = username;
        this.nick = nick;
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#dispose()
     */
    @Override
    public void dispose() {
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IrcUser other = (IrcUser) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#getFields()
     */
    @Override
    public IrcUserField[] getFields() {
        return IrcUserField.values();
    }

    public String getHost() {
        return host;
    }

    public UUID getId() {
        return id;
    }

    public String getNick() {
        return nick;
    }

    @Override
    public IPath getPath() {
        if (path == null) {
            path = parentFolderPath.append(id.toString() + FILE_EXTENSION);
        }
        return path;
    }

    /**
     * @return
     */
    public String getPreviousNick() {
        if (previousNicks == null || previousNicks.isEmpty()) {
            return null;
        }
        return previousNicks.get(previousNicks.size() - 1);
    }

    public String getPreviousNicksString() {
        if (!previousNicks.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String nick : previousNicks) {
                sb.append(nick).append(' ');
            }
            return sb.toString();
        }
        return null;
    }

    public IrcServer getServer() {
        return server;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    public boolean hasOrHadNick(String nick) {
        if (nick == null) {
            return false;
        }
        if (nick.equals(this.nick)) {
            return true;
        }
        ListIterator<String> it = previousNicks.listIterator(previousNicks.size());
        while (it.hasPrevious()) {
            if (nick.equals(it.previous())) {
                return true;
            }
        }
        return false;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setNick(String nick) {
        String oldNick = this.nick;
        this.nick = nick;
        if (oldNick != null && !oldNick.equals(nick)) {
            previousNicks.add(oldNick);
            getServer().getAccount().getModel().fire(new IrcModelEvent(EventType.NICK_CHANGED, this));
        }
    }

    public void setPreviousNicksString(String nicks) {
        previousNicks.clear();
        if (nicks != null) {
            StringTokenizer st = new StringTokenizer(nicks, " ");
            while (st.hasMoreTokens()) {
                previousNicks.add(st.nextToken());
            }
        }
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return nick;
    }

}
