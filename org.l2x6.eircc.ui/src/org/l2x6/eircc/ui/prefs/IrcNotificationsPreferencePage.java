/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.prefs;

import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.IrcUiMessages;
import org.l2x6.eircc.ui.prefs.IrcPreferences.PreferenceKey;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcNotificationsPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
    private final class NicksEditor extends ListEditor {

        /**
         * Creates a path field editor.
         *
         * @param name
         *            the name of the preference this field editor works on
         * @param labelText
         *            the label text of the field editor
         * @param dirChooserLabelText
         *            the label text displayed for the directory chooser
         * @param parent
         *            the parent of the field editor's control
         */
        public NicksEditor(String name, String labelText, Composite parent) {
            init(name, labelText);
            createControl(parent);
        }

        /*
         * (non-Javadoc) Method declared on ListEditor. Creates a single string
         * from the given array by separating each string with the appropriate
         * OS-specific path separator.
         */
        @Override
        protected String createList(String[] items) {
            StringBuffer path = new StringBuffer("");//$NON-NLS-1$

            for (int i = 0; i < items.length; i++) {
                path.append(items[i]);
                path.append(IrcPreferences.NICKS_DELIMITER);
            }
            return path.toString();
        }

        /*
         * (non-Javadoc) Method declared on ListEditor. Creates a new path
         * element by means of a directory dialog.
         */
        @Override
        protected String getNewInputObject() {
            return IrcPreferences.getInstance().showAddNichPatternDialog("");
        }

        /*
         * (non-Javadoc) Method declared on ListEditor.
         */
        @Override
        protected String[] parseString(String stringList) {
            StringTokenizer st = new StringTokenizer(stringList, "" + IrcPreferences.NICKS_DELIMITER + "\n\t\r");
            Set<String> v = new TreeSet<String>();
            while (st.hasMoreTokens()) {
                v.add(st.nextToken());
            }
            return v.toArray(new String[v.size()]);
        }
    }

    private NicksEditor nicksEditor;

    /**
     *
     */
    public IrcNotificationsPreferencePage() {
        super();
        setPreferenceStore(EirccUi.getDefault().getPreferenceStore());
    }

    /**
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
     */
    @Override
    protected void createFieldEditors() {
        nicksEditor = new NicksEditor(PreferenceKey.TRACKED_NICKS.toString(),
                IrcUiMessages.IrcNotificationsPreferencePage_senderBasedNotification, getFieldEditorParent());
        addField(nicksEditor);
    }

    /**
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(IWorkbench workbench) {
    }

}
