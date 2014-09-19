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
public class IrcChannelDocumentProvider extends ForwardingDocumentProvider {

    private static final TextFileDocumentProvider PARENT = new TextFileDocumentProvider();

    private static volatile IrcChannelDocumentProvider INSTANCE;

    public static IrcChannelDocumentProvider getInstance() {
        synchronized (IrcChannelDocumentProvider.class) {
            if (INSTANCE == null) {
                new IrcChannelDocumentProvider();
            }
            return INSTANCE;
        }
    }

    /**
     *
     */
    public IrcChannelDocumentProvider() {
        super(PARENT);
        INSTANCE = this;
    }

}
