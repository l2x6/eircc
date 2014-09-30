/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.editor;

import org.eclipse.ui.editors.text.TextFileDocumentProvider;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcDocumentProvider extends ForwardingDocumentProvider {
    public static class InternalIrcDocumentProvider extends TextFileDocumentProvider {

    }

    private static volatile IrcDocumentProvider INSTANCE;

    private static final TextFileDocumentProvider PARENT = new InternalIrcDocumentProvider();

    public static IrcDocumentProvider getInstance() {
        synchronized (IrcDocumentProvider.class) {
            if (INSTANCE == null) {
                new IrcDocumentProvider();
            }
            return INSTANCE;
        }
    }

    /**
     *
     */
    public IrcDocumentProvider() {
        super(INSTANCE != null ? INSTANCE : PARENT);
        if (INSTANCE == null) {
            INSTANCE = this;
        }
    }
}
