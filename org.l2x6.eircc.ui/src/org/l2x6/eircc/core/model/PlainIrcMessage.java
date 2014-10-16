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
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.l2x6.eircc.core.util.IrcLogReader;
import org.l2x6.eircc.core.util.IrcLogReader.IrcLogChunk;
import org.l2x6.eircc.core.util.IrcLogReader.IrcLogReaderException;
import org.l2x6.eircc.core.util.IrcToken;
import org.l2x6.eircc.core.util.IrcTokenizer;
import org.l2x6.eircc.ui.misc.Colors;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class PlainIrcMessage {

    protected static final char FIELD_DELIMITER = ' ';
    protected static final char MULTILINE_MARKER = ' ';
    protected static final char RECORD_DELIMITER = '\n';
    /**  */
    public static final String SENDER_TEXT_DELIMITER = ": ";
    protected final OffsetDateTime arrivedAt;
    private final boolean isP2pChannel;
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
    private final String myNick;
    private final String nick;
    /** Character length of this message within the IRC log file */
    protected final int recordLenght;
    /** This message starts at this character offset in the IRC log file */
    protected final int recordOffset;
    private String string;
    protected final String text;
    protected final int textOffset;
    private boolean tokenized = false;
    protected final int userColorIndex;

    /**
     * @param arrivedAt
     * @param nick
     * @param text
     * @param userColorIndex
     * @param myNick
     * @param isP2pChannel
     * @param recordOffset
     */
    public PlainIrcMessage(int recordOffset, int lineIndex, OffsetDateTime arrivedAt, String nick, String text,
            int userColorIndex, String myNick, boolean isP2pChannel) {
        super();
        this.recordOffset = recordOffset;
        this.lineIndex = lineIndex;
        this.arrivedAt = arrivedAt.truncatedTo(ChronoUnit.SECONDS);
        this.nick = nick;
        this.text = text;
        this.userColorIndex = userColorIndex;
        this.myNick = myNick;
        this.isP2pChannel = isP2pChannel;
        this.lineCount = countLines();
        this.textOffset = computeTextOffset();
        this.recordLenght = computeRecordLength();
    }

    /**
     * @param in
     * @param myNick
     * @throws IOException
     */
    public PlainIrcMessage(IrcLogReader in, boolean isP2pChannel) throws IrcLogReaderException, IOException {
        this.recordOffset = in.getCharCount();
        this.lineIndex = in.getLineIndex();
        String timeString = in.readToken(FIELD_DELIMITER, MULTILINE_MARKER);
        try {
            this.arrivedAt = OffsetDateTime.parse(timeString, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new IrcLogReaderException("Could not parse " + in.getSource(), e);
        }
        String nick = in.readToken(FIELD_DELIMITER, MULTILINE_MARKER);
        this.nick = nick == null || nick.isEmpty() ? null : nick;
        IrcLogChunk txtChunk = in.readChunk(RECORD_DELIMITER, MULTILINE_MARKER);

        this.myNick = txtChunk.tail(FIELD_DELIMITER);
        String index = txtChunk.tail(FIELD_DELIMITER);
        this.userColorIndex = Integer.parseInt(index);
        this.text = txtChunk.rest();

        this.textOffset = computeTextOffset();
        this.recordLenght = computeRecordLength();
        this.lineCount = countLines();
        this.isP2pChannel = isP2pChannel;
    }

    /**
     * @return
     */
    private int computeRecordLength() {
        return computeTextOffset() + text.length() + 1 + String.valueOf(userColorIndex).length() + 1 + myNick.length()
                + 1;
    }

    private int computeTextOffset() {
        return 25 + 1 + (nick == null ? 0 : nick.length()) + 1;
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

    public String getNick() {
        return nick;
    }

    public int getRecordLenght() {
        return recordLenght;
    }

    public int getRecordOffset() {
        return recordOffset;
    }

    public String getText() {
        return text;
    }

    public int getTextOffset() {
        return textOffset;
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
        if (isSystemMessage()) {
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
        return nick == null;
    }

    public IrcMessage toIrcMessage(IrcLog log) {
        IrcUser u = nick == null ? null : log.getChannel().getAccount().getServer().getOrCreateUser(nick, nick);
        return new IrcMessage(log, arrivedAt, u, text, myNick, isP2pChannel);
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
            StringBuilder sb = new StringBuilder().append(arrivedAt.toString()).append(' ');
            if (nick != null) {
                sb.append(nick).append(SENDER_TEXT_DELIMITER);
            }
            sb.append(text);
            this.string = sb.toString();
        }
        return string;
    }

    public void write(IDocument document) {
        try {
            StringBuilder out = new StringBuilder();
            out.append(arrivedAt.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
            out.append(FIELD_DELIMITER);
            if (nick != null) {
                out.append(nick);
            }
            out.append(FIELD_DELIMITER);
            IrcLogReader.append(text, out, FIELD_DELIMITER);
            out.append(String.valueOf(userColorIndex));
            out.append(FIELD_DELIMITER);
            out.append(myNick);
            out.append(RECORD_DELIMITER);

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
