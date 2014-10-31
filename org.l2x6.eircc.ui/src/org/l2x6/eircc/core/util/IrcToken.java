/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.util;

public class IrcToken implements CharSequence {
    private int hash;

    private final CharSequence input;
    private final int length;
    private final int offset;

    /**
     * @param input
     * @param offset
     * @param length
     */
    public IrcToken(CharSequence input, int offset, int length) {
        super();
        this.input = input;
        this.offset = offset;
        this.length = length;
    }

    /**
     * @see java.lang.CharSequence#charAt(int)
     */
    @Override
    public char charAt(int index) {
        return input.charAt(offset + index);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CharSequence) {
            CharSequence other = (CharSequence) o;
            if (other.length() != this.length) {
                return false;
            } else {
                for (int i = 0; i < length; i++) {
                    if (this.charAt(i) != other.charAt(i)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0 && length > 0) {
            for (int i = 0; i < length; i++) {
                h = 31 * h + charAt(i);
            }
            hash = h;
        }
        return h;
    }

    /**
     * @see java.lang.CharSequence#length()
     */
    @Override
    public int length() {
        return length;
    }

    /**
     * @see java.lang.CharSequence#subSequence(int, int)
     */
    @Override
    public CharSequence subSequence(int start, int end) {
        if (end > length) {
            throw new IllegalArgumentException("end > length");
        }
        return input.subSequence(start + offset, end + offset);
    }

    @Override
    public String toString() {
        return new StringBuilder(this).toString();
    }

    public int indexOf(int start, char ch) {
        for (int i = start; i < length; i++) {
            char c = charAt(i);
            if (c == ch) {
                return i;
            }
        }
        return -1;
    }

}