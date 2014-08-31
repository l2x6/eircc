/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.FindReplaceDocumentAdapterContentProposalProvider;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.search.internal.core.text.PatternConstructor;
import org.eclipse.search.internal.ui.SearchPlugin;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.search.ui.text.TextSearchQueryProvider;
import org.eclipse.search.ui.text.TextSearchQueryProvider.TextSearchInput;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.core.model.IrcAccount;
import org.l2x6.eircc.core.model.IrcModel;
import org.l2x6.eircc.ui.IrcUiMessages;
import org.l2x6.eircc.ui.misc.IrcUiUtils;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcSearchPage extends DialogPage implements ISearchPage {

    private static class SearchPatternData {
        public static SearchPatternData create(IDialogSettings settings) {
            String textPattern= settings.get("textPattern"); //$NON-NLS-1$
            String[] wsIds= settings.getArray("workingSets"); //$NON-NLS-1$
            IWorkingSet[] workingSets= null;
            if (wsIds != null && wsIds.length > 0) {
                IWorkingSetManager workingSetManager= PlatformUI.getWorkbench().getWorkingSetManager();
                workingSets= new IWorkingSet[wsIds.length];
                for (int i= 0; workingSets != null && i < wsIds.length; i++) {
                    workingSets[i]= workingSetManager.getWorkingSet(wsIds[i]);
                    if (workingSets[i] == null) {
                        workingSets= null;
                    }
                }
            }
            String[] fileNamePatterns= settings.getArray("fileNamePatterns"); //$NON-NLS-1$
            if (fileNamePatterns == null) {
                fileNamePatterns= new String[0];
            }
            try {
                int scope= settings.getInt("scope"); //$NON-NLS-1$
                boolean isRegExSearch= settings.getBoolean("isRegExSearch"); //$NON-NLS-1$
                boolean ignoreCase= settings.getBoolean("ignoreCase"); //$NON-NLS-1$
                boolean isWholeWord= settings.getBoolean("isWholeWord"); //$NON-NLS-1$

                return new SearchPatternData(textPattern, !ignoreCase, isRegExSearch, isWholeWord, fileNamePatterns, scope, workingSets);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        public final String[] fileNamePatterns;
        public final boolean isCaseSensitive;
        public final boolean isRegExSearch;
        public final boolean isWholeWord;
        public final int scope;
        public final String textPattern;

        public final IWorkingSet[] workingSets;

        public SearchPatternData(String textPattern, boolean isCaseSensitive, boolean isRegExSearch, boolean isWholeWord, String[] fileNamePatterns, int scope, IWorkingSet[] workingSets) {
            Assert.isNotNull(fileNamePatterns);
            this.isCaseSensitive= isCaseSensitive;
            this.isRegExSearch= isRegExSearch;
            this.isWholeWord= isWholeWord;
            this.textPattern= textPattern;
            this.fileNamePatterns= fileNamePatterns;
            this.scope= scope;
            this.workingSets= workingSets; // can be null
        }

        public void store(IDialogSettings settings) {
            settings.put("ignoreCase", !isCaseSensitive); //$NON-NLS-1$
            settings.put("isRegExSearch", isRegExSearch); //$NON-NLS-1$
            settings.put("isWholeWord", isWholeWord); //$NON-NLS-1$
            settings.put("textPattern", textPattern); //$NON-NLS-1$
            settings.put("fileNamePatterns", fileNamePatterns); //$NON-NLS-1$
            settings.put("scope", scope); //$NON-NLS-1$
            if (workingSets != null) {
                String[] wsIds= new String[workingSets.length];
                for (int i= 0; i < workingSets.length; i++) {
                    wsIds[i]= workingSets[i].getLabel();
                }
                settings.put("workingSets", wsIds); //$NON-NLS-1$
            } else {
                settings.put("workingSets", new String[0]); //$NON-NLS-1$
            }

        }

    }
    private static class TextSearchPageInput extends TextSearchInput {

        private final boolean fIsCaseSensitive;
        private final boolean fIsRegEx;
        private final boolean fIsWholeWord;
        private final FileTextSearchScope fScope;
        private final String fSearchText;

        public TextSearchPageInput(String searchText, boolean isCaseSensitive, boolean isRegEx, boolean isWholeWord, FileTextSearchScope scope) {
            fSearchText= searchText;
            fIsCaseSensitive= isCaseSensitive;
            fIsRegEx= isRegEx;
            fIsWholeWord= isWholeWord;
            fScope= scope;
        }

        public FileTextSearchScope getScope() {
            return fScope;
        }

        public String getSearchText() {
            return fSearchText;
        }

        public boolean isCaseSensitiveSearch() {
            return fIsCaseSensitive;
        }

        public boolean isRegExSearch() {
            return fIsRegEx;
        }

        public boolean isWholeWordSearch() {
            return fIsWholeWord;
        }
    }

    private static final int HISTORY_SIZE= 12;
    public static final String ID= "org.l2x6.eircc.ui.search.IrcSearchPage"; //$NON-NLS-1$
    // Dialog store id constants
    private static final String PAGE_NAME= "IrcSearchPage"; //$NON-NLS-1$
    private static final String STORE_CASE_SENSITIVE= "CASE_SENSITIVE"; //$NON-NLS-1$
    /**
     * Section name for the stored file extensions.
     * @since 3.6
     */
    private static final String STORE_EXTENSIONS= "EXTENSIONS"; //$NON-NLS-1$
    private static final String STORE_HISTORY= "HISTORY"; //$NON-NLS-1$

    private static final String STORE_HISTORY_SIZE= "HISTORY_SIZE"; //$NON-NLS-1$

    private static final String STORE_IS_REG_EX_SEARCH= "REG_EX_SEARCH"; //$NON-NLS-1$

    private static final String STORE_IS_WHOLE_WORD= "WHOLE_WORD"; //$NON-NLS-1$
    private IrcChannelsSelector channelsSelector;
    private ISearchPageContainer fContainer;
    private boolean fFirstTime= true;

    private boolean fIsCaseSensitive;
    private Button fIsCaseSensitiveCheckbox;
    private Button fIsRegExCheckbox;
    private boolean fIsRegExSearch;
    private boolean fIsWholeWord;
    private Button fIsWholeWordCheckbox;

    private ContentAssistCommandAdapter fPatterFieldContentAssist;
    private Combo fPattern;

    /**
     * The previous file extensions.
     * @since 3.6
     */
    private String[] fPreviousExtensions;

    private List<SearchPatternData> fPreviousSearchPatterns= new ArrayList<SearchPatternData>(HISTORY_SIZE);


    private CLabel fStatusLabel;

    private Combo searchInChannelsCombo;

    //---- Action Handling ------------------------------------------------

    private void addFileNameControls(Composite group) {
        // grid layout with 2 columns

        // Line with label, combo and button
        Label label= new Label(group, SWT.LEAD);
        label.setText(IrcUiMessages.IrcSearchPage_Search_in_Channels_label);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
        label.setFont(group.getFont());

        searchInChannelsCombo= new Combo(group, SWT.SINGLE | SWT.BORDER);
        searchInChannelsCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateOKStatus();
            }
        });
        GridData data= new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1);
        data.widthHint= convertWidthInCharsToPixels(50);
        searchInChannelsCombo.setLayoutData(data);
        searchInChannelsCombo.setFont(group.getFont());

        Button button= new Button(group, SWT.PUSH);
        button.setText(IrcUiMessages.IrcSearchPage_Select_button);
        GridData gridData= new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1);
        gridData.widthHint= IrcUiUtils.getButtonWidthHint(button);
        button.setLayoutData(gridData);
        button.setFont(group.getFont());

        channelsSelector= new IrcChannelsSelector(searchInChannelsCombo, button);

        // Text line which explains the special characters
        Label description= new Label(group, SWT.LEAD);
        description.setText(IrcUiMessages.IrcSearchPage_channelNamePatterns_hint);
        description.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
        description.setFont(group.getFont());

    }

    private void addTextPatternControls(Composite group) {
        // grid layout with 2 columns

        // Info text
        Label label= new Label(group, SWT.LEAD);
        label.setText(IrcUiMessages.IrcSearchPage_containingText_text);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
        label.setFont(group.getFont());

        // Pattern combo
        fPattern= new Combo(group, SWT.SINGLE | SWT.BORDER);
        // Not done here to prevent page from resizing
        // fPattern.setItems(getPreviousSearchPatterns());
        fPattern.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                handleWidgetSelected();
                updateOKStatus();
            }
        });
        // add some listeners for regex syntax checking
        fPattern.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateOKStatus();
            }
        });
        fPattern.setFont(group.getFont());
        GridData data= new GridData(GridData.FILL, GridData.FILL, true, false, 1, 2);
        data.widthHint= convertWidthInCharsToPixels(50);
        fPattern.setLayoutData(data);

        ComboContentAdapter contentAdapter= new ComboContentAdapter();
        FindReplaceDocumentAdapterContentProposalProvider findProposer= new FindReplaceDocumentAdapterContentProposalProvider(true);
        fPatterFieldContentAssist= new ContentAssistCommandAdapter(
                fPattern,
                contentAdapter,
                findProposer,
                ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS,
                new char[0],
                true);
        fPatterFieldContentAssist.setEnabled(fIsRegExSearch);

        fIsCaseSensitiveCheckbox= new Button(group, SWT.CHECK);
        fIsCaseSensitiveCheckbox.setText(IrcUiMessages.IrcSearchPage_caseSensitive);
        fIsCaseSensitiveCheckbox.setSelection(fIsCaseSensitive);
        fIsCaseSensitiveCheckbox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                fIsCaseSensitive= fIsCaseSensitiveCheckbox.getSelection();
            }
        });
        fIsCaseSensitiveCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        fIsCaseSensitiveCheckbox.setFont(group.getFont());

        // RegEx checkbox
        fIsRegExCheckbox= new Button(group, SWT.CHECK);
        fIsRegExCheckbox.setText(IrcUiMessages.IrcSearchPage_regularExpression);
        fIsRegExCheckbox.setSelection(fIsRegExSearch);

        fIsRegExCheckbox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                fIsRegExSearch= fIsRegExCheckbox.getSelection();
                updateOKStatus();

                writeConfiguration();
                fPatterFieldContentAssist.setEnabled(fIsRegExSearch);
                fIsWholeWordCheckbox.setEnabled(!fIsRegExSearch);
            }
        });
        fIsRegExCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 2));
        fIsRegExCheckbox.setFont(group.getFont());

        // Text line which explains the special characters
        fStatusLabel= new CLabel(group, SWT.LEAD);
        fStatusLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 2));
        fStatusLabel.setFont(group.getFont());
        fStatusLabel.setAlignment(SWT.LEFT);
        fStatusLabel.setText(IrcUiMessages.IrcSearchPage_containingText_hint);

        // Whole Word checkbox
        fIsWholeWordCheckbox= new Button(group, SWT.CHECK);
        fIsWholeWordCheckbox.setText(IrcUiMessages.IrcSearchPage_wholeWord);
        fIsWholeWordCheckbox.setSelection(fIsWholeWord);
        fIsWholeWordCheckbox.setEnabled(!fIsRegExSearch);
        fIsWholeWordCheckbox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                fIsWholeWord= fIsWholeWordCheckbox.getSelection();
            }
        });
        fIsWholeWordCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        fIsWholeWordCheckbox.setFont(group.getFont());
    }

    public void createControl(Composite parent) {
        initializeDialogUnits(parent);
        readConfiguration();

        Composite result= new Composite(parent, SWT.NONE);
        result.setFont(parent.getFont());
        GridLayout layout= new GridLayout(2, false);
        result.setLayout(layout);

        addTextPatternControls(result);

        Label separator= new Label(result, SWT.NONE);
        separator.setVisible(false);
        GridData data= new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1);
        data.heightHint= convertHeightInCharsToPixels(1) / 3;
        separator.setLayoutData(data);

        addFileNameControls(result);

        setControl(result);
        Dialog.applyDialogFont(result);
}

    public FileTextSearchScope createTextSearchScope() throws CoreException {
        return getAllScope();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.DialogPage#dispose()
     */
    public void dispose() {
        writeConfiguration();
        super.dispose();
    }

    private SearchPatternData findInPrevious(String pattern) {
        for (SearchPatternData element : fPreviousSearchPatterns) {
            if (pattern.equals(element.textPattern)) {
                return element;
            }
        }
        return null;
    }


    private FileTextSearchScope getAllScope() throws CoreException {
        List<IResource> resources= new ArrayList<IResource>();
        for (IrcAccount account : IrcModel.getInstance().getSearchableAccounts()) {
            for (AbstractIrcChannel channel : account.getSearchableChannels()) {
                for (IFile log : channel.listSearchableLogFiles()) {
                    resources.add(log);
                }
            }
        }
        IResource[] arr= (IResource[]) resources.toArray(new IResource[resources.size()]);
        return FileTextSearchScope.newSearchScope(arr, getExtensions(), false);
    }

    private ISearchPageContainer getContainer() {
        return fContainer;
    }

    /**
     * Returns the page settings for this Text search page.
     *
     * @return the page settings to be used
     */
    private IDialogSettings getDialogSettings() {
        return SearchPlugin.getDefault().getDialogSettingsSection(PAGE_NAME);
    }

    private FileTextSearchScope getEnclosingProjectScope() {
        String[] enclosingProjectName= getContainer().getSelectedProjectNames();
        if (enclosingProjectName == null) {
            return FileTextSearchScope.newWorkspaceScope(getExtensions(), false);
        }

        IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
        IResource[] res= new IResource[enclosingProjectName.length];
        for (int i= 0; i < res.length; i++) {
            res[i]= root.getProject(enclosingProjectName[i]);
        }

        return FileTextSearchScope.newSearchScope(res, getExtensions(), false);
    }

    private String[] getExtensions() {
        return channelsSelector.getChannelNames();
    }

    /**
     * Return search pattern data and update previous searches.
     * An existing entry will be updated.
     * @return the search pattern data
     */
    private SearchPatternData getPatternData() {
        SearchPatternData match= findInPrevious(fPattern.getText());
        if (match != null) {
            fPreviousSearchPatterns.remove(match);
        }
        match= new SearchPatternData(
                    fPattern.getText(),
                    isCaseSensitive(),
                    fIsRegExCheckbox.getSelection(),
                    fIsWholeWordCheckbox.getSelection(),
                    getExtensions(),
                    getContainer().getSelectedScope(),
                    getContainer().getSelectedWorkingSets());
        fPreviousSearchPatterns.add(0, match);
        return match;
    }

    private String[] getPreviousExtensionsOldStyle() {
        List<String> extensions= new ArrayList<String>(fPreviousSearchPatterns.size());
        int size= fPreviousSearchPatterns.size();
        for (int i= 0; i < size; i++) {
            SearchPatternData data= (SearchPatternData)fPreviousSearchPatterns.get(i);
            String text= IrcChannelsSelector.typesToString(data.fileNamePatterns);
            if (!extensions.contains(text))
                extensions.add(text);
        }
        return extensions.toArray(new String[extensions.size()]);
    }

    private String[] getPreviousSearchPatterns() {
        int size= fPreviousSearchPatterns.size();
        String [] patterns= new String[size];
        for (int i= 0; i < size; i++)
            patterns[i]= ((SearchPatternData) fPreviousSearchPatterns.get(i)).textPattern;
        return patterns;
    }

    //---- Widget creation ------------------------------------------------

    private FileTextSearchScope getSelectedResourcesScope() {
        Set<IResource> resources= new HashSet<IResource>();
        ISelection sel= getContainer().getSelection();
        if (sel instanceof IStructuredSelection && !sel.isEmpty()) {
            Iterator<?> iter = ((IStructuredSelection) sel).iterator();
            while (iter.hasNext()) {
                Object curr= iter.next();
                if (curr instanceof IAdaptable) {
                    IResource resource= (IResource) ((IAdaptable)curr).getAdapter(IResource.class);
                    if (resource != null && resource.isAccessible()) {
                        resources.add(resource);
                    }
                }
            }
        } else if (getContainer().getActiveEditorInput() != null) {
            resources.add((IResource)getContainer().getActiveEditorInput().getAdapter(IFile.class));
        }
        IResource[] arr= (IResource[]) resources.toArray(new IResource[resources.size()]);
        return FileTextSearchScope.newSearchScope(arr, getExtensions(), false);
    }

    private ISelection getSelection() {
        return fContainer.getSelection();
    }

    private void handleWidgetSelected() {
        int selectionIndex= fPattern.getSelectionIndex();
        if (selectionIndex < 0 || selectionIndex >= fPreviousSearchPatterns.size())
            return;

        SearchPatternData patternData= (SearchPatternData) fPreviousSearchPatterns.get(selectionIndex);
        if (!fPattern.getText().equals(patternData.textPattern))
            return;
        fIsCaseSensitiveCheckbox.setSelection(patternData.isCaseSensitive);
        fIsRegExSearch= patternData.isRegExSearch;
        fIsRegExCheckbox.setSelection(fIsRegExSearch);
        fIsWholeWord= patternData.isWholeWord;
        fIsWholeWordCheckbox.setSelection(fIsWholeWord);
        fIsWholeWordCheckbox.setEnabled(!fIsRegExSearch);
        fPattern.setText(patternData.textPattern);
        fPatterFieldContentAssist.setEnabled(fIsRegExSearch);
        channelsSelector.setFileTypes(patternData.fileNamePatterns);
        if (patternData.workingSets != null)
            getContainer().setSelectedWorkingSets(patternData.workingSets);
        else
            getContainer().setSelectedScope(patternData.scope);
    }

    private boolean initializePatternControl() {
        ISelection selection= getSelection();
        if (selection instanceof ITextSelection && !selection.isEmpty() && ((ITextSelection)selection).getLength() > 0) {
            String text= ((ITextSelection) selection).getText();
            if (text != null) {
                if (fIsRegExSearch)
                    fPattern.setText(FindReplaceDocumentAdapter.escapeForRegExPattern(text));
                else
                    fPattern.setText(insertEscapeChars(text));

                if (fPreviousExtensions.length > 0) {
                    searchInChannelsCombo.setText(fPreviousExtensions[0]);
                } else {
                    searchInChannelsCombo.setText("*"); //$NON-NLS-1$
                }
                return true;
            }
        }
        return false;
    }

    private String insertEscapeChars(String text) {
        if (text == null || text.equals("")) //$NON-NLS-1$
            return ""; //$NON-NLS-1$
        StringBuffer sbIn= new StringBuffer(text);
        BufferedReader reader= new BufferedReader(new StringReader(text));
        int lengthOfFirstLine= 0;
        try {
            lengthOfFirstLine= reader.readLine().length();
        } catch (IOException ex) {
            return ""; //$NON-NLS-1$
        }
        StringBuffer sbOut= new StringBuffer(lengthOfFirstLine + 5);
        int i= 0;
        while (i < lengthOfFirstLine) {
            char ch= sbIn.charAt(i);
            if (ch == '*' || ch == '?' || ch == '\\')
                sbOut.append("\\"); //$NON-NLS-1$
            sbOut.append(ch);
            i++;
        }
        return sbOut.toString();
    }

    private boolean isCaseSensitive() {
        return fIsCaseSensitiveCheckbox.getSelection();
    }

    private ISearchQuery newQuery() throws CoreException {
        SearchPatternData data= getPatternData();
        TextSearchPageInput input= new TextSearchPageInput(data.textPattern, data.isCaseSensitive, data.isRegExSearch, data.isWholeWord && !data.isRegExSearch, createTextSearchScope());
        return TextSearchQueryProvider.getPreferred().createQuery(input);
    }

    public boolean performAction() {
        try {
            NewSearchUI.runQueryInBackground(newQuery());
        } catch (CoreException e) {
            ErrorDialog.openError(getShell(), IrcUiMessages.IrcSearchPage_searchproblems_title, IrcUiMessages.IrcSearchPage_searchproblems_message, e.getStatus());
            return false;
        }
        return true;
    }

    /**
     * Initializes itself from the stored page settings.
     */
    private void readConfiguration() {
        IDialogSettings s= getDialogSettings();
        fIsCaseSensitive= s.getBoolean(STORE_CASE_SENSITIVE);
        fIsRegExSearch= s.getBoolean(STORE_IS_REG_EX_SEARCH);
        fIsWholeWord= s.getBoolean(STORE_IS_WHOLE_WORD);

        try {
            int historySize= s.getInt(STORE_HISTORY_SIZE);
            for (int i= 0; i < historySize; i++) {
                IDialogSettings histSettings= s.getSection(STORE_HISTORY + i);
                if (histSettings != null) {
                    SearchPatternData data= SearchPatternData.create(histSettings);
                    if (data != null) {
                        fPreviousSearchPatterns.add(data);
                    }
                }
            }
        } catch (NumberFormatException e) {
            // ignore
        }

        Set<String> previousExtensions= new LinkedHashSet<String>(HISTORY_SIZE);
        IDialogSettings extensionsSettings= s.getSection(STORE_EXTENSIONS);
        if (extensionsSettings != null) {
            for (int i= 0; i < HISTORY_SIZE; i++) {
                String extension= extensionsSettings.get(Integer.toString(i));
                if (extension == null)
                    break;
                previousExtensions.add(extension);
            }
            fPreviousExtensions= new String[previousExtensions.size()];
            previousExtensions.toArray(fPreviousExtensions);
        } else
            fPreviousExtensions= getPreviousExtensionsOldStyle();

    }

    /**
     * Sets the search page's container.
     * @param container the container to set
     */
    public void setContainer(ISearchPageContainer container) {
        fContainer= container;
    }


    //--------------- Configuration handling --------------

    /*
     * Implements method from IDialogPage
     */
    public void setVisible(boolean visible) {
        if (visible && fPattern != null) {
            if (fFirstTime) {
                fFirstTime= false;
                // Set item and text here to prevent page from resizing
                fPattern.setItems(getPreviousSearchPatterns());
                searchInChannelsCombo.setItems(fPreviousExtensions);
//              if (fExtensions.getItemCount() == 0) {
//                  loadFilePatternDefaults();
//              }
                if (!initializePatternControl()) {
                    fPattern.select(0);
                    searchInChannelsCombo.setText("*"); //$NON-NLS-1$
                    handleWidgetSelected();
                }
            }
            fPattern.setFocus();
        }
        updateOKStatus();

        IEditorInput editorInput= getContainer().getActiveEditorInput();
        getContainer().setActiveEditorCanProvideScopeSelection(editorInput != null && editorInput.getAdapter(IFile.class) != null);

        super.setVisible(visible);
    }

    private void statusMessage(boolean error, String message) {
        fStatusLabel.setText(message);
        if (error)
            fStatusLabel.setForeground(JFaceColors.getErrorText(fStatusLabel.getDisplay()));
        else
            fStatusLabel.setForeground(null);
    }


    final void updateOKStatus() {
        boolean regexStatus= validateRegex();
        boolean hasFilePattern= searchInChannelsCombo.getText().length() > 0;
        getContainer().setPerformActionEnabled(regexStatus && hasFilePattern);
    }

    private boolean validateRegex() {
        if (fIsRegExCheckbox.getSelection()) {
            try {
                PatternConstructor.createPattern(fPattern.getText(), fIsCaseSensitive, true);
            } catch (PatternSyntaxException e) {
                String locMessage= e.getLocalizedMessage();
                int i= 0;
                while (i < locMessage.length() && "\n\r".indexOf(locMessage.charAt(i)) == -1) { //$NON-NLS-1$
                    i++;
                }
                statusMessage(true, locMessage.substring(0, i)); // only take first line
                return false;
            }
            statusMessage(false, ""); //$NON-NLS-1$
        } else {
            statusMessage(false, IrcUiMessages.IrcSearchPage_containingText_hint);
        }
        return true;
    }

    /**
     * Stores it current configuration in the dialog store.
     */
    private void writeConfiguration() {
        IDialogSettings s= getDialogSettings();
        s.put(STORE_CASE_SENSITIVE, fIsCaseSensitive);
        s.put(STORE_IS_REG_EX_SEARCH, fIsRegExSearch);
        s.put(STORE_IS_WHOLE_WORD, fIsWholeWord);

        int historySize= Math.min(fPreviousSearchPatterns.size(), HISTORY_SIZE);
        s.put(STORE_HISTORY_SIZE, historySize);
        for (int i= 0; i < historySize; i++) {
            IDialogSettings histSettings= s.addNewSection(STORE_HISTORY + i);
            SearchPatternData data= ((SearchPatternData) fPreviousSearchPatterns.get(i));
            data.store(histSettings);
        }

        IDialogSettings extensionsSettings= s.addNewSection(STORE_EXTENSIONS);
        extensionsSettings.put(Integer.toString(0), searchInChannelsCombo.getText());
        Set<String> extensions= new HashSet<String>(HISTORY_SIZE);
        extensions.add(searchInChannelsCombo.getText());
        int length= Math.min(searchInChannelsCombo.getItemCount(), HISTORY_SIZE - 1);
        int j= 1;
        for (int i= 0; i < length; i++) {
            String extension= searchInChannelsCombo.getItem(i);
            if (extensions.add(extension))
                extensionsSettings.put(Integer.toString(j++), extension);
        }

    }

}
