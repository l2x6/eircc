/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.util;

import java.io.IOException;
import java.io.Reader;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class DocumenReader extends Reader {
    private final IDocument document;
    private int offset = 0;

    /**
     * @param document
     */
    public DocumenReader(IDocument document) {
        super();
        this.document = document;
    }

    /**
     * @see java.io.Reader#close()
     */
    @Override
    public void close() throws IOException {
    }

    /**
     * @see java.io.Reader#read(char[], int, int)
     */
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (offset < document.getLength()) {
            try {
                int count = 0;
                while (offset < document.getLength() && count < len) {
                    cbuf[off + count] = document.getChar(offset++);
                    count++;
                }
                return count;
            } catch (BadLocationException e) {
                throw new IOException(e);
            }
        } else {
            return -1;
        }
    }
}
