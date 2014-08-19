/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.l2x6.eircc.core.client.IrcClient;
import org.l2x6.eircc.core.model.IrcAccount;
import org.l2x6.eircc.core.model.IrcAccount.IrcAccountState;
import org.l2x6.eircc.core.model.IrcChannel;
import org.l2x6.eircc.core.model.IrcLog;
import org.l2x6.eircc.core.model.IrcMessage;
import org.l2x6.eircc.core.model.IrcServer;
import org.l2x6.eircc.core.model.IrcUser;
import org.l2x6.eircc.core.util.IrcUtils;
import org.l2x6.eircc.ui.IrcUiMessages;
import org.schwering.irc.lib.IRCUser;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcController {
    private static final IrcController INSTANCE = new IrcController();

    public static IrcController getInstance() {
        return INSTANCE;
    }

    private final Map<UUID, IrcClient> clients = new HashMap<UUID, IrcClient>();

    /**
     *
     */
    public IrcController() {
        super();
    }

    public void changeNick(IrcServer server, String oldNick, String newNick, String username) {
        IrcUtils.assertUiThread();
        server.changeNick(oldNick, newNick, username);
    }

    public void connect(IrcAccount ircAccount) throws IOException {
        IrcUtils.assertUiThread();
        getClientOrConnect(ircAccount);
    }

    public void dispose() {
        quitAll();
    }

    public IrcChannel getAccountChannel(IrcAccount ircAccount, String channelName) {
        IrcUtils.assertUiThread();
        IrcChannel result = ircAccount.findChannel(channelName);
        if (result == null) {
            result = ircAccount.createChannel(channelName);
        }
        return result;

    }

    /**
     * @param ircAccount
     * @throws IOException
     */
    private IrcClient getClientOrConnect(IrcAccount ircAccount) throws IOException {
        IrcClient client = clients.get(ircAccount.getId());
        if (client != null && !client.isConnected()) {
            client.close();
            client = null;
        }
        if (client == null) {
            client = new IrcClient();
            client.connect(ircAccount);
            clients.put(ircAccount.getId(), client);
        }
        return client;
    }

    public IrcChannel getOrCreateAccountChannel(IrcAccount ircAccount, String channelName) {
        return getOrCreateAccountChannel(ircAccount, channelName, null);
    }

    public IrcChannel getOrCreateAccountChannel(IrcAccount ircAccount, String channelName, IRCUser p2pUser) {
        IrcUtils.assertUiThread();
        IrcChannel result = ircAccount.findChannel(channelName);
        if (result == null) {
            result = ircAccount.createChannel(channelName);
            if (p2pUser != null) {
                IrcUser p2p = getOrCreateUser(ircAccount.getServer(), p2pUser.getNick(), p2pUser.getUsername());
                result.setP2pUser(p2p);
            }
        }
        ircAccount.ensureChannelKept(result);
        return result;
    }

    public IrcUser getOrCreateUser(IrcServer server, String nick, String username) {
        IrcUtils.assertUiThread();
        IrcUser result = server.findUser(nick);
        if (result == null) {
            result = server.createUser(nick, username);
            server.addUser(result);
        }
        return result;
    }

    /**
     * @param channel
     * @throws IOException
     */
    public void joinChannel(IrcChannel channel) throws IOException {
        IrcUtils.assertUiThread();
        if (!channel.isJoined()) {
            getClientOrConnect(channel.getAccount()).joinChannel(channel);
        }
    }

    /**
     * @param account
     * @throws IOException
     */
    public void listChannels(IrcAccount account) throws IOException {
        IrcUtils.assertUiThread();
        getClientOrConnect(account).listChannels();
    }

    /**
     * @param channel
     * @throws IOException
     */
    public void partChannel(IrcChannel channel) throws IOException {
        IrcUtils.assertUiThread();
        if (channel.isJoined()) {
            getClientOrConnect(channel.getAccount()).partChannel(channel);
        }
    }

    /**
     * @param channel
     * @param text
     * @throws IOException
     */
    public void postMessage(IrcChannel channel, String text) throws IOException {
        IrcUtils.assertUiThread();
        getClientOrConnect(channel.getAccount()).postMessage(channel, text);
    }

    public void quit(IrcAccount ircAccount) {
        IrcUtils.assertUiThread();
        IrcClient client = clients.remove(ircAccount.getId());
        if (client != null && !client.isConnected()) {
            client.quitAndClose();
        }
    }

    public void quitAll() {
        IrcUtils.assertUiThread();
        for (Iterator<IrcClient> i = clients.values().iterator(); i.hasNext();) {
            IrcClient client = i.next();
            if (client != null && !client.isConnected()) {
                client.quitAndClose();
            }
        }
    }

    /**
     * @param unseenNicks
     * @throws IOException
     */
    public void resolveNicks(IrcServer server, Collection<String> nicks) throws IOException {
        IrcUtils.assertUiThread();
        //TODO nicks resolving
        //getClientOrConnect(server.getAccount()).whois(nicks);
    }

    public void userQuit(IrcAccount account, String nick, String msg) {
        IrcUtils.assertUiThread();
        if (nick.equals(account.getAcceptedNick())) {
            account.setState(IrcAccountState.OFFLINE);
        } else {
            /* someone else has quit */
            for (IrcChannel channel : account.getChannels()) {
                if (channel.isJoined()) {
                    userLeft(channel, nick, msg);
                }
            }
        }
    }
    /**
     * @param channel
     * @param nick
     */
    public void userLeft(IrcChannel channel, String nick, String msg) {
        IrcUtils.assertUiThread();
        if (nick.equals(channel.getAccount().getAcceptedNick())) {
            /* It is me who left */
            channel.setJoined(false);
            String text = IrcUiMessages.Message_You_left;
            IrcLog log = channel.getLog();
            IrcMessage m = new IrcMessage(log, System.currentTimeMillis(), text);
            log.appendMessage(m);
        } else {
            if (channel.isPresent(nick)) {
                channel.removeNick(nick);
                String text;
                if (msg != null && msg.length() > 0) {
                    text = MessageFormat.format(IrcUiMessages.Message_x_left_with_message, nick, msg);
                } else {
                    text = MessageFormat.format(IrcUiMessages.Message_x_left, nick);
                }
                IrcLog log = channel.getLog();
                IrcMessage m = new IrcMessage(log, System.currentTimeMillis(), text);
                log.appendMessage(m);
            }
        }
    }

}
