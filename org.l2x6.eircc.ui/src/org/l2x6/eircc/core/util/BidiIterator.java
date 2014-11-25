/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.util;

import java.util.ListIterator;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class BidiIterator<E> implements ListIterator<E> {
    private final ListIterator<E> delegate;

    public BidiIterator(ListIterator<E> delegate) {
        this.delegate = delegate;
    }

    public void add(E e) {
        throw new UnsupportedOperationException();
    }

    public boolean hasNext() {
        return delegate.hasNext();
    }

    public boolean hasNext(int direction) {
        return direction > 0 ? hasNext() : hasPrevious();
    }

    public boolean hasPrevious() {
        return delegate.hasPrevious();
    }

    public E next() {
        return delegate.next();
    }

    public E next(int direction) {
        return direction > 0 ? next() : previous();
    }

    public int nextIndex() {
        return delegate.nextIndex();
    }

    /**
     * @param direction
     * @return
     */
    public int nextIndex(int direction) {
        return direction > 0 ? nextIndex() : previousIndex();
    }

    public E previous() {
        return delegate.previous();
    }

    public int previousIndex() {
        return delegate.previousIndex();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public void set(E e) {
        throw new UnsupportedOperationException();
    }
}