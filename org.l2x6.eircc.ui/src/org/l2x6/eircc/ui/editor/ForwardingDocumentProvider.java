/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.editor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.editors.text.IStorageDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProviderExtension;
import org.eclipse.ui.texteditor.IDocumentProviderExtension2;
import org.eclipse.ui.texteditor.IDocumentProviderExtension3;
import org.eclipse.ui.texteditor.IDocumentProviderExtension4;
import org.eclipse.ui.texteditor.IDocumentProviderExtension5;
import org.eclipse.ui.texteditor.IElementStateListener;

public class ForwardingDocumentProvider implements IDocumentProvider, IDocumentProviderExtension,
        IDocumentProviderExtension2, IDocumentProviderExtension3, IDocumentProviderExtension5,
        IStorageDocumentProvider, IDocumentProviderExtension4 {
    private final IDocumentProvider delegate;

    private final IDocumentProviderExtension delegate1;
    private final IDocumentProviderExtension2 delegate2;
    private final IDocumentProviderExtension3 delegate3;
    private final IDocumentProviderExtension4 delegate4;
    private final IDocumentProviderExtension5 delegate5;
    private final IStorageDocumentProvider storageDelegate;

    /**
     * @param delegate
     * @param delegate1
     * @param delegate2
     * @param delegate3
     * @param delegate4
     * @param delegate5
     * @param storageDelegate
     */
    public ForwardingDocumentProvider(IDocumentProvider delegate) {
        super();
        this.delegate = delegate;
        this.delegate1 = (IDocumentProviderExtension) delegate;
        this.delegate2 = (IDocumentProviderExtension2) delegate;
        this.delegate3 = (IDocumentProviderExtension3) delegate;
        this.delegate4 = (IDocumentProviderExtension4) delegate;
        this.delegate5 = (IDocumentProviderExtension5) delegate;
        this.storageDelegate = (IStorageDocumentProvider) delegate;
    }

    public void aboutToChange(Object element) {
        delegate.aboutToChange(element);
    }

    public void addElementStateListener(IElementStateListener listener) {
        delegate.addElementStateListener(listener);
    }

    public boolean canSaveDocument(Object element) {
        return delegate.canSaveDocument(element);
    }

    public void changed(Object element) {
        delegate.changed(element);
    }

    public void connect(Object element) throws CoreException {
        delegate.connect(element);
    }

    public void disconnect(Object element) {
        delegate.disconnect(element);
    }

    public IAnnotationModel getAnnotationModel(Object element) {
        return delegate.getAnnotationModel(element);
    }

    public IContentType getContentType(Object element) throws CoreException {
        return delegate4.getContentType(element);
    }

    public String getDefaultEncoding() {
        return storageDelegate.getDefaultEncoding();
    }

    public IDocument getDocument(Object element) {
        return delegate.getDocument(element);
    }

    public String getEncoding(Object element) {
        return storageDelegate.getEncoding(element);
    }

    public long getModificationStamp(Object element) {
        return delegate.getModificationStamp(element);
    }

    public IProgressMonitor getProgressMonitor() {
        return delegate2.getProgressMonitor();
    }

    public IStatus getStatus(Object element) {
        return delegate1.getStatus(element);
    }

    public long getSynchronizationStamp(Object element) {
        return delegate.getSynchronizationStamp(element);
    }

    public boolean isDeleted(Object element) {
        return delegate.isDeleted(element);
    }

    public boolean isModifiable(Object element) {
        return delegate1.isModifiable(element);
    }

    public boolean isNotSynchronizedException(Object element, CoreException ex) {
        return delegate5.isNotSynchronizedException(element, ex);
    }

    public boolean isReadOnly(Object element) {
        return delegate1.isReadOnly(element);
    }

    public boolean isStateValidated(Object element) {
        return delegate1.isStateValidated(element);
    }

    public boolean isSynchronized(Object element) {
        return delegate3.isSynchronized(element);
    }

    public boolean mustSaveDocument(Object element) {
        return delegate.mustSaveDocument(element);
    }

    public void removeElementStateListener(IElementStateListener listener) {
        delegate.removeElementStateListener(listener);
    }

    public void resetDocument(Object element) throws CoreException {
        delegate.resetDocument(element);
    }

    public void saveDocument(IProgressMonitor monitor, Object element, IDocument document, boolean overwrite)
            throws CoreException {
        delegate.saveDocument(monitor, element, document, overwrite);
    }

    public void setCanSaveDocument(Object element) {
        delegate1.setCanSaveDocument(element);
    }

    public void setEncoding(Object element, String encoding) {
        storageDelegate.setEncoding(element, encoding);
    }

    public void setProgressMonitor(IProgressMonitor progressMonitor) {
        delegate2.setProgressMonitor(progressMonitor);
    }

    public void synchronize(Object element) throws CoreException {
        delegate1.synchronize(element);
    }

    public void updateStateCache(Object element) throws CoreException {
        delegate1.updateStateCache(element);
    }

    public void validateState(Object element, Object computationContext) throws CoreException {
        delegate1.validateState(element, computationContext);
    }

}