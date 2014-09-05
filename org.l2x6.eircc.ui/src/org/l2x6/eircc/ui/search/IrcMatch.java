/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.search;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.Region;
import org.eclipse.search.ui.text.Match;

/**
 * Adapted from {@code org.eclipse.search.internal.ui.text.FileMatch} as
 * available in org.eclipse.search 3.9.100.v20140226-1637.
 *
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcMatch extends Match {
    private final IrcMessageMatches messageMatches;
    private final int offsetInMessageText;

    private Region originalLocation;

    public IrcMatch(IFile element) {
        super(element, -1, -1);
        this.offsetInMessageText = -1;
        this.messageMatches = null;
    }

    public IrcMatch(IFile element, IrcMessageMatches messageMatches, int offsetInMessageText, int length) {
        super(element, messageMatches.getMessage().getTextOffset() + offsetInMessageText, length);
        this.offsetInMessageText = offsetInMessageText;
        Assert.isLegal(messageMatches != null);
        this.messageMatches = messageMatches;
    }

    public IFile getFile() {
        return (IFile) getElement();
    }

    public IrcMessageMatches getMessageMatches() {
        return messageMatches;
    }

    public int getOffsetInMessageText() {
        return offsetInMessageText;
    }

    public int getOriginalLength() {
        if (originalLocation != null) {
            return originalLocation.getLength();
        }
        return getLength();
    }

    public int getOriginalOffset() {
        if (originalLocation != null) {
            return originalLocation.getOffset();
        }
        return getOffset();
    }

    public boolean isFileSearch() {
        return messageMatches == null;
    }

    public void setLength(int length) {
        if (originalLocation == null) {
            // remember the original location before changing it
            originalLocation = new Region(getOffset(), getLength());
        }
        super.setLength(length);
    }

    public void setOffset(int offset) {
        if (originalLocation == null) {
            // remember the original location before changing it
            originalLocation = new Region(getOffset(), getLength());
        }
        super.setOffset(offset);
    }
}
