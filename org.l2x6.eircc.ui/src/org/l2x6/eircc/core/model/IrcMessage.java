/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.io.IOException;
import java.io.PushbackReader;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
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
    private static final char MULTILINE_MARKER = ' ';
    private static final char RECORD_DELIMITER = '\n';
    private final OffsetDateTime arrivedAt;
    private final boolean historic;
    private final IrcLog log;
    private boolean meNamed;
    private final String text;
    private boolean tokenized = false;
    private final IrcUser user;
    private final int userColorIndex;

    /**
     * @param log
     * @param arrivedAt
     * @param user
     * @param text
     */
    public IrcMessage(IrcLog log, OffsetDateTime arrivedAt, IrcUser user, String text) {
        super();
        this.log = log;
        this.historic = false;
        this.arrivedAt = arrivedAt.truncatedTo(ChronoUnit.SECONDS);
        this.user = user;
        this.text = text;
        this.userColorIndex = user != null ? log.getChannel().getUserIndex(user.getNick()) : Colors.INVALID_INDEX;
    }

    public IrcMessage(IrcLog log, OffsetDateTime arrivedAt, String text) {
        this(log, arrivedAt, null, text);
    }

    public IrcMessage(IrcLog log, PushbackReader in) throws IOException {
        this.log = log;
        this.historic = true;
        String timeString = IrcUtils.read(in, FIELD_DELIMITER, MULTILINE_MARKER);
        this.arrivedAt = OffsetDateTime.parse(timeString, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        String nick = IrcUtils.read(in, FIELD_DELIMITER, MULTILINE_MARKER);
        this.user = nick == null || nick.isEmpty() ? null : new IrcHistoricUser(nick);
        String txt = IrcUtils.read(in, RECORD_DELIMITER, MULTILINE_MARKER);

        int lastDelim = txt.lastIndexOf(FIELD_DELIMITER);
        String index = txt.substring(lastDelim + 1);
        this.userColorIndex = Integer.parseInt(index);
        this.text = txt.substring(0, lastDelim);
    }

    public OffsetDateTime getArrivedAt() {
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

    public void write(IDocument document) {
        try {
            StringBuilder out = new StringBuilder();
            out.append(arrivedAt.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
            out.append(FIELD_DELIMITER);
            if (user != null) {
                out.append(user.getNick());
            }
            out.append(FIELD_DELIMITER);
            IrcUtils.append(text, out, FIELD_DELIMITER);
            out.append(String.valueOf(userColorIndex));
            out.append(RECORD_DELIMITER);

            document.replace(document.getLength(), 0, out.toString());
        } catch (IOException e) {
            /* Should not happen as StringBuilder.append() never throws IOException */
            throw new RuntimeException(e);
        } catch (BadLocationException e) {
            /* Should not happen as we only insert at the very end an IDocument */
            throw new RuntimeException(e);
        }
    }

}
