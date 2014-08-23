/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core;

import org.l2x6.eircc.core.model.IrcObject;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcException extends Exception {
    /**  */
    private static final long serialVersionUID = 3450160748176116426L;
    private final IrcObject modelObject;

    /**
     * @param message
     * @param modelObject
     */
    public IrcException(String message, IrcObject modelObject) {
        super(message);
        this.modelObject = modelObject;
    }
    /**
     * @param message
     * @param cause
     * @param modelObject
     */
    public IrcException(String message, Throwable cause, IrcObject modelObject) {
        super(message, cause);
        this.modelObject = modelObject;
    }
    public IrcObject getModelObject() {
        return modelObject;
    }



}
