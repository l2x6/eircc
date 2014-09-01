/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.core.model.IrcChannelUser;
import org.l2x6.eircc.core.util.IrcConstants;
import org.l2x6.eircc.ui.EirccUi;
import org.schwering.irc.lib.IRCCommand;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcContentAssistProcessor implements IContentAssistProcessor {
    private static final class Proposal implements ICompletionProposal, ICompletionProposalExtension,
            ICompletionProposalExtension2, ICompletionProposalExtension3, ICompletionProposalExtension4 {

        private final int fOffset;
        private final String fPrefix;
        private final String fString;

        public Proposal(String string, String prefix, int offset) {
            fString = string;
            fPrefix = prefix;
            fOffset = offset;
        }

        public void apply(IDocument document) {
            apply(null, '\0', 0, fOffset);
        }

        public void apply(IDocument document, char trigger, int offset) {
            try {
                // String replacement = fString.substring(offset - fOffset);
                int prefixLen = fPrefix.length();
                document.replace(offset - prefixLen, prefixLen, fString);
            } catch (BadLocationException e) {
                EirccUi.log(e);
            }
        }

        public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
            apply(viewer.getDocument(), trigger, offset);
        }

        public String getAdditionalProposalInfo() {
            return null;
        }

        public IContextInformation getContextInformation() {
            return null;
        }

        public int getContextInformationPosition() {
            return 0;
        }

        public String getDisplayString() {
            return fString;
        }

        public Image getImage() {
            return null;
        }

        public IInformationControlCreator getInformationControlCreator() {
            return null;
        }

        public int getPrefixCompletionStart(IDocument document, int completionOffset) {
            return fOffset - fPrefix.length();
        }

        public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
            return fPrefix + fString;
        }

        public Point getSelection(IDocument document) {
            return new Point(fOffset + fString.length(), 0);
        }

        public char[] getTriggerCharacters() {
            return null;
        }

        public boolean isAutoInsertable() {
            return true;
        }

        public boolean isValidFor(IDocument document, int offset) {
            return validate(document, offset, null);
        }

        public void selected(ITextViewer viewer, boolean smartToggle) {
        }

        public void unselected(ITextViewer viewer) {
        }

        public boolean validate(IDocument document, int offset, DocumentEvent event) {
            try {
                int prefixStart = fOffset - fPrefix.length();
                return offset >= fOffset
                        && offset < fOffset + fString.length()
                        && document.get(prefixStart, offset - (prefixStart)).equals(
                                (fPrefix + fString).substring(0, offset - prefixStart));
            } catch (BadLocationException x) {
                return false;
            }
        }

    }

    private static final IContextInformation[] NO_CONTEXTS = new IContextInformation[0];
    private static final ICompletionProposal[] NO_PROPOSALS = new ICompletionProposal[0];

    private final AbstractIrcChannel channel;

    /**
     * @param channel
     */
    public IrcContentAssistProcessor(AbstractIrcChannel channel) {
        super();
        this.channel = channel;
    }

    /**
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer,
     *      int)
     */

    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
        try {
            String prefix = getPrefix(viewer, offset);
            if (prefix == null || prefix.length() == 0)
                return NO_PROPOSALS;

            List<String> suggestions = getSuggestions(viewer, offset, prefix);

            List<ICompletionProposal> result = new ArrayList<ICompletionProposal>(suggestions.size());
            for (String string : suggestions) {
                if (string.length() > 0) {
                    result.add(new Proposal(string, prefix, offset));
                }
            }

            return (ICompletionProposal[]) result.toArray(new ICompletionProposal[result.size()]);

        } catch (BadLocationException e) {
            return NO_PROPOSALS;
        }
    }

    /**
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer,
     *      int)
     */
    @Override
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
        return NO_CONTEXTS;
    }

    /**
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
     */
    @Override
    public char[] getCompletionProposalAutoActivationCharacters() {
        return null;
    }

    /**
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
     */
    @Override
    public char[] getContextInformationAutoActivationCharacters() {
        return null;
    }

    /**
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
     */
    @Override
    public IContextInformationValidator getContextInformationValidator() {
        return null;
    }

    /**
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
     */
    @Override
    public String getErrorMessage() {
        return null;
    }

    private String getPrefix(ITextViewer viewer, int offset) throws BadLocationException {
        IDocument doc = viewer.getDocument();
        if (doc == null || offset > doc.getLength())
            return null;

        int length = 0;
        while (--offset >= 0 && !Character.isWhitespace(doc.getChar(offset)))
            length++;

        return doc.get(offset + 1, length).toLowerCase();
    }

    /**
     * Create the array of suggestions. It scans all open text editors and
     * prefers suggestions from the currently open editor. It also adds the
     * empty suggestion at the end.
     *
     * @param viewer
     *            the viewer
     * @param offset
     *            the offset
     * @param prefix
     *            the prefix to search for
     * @return the list of all possible suggestions in the currently open
     *         editors
     * @throws BadLocationException
     *             if accessing the current document fails
     */
    private List<String> getSuggestions(ITextViewer viewer, int offset, String prefix) throws BadLocationException {
        List<String> suggestions = new ArrayList<String>();
        if (prefix.length() >= 2 && prefix.charAt(0) == IrcConstants.COMMAND_MARKER) {
            String commandPrefix = prefix.substring(1);
            for (IRCCommand cmd : IRCCommand.values()) {
                String cmdName = cmd.name();
                if (cmdName.toLowerCase().startsWith(commandPrefix)) {
                    suggestions.add("" + IrcConstants.COMMAND_MARKER + cmdName);
                }
            }
        }

        for (IrcChannelUser user : channel.getUsers()) {
            String nick = user.getNick();
            if (nick.toLowerCase().startsWith(prefix)) {
                suggestions.add(nick);
            }
        }

        return suggestions;
    }

}
