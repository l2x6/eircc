/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model.resource;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcResourceException extends Exception {

    /**  */
    private static final long serialVersionUID = -7347382112816334709L;

    /**
     * @param message
     */
    public IrcResourceException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public IrcResourceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public IrcResourceException(Throwable cause) {
        super(cause);
    }

}
