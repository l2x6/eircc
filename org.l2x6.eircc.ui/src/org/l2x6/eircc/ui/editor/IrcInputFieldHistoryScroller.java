/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.editor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.l2x6.eircc.core.model.IrcLog;
import org.l2x6.eircc.core.model.IrcMessage;
import org.l2x6.eircc.core.util.BidiIterator;
import org.l2x6.eircc.ui.EirccUi;

public class IrcInputFieldHistoryScroller {

    private static class IrcScrollState {
        private final int inputFieldLength;
        private final int inputFieldStart;
        private final int messageIndex;

        /**
         * @param messageIndex
         * @param inputFieldStart
         * @param inputFieldLength
         */
        public IrcScrollState(int messageIndex, int inputFieldStart, int inputFieldLength) {
            super();
            this.messageIndex = messageIndex;
            this.inputFieldStart = inputFieldStart;
            this.inputFieldLength = inputFieldLength;
        }

    }

    private final IrcEditor editor;

    private final IrcInputField inputField;
    private IrcScrollState state;

    /**
     * @param editor
     * @param inputField
     */
    public IrcInputFieldHistoryScroller(IrcEditor editor, IrcInputField inputField) {
        super();
        this.editor = editor;
        this.inputField = inputField;
    }

    /**
     *
     */
    public void reset() {
        state = null;
    }

    public void scroll(int direction) {
        IrcScrollState oldState = state;

        IrcLog log = editor.getChannel().getLog();
        int oldIndex = oldState != null ? oldState.messageIndex : log.getMessageCount();

        BidiIterator<IrcMessage> it = log.listIterator(oldIndex);
        if (direction > 0 && it.hasNext(direction)) {
            /* necessary when going forward */
            it.next(direction);
        }
        IDocument doc = inputField.getDocument();
        while (it.hasNext(direction)) {
            int index = it.nextIndex(direction);
            IrcMessage m = it.next(direction);
            if (m.isFromMe() || m.getRawInput() != null) {
                int newStart = oldState != null ? oldState.inputFieldStart : doc.getLength();
                int replacementLength = oldState != null ? oldState.inputFieldLength : 0;
                String append = m.getRawInput() != null ? m.getRawInput() : m.getText();
                if (doc.getLength() - replacementLength > 0) {
                    append = " "+ append;
                }
                this.state = new IrcScrollState(index, newStart, append.length());
                try {
                    doc.replace(newStart, replacementLength, append);
                    inputField.setCaretOffset(doc.getLength());

                } catch (BadLocationException e) {
                    EirccUi.log(e);
                }
                return;
            }
        }

        if (direction > 0 && oldState != null) {
            /* let's clean the input when we are behind the last message from the history */
            try {
                doc.replace(oldState.inputFieldStart, oldState.inputFieldLength, "");
                inputField.setCaretOffset(doc.getLength());
            } catch (BadLocationException e) {
                EirccUi.log(e);
            }
            reset();
        }

    }


}