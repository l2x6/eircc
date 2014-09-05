/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.editor;

import org.l2x6.eircc.core.model.PlainIrcMessage;
import org.l2x6.eircc.ui.misc.StyledWrapper;
import org.l2x6.eircc.ui.prefs.IrcPreferences;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcSystemMessageFormatter extends IrcDefaultMessageFormatter {

    /**
     * @param preferences
     */
    public IrcSystemMessageFormatter(IrcPreferences preferences) {
        super(preferences);
    }

    @Override
    public void format(StyledWrapper target, PlainIrcMessage message, TimeStyle timeStyle) {
        ensureInitialNewline(target, message);
        appendTime(target, message, timeStyle);
        appendTab(target, message);
        target.append(message.getText(), preferences.getSystemMessageStyle());
    }

}
