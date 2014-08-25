/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.prefs;

import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.TextStyle;
import org.l2x6.eircc.ui.misc.ExtendedTextStyle;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcUserStyler extends Styler {
    private final int index;
    private IrcPreferences preferences;

    /**
     * @param preferences
     * @param index
     */
    public IrcUserStyler(IrcPreferences preferences, int index) {
        super();
        this.preferences = preferences;
        this.index = index;
    }

    /**
     * @see org.eclipse.jface.viewers.StyledString.Styler#applyStyles(org.eclipse.swt.graphics.TextStyle)
     */
    @Override
    public void applyStyles(TextStyle targetStyle) {
        ExtendedTextStyle sourceStyle = preferences.getUserStyle(index, false);
        targetStyle.foreground = sourceStyle.foreground;
    }

}
