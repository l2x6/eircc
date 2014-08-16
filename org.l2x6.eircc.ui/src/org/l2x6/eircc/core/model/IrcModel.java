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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.l2x6.eircc.core.IrcModelEvent;
import org.l2x6.eircc.core.IrcModelEvent.EventType;
import org.l2x6.eircc.core.IrcModelEventListener;
import org.l2x6.eircc.core.IrcUtils;
import org.l2x6.eircc.core.client.IrcClient;
import org.l2x6.eircc.core.client.TrafficLoggerFactory;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.IrcUiMessages;
import org.schwering.irc.lib.TrafficLogger;

/**
 * Model root
 *
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcModel extends IrcObject {
    public enum IrcModelField {};

    private static final IrcModel INSTANCE = new IrcModel();

    public static IrcModel getInstance() {
        return INSTANCE;
    }

    private final List<IrcAccount> accounts = new ArrayList<IrcAccount>();

    private IrcAccount[] accountsArray;
    private List<IrcModelEventListener> listeners = Collections.emptyList();

    private TrafficLoggerFactory trafficLoggerFactory;
    /**
     *
     */
    public IrcModel() {
    }

    public void addAccount(IrcAccount account) {
        if (account.getModel() != this) {
            throw new IllegalArgumentException("Cannot add account with parent distinct from this "+ this.getClass().getSimpleName());
        }
        accounts.add(account);
        accountsArray = null;
        fire(new IrcModelEvent(EventType.ACCOUNT_ADDED, account));
    }

    public void addModelEventListener(IrcModelEventListener listener) {
        List<IrcModelEventListener> newList = new ArrayList<IrcModelEventListener>(listeners.size() + 1);
        newList.addAll(listeners);
        newList.add(listener);
        listeners = newList;
    }

    public IrcAccount createAccount(String label) {
        return new IrcAccount(this, UUID.randomUUID(), label, System.currentTimeMillis());
    }

    TrafficLogger createTrafficLogger(IrcAccount account) {
        return trafficLoggerFactory.createTrafficLogger(account);
    }

    public void dispose() {
        for (IrcAccount account : accounts) {
            account.dispose();
        }
        accountsArray = null;
    }

    /**
     * @param ircModelEvent
     */
    void fire(IrcModelEvent ircModelEvent) {
        for (IrcModelEventListener listener : listeners) {
            try {
                listener.handle(ircModelEvent);
            } catch (Exception e) {
                EirccUi.log(e);
            }
        }
    }

    /**
     * @return
     */
    public IrcAccount[] getAccounts() {
        if (accountsArray == null) {
            accountsArray = accounts.toArray(new IrcAccount[accounts.size()]);
        }
        return accountsArray;
    }

    /**
     * @see org.l2x6.eircc.core.model.IrcObject#getFields()
     */
    @Override
    public IrcModelField[] getFields() {
        return IrcModelField.values();
    }

    @Override
    protected File getSaveFile(File parentDir) {
        return new File(parentDir, "model.properties");
    }

    public boolean hasAccounts() {
        return !accounts.isEmpty();
    }

    public void load(File storageRoot) throws IOException {
        if (storageRoot.exists()) {
            for (File f : storageRoot.listFiles()) {
                String fileName = f.getName();
                if (f.isFile() && fileName.endsWith(IrcAccount.FILE_EXTENSION)) {
                    String bareName = fileName.substring(0, fileName.length() - IrcAccount.FILE_EXTENSION.length());
                    int minusPos = bareName.lastIndexOf('-');
                    if (minusPos >= 0) {
                        String uuidString = bareName.substring(0, minusPos);
                        UUID uuid = UUID.fromString(uuidString);
                        String label = bareName.substring(minusPos+1);
                        IrcAccount account = new IrcAccount(this, uuid, label);
                        account.load(f);
                        accounts.add(account);
                    }
                }
            }
        }
    }

    public IrcAccount proposeNextAccount() {
        String newLabel = IrcUiMessages.Account + "#" + (accounts.size() + 1);
        IrcAccount result = createAccount(newLabel);
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
            for (IrcAccount ircAccount : accounts) {
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
        List<IrcModelEventListener> newList = new ArrayList<IrcModelEventListener>(listeners);
        newList.remove(listener);
        listeners = newList;
    }

    public void save(File storageRoot) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        for (IrcAccount account : accounts) {
            account.save(storageRoot);
        }
    }

    /**
     * @param instance2
     */
    public void setTrafficLogFactory(TrafficLoggerFactory trafficLoggerFactory) {
        this.trafficLoggerFactory = trafficLoggerFactory;
    }

}
