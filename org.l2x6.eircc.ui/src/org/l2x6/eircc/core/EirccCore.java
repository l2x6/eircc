/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core;


/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class EirccCore {
    private static final EirccCore instance = new EirccCore();

    public static EirccCore getInstance() {
        return instance;
    }

    /**
     *
     */
    private EirccCore() {
        super();
    }

    public void dispose() {
    }

}
