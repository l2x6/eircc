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
import org.l2x6.eircc.ui.search.IrcSearchLabelProvider;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcSearchMessageFormatter extends IrcDefaultMessageFormatter {
    /**
     * @param preferences
     */
    public IrcSearchMessageFormatter(IrcPreferences preferences) {
        super(preferences);
    }

    @Override
    protected void appendTab(StyledWrapper target, PlainIrcMessage message) {
        appendSpace(target, message);
    }

    /**
     * Do not append the text we'll do it in the {@link IrcSearchLabelProvider}.
     *
     * @see org.l2x6.eircc.ui.editor.IrcDefaultMessageFormatter#appendText(org.l2x6.eircc.ui.misc.StyledWrapper,
     *      org.l2x6.eircc.core.model.PlainIrcMessage)
     */
    @Override
    protected void appendText(StyledWrapper target, PlainIrcMessage message) {
    }

}
