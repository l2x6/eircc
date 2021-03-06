/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public interface IrcMessageReplacer {
    enum IrcMessageMatcherState {MATCH, CONTINUE, STOP};
    IrcMessageMatcherState match(IrcMessage message);
    IrcMessage createReplacementMessage(IrcMessage replacedMessage);
    IrcMessage createNewMessage(IrcLog log);
}
