/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.editor;

import org.eclipse.swt.custom.StyledText;
import org.l2x6.eircc.core.model.IrcMessage;
import org.l2x6.eircc.core.model.IrcUser;
import org.l2x6.eircc.ui.prefs.IrcPreferences;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcOwnMessageFormatter extends IrcDefaultMessageFormatter {

    /**
     * @param preferences
     */
    public IrcOwnMessageFormatter(IrcPreferences preferences) {
        super(preferences);
    }

    protected void appendUser(StyledText target, IrcMessage message, IrcUser user, String suffix) {
        String nickToken = user.getNick() + suffix;
        append(target, nickToken, preferences.getUserStyle(0, true));
    }

}
