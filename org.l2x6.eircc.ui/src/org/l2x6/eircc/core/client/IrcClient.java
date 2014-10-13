/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.client;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.swt.widgets.Display;
import org.l2x6.eircc.core.IrcController;
import org.l2x6.eircc.core.IrcException;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.core.model.IrcAccount;
import org.l2x6.eircc.core.model.IrcAccount.IrcAccountState;
import org.l2x6.eircc.core.model.IrcLog;
import org.l2x6.eircc.core.model.IrcMessage;
import org.l2x6.eircc.core.model.IrcNick;
import org.l2x6.eircc.core.model.IrcServer;
import org.l2x6.eircc.core.model.IrcUser;
import org.l2x6.eircc.core.model.PlainIrcChannel;
import org.l2x6.eircc.core.model.resource.IrcResourceException;
import org.l2x6.eircc.core.util.IrcUtils;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.IrcUiMessages;
import org.schwering.irc.lib.IRCCommand;
import org.schwering.irc.lib.IRCConnection;
import org.schwering.irc.lib.IRCEventListener;
import org.schwering.irc.lib.IRCModeParser;
import org.schwering.irc.lib.IRCUser;
import org.schwering.irc.lib.Rpl;
import org.schwering.irc.lib.ssl.SSLIRCConnection;
import org.schwering.irc.lib.ssl.SSLTrustManager;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcClient {
    private static class IrcExecutor extends ThreadPoolExecutor {
        /**
         * @param executorType
         * @return
         */
        private static ThreadFactory createThreadFactory(String executorType) {
            return new ThreadFactory() {
                final ThreadFactory defaultFactory = Executors.defaultThreadFactory();

                public Thread newThread(final Runnable r) {
                    Thread thread = defaultFactory.newThread(r);
                    thread.setName("IrcClient"+ executorType +"-" + thread.getName());
                    return thread;
                }
            };
        }

        /**
         * @param executorType
         * @param corePoolSize
         * @param maximumPoolSize
         * @param keepAliveTime
         * @param unit
         * @param workQueue
         */
        public IrcExecutor(String executorType) {
            super(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), createThreadFactory(executorType));
        }

    }
    /**
     * A place where the <i>Man in the Middle</i> likes to reside.
     *
     * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
     */
    private static class MitmLounge implements SSLTrustManager {
        private X509Certificate[] chain;

        public X509Certificate[] getAcceptedIssuers() {
            return chain != null ? chain : new X509Certificate[0];
        }

        public boolean isTrusted(X509Certificate[] chain) {
            return true;
        }
    }

    private class TimeoutCheckTask implements Runnable {
        private final Future<?> future;

        /**
         * @param future
         */
        public TimeoutCheckTask(Future<?> future) {
            super();
            this.future = future;
        }

        /**
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {

            try {
                future.get(commandTimeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
            } catch (ExecutionException e) {
                String msg = MessageFormat.format(IrcUiMessages.IrcClient_commandExecutionException, e.getMessage());
                account.setOffline(new IrcException(msg, e, account));
            } catch (TimeoutException e) {
                account.setOffline(new IrcException(IrcUiMessages.IrcClient_commandTimeOut, e, account));
            }

        }

    }

    private class UiListener implements IRCEventListener {

        private final List<PlainIrcChannel> channelBuffer = new ArrayList<PlainIrcChannel>(LIST_BUFFER_SIZE);

        /**
         *
         */
        private void flushChannelBuffer() {
            final PlainIrcChannel[] channels = channelBuffer.toArray(new PlainIrcChannel[channelBuffer.size()]);
            channelBuffer.clear();
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    account.getServer().addChannels(channels);
                }
            });
        }

        /**
         * @param num
         * @param value
         * @param msg
         * @throws IrcResourceException
         */
        private void handleListReply(int num, String value, String msg) throws IrcResourceException {
            StringTokenizer st = new StringTokenizer(value, " ");
            if (st.hasMoreTokens()) {
                String myNick = st.nextToken();
                if (!myNick.equals(account.getAcceptedNick())) {
                    EirccUi.warn("Probably something wrong: " + myNick + " != " + account.getAcceptedNick());
                }
                if (st.hasMoreTokens()) {
                    final String channelName = st.nextToken();
                    PlainIrcChannel channel = account.getServer().createChannel(channelName);
                    channelBuffer.add(channel);
                    if (channelBuffer.size() >= LIST_BUFFER_SIZE) {
                        flushChannelBuffer();
                    }
                }
            }
        }

        /**
         * @param num
         * @param channelSpec
         * @param msg
         */
        private void handleNamReply(int num, String channelSpec, final String msg) {
            int lastSpace = channelSpec.lastIndexOf(' ');
            String chName;
            if (lastSpace >= 0) {
                chName = channelSpec.substring(lastSpace + 1);
            } else {
                chName = channelSpec;
            }
            switch (chName.charAt(0)) {
            case '=':
            case '*':
            case '@':
                chName = chName.substring(1);
                break;
            default:
                break;
            }
            final String channelName = chName;
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    try {
                        AbstractIrcChannel channel = controller.getOrCreateAccountChannel(account, channelName);
                        IrcServer server = account.getServer();
                        StringTokenizer st = new StringTokenizer(msg, " ");
                        List<IrcNick> unseenNicks = new ArrayList<IrcNick>();
                        List<IrcNick> allNicks = new ArrayList<IrcNick>();
                        while (st.hasMoreTokens()) {
                            IrcNick nick = IrcNick.parse(st.nextToken());
                            allNicks.add(nick);
                            IrcUser ircUser = server.findUser(nick.getCleanNick());
                            if (ircUser == null) {
                                unseenNicks.add(nick);
                            }
                        }
                        channel.addNicks(allNicks);
                        if (!unseenNicks.isEmpty()) {
                            controller.resolveNicks(server, unseenNicks);
                        }
                    } catch (IOException | IrcResourceException e) {
                        EirccUi.log(e);
                    }
                }
            });
        }

        /**
         * @see org.schwering.irc.lib.IRCEventListener#onDisconnected()
         */
        @Override
        public void onDisconnected() {
            System.out.println("disconnected");
        }

        /**
         * @see org.schwering.irc.lib.IRCEventListener#onError(int,
         *      java.lang.String)
         */
        @Override
        public void onError(int num, String msg) {
            Rpl rpl = Rpl.valueByCode(num);
            if (rpl != null) {
                switch (rpl) {
                case ERR_NICKNAMEINUSE:
                case ERR_ERRONEUSNICKNAME:
                case ERR_NONICKNAMEGIVEN:
                    String newNick = nickGenerator.newNick(connection.getNick());
                    if (newNick != null) {
                        connection.doNick(newNick);
                    } else {
                        connection.doQuit();
                    }
                    break;
                default:
                    break;
                }
            }
        }

        /**
         * @see org.schwering.irc.lib.IRCEventListener#onError(java.lang.String)
         */
        @Override
        public void onError(String msg) {
            System.out.println("error: " + msg);
        }

        /**
         * @see org.schwering.irc.lib.IRCEventListener#onInvite(java.lang.String,
         *      org.schwering.irc.lib.IRCUser, java.lang.String)
         */
        @Override
        public void onInvite(String chan, IRCUser user, String passiveNick) {
            System.out.println("invite " + chan + " " + user + " " + passiveNick);
        }

        /**
         * @see org.schwering.irc.lib.IRCEventListener#onJoin(java.lang.String,
         *      org.schwering.irc.lib.IRCUser)
         */
        @Override
        public void onJoin(final String chan, final IRCUser user) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    try {
                        AbstractIrcChannel channel = controller.getOrCreateAccountChannel(account,
                                chan);
                        String nick = user.getNick();
                        if (nick.equals(account.getAcceptedNick())) {
                            /* It is me who joined */
                            channel.setJoined(true);
                        } else {
                            /* make sure the user info is stored in server */
                            controller.getOrCreateUser(account.getServer(), nick, user.getUsername());
                            channel.addNick(IrcNick.parse(nick));
                        }
                    } catch (IrcResourceException e) {
                        EirccUi.log(e);
                    }
                }
            });
        }

        /**
         * @see org.schwering.irc.lib.IRCEventListener#onKick(java.lang.String,
         *      org.schwering.irc.lib.IRCUser, java.lang.String,
         *      java.lang.String)
         */
        @Override
        public void onKick(String chan, IRCUser user, String passiveNick, String msg) {
            System.out.println("kick " + chan + " " + user + " " + passiveNick + " " + msg);
        }

        /**
         * @see org.schwering.irc.lib.IRCEventListener#onMode(org.schwering.irc.lib.IRCUser,
         *      java.lang.String, java.lang.String)
         */
        @Override
        public void onMode(IRCUser user, String passiveNick, String mode) {
            System.out.println("mode " + user);
        }

        /**
         * @see org.schwering.irc.lib.IRCEventListener#onMode(java.lang.String,
         *      org.schwering.irc.lib.IRCUser,
         *      org.schwering.irc.lib.IRCModeParser)
         */
        @Override
        public void onMode(String chan, IRCUser user, IRCModeParser modeParser) {
            System.out.println("mode " + chan + " " + user);
        }

        /**
         * @see org.schwering.irc.lib.IRCEventListener#onNick(org.schwering.irc.lib.IRCUser,
         *      java.lang.String)
         */
        @Override
        public void onNick(final IRCUser user, final String newNick) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    controller.changeNick(account.getServer(), user.getNick(), newNick,
                            user.getUsername());
                }
            });
        }

        /**
         * @see org.schwering.irc.lib.IRCEventListener#onNotice(java.lang.String,
         *      org.schwering.irc.lib.IRCUser, java.lang.String)
         */
        @Override
        public void onNotice(String target, IRCUser user, String msg) {
            System.out.println("notice " + user + " " + msg);
        }

        /**
         * @see org.schwering.irc.lib.IRCEventListener#onPart(java.lang.String,
         *      org.schwering.irc.lib.IRCUser, java.lang.String)
         */
        @Override
        public void onPart(final String chan, final IRCUser user, final String msg) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    AbstractIrcChannel channel = controller.getAccountChannel(account, chan);
                    if (channel != null) {
                        controller.userLeft(channel, user.getNick(), msg);
                    }
                }
            });
        }

        /**
         * @see org.schwering.irc.lib.IRCEventListener#onPing(java.lang.String)
         */
        @Override
        public void onPing(String ping) {
            System.out.println("ping " + ping);
        }

        /**
         * @see org.schwering.irc.lib.IRCEventListener#onPrivmsg(java.lang.String,
         *      org.schwering.irc.lib.IRCUser, java.lang.String)
         */
        @Override
        public void onPrivmsg(String target, final IRCUser user, final String msg) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    try {
                        final boolean isP2p = target.equals(account.getAcceptedNick());
                        IrcUser ircUser = controller.getOrCreateUser(account.getServer(), user.getNick(),
                                user.getUsername());
                        AbstractIrcChannel channel;
                        if (!isP2p) {
                            channel = controller.getOrCreateAccountChannel(account, target);
                        } else {
                            channel = controller.getOrCreateP2pChannel(ircUser);
                        }
                        channel.setJoined(true);
                        IrcLog log = channel.getLog();
                        IrcMessage message = new IrcMessage(log, OffsetDateTime.now(), ircUser, msg, channel.isP2p());
                        log.appendMessage(message);
                    } catch (IrcResourceException e) {
                        EirccUi.log(e);
                    }
                }
            });
        }

        /**
         * @see org.schwering.irc.lib.IRCEventListener#onQuit(org.schwering.irc.lib.IRCUser,
         *      java.lang.String)
         */
        @Override
        public void onQuit(final IRCUser user, final String msg) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    controller.userQuit(account, user.getNick(), msg);
                }
            });
        }

        /**
         * @see org.schwering.irc.lib.IRCEventListener#onRegistered()
         */
        @Override
        public void onRegistered() {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    IrcUser me = controller.getOrCreateUser(account.getServer(), connection.getNick(),
                            account.getUsername());
                    account.setMe(me);
                    account.setState(IrcAccountState.ONLINE);
                }
            });
        }

        /**
         * @see org.schwering.irc.lib.IRCEventListener#onReply(int,
         *      java.lang.String, java.lang.String)
         */
        @Override
        public void onReply(int num, String value, String msg) {
            try {
                Rpl rpl = Rpl.valueByCode(num);
                if (rpl != null) {
                    switch (rpl) {
                    case RPL_LISTSTART:
                        /* ignore */
                        break;
                    case RPL_LIST:
                        handleListReply(num, value, msg);
                        break;
                    case RPL_LISTEND:
                        if (!channelBuffer.isEmpty()) {
                            flushChannelBuffer();
                        }
                        break;
                    case RPL_NAMREPLY:
                        handleNamReply(num, value, msg);
                        break;
                    case RPL_ENDOFNAMES:
                        /* ignore */
                        break;
                    default:
                        break;
                    }
                }
            } catch (IrcResourceException e) {
                EirccUi.log(e);
            }
        }

        /**
         * @see org.schwering.irc.lib.IRCEventListener#onTopic(java.lang.String,
         *      org.schwering.irc.lib.IRCUser, java.lang.String)
         */
        @Override
        public void onTopic(String chan, IRCUser user, String topic) {
            System.out.println("topic " + user + " " + topic);
        }

        /**
         * @see org.schwering.irc.lib.IRCEventListener#unknown(java.lang.String,
         *      java.lang.String, java.lang.String, java.lang.String)
         */
        @Override
        public void unknown(String prefix, String command, String middle, String trailing) {
            System.out.println("unknown " + prefix + " " + command + " " + middle + " " + trailing);
        }

    }

    public static final int DEFAULT_PORT = 6667;

    /** Update UI by this many list entries. */
    private static final int LIST_BUFFER_SIZE = 16;

    private IrcAccount account;
    private Duration commandTimeout;

    private IRCConnection connection;

    private final IrcController controller;

    private ExecutorService executor;

    private final IrcNickGenerator nickGenerator = IrcNickGenerator.DEFAULT;
    private Duration pingInterval;

    private final IrcExecutor timeoutChecker;

    /**
     * @param commandTimeout
     * @param pingInterval
     *
     */
    public IrcClient(IrcController controller, Duration commandTimeout, Duration pingInterval) {
        super();
        this.controller = controller;
        this.pingInterval = pingInterval;
        this.commandTimeout = commandTimeout;
        executor = new IrcExecutor("Executor");
        timeoutChecker = new IrcExecutor("TaskValidator");
    }

    public void close() {
        IrcUtils.assertUiThread();
        if (connection != null) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    if (connection != null) {
                        connection.close();
                        connection = null;
                    }
                }
            });
        }
    }

    public void connect(IrcAccount account) throws IrcException {
        IrcUtils.assertUiThread();
        this.account = account;
        if (account.isSsl()) {
            SSLIRCConnection conn = new SSLIRCConnection(account.getHost(), new int[] { account.getPort() },
                    account.getPassword(), account.getPreferedNickOrUser(), account.getUsername(), account.getName(),
                    account.getTraffciLogger());
            conn.addTrustManager(new MitmLounge());
            connection = conn;

        } else {
            connection = new IRCConnection(account.getHost(), new int[] { account.getPort() }, account.getPassword(),
                    account.getPreferedNickOrUser(), account.getUsername(), account.getName(),
                    account.getTraffciLogger());
        }
        connection.addIRCEventListener(new UiListener());
        connection.setEncoding("UTF-8");
        connection.setPong(true);
        connection.setDaemon(false);
        connection.setColors(false);
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    connection.connect();
                } catch (Exception e) {
                    controller.handle(new IrcException("Could not connect to '" + account.getLabel() + "': " + e.getClass().getName()
                            + ": " + e.getMessage(), e, account));
                }
            }
        });

    }

    /**
     * Only from UI thread
     *
     * @throws IOException
     */
    private void ensureConnected() throws IrcException {
        if (connection != null && connection.isConnected()) {
            return;
        } else if (connection != null) {
            try {
                connection.connect();
            } catch (IOException e) {
                throw new IrcException("Could not connect to '" + account.getLabel() + "': " + e.getMessage(), e,
                        account);
            }
        } else {
            throw new IllegalStateException("Must be connected");
        }
    }

    public Duration getCommandTimeout() {
        return commandTimeout;
    }

    public Duration getPingInterval() {
        return pingInterval;
    }

    /**
     * @return
     */
    public boolean isConnected() {
        return connection != null && connection.isConnected();
    }

    /**
     * @param channel
     * @throws IOException
     */
    public void joinChannel(final AbstractIrcChannel channel) throws IrcException {
        submit(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureConnected();
                    connection.doJoin(channel.getName());
                } catch (final IrcException e) {
                    notifyUi(e);
                }
            }
        });
    }

    public void listChannels() throws IrcException {
        submit(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureConnected();
                    connection.doList();
                } catch (IrcException e) {
                    notifyUi(e);
                }
            }
        });
    }

    public void nick(final String newNick) throws IrcException {
        submit(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureConnected();
                    connection.doNick(newNick);
                } catch (IrcException e) {
                    notifyUi(e);
                }
            }
        });
    }

    private void notifyUi(final IrcException e) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                EirccUi.log(e);
                account.setOffline(e);
            }
        });
    }

    /**
     * @param channel
     */
    public void partChannel(final AbstractIrcChannel channel) {
        if (isConnected()) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    connection.doPart(channel.getName());
                }
            });
        } else {
            quitAndClose();
        }
    }

    public void ping() throws IrcException {
        submit(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureConnected();
                    connection.send(IRCCommand.PING.name());
                } catch (IrcException e) {
                    notifyUi(e);
                }
            }
        });
    }

    /**
     * @throws IOException
     *
     */
    public void postMessage(final AbstractIrcChannel channel, final String message) throws IrcException {
        submit(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureConnected();
                    connection.doPrivmsg(channel.getName(), message);
                    /*
                     * post back to model only after the above has not thrown an
                     * exception
                     */
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            IrcMessage m = new IrcMessage(channel.getLog(), OffsetDateTime.now(), account.getMe(), message,
                                    channel.isP2p());
                            channel.getLog().appendMessage(m);
                        }
                    });
                } catch (IrcException e) {
                    notifyUi(e);
                }
            }
        });
    }

    /**
     * @param rawCommand
     * @throws IOException
     */
    public void postRaw(final String rawCommand) throws IrcException {
        submit(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureConnected();
                    connection.send(rawCommand);
                } catch (IrcException e) {
                    notifyUi(e);
                }
            }
        });
    }

    /**
     *
     */
    public void quitAndClose() {
        IrcUtils.assertUiThread();
        if (isConnected()) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    if (connection != null) {
                        try {
                            String msg = account.getQuitMessage();
                            if (msg == null) {
                                connection.doQuit();
                            } else {
                                connection.doQuit(msg);
                            }
                        } finally {
                            connection.close();
                            connection = null;
                        }
                    }
                }
            });
        }
        if (account.getState() == IrcAccountState.ONLINE) {
            account.setState(IrcAccountState.OFFLINE);
        }
    }

    public void setCommandTimeout(Duration commandTimeout) {
        this.commandTimeout = commandTimeout;
    }

    public void setPingInterval(Duration pingInterval) {
        this.pingInterval = pingInterval;
    }

    /**
     *
     */
    private void submit(Runnable task) {
        IrcUtils.assertUiThread();
        Future<?> future = executor.submit(task);
        timeoutChecker.submit(new TimeoutCheckTask(future));
    }

    public void whois(Collection<String> nicks) throws IrcException {
        if (nicks != null && !nicks.isEmpty()) {
            IrcUtils.assertUiThread();
            for (final String nick : nicks) {
                submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ensureConnected();
                            connection.doWhois(nick);
                        } catch (IrcException e) {
                            notifyUi(e);
                        }
                    }
                });
            }
        }
    }

}
