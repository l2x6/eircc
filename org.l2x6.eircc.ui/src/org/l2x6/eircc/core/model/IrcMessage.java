/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.l2x6.eircc.core.model.IrcUser.IrcHistoricUser;
import org.l2x6.eircc.core.util.IrcToken;
import org.l2x6.eircc.core.util.IrcTokenizer;
import org.l2x6.eircc.core.util.IrcUtils;
import org.l2x6.eircc.ui.misc.Colors;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcMessage {
    private static final char FIELD_DELIMITER = ' ';
    private static final char RECORD_DELIMITER = '\n';
    private final ZonedDateTime arrivedAt;
    private final boolean historic;
    private final IrcLog log;
    private boolean meNamed;
    private final String text;
    private boolean tokenized = false;
    private final IrcUser user;
    private final int userColorIndex;

    public IrcMessage(IrcLog log, Reader in) throws IOException {
        this.log = log;
        this.historic = true;
        String timeString = IrcUtils.read(in, FIELD_DELIMITER);
        this.arrivedAt = ZonedDateTime.parse(timeString, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        String index = IrcUtils.read(in, FIELD_DELIMITER);
        this.userColorIndex = Integer.parseInt(index);
        String nick = IrcUtils.read(in, FIELD_DELIMITER);
        this.user = nick == null || nick.isEmpty() ? null : new IrcHistoricUser(nick);
        this.text = IrcUtils.read(in, RECORD_DELIMITER);
    }

    /**
     * @param log
     * @param arrivedAt
     * @param user
     * @param text
     */
    public IrcMessage(IrcLog log, ZonedDateTime arrivedAt, IrcUser user, String text) {
        super();
        this.log = log;
        this.historic = false;
        this.arrivedAt = arrivedAt.truncatedTo(ChronoUnit.SECONDS);
        this.user = user;
        this.text = text;
        this.userColorIndex = user != null ? log.getChannel().getUserIndex(user.getNick()) : Colors.INVALID_INDEX;
    }

    public IrcMessage(IrcLog log, ZonedDateTime arrivedAt, String text) {
        this(log, arrivedAt, null, text);
    }

    public ZonedDateTime getArrivedAt() {
        return arrivedAt;
    }

    /**
     * @return
     */
    public IrcLog getLog() {
        return log;
    }

    public String getText() {
        return text;
    }

    public IrcUser getUser() {
        return user;
    }

    public int getUserColorIndex() {
        return userColorIndex;
    }

    public boolean isFromMe() {
        return userColorIndex == Colors.MY_INDEX;
    }

    public boolean isHistoric() {
        return historic;
    }

    /**
     * @param nick
     * @return
     */
    public boolean isMeNamed() {
        if (isSystemMessage()) {
            return false;
        }
        if (isFromMe()) {
            return false;
        }
        if (getLog().getChannel().isP2p()) {
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
        StringBuilder sb = new StringBuilder().append(arrivedAt.toString()).append(' ');
        if (user != null) {
            sb.append(user.getNick()).append(": ");
        }
        sb.append(text);
        return sb.toString();
    }

    public void write(Writer out) throws IOException {
        out.write(arrivedAt.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        out.write(FIELD_DELIMITER);
        out.write(String.valueOf(userColorIndex));
        out.write(FIELD_DELIMITER);
        if (user != null) {
            out.write(user.getNick());
        }
        out.write(FIELD_DELIMITER);
        IrcUtils.write(text, out, RECORD_DELIMITER);
    }

}
