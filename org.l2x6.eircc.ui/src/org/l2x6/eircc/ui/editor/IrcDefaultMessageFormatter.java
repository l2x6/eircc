/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.editor;

import java.time.format.DateTimeFormatter;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.l2x6.eircc.core.model.IrcMessage;
import org.l2x6.eircc.core.model.IrcUser;
import org.l2x6.eircc.ui.misc.ExtendedTextStyle;
import org.l2x6.eircc.ui.prefs.IrcPreferences;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcDefaultMessageFormatter {

    protected final IrcPreferences preferences;

    /**
     * @param preferences
     */
    public IrcDefaultMessageFormatter(IrcPreferences preferences) {
        super();
        this.preferences = preferences;
    }

    protected void append(StyledText target, String token, ExtendedTextStyle style) {
        if (token != null && token.length() > 0) {
            int offset = target.getCharCount();
            target.append(token);
            StyleRange range = style.createRange(offset, token.length());
            target.setStyleRange(range);
        }
    }

    /**
     * @param target
     */
    protected void appendSpace(StyledText target, IrcMessage message) {
        target.append(" ");
    }

    /**
     * @param target
     * @param message
     */
    protected void appendText(StyledText target, IrcMessage message) {
        target.append(message.getText());
    }

    /**
     * @param target
     * @param message
     */
    protected void appendTime(StyledText target, IrcMessage message) {
        append(target, message.getArrivedAt().format(DateTimeFormatter.ISO_LOCAL_TIME),
                preferences.getMessageTimeStyle());
    }

    protected void appendUser(StyledText target, IrcMessage message, IrcUser user, String suffix) {
        // append(target, user.getNick() + suffix,
        // preferences.getMessageTimeStyle());

        String nickToken = user.getNick() + suffix;
        int index = message.getLog().getChannel().getUserIndex(message.getUser().getNick());
        append(target, nickToken, preferences.getUserStyle(index, message.isMeNamed()));
    }

    protected void ensureInitialNewline(StyledText target, IrcMessage message) {
        if (target.getCharCount() > 0) {
            target.append("\n");
        }
    }

    public void format(StyledText target, IrcMessage message) {
        ensureInitialNewline(target, message);
        appendTime(target, message);

        appendSpace(target, message);
        appendUser(target, message, message.getUser(), ":");

        appendSpace(target, message);
        appendText(target, message);
    }

}
