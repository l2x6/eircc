/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.search;

import org.eclipse.jface.action.Action;
import org.l2x6.eircc.ui.search.IrcSearchLabelProvider.LabelOrder;

/**
 * Adapted from {@code org.eclipse.search.internal.ui.text.SortAction} as
 * available in org.eclipse.search 3.9.100.v20140226-1637.
 *
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class SortIrcSearchResultsAction extends Action {
    private IrcSearchResultPage fPage;
    private LabelOrder fSortOrder;

    public SortIrcSearchResultsAction(String label, IrcSearchResultPage page, LabelOrder sortOrder) {
        super(label);
        fPage = page;
        fSortOrder = sortOrder;
    }

    public LabelOrder getSortOrder() {
        return fSortOrder;
    }

    public void run() {
        fPage.setSortOrder(fSortOrder);
    }
}