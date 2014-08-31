/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class ReadableByteArrayOutputStream extends ByteArrayOutputStream {

    /**
     *
     */
    public ReadableByteArrayOutputStream() {
        super();
    }

    /**
     * @param size
     */
    public ReadableByteArrayOutputStream(int size) {
        super(size);
    }

    public InputStream createInputStream() {
        return new ByteArrayInputStream(buf);
    }
}
