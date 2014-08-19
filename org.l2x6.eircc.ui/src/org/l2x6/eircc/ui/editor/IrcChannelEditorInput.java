/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.editor;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.l2x6.eircc.core.model.IrcChannel;
import org.l2x6.eircc.core.model.IrcModel;
import org.l2x6.eircc.ui.IrcImages;
import org.l2x6.eircc.ui.views.IrcLabelProvider;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcChannelEditorInput implements IEditorInput, IPersistableElement {
    public enum IrcChannelEditorInputField {CHANNEL_NAME, ACCOUNT_LABEL};

    private final IrcChannel channel;

    /**
     * @param channel
     */
    public IrcChannelEditorInput(IrcChannel channel) {
        super();
        this.channel = channel;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof IrcChannelEditorInput) {
            return ((IrcChannelEditorInput) other).channel.equals(channel);
        }
        return false;
    }

    /**
     * @see org.eclipse.ui.IEditorInput#exists()
     */
    @Override
    public boolean exists() {
        return channel != null && channel.getAccount().getModel() == IrcModel.getInstance();
    }

    /**
     * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
        return null;
    }

    public IrcChannel getChannel() {
        return channel;
    }

    /**
     * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return IrcImages.getInstance().getImageDescriptor(channel);
    }

    /**
     * @see org.eclipse.ui.IEditorInput#getName()
     */
    @Override
    public String getName() {
        return channel.getName();
    }

    /**
     * @see org.eclipse.ui.IEditorInput#getPersistable()
     */
    @Override
    public IPersistableElement getPersistable() {
        return this;
    }

    /**
     * @see org.eclipse.ui.IEditorInput#getToolTipText()
     */
    @Override
    public String getToolTipText() {
        return IrcLabelProvider.getInstance().getTooltipText(channel);
    }

    @Override
    public int hashCode() {
        return channel.hashCode();
    }

    /**
     * @see org.eclipse.ui.IPersistable#saveState(org.eclipse.ui.IMemento)
     */
    @Override
    public void saveState(IMemento memento) {
        memento.putString(IrcChannelEditorInputField.ACCOUNT_LABEL.name(), channel.getAccount().getLabel());
        memento.putString(IrcChannelEditorInputField.CHANNEL_NAME.name(), channel.getName());
    }

    /**
     * @see org.eclipse.ui.IPersistableElement#getFactoryId()
     */
    @Override
    public String getFactoryId() {
        return IrcChannelElementFactory.ID;
    }

}
