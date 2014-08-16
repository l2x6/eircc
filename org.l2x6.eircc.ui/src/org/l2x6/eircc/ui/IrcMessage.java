/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui;

import org.l2x6.eircc.core.model.IrcUser;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcMessage {
    private final long postedOn;
    private final String text;
    private final IrcUser user;
    /**
     * @param postedOn
     * @param user
     * @param text
     */
    public IrcMessage(long postedOn, IrcUser user, String text) {
        super();
        this.postedOn = postedOn;
        this.user = user;
        this.text = text;
    }
    public long getPostedOn() {
        return postedOn;
    }
    public String getText() {
        return text;
    }
    public IrcUser getUser() {
        return user;
    }
}
