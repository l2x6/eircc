/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.io.IOException;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public interface IrcUserBase {

    String getUsername();

    String getNick();

    String getHost();

    default void append(Appendable out) throws IOException {
        out.append(getNick()).append('!').append(getUsername()).append('@').append(getHost());
    }

    default int length() {
        return getLength(getNick()) + 1 + getLength(getUsername()) + 1 + getLength(getHost());
    }

    default int getLength(String string) {
        return string == null ? 0 : string.length();
    }

}
