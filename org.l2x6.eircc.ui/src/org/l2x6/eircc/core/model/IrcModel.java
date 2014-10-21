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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.l2x6.eircc.core.client.IrcClient;
import org.l2x6.eircc.core.client.TrafficLoggerFactory;
import org.l2x6.eircc.core.model.event.IrcModelEvent;
import org.l2x6.eircc.core.model.event.IrcModelEvent.EventType;
import org.l2x6.eircc.core.model.event.IrcModelEventListener;
import org.l2x6.eircc.core.model.resource.IrcAccountResource;
import org.l2x6.eircc.core.model.resource.IrcResourceException;
import org.l2x6.eircc.core.model.resource.IrcRootResource;
import org.l2x6.eircc.core.util.IrcUtils;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.IrcUiMessages;
import org.schwering.irc.lib.TrafficLogger;

/**
 * Model root
 *
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcModel extends IrcBase {

    /** {@link IrcAccount}s by {@link IrcAccount#getLabel()} */
    private final Map<String, IrcAccount> accounts = new TreeMap<String, IrcAccount>();

    private IrcAccount[] accountsArray;

    private List<IrcModelEventListener> listeners = Collections.emptyList();
    private final IrcNotificationLevelProvider notificationLevelProvider;

    private IrcRootResource rootResource;

    private final TrafficLoggerFactory trafficLoggerFactory;

    /**
     * @param trafficLoggerFactory
     * @param notificationLevelProvider
     */
    public IrcModel(TrafficLoggerFactory trafficLoggerFactory, IrcNotificationLevelProvider notificationLevelProvider) {
        super();
        this.trafficLoggerFactory = trafficLoggerFactory;
        this.notificationLevelProvider = notificationLevelProvider;
    }
    public void addAccount(IrcAccount account) {
        if (account.getModel() != this) {
            throw new IllegalArgumentException("Cannot add account with parent distinct from this "
                    + this.getClass().getSimpleName());
        }
        accounts.put(account.getLabel(), account);
        accountsArray = null;
        fire(new IrcModelEvent(EventType.ACCOUNT_ADDED, account));
    }

    public void addModelEventListener(IrcModelEventListener listener) {
        if (!listeners.contains(listener)) {
            List<IrcModelEventListener> newList = new ArrayList<IrcModelEventListener>(listeners.size() + 1);
            newList.addAll(listeners);
            newList.add(listener);
            Collections.sort(newList, IrcModelEventListener.COMPARATOR);
            listeners = newList;
        }
    }

    public IrcAccount createAccount(String label) throws IrcResourceException {
        return new IrcAccount(this, label, System.currentTimeMillis());
    }

    TrafficLogger createTrafficLogger(IrcAccount account) {
        return trafficLoggerFactory.createTrafficLogger(account);
    }

    public void dispose() {
        for (IrcAccount account : accounts.values()) {
            account.dispose();
        }
        accountsArray = null;
    }

    /**
     * @param ircModelEvent
     */
    void fire(IrcModelEvent ircModelEvent) {
        IrcUtils.assertUiThread();
        for (IrcModelEventListener listener : listeners) {
            try {
                listener.handle(ircModelEvent);
            } catch (Exception e) {
                EirccUi.log(e);
            }
        }
    }

    /**
     * @param accountLabel
     * @return
     */
    public IrcAccount getAccount(String accountLabel) {
        return accounts.get(accountLabel);
    }

    /**
     * @return
     */
    public IrcAccount[] getAccounts() {
        if (accountsArray == null) {
            accountsArray = accounts.values().toArray(new IrcAccount[accounts.size()]);
        }
        return accountsArray;
    }

    public IrcAccountsStatistics getAccountsStatistics() {
        int channelsOnline = 0;
        int channelsOffline = 0;
        int channelsWithUnreadMessages = 0;
        int channelsWithUnreadFromTrackedUsers = 0;
        int channelsNamingMe = 0;
        int channelsOfflineAfterError = 0;
        for (IrcAccount account : accounts.values()) {
            switch (account.getState()) {
            case ONLINE:
                channelsOnline++;
                break;
            case OFFLINE:
                channelsOffline++;
                break;
            case OFFLINE_AFTER_ERROR:
                channelsOfflineAfterError++;
                break;
            default:
                break;
            }
            for (AbstractIrcChannel channel : account.getChannels()) {
                IrcLog log = channel.getLog();
                if (log != null) {
                    switch (log.getNotificationLevel()) {
                    case ME_NAMED:
                        channelsNamingMe++;
                        break;
                    case UNREAD_MESSAGES:
                        channelsWithUnreadMessages++;
                        break;
                    case UNREAD_MESSAGES_FROM_A_TRACKED_USER:
                        channelsWithUnreadFromTrackedUsers++;
                        break;
                    case NO_NOTIFICATION:
                        /* do nothing */
                        break;
                    }
                }
            }
        }
        return new IrcAccountsStatistics(channelsOnline, channelsOffline, channelsWithUnreadMessages, channelsWithUnreadFromTrackedUsers, channelsNamingMe,
                channelsOfflineAfterError);
    }

    public IrcNotificationLevelProvider getNotificationLevelProvider() {
        return notificationLevelProvider;
    }

    public IProject getProject() {
        return rootResource.getProject();
    }

    public IWorkspaceRoot getRoot() {
        return getProject().getWorkspace().getRoot();
    }

    public IrcRootResource getRootResource() {
        return rootResource;
    }

    /**
     * @return
     */
    public Collection<IrcAccount> getSearchableAccounts() {
        return new ArrayList<IrcAccount>(accounts.values());
    }

    public boolean hasAccounts() {
        return !accounts.isEmpty();
    }

    public void load(IrcRootResource rootResource) throws IrcResourceException {
        this.rootResource = rootResource;
        try {
            for (IResource r : rootResource.getProject().members()) {
                if (IrcAccountResource.isAccountFile(r)) {
                    IrcAccount account = new IrcAccount(this, (IFile) r);
                    accounts.put(account.getLabel(), account);
                }
            }
        } catch (CoreException | IOException e) {
            throw new IrcResourceException(e);
        }
    }

    public InitialIrcAccount proposeNextAccount() {
        String newLabel = IrcUiMessages.Account + "#" + (accounts.size() + 1);
        InitialIrcAccount result = new InitialIrcAccount(this, newLabel);
        result.setHost("irc.devel.redhat.com");
        result.setPort(IrcClient.DEFAULT_PORT);
        result.setUsername(System.getProperty("user.name"));
        result.setAutoConnect(true);
        try {
            result.setName(IrcUtils.getRealUserName());
        } catch (IOException | InterruptedException e) {
            EirccUi.log(e);
        }

        if (accounts.size() > 0) {
            IrcAccount lastAccount = null;
            for (IrcAccount ircAccount : accounts.values()) {
                if (lastAccount == null || ircAccount.getCreatedOn() > lastAccount.getCreatedOn()) {
                    lastAccount = ircAccount;
                }
            }
            String lastName = lastAccount.getName();
            if (lastName != null) {
                result.setName(lastName);
            }
            result.setPreferedNick(lastAccount.getPreferedNick());
        }
        return result;
    }

    public void removeAccount(IrcAccount account) {
        accounts.remove(account);
        accountsArray = null;
        fire(new IrcModelEvent(EventType.ACCOUNT_REMOVED, account));
    }

    public void removeModelEventListener(IrcModelEventListener listener) {
        if (listeners.contains(listener)) {
            List<IrcModelEventListener> newList = new ArrayList<IrcModelEventListener>(listeners);
            newList.remove(listener);
            listeners = newList;
        }
    }

    public void save(IProgressMonitor monitor) throws UnsupportedEncodingException, FileNotFoundException, IOException,
            CoreException {
        for (IrcAccount account : accounts.values()) {
            account.save(monitor);
        }
    }

}
