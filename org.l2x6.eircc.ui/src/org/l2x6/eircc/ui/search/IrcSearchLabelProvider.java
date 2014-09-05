/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.search;

import java.text.MessageFormat;
import java.time.OffsetDateTime;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.core.model.IrcAccount;
import org.l2x6.eircc.core.model.IrcLog;
import org.l2x6.eircc.core.model.PlainIrcMessage;
import org.l2x6.eircc.ui.IrcUiMessages;
import org.l2x6.eircc.ui.editor.IrcDefaultMessageFormatter;
import org.l2x6.eircc.ui.editor.IrcDefaultMessageFormatter.TimeStyle;
import org.l2x6.eircc.ui.misc.IrcImages;
import org.l2x6.eircc.ui.misc.IrcImages.ImageKey;
import org.l2x6.eircc.ui.prefs.IrcPreferences;

/**
 * Adapted from {@code org.eclipse.search.internal.ui.text.FileLabelProvider} as
 * available in org.eclipse.search 3.9.100.v20140226-1637.
 *
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcSearchLabelProvider extends LabelProvider implements IStyledLabelProvider {

    public enum LabelOrder {
        SHOW_LABEL, SHOW_LABEL_PATH, SHOW_PATH_LABEL
    }
    private static final String ELLIPSES = " ... "; //$NON-NLS-1$
    /** minimal number of characters shown after and before a match */
    private static final int MIN_MATCH_CONTEXT = 10;

    private static final String SEPARATOR_FORMAT = "{0} - {1}"; //$NON-NLS-1$;

    private LabelOrder labelOrder;
    private final TimeStyle messsageTimeStyle;
    private final AbstractTextSearchViewPage page;
    private final WorkbenchLabelProvider workbenchLabelProvider;

    public IrcSearchLabelProvider(AbstractTextSearchViewPage page, LabelOrder labelOrder, TimeStyle messsageTimeStyle) {
        this.workbenchLabelProvider = new WorkbenchLabelProvider();
        this.labelOrder = labelOrder;
        this.page = page;
        this.messsageTimeStyle = messsageTimeStyle;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.viewers.BaseLabelProvider#addListener(org.eclipse.jface
     * .viewers.ILabelProviderListener)
     */
    public void addListener(ILabelProviderListener listener) {
        super.addListener(listener);
        workbenchLabelProvider.addListener(listener);
    }

    private int appendShortenedGap(String content, int start, int end, int charsToCut, boolean isFirst, StyledString str) {
        int gapLength = end - start;
        if (!isFirst) {
            gapLength -= MIN_MATCH_CONTEXT;
        }
        if (end < content.length()) {
            gapLength -= MIN_MATCH_CONTEXT;
        }
        if (gapLength < MIN_MATCH_CONTEXT) { // don't cut, gap is too small
            str.append(content.substring(start, end));
            return charsToCut;
        }

        int context = MIN_MATCH_CONTEXT;
        if (gapLength > charsToCut) {
            context += gapLength - charsToCut;
        }

        if (!isFirst) {
            str.append(content.substring(start, start + context)); // give all
                                                                   // extra
                                                                   // context to
                                                                   // the right
                                                                   // side of a
                                                                   // match
            context = MIN_MATCH_CONTEXT;
        }

        str.append(ELLIPSES, StyledString.QUALIFIER_STYLER);

        if (end < content.length()) {
            str.append(content.substring(end - context, end));
        }
        return charsToCut - gapLength + ELLIPSES.length();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
     */
    public void dispose() {
        super.dispose();
        workbenchLabelProvider.dispose();
    }

    private int getCharsToCut(int contentLength, Match[] matches) {
        if (contentLength <= 256 || matches.length == 0) { //$NON-NLS-1$
            return 0; // no shortening required
        }
        // XXX: workaround for
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=38519
        return contentLength - 256 + Math.max(matches.length * ELLIPSES.length(), 100);
    }

    private StyledString getColoredLabelWithCounts(Object element, StyledString coloredName) {
        AbstractTextSearchResult result = page.getInput();
        if (result == null)
            return coloredName;

        int matchCount = result.getMatchCount(element);
        if (matchCount <= 1)
            return coloredName;

        String countInfo = MessageFormat.format(IrcUiMessages.FileLabelProvider_count_format, matchCount);
        coloredName.append(' ').append(countInfo, StyledString.COUNTER_STYLER);
        return coloredName;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
     */
    public Image getImage(Object element) {
        if (element instanceof IrcMessageMatches) {
            return IrcImages.getInstance().getImage(ImageKey.MESSAGE);
        }
        if (!(element instanceof IResource))
            return null;

        IResource resource = (IResource) element;

        if (AbstractIrcChannel.isChannelLogsFolder(resource)) {
            return IrcImages.getInstance().getImage(ImageKey.CHANNEL);
        } else if (IrcLog.isLogFile(resource)) {
            return IrcImages.getInstance().getImage(ImageKey.CHANNEL_HISTORY);
        } else if (IrcAccount.isChannelsFolder(resource)) {
            return IrcImages.getInstance().getImage(ImageKey.ACCOUNT);
        } else {
            return workbenchLabelProvider.getImage(resource);
        }
    }

    private StyledString getLineElementLabel(IrcMessageMatches messageMatches) {
        PlainIrcMessage message = messageMatches.getMessage();
        IrcDefaultMessageFormatter formatter = IrcPreferences.getInstance().getSearchFormatter(message);
        StyledString styledText = formatter.format(message, messsageTimeStyle);

        String messageText = message.getText();
        int styledTextStart = styledText.length();
        int styledTextPos = styledTextStart;

        IrcMatch[] matches = messageMatches.getMatches();
        int charsToCut = getCharsToCut(messageText.length(), matches); // number
                                                                       // of
                                                                       // characters
                                                                       // to
                                                                       // leave
                                                                       // away
                                                                       // if the
                                                                       // line
                                                                       // is too
                                                                       // long
        for (int i = 0; i < matches.length; i++) {
            IrcMatch match = matches[i];
            int highlightStart = styledTextStart + match.getOffsetInMessageText();
            if (styledTextPos < highlightStart) {
                /* unhighlighted part */
                int start = styledTextPos - styledTextStart;
                int end = match.getOffsetInMessageText();
                if (charsToCut > 0) {
                    charsToCut = appendShortenedGap(messageText, start, end, charsToCut, i == 0, styledText);
                } else {
                    String str = messageText.substring(start, end);
                    styledText.append(str);
                }
                styledTextPos += (end - start);
            }
            /* the highlighted match */
            String str = messageText.substring(match.getOffsetInMessageText(),
                    match.getOffsetInMessageText() + match.getLength());
            styledText.append(str, IrcSearchDecoratingLabelProvider.HIGHLIGHT_STYLE);
            styledTextPos += str.length();
        }

        /* append the unhighlighted rest */
        if (styledTextPos < styledTextStart + messageText.length()) {
            int start = styledTextPos - styledTextStart;
            int end = messageText.length();
            if (charsToCut > 0) {
                charsToCut = appendShortenedGap(messageText, start, end, charsToCut, false, styledText);
            } else {
                String str = messageText.substring(start, end);
                styledText.append(str);
            }
        }

        return styledText;
    }

    public LabelOrder getOrder() {
        return labelOrder;
    }

    public StyledString getStyledText(Object element) {
        if (element instanceof IrcMessageMatches)
            return getLineElementLabel((IrcMessageMatches) element);

        if (!(element instanceof IResource))
            return new StyledString();

        IResource resource = (IResource) element;
        if (!resource.exists())
            new StyledString(IrcUiMessages.FileLabelProvider_removed_resource_label);

        final String name;
        if (AbstractIrcChannel.isChannelLogsFolder(resource)) {
            name = AbstractIrcChannel.getChannelName(resource.getFullPath());
        } else if (IrcLog.isLogFile(resource)) {
            OffsetDateTime date = IrcLog.getDate(resource.getFullPath());
            name = date.format(TimeStyle.DATE_TIME.getFormatter());
        } else if (IrcAccount.isChannelsFolder(resource)) {
            name = IrcAccount.getAccountNameFromChannelsFolder(resource.getFullPath());
        } else {
            name = TextProcessor.process(resource.getName(), ":.");
        }
        if (labelOrder == LabelOrder.SHOW_LABEL) {
            return getColoredLabelWithCounts(resource, new StyledString(name));
        }

        String pathString = TextProcessor.process(resource.getParent().getFullPath().toString(), "/\\:.");
        if (labelOrder == LabelOrder.SHOW_LABEL_PATH) {
            StyledString str = new StyledString(name);
            String decorated = MessageFormat.format(SEPARATOR_FORMAT, str.getString(), pathString);

            StyledCellLabelProvider.styleDecoratedString(decorated, StyledString.QUALIFIER_STYLER, str);
            return getColoredLabelWithCounts(resource, str);
        }

        StyledString str = new StyledString(MessageFormat.format(SEPARATOR_FORMAT, pathString, name));
        return getColoredLabelWithCounts(resource, str);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
     */
    public String getText(Object object) {
        return getStyledText(object).getString();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.viewers.BaseLabelProvider#isLabelProperty(java.lang
     * .Object, java.lang.String)
     */
    public boolean isLabelProperty(Object element, String property) {
        return workbenchLabelProvider.isLabelProperty(element, property);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.viewers.BaseLabelProvider#removeListener(org.eclipse
     * .jface.viewers.ILabelProviderListener)
     */
    public void removeListener(ILabelProviderListener listener) {
        super.removeListener(listener);
        workbenchLabelProvider.removeListener(listener);
    }

    public void setOrder(LabelOrder orderFlag) {
        labelOrder = orderFlag;
    }

}
