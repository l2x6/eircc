/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.search;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.l2x6.eircc.core.model.PlainIrcMessage;
import org.l2x6.eircc.ui.IrcUiMessages;
import org.l2x6.eircc.ui.search.IrcSearchPage.IrcSearchPatternData;
import org.l2x6.eircc.ui.search.IrcSearchPage.TimeSpan;

/**
 * Adapted from {@code org.eclipse.search.internal.ui.text.FileSearchQuery} as
 * available in org.eclipse.search 3.9.100.v20140226-1637.
 *
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcSearchQuery implements ISearchQuery {

    public final static class IrcSearchResultCollector extends TextSearchRequestor {

        private final boolean isFileLevelSearch;
        private final IrcSearchResult result;

        private ArrayList<IrcMatch> resultingMatches;

        private IrcSearchResultCollector(IrcSearchResult result, boolean isFileSearchOnly) {
            this.result = result;
            isFileLevelSearch = isFileSearchOnly;
        }

        public boolean acceptFile(IFile file) {
            if (isFileLevelSearch) {
                result.addMatch(new IrcMatch(file));
            }
            flushMatches();
            return true;
        }

        public boolean acceptPatternMatch(IFile file, PlainIrcMessage message, int start, int length)
                throws CoreException {

            IrcMessageMatches messageMatches = getLineElement(file, message);
            if (messageMatches != null) {
                IrcMatch match = new IrcMatch(file, messageMatches, start, length);
                messageMatches.add(match);
                resultingMatches.add(match);
            }
            return true;
        }

        public void beginReporting() {
            resultingMatches = new ArrayList<IrcMatch>();
        }

        public void endReporting() {
            flushMatches();
            resultingMatches = null;
        }

        private void flushMatches() {
            if (!resultingMatches.isEmpty()) {
                result.addMatches(resultingMatches.toArray(new IrcMatch[resultingMatches.size()]));
                resultingMatches.clear();
            }
        }

        private IrcMessageMatches getLineElement(IFile file, PlainIrcMessage message) {
            if (!resultingMatches.isEmpty()) {
                // match on same line as last?
                IrcMatch last = resultingMatches.get(resultingMatches.size() - 1);
                IrcMessageMatches messageMatches = last.getMessageMatches();
                if (messageMatches.getMessage() == message) {
                    return messageMatches;
                }
            }
            return new IrcMessageMatches(file, message);
        }

        /**
         * @see org.eclipse.search.core.text.TextSearchRequestor#reportBinaryFile(org.eclipse.core.resources.IFile)
         */
        public boolean reportBinaryFile(IFile file) {
            return false;
        }
    }

    private final boolean isWholeWord;
    private final FileTextSearchScope scope;
    private final IrcSearchPatternData searchData;

    private IrcSearchResult searchResult;
    private final String searchText;

    /**
     * @param searchText
     * @param isRegEx
     * @param isCaseSensitive
     * @param isWholeWord
     * @param scope
     */
    public IrcSearchQuery(String searchText, boolean isWholeWord, IrcSearchPatternData searchData,
            FileTextSearchScope scope) {
        Assert.isLegal(!(isWholeWord && searchData.isRegExSearch));
        this.searchText = searchText;
        this.isWholeWord = isWholeWord;
        this.searchData = searchData;
        this.scope = scope;
    }

    public boolean canRerun() {
        return true;
    }

    public boolean canRunInBackground() {
        return true;
    }

    public String getLabel() {
        return IrcUiMessages.IrcSearchQuery_label;
    }

    public String getResultLabel(int nMatches) {
        StringBuilder sb = new StringBuilder();
        String searchString = getSearchString();
        if (searchString != null && !searchString.trim().isEmpty()) {
            sb.append(searchString);
        }

        if (searchData.nickPrefixes != null && !searchData.nickPrefixes.trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(MessageFormat.format(IrcUiMessages.FileSearchQuery_from, searchData.nickPrefixes.trim()));
        }

        if (searchData.channels != null && !searchData.channels.trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(MessageFormat.format(IrcUiMessages.FileSearchQuery_channels, searchData.channels.trim()));
        }

        if (searchData.timeSpan != null && searchData.timeSpan != TimeSpan.A_ANY_TIME) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(MessageFormat.format(IrcUiMessages.FileSearchQuery_time, searchData.timeSpan.getLabel()));
        }
        if (sb.length() > 0) {
            if (nMatches == 1) {
                return MessageFormat.format(IrcUiMessages.FileSearchQuery_singularLabel, sb.toString());
            } else {
                return MessageFormat.format(IrcUiMessages.FileSearchQuery_pluralPattern, sb.toString(), nMatches);
            }
        } else {
            return "wtf?";
        }
    }

    @SuppressWarnings("restriction")
    protected Pattern getSearchPattern() {
        return org.eclipse.search.internal.core.text.PatternConstructor.createPattern(searchText,
                searchData.isRegExSearch, true, searchData.isCaseSensitive, isWholeWord);
    }

    public IrcSearchResult getSearchResult() {
        if (searchResult == null) {
            searchResult = new IrcSearchResult(this);
            new IrcSearchResultUpdater(searchResult);
        }
        return searchResult;
    }

    public String getSearchString() {
        return searchText;
    }

    public boolean isFileLevelSearch() {
        return (searchText == null || searchText.isEmpty())
                && (searchData.nickPrefixes == null || searchData.nickPrefixes.trim().isEmpty());
    }

    @Override
    public IStatus run(IProgressMonitor monitor) {
        IrcSearchResult textResult = getSearchResult();
        textResult.removeAll();
        Pattern searchPattern = getSearchPattern();

        IrcSearchResultCollector collector = new IrcSearchResultCollector(textResult, isFileLevelSearch());
        return new IrcSearchVisitor(isFileLevelSearch(), collector, searchPattern, searchData).search(scope, monitor);
    }
}
