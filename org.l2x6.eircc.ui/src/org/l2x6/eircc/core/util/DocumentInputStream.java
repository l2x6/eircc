/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.util;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class DocumentInputStream extends InputStream {
    private final IDocument document;
    private int offset = 0;

    /**
     * @param document
     */
    public DocumentInputStream(IDocument document) {
        super();
        this.document = document;
    }

    @Override
    public int read() throws IOException {
        if (offset < document.getLength()) {
            try {
                return document.getChar(offset++);
            } catch (BadLocationException e) {
                throw new IOException(e);
            }
        } else {
            return -1;
        }
    }
}
