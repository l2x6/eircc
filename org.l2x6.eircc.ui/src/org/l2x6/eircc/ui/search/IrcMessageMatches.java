/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.search;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;
import org.l2x6.eircc.core.model.PlainIrcMessage;

/**
 * Adapted from {@code org.eclipse.search.internal.ui.text.LineElement} as
 * available in org.eclipse.search 3.9.100.v20140226-1637.
 *
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcMessageMatches {

    private final IResource parent;
    private final List<IrcMatch> matches;
    private IrcMatch[] matchesArray;
    private final PlainIrcMessage message;

    public IrcMessageMatches(IResource parent, PlainIrcMessage message) {
        this.parent = parent;
        this.message = message;
        this.matches = new ArrayList<IrcMatch>();
    }

    public void add(IrcMatch match) {
        this.matches.add(match);
        this.matchesArray = null;
    }

    public String getContents() {
        return message.getText();
    }

    public int getLength() {
        return message.toString().length();
    }

    public int getLine() {
        return message.getLineIndex() + 1;
    }

    public IrcMatch[] getMatches() {
        if (matchesArray == null) {
            matchesArray = matches.toArray(new IrcMatch[matches.size()]);
        }
        return matchesArray;
    }

    public PlainIrcMessage getMessage() {
        return message;
    }

    public int getNumberOfMatches(AbstractTextSearchResult result) {
        int count = 0;
        Match[] matches = result.getMatches(parent);
        for (int i = 0; i < matches.length; i++) {
            IrcMatch curr = (IrcMatch) matches[i];
            if (curr.getMessageMatches() == this) {
                count++;
            }
        }
        return count;
    }

    public int getOffset() {
        return message.getRecordOffset();
    }

    public IResource getParent() {
        return parent;
    }

}