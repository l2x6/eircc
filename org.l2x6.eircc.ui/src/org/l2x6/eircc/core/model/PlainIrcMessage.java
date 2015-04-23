/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.l2x6.eircc.core.util.IrcLogReader;
import org.l2x6.eircc.core.util.IrcToken;
import org.l2x6.eircc.core.util.IrcTokenizer;
import org.l2x6.eircc.ui.misc.Colors;
import org.schwering.irc.lib.util.IRCCommand;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class PlainIrcMessage {

    public enum IrcMessageType {CHAT, ERROR, NOTIFICATION, SYSTEM;
        private static final Map<String, IrcMessageType> FAST_LOOKUP;
        static {
            Map<String, IrcMessageType> fastLookUp = new HashMap<String, IrcMessageType>(64);
            IrcMessageType[] values = values();
            for (IrcMessageType value : values) {
                fastLookUp.put(value.name(), value);
            }
            FAST_LOOKUP = Collections.unmodifiableMap(fastLookUp);
        }
        /**
         * A {@link HashMap}-backed and {@code null}-tolerant alternative to
         * {@link #valueOf(String)}. The lookup is case-sensitive.
         *
         * @param command
         *            the command as a {@link String}
         * @return the {@link IRCCommand} that corresponds to the given string
         *         {@code command} or {@code null} if no such command exists
         */
        public static IrcMessageType fastValueOf(String command) {
            return FAST_LOOKUP.get(command);
        }
    };

    protected final OffsetDateTime arrivedAt;
    protected final boolean isP2pChannel;
    /**
     * Number of lines of this message. Basically
     * {@code 1 + numberOfEolsInMessageText}
     */
    protected final int lineCount;
    /**
     * This message starts at this line index in the IRC log file. First line
     * index is {@code 0}.
     */
    protected final int lineIndex;
    private boolean meNamed;
    protected final String myNick;
    /** Character length of this message within the IRC log file. */
    protected final int recordLenght;

    /** This message starts at this character offset in the IRC log file. */
    protected final int recordOffset;
    private final IrcUserBase sender;
    private String string;
    protected final String text;

    protected final int textOffset;
    private boolean tokenized = false;
    protected final IrcMessageType type;
    protected final int userColorIndex;
    /**
     * @param arrivedAt
     * @param nick
     * @param text
     * @param userColorIndex
     * @param myNick
     * @param isP2pChannel
     * @param recordOffset
     * @param type
     */
    public PlainIrcMessage(int recordOffset, int lineIndex, OffsetDateTime arrivedAt, IrcUserBase sender, String text,
            int userColorIndex, String myNick, boolean isP2pChannel, IrcMessageType type) {
        super();
        this.recordOffset = recordOffset;
        this.lineIndex = lineIndex;
        this.arrivedAt = arrivedAt.truncatedTo(ChronoUnit.SECONDS);
        this.sender = sender;
        this.text = text;
        this.userColorIndex = userColorIndex;
        this.myNick = myNick;
        this.isP2pChannel = isP2pChannel;
        this.type = type;
        this.lineCount = countLines();
        this.textOffset = computeTextOffset();
        this.recordLenght = computeRecordLength();
    }

    public void append(Appendable out) throws IOException {
        out.append(arrivedAt.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        out.append(IrcLogReader.FIELD_DELIMITER);
        if (sender != null) {
            sender.append(out);
        }
        out.append(IrcLogReader.FIELD_DELIMITER);
        IrcLogReader.append(text, out, IrcLogReader.FIELD_DELIMITER);
        out.append(String.valueOf(userColorIndex));
        out.append(IrcLogReader.FIELD_DELIMITER);
        out.append(myNick);
        out.append(IrcLogReader.FIELD_DELIMITER);
        out.append(type.name());
        out.append(IrcLogReader.RECORD_DELIMITER);
    }

    /**
     * @return
     */
    private int computeRecordLength() {
        return computeTextOffset() + text.length() + 1 + String.valueOf(userColorIndex).length() + 1 + myNick.length()
                + 1;
    }

    private int computeTextOffset() {
        return 25 + 1 + (sender == null ? 0 : sender.length()) + 1;
    }

    /**
     * @return
     */
    private int countLines() {
        int result = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                result++;
            }
        }
        return result;
    }

    public OffsetDateTime getArrivedAt() {
        return arrivedAt;
    }

    /**
     * @return
     */
    public int getLineCount() {
        return lineCount;
    }

    /**
     * @return
     */
    public int getLineIndex() {
        return lineIndex;
    }

    public String getMyNick() {
        return myNick;
    }

    public String getNick() {
        return sender != null ? sender.getNick() : null;
    }

    /**
     * Returns the character length of this whole message (incl. date, sender and flags) within the IRC log file.
     * @return the record length
     */
    public int getRecordLenght() {
        return recordLenght;
    }

    /**
     * Returns a character offset at which this whole message (incl. date, sender and flags) starts in the IRC log file.
     * @return the record offset
     */
    public int getRecordOffset() {
        return recordOffset;
    }

    public IrcUserBase getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }

    public int getTextOffset() {
        return textOffset;
    }

    public IrcMessageType getType() {
        return type;
    }

    public int getUserColorIndex() {
        return userColorIndex;
    }

    public boolean isFromMe() {
        return userColorIndex == Colors.MY_INDEX;
    }

    /**
     * @param nick
     * @return
     */
    public boolean isMeNamed() {
        if (type != IrcMessageType.CHAT) {
            return false;
        }
        if (isFromMe()) {
            return false;
        }
        if (isP2pChannel) {
            return true;
        }
        if (!tokenized) {
            tokenize();
        }
        return meNamed;
    }

    public boolean isSystemMessage() {
        return this.type == IrcMessageType.SYSTEM;
    }

    public IrcMessage toIrcMessage(IrcLog log) {
        IrcUser u = sender == null ? null : log.getChannel().getAccount().getServer().getOrCreateUser(sender.getNick(), sender.getUsername(), sender.getHost());
        return new IrcMessage(log, arrivedAt, u, text, myNick, isP2pChannel, type);
    }

    /**
     *
     */
    private void tokenize() {
        if (text.length() >= myNick.length()) {
            for (IrcToken token : new IrcTokenizer(text)) {
                if (token.equals(myNick)) {
                    meNamed = true;
                    break;
                }
            }
        }
        tokenized = true;
    }

    @Override
    public String toString() {
        if (string == null) {
            StringBuilder sb = new StringBuilder();
            try {
                append(sb);
            } catch (IOException e) {
                /*
                 * Should not happen as StringBuilder.append() never throws
                 * IOException
                 */
                throw new RuntimeException();
            }
            this.string = sb.toString();
        }
        return string;
    }

    public void write(IDocument document) {
        try {
            StringBuilder out = new StringBuilder();
            append(out);
            document.replace(document.getLength(), 0, out.toString());
        } catch (IOException e) {
            /*
             * Should not happen as StringBuilder.append() never throws
             * IOException
             */
            throw new RuntimeException(e);
        } catch (BadLocationException e) {
            /* Should not happen as we only insert at the very end an IDocument */
            throw new RuntimeException(e);
        }
    }

}
