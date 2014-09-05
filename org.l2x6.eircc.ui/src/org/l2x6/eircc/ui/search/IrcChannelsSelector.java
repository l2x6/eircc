/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.search;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.dialogs.TypeFilteringDialog;
import org.l2x6.eircc.ui.IrcUiMessages;

/**
 * Adapted from {@code org.eclipse.search.internal.ui.util.FileTypeEditor} as
 * available in org.eclipse.search 3.9.100.v20140226-1637.
 *
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcChannelsSelector extends SelectionAdapter implements DisposeListener {

    private final static String CHANNEL_NAME_DELIMITER = IrcUiMessages.IrcChannelsSelector_delimiter;
    public final static String CHANNEL_PATTERN_NEGATOR = "!"; //$NON-NLS-1$

    private static final Comparator<String> FILE_TYPES_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(String fp1, String fp2) {
            boolean isNegative1 = fp1.startsWith(CHANNEL_PATTERN_NEGATOR);
            boolean isNegative2 = fp2.startsWith(CHANNEL_PATTERN_NEGATOR);
            if (isNegative1 != isNegative2) {
                return isNegative1 ? 1 : -1;
            }
            return fp1.compareTo(fp2);
        }
    };

    public static String typesToString(String[] types) {
        Arrays.sort(types, FILE_TYPES_COMPARATOR);
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < types.length; i++) {
            if (i > 0) {
                result.append(CHANNEL_NAME_DELIMITER);
                result.append(" "); //$NON-NLS-1$
            }
            result.append(types[i]);
        }
        return result.toString();
    }

    private Button browseButton;

    private Combo textField;

    public IrcChannelsSelector(Combo textField, Button browseButton) {
        this.textField = textField;
        this.browseButton = browseButton;

        textField.addDisposeListener(this);
        browseButton.addDisposeListener(this);
        browseButton.addSelectionListener(this);
    }

    public String[] getChannelNames() {
        Set<String> result = new HashSet<String>();
        StringTokenizer tokenizer = new StringTokenizer(textField.getText(), CHANNEL_NAME_DELIMITER);

        while (tokenizer.hasMoreTokens()) {
            String currentExtension = tokenizer.nextToken().trim();
            result.add(currentExtension);
        }
        return (String[]) result.toArray(new String[result.size()]);
    }

    protected void handleBrowseButton() {
        TypeFilteringDialog dialog = new TypeFilteringDialog(textField.getShell(), Arrays.asList(getChannelNames()));
        if (dialog.open() == Window.OK) {
            Object[] result = dialog.getResult();
            HashSet<String> patterns = new HashSet<String>();
            boolean starIncluded = false;
            for (int i = 0; i < result.length; i++) {
                String curr = result[i].toString();
                if (curr.equals("*")) { //$NON-NLS-1$
                    starIncluded = true;
                } else {
                    patterns.add("*." + curr); //$NON-NLS-1$
                }
            }
            if (patterns.isEmpty() && starIncluded) { // remove star when other
                                                      // file extensions active
                patterns.add("*"); //$NON-NLS-1$
            }
            String[] filePatterns = (String[]) patterns.toArray(new String[patterns.size()]);
            Arrays.sort(filePatterns);
            setFileTypes(filePatterns);
        }
    }

    public void setFileTypes(String[] types) {
        textField.setText(typesToString(types));
    }

    public void widgetDisposed(DisposeEvent event) {
        Widget widget = event.widget;
        if (widget == textField)
            textField = null;
        else if (widget == browseButton)
            browseButton = null;
    }

    public void widgetSelected(SelectionEvent event) {
        if (event.widget == browseButton)
            handleBrowseButton();
    }
}
