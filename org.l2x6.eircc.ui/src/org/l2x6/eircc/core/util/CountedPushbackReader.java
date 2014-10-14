/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.util;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class CountedPushbackReader extends PushbackReader {
    private static final int DECREMENT = -1;
    private static final int INCREMENT = 1;
    private int charCount = 0;
    private int lineIndex = 0;

    /**
     * @param in
     */
    public CountedPushbackReader(Reader in) {
        super(in);
    }

    /**
     * @param in
     * @param size
     */
    public CountedPushbackReader(Reader in, int size) {
        super(in, size);
    }

    /**
     * @param result
     */
    private void checkLine(int result, int operation) {
        if (result == '\n') {
            lineIndex += operation;
        }
    }

    public int getCharCount() {
        return charCount;
    }

    /**
     * @return
     */
    public int getLineIndex() {
        return lineIndex;
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        throw new IOException("mark/reset not supported");
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int read() throws IOException {
        charCount++;
        int result = super.read();
        checkLine(result, INCREMENT);
        String s = result >= 0 ? String.valueOf((char) result) : String.valueOf(result);
        System.out.println("CountedPushbackReader.read() = "+ s);
        return result;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int result = super.read(cbuf, off, len);
        for (int i = off; i < off + result; i++) {
            checkLine(cbuf[i], INCREMENT);
        }
        charCount += result;
        return result;
    }

    @Override
    public void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    @Override
    public long skip(long n) throws IOException {
        throw new IOException("skip not supported");
    }

    @Override
    public void unread(char[] cbuf, int off, int len) throws IOException {
        charCount -= len;
        for (int i = off; i < off + len; i++) {
            checkLine(cbuf[i], DECREMENT);
        }
        super.unread(cbuf, off, len);
    }

    @Override
    public void unread(int c) throws IOException {
        String s = c >= 0 ? String.valueOf((char) c) : String.valueOf(c);
        System.out.println("CountedPushbackReader.unread() = "+ s);
        charCount--;
        checkLine(c, DECREMENT);
        super.unread(c);
    }

}
