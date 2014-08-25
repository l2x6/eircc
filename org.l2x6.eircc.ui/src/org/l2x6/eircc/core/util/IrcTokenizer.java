/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.util;

import java.util.Iterator;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcTokenizer implements Iterable<IrcToken> {
    private class IrcTokenIterator implements Iterator<IrcToken> {
        private int offset = 0;

        private void consumeDelimiters() {
            while (offset < inputLenght) {
                if (!isDelimiter(input.charAt(offset))) {
                    return;
                }
                offset++;
            }
        }

        /**
         *
         */
        private void consumeToken() {
            while (offset < inputLenght) {
                if (isDelimiter(input.charAt(offset))) {
                    return;
                }
                offset++;
            }
        }

        /**
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            consumeDelimiters();
            return offset < inputLenght;
        }

        private boolean isDelimiter(char ch) {
            return ch != '_' && !Character.isLetterOrDigit(ch);
        }

        /**
         * @see java.util.Iterator#next()
         */
        @Override
        public IrcToken next() {
            if (!hasNext()) {
                throw new IllegalStateException();
            }
            int tokenStart = offset;
            consumeToken();
            return new IrcToken(input, tokenStart, offset - tokenStart);
        }

        /**
         * @see java.util.Iterator#remove()
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    private final CharSequence input;
    private int inputLenght;

    /**
     * @param input
     */
    public IrcTokenizer(CharSequence input) {
        super();
        this.input = input;
        this.inputLenght = input.length();
    }

    /**
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<org.l2x6.eircc.core.util.IrcToken> iterator() {
        return new IrcTokenIterator();
    }

}
