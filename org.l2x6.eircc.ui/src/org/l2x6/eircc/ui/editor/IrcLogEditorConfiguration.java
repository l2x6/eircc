/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.editor;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcLogEditorConfiguration extends TextSourceViewerConfiguration {

    /**
     * @param preferenceStore
     */
    public IrcLogEditorConfiguration(IPreferenceStore preferenceStore) {
        super(preferenceStore);
    }

    /**
     * super{@link #getReconciler(ISourceViewer)} returns a spell checker
     * reconciler, which makes no sense here.
     *
     * @see org.eclipse.ui.editors.text.TextSourceViewerConfiguration#getReconciler(org.eclipse.jface.text.source.ISourceViewer)
     */
    @Override
    public IReconciler getReconciler(ISourceViewer sourceViewer) {
        return null;
    }

}
