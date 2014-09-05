/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.editor;

import org.l2x6.eircc.core.model.IrcUser;
import org.l2x6.eircc.core.model.PlainIrcMessage;
import org.l2x6.eircc.ui.misc.StyledWrapper;
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

    protected void appendUser(StyledWrapper target, PlainIrcMessage message, IrcUser user, String suffix) {
        String nickToken = user.getNick() + suffix;
        target.append(nickToken, preferences.getUserStyle(0, true));
    }

}
