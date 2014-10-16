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
import org.eclipse.jface.text.ISynchronizable;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class DocumenReader extends Reader {
    private final IDocument document;
    private final Object lock;
    private int offset = 0;

    /**
     * @param document
     */
    public DocumenReader(IDocument document) {
        super();
        this.document = document;

        if (document instanceof ISynchronizable) {
            Object l = ((ISynchronizable) document).getLockObject();
            if (l == null) {
                l = new Object();
                ((ISynchronizable) document).setLockObject(l);
            }
            this.lock = l;
        } else {
            this.lock = new Object();
        }

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
        synchronized (lock) {
            IrcUtils.assertUiThread();
            try {
                int count = 0;
                while (count < len) {
                    System.out.println("DocumenReader.read() docLen = " + document.getLength() + " offset = " + offset);
                    char ch = document.getChar(offset++);
                    System.out.println("DocumenReader.read() ch = " + ch);
                    cbuf[off + count] = ch;
                    count++;
                }
                return count;
            } catch (BadLocationException e) {
                return -1;
            }
        }
    }

}
