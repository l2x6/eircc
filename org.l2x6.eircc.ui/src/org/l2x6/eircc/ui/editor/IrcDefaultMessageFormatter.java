/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.editor;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.StyledString;
import org.l2x6.eircc.core.model.PlainIrcMessage;
import org.l2x6.eircc.ui.misc.StyledWrapper;
import org.l2x6.eircc.ui.misc.StyledWrapper.TextViewerWrapper;
import org.l2x6.eircc.ui.prefs.IrcPreferences;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcDefaultMessageFormatter {
    public enum TimeStyle {

        DATE_TIME(new DateTimeFormatterBuilder().appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD).appendLiteral('-')
                .appendValue(MONTH_OF_YEAR, 2).appendLiteral('-').appendValue(DAY_OF_MONTH, 2).appendLiteral(' ')
                .appendValue(HOUR_OF_DAY, 2).appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2).optionalStart()
                .appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2).toFormatter()), //

        TIME(new DateTimeFormatterBuilder().appendValue(HOUR_OF_DAY, 2).appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2).optionalStart().appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2)
                .toFormatter());

        private final int characterLength;
        private final DateTimeFormatter formatter;

        /**
         * @param formatter
         */
        private TimeStyle(DateTimeFormatter formatter) {
            this.formatter = formatter;
            this.characterLength = formatter.format(A_DATE).length();
        }

        public int getCharacterLength() {
            return characterLength;
        }

        public DateTimeFormatter getFormatter() {
            return formatter;
        }

    }

    public static final OffsetDateTime A_DATE = OffsetDateTime.of(2014, 12, 30, 23, 59, 59, 0, ZoneOffset.ofHours(10));

    protected final IrcPreferences preferences;

    /**
     * @param preferences
     */
    public IrcDefaultMessageFormatter(IrcPreferences preferences) {
        super();
        this.preferences = preferences;
    }

    protected int append(TextViewer target, String token) {
        IDocument doc = target.getDocument();
        int offset = doc.getLength();
        try {
            doc.replace(offset, 0, token);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
        return offset;
    }

    //
    // protected void append(TextViewer target, String token, ExtendedTextStyle
    // style) {
    // if (token != null && token.length() > 0) {
    // int offset = append(target, token);
    // StyleRange range = style.createRange(offset, token.length());
    // target.getTextWidget().setStyleRange(range);
    // }
    // }

    /**
     * @param target
     */
    protected void appendSpace(StyledWrapper target, PlainIrcMessage message) {
        target.append(" ");
    }

    /**
     * @param target
     * @param message
     */
    protected void appendTab(StyledWrapper target, PlainIrcMessage message) {
        target.append("\t");
    }

    /**
     * @param target
     * @param message
     */
    protected void appendText(StyledWrapper target, PlainIrcMessage message) {
        target.append(message.getText());
    }

    /**
     * @param target
     * @param message
     * @param timeStyle
     */
    protected void appendTime(StyledWrapper target, PlainIrcMessage message, TimeStyle timeStyle) {
        target.append(message.getArrivedAt().format(timeStyle.getFormatter()), preferences.getMessageTimeStyle());
    }

    protected void appendUser(StyledWrapper target, PlainIrcMessage message, String suffix) {
        // append(target, user.getNick() + suffix,
        // preferences.getMessageTimeStyle());

        String nick = message.getNick();
        if (nick != null) {
            String nickToken = nick + suffix;
            int index = message.getUserColorIndex();
            target.append(nickToken, preferences.getUserStyle(index, message.isMeNamed()));
        }
    }

    protected void ensureInitialNewline(StyledWrapper target, PlainIrcMessage message) {
        if (target.getLength() > 0) {
            target.append("\n");
        }
    };

    public StyledString format(PlainIrcMessage message, TimeStyle timeStyle) {
        StyledString result = new StyledString();
        format(new StyledWrapper.StyledStringWrapper(result), message, timeStyle);
        return result;
    }

    protected void format(StyledWrapper wrapper, PlainIrcMessage message, TimeStyle timeStyle) {
        ensureInitialNewline(wrapper, message);
        appendTime(wrapper, message, timeStyle);

        appendTab(wrapper, message);
        appendUser(wrapper, message, ":");

        appendSpace(wrapper, message);
        appendText(wrapper, message);
    }

    public void format(TextViewer target, PlainIrcMessage message) {
        TextViewerWrapper wrapper = new TextViewerWrapper(target);
        format(wrapper, message, TimeStyle.TIME);
    }

}
