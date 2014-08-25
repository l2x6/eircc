/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.prefs;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.l2x6.eircc.core.model.IrcMessage;
import org.l2x6.eircc.ui.editor.IrcDefaultMessageFormatter;
import org.l2x6.eircc.ui.editor.IrcSystemMessageFormatter;
import org.l2x6.eircc.ui.misc.Colors;
import org.l2x6.eircc.ui.misc.ExtendedTextStyle;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcPreferences {
    private static final IrcPreferences INSTANCE = new IrcPreferences();

    public static IrcPreferences getInstance() {
        return INSTANCE;
    }

    private final IrcDefaultMessageFormatter defaultFormatter = new IrcDefaultMessageFormatter(this);

    private final ExtendedTextStyle messageTimeStyle;

    private final IrcSystemMessageFormatter systemFormatter = new IrcSystemMessageFormatter(this);
    private final ExtendedTextStyle systemMessageStyle;

    private final IrcUserStyler[] userStylers;
    private final ExtendedTextStyle[] userStyles;
    private final ExtendedTextStyle[] userStylesNamingMe;

    /**
     *
     */
    public IrcPreferences() {
        super();
        systemMessageStyle = new ExtendedTextStyle(Display.getDefault().getSystemColor(
                SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
        messageTimeStyle = new ExtendedTextStyle(Display.getDefault().getSystemColor(
                SWT.COLOR_TITLE_INACTIVE_FOREGROUND));

        Colors colors = Colors.getInstance();
        ColorRegistry reg = JFaceResources.getColorRegistry();
        String keyPrefix = this.getClass().getName() + ".user#";
        userStyles = new ExtendedTextStyle[colors.getColorCount()];
        userStylesNamingMe = new ExtendedTextStyle[colors.getColorCount()];
        userStylers = new IrcUserStyler[colors.getColorCount()];
        for (int i = 0; i < userStyles.length; i++) {
            String key = keyPrefix + i;
            reg.put(key, colors.getRGB(i));
            Color c = reg.get(key);
            userStyles[i] = new ExtendedTextStyle(c);
            userStylesNamingMe[i] = new ExtendedTextStyle(c, SWT.BOLD);
            userStylers[i] = new IrcUserStyler(this, i);
        }

    }

    public void dispose() {

    }

    /**
     * @param m
     * @return
     */
    public IrcDefaultMessageFormatter getFormatter(IrcMessage m) {
        if (m.isSystemMessage()) {
            return systemFormatter;
        }
        return defaultFormatter;
    }

    public ExtendedTextStyle getMessageTimeStyle() {
        return messageTimeStyle;
    }

    public ExtendedTextStyle getSystemMessageStyle() {
        return systemMessageStyle;
    }

    public ExtendedTextStyle getUserStyle(int index, boolean namingMe) {
        index %= userStylesNamingMe.length;
        return namingMe ? userStylesNamingMe[index] : userStyles[index];
    }

    public IrcUserStyler getUserStyler(int index) {
        index %= userStylers.length;
        return userStylers[index];
    }

}
