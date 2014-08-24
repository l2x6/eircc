/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import org.l2x6.eircc.core.util.IrcToken;
import org.l2x6.eircc.core.util.IrcTokenizer;
import org.l2x6.eircc.core.util.IrcUtils;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcMessage {
    private final IrcLog log;
    private boolean meNamed;
    private final long postedOn;
    private final String text;
    private boolean tokenized = false;
    private final IrcUser user;
    /**
     * @param log
     * @param postedOn
     * @param user
     * @param text
     */
    public IrcMessage(IrcLog log, long postedOn, IrcUser user, String text) {
        super();
        this.log = log;
        this.postedOn = postedOn;
        this.user = user;
        this.text = text;
    }
    public IrcMessage(IrcLog log, long postedOn, String text) {
        this(log, postedOn, null, text);
    }

    /**
     * @return
     */
    public IrcLog getLog() {
        return log;
    }
    public long getPostedOn() {
        return postedOn;
    }
    public String getText() {
        return text;
    }

    public IrcUser getUser() {
        return user;
    }

    public boolean isFromMe() {
        return log.getChannel().getAccount().getMe() == user;
    }

    /**
     * @param nick
     * @return
     */
    public boolean isMeNamed() {
        if (isSystemMessage()) {
            return false;
        }
        if (!isFromMe() && getLog().getChannel().isP2p()) {
            return true;
        }
        if (!tokenized) {
            tokenize();
        }
        return meNamed;
    }

    public boolean isSystemMessage() {
        return user == null;
    }

    /**
     *
     */
    private void tokenize() {
        String nick = log.getChannel().getAccount().getMe().getNick();
        if (text.length() >= nick.length()) {
            for (IrcToken token : new IrcTokenizer(text)) {
                if (token.equals(nick)) {
                    meNamed = true;
                    break;
                }
            }
        }
        tokenized = true;
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append(IrcUtils.toTimeString(postedOn)).append(' ');
        if (user != null) {
            sb.append(user.getNick()).append(": ");
        }
        sb.append(text);
        return sb.toString();
    }
}
