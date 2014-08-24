/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.l2x6.eircc.ui.utils.ExtendedTextStyle;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcPreferences {
    private static final IrcPreferences INSTANCE = new IrcPreferences();

    public static IrcPreferences getInstance() {
        return INSTANCE;
    }

    private final ExtendedTextStyle messageTimeStyle;

    private final ExtendedTextStyle namedMeSenderStyle;

    private final ExtendedTextStyle systemMessageStyle;

    /**
     *
     */
    public IrcPreferences() {
        super();
        namedMeSenderStyle = new ExtendedTextStyle(SWT.BOLD);
        systemMessageStyle = new ExtendedTextStyle(Display.getDefault().getSystemColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
        messageTimeStyle = new ExtendedTextStyle(Display.getDefault().getSystemColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
    }
    public ExtendedTextStyle getMessageTimeStyle() {
        return messageTimeStyle;
    }

    /**
     * @return
     */
    public ExtendedTextStyle getNamedMeSenderStyle() {
        return namedMeSenderStyle;
    }

    public ExtendedTextStyle getSystemMessageStyle() {
        return systemMessageStyle;
    }

}
