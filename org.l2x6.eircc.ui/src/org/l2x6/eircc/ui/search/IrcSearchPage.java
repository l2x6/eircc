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
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
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
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
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

/**
 * Adapted from {@code org.eclipse.search.internal.ui.text.TextSearchPage} as
 * available in org.eclipse.search 3.9.100.v20140226-1637.
 *
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcSearchPage extends DialogPage implements ISearchPage {

    public static class IrcSearchPatternData {
        public static IrcSearchPatternData create(IDialogSettings settings) {
            String textPattern = settings.get("textPattern"); //$NON-NLS-1$
            String[] wsIds = settings.getArray("workingSets"); //$NON-NLS-1$
            IWorkingSet[] workingSets = null;
            if (wsIds != null && wsIds.length > 0) {
                IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
                workingSets = new IWorkingSet[wsIds.length];
                for (int i = 0; workingSets != null && i < wsIds.length; i++) {
                    workingSets[i] = workingSetManager.getWorkingSet(wsIds[i]);
                    if (workingSets[i] == null) {
                        workingSets = null;
                    }
                }
            }
            String[] fileNamePatterns = settings.getArray("fileNamePatterns"); //$NON-NLS-1$
            if (fileNamePatterns == null) {
                fileNamePatterns = new String[0];
            }
            try {
                int scope = settings.getInt("scope"); //$NON-NLS-1$
                boolean isRegExSearch = settings.getBoolean("isRegExSearch"); //$NON-NLS-1$
                boolean ignoreCase = settings.getBoolean("ignoreCase"); //$NON-NLS-1$
                boolean isWholeWord = settings.getBoolean("isWholeWord"); //$NON-NLS-1$
                boolean ignoreSystemMessages = settings.getBoolean("ignoreSystemMessages"); //$NON-NLS-1$
                boolean ignoreMessagesFromMe = settings.getBoolean("ignoreMessagesFromMe"); //$NON-NLS-1$
                String nickPrefixes = settings.get("nickPrefixes"); //$NON-NLS-1$
                String channels = settings.get("channels"); //$NON-NLS-1$
                TimeSpan timeSpan = TimeSpan.A_ANY_TIME;
                try {
                    String val = settings.get("timeSpan");
                    if (val != null) {
                        timeSpan = TimeSpan.valueOf(val); //$NON-NLS-1$
                    }
                } catch (IllegalArgumentException e) {
                }
                return new IrcSearchPatternData(textPattern, !ignoreCase, isRegExSearch, isWholeWord,
                        ignoreSystemMessages, ignoreMessagesFromMe, nickPrefixes, channels, timeSpan, fileNamePatterns,
                        scope, workingSets);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        public final String channels;
        public final String[] fileNamePatterns;
        public final Boolean ignoreMessagesFromMe;
        public final boolean ignoreSystemMessages;
        public final boolean isCaseSensitive;
        public final boolean isRegExSearch;
        public final boolean isWholeWord;

        public final String nickPrefixes;
        public final int scope;
        public final String textPattern;
        public final TimeSpan timeSpan;
        public final IWorkingSet[] workingSets;

        public IrcSearchPatternData(String textPattern, boolean isCaseSensitive, boolean isRegExSearch,
                boolean isWholeWord, boolean ignoreSystemMessages, Boolean ignoreMessagesFromMe, String fromNick,
                String channels, TimeSpan timeSpan, String[] fileNamePatterns, int scope, IWorkingSet[] workingSets) {
            Assert.isNotNull(fileNamePatterns);
            this.isCaseSensitive = isCaseSensitive;
            this.isRegExSearch = isRegExSearch;
            this.isWholeWord = isWholeWord;
            this.textPattern = textPattern;
            this.fileNamePatterns = fileNamePatterns;
            this.scope = scope;
            this.workingSets = workingSets; // can be null
            this.ignoreSystemMessages = ignoreSystemMessages;
            this.ignoreMessagesFromMe = ignoreMessagesFromMe;
            this.nickPrefixes = fromNick;
            this.channels = channels;
            this.timeSpan = timeSpan;
        }

        public void store(IDialogSettings settings) {
            settings.put("ignoreCase", !isCaseSensitive); //$NON-NLS-1$
            settings.put("isRegExSearch", isRegExSearch); //$NON-NLS-1$
            settings.put("isWholeWord", isWholeWord); //$NON-NLS-1$
            settings.put("ignoreSystemMessages", ignoreSystemMessages); //$NON-NLS-1$
            settings.put("ignoreMessagesFromMe", ignoreMessagesFromMe); //$NON-NLS-1$
            settings.put("textPattern", textPattern); //$NON-NLS-1$

            settings.put("channels", channels); //$NON-NLS-1$
            settings.put("nickPrefixes", nickPrefixes); //$NON-NLS-1$
            settings.put("timeSpan", timeSpan.name()); //$NON-NLS-1$

            settings.put("fileNamePatterns", fileNamePatterns); //$NON-NLS-1$
            settings.put("scope", scope); //$NON-NLS-1$
            if (workingSets != null) {
                String[] wsIds = new String[workingSets.length];
                for (int i = 0; i < workingSets.length; i++) {
                    wsIds[i] = workingSets[i].getLabel();
                }
                settings.put("workingSets", wsIds); //$NON-NLS-1$
            } else {
                settings.put("workingSets", new String[0]); //$NON-NLS-1$
            }

        }

    }

    public enum TimeSpan {
        A_ANY_TIME(IrcUiMessages.IrcSearchPage_anyTime) {
            @Override
            public OffsetDateTime getStart() {
                return null;
            }
        },
        B_TODAY(IrcUiMessages.IrcSearchPage_today) {
            @Override
            public OffsetDateTime getStart() {
                return OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS);
            }
        },
        C_LAST_WEEK(IrcUiMessages.IrcSearchPage_lastWeek) {
            @Override
            public OffsetDateTime getStart() {
                return OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS).minus(7, ChronoUnit.DAYS);
            }
        },
        D_LAST_MONTH(IrcUiMessages.IrcSearchPage_lastMonth) {
            @Override
            public OffsetDateTime getStart() {
                return OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS).minus(30, ChronoUnit.DAYS);
            }
        },
        E_LAST_YEAR(IrcUiMessages.IrcSearchPage_lastYear) {
            @Override
            public OffsetDateTime getStart() {
                return OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS).minus(1, ChronoUnit.YEARS);
            }
        },
        // F_CUSTOM(IrcUiMessages.IrcSearchPage_custom) {
        // @Override
        // public OffsetDateTime getStart() {
        // return null;
        // }
        // }
        ;
        private final String label;

        /**
         * @param label
         */
        private TimeSpan(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public abstract OffsetDateTime getStart();

    }

    private static final int HISTORY_SIZE = 12;
    public static final String ID = "org.l2x6.eircc.ui.search.IrcSearchPage"; //$NON-NLS-1$
    private static final String PAGE_NAME = "IrcSearchPage"; //$NON-NLS-1$
    private static final String STORE_CASE_SENSITIVE = "CASE_SENSITIVE"; //$NON-NLS-1$
    private static final String STORE_HISTORY = "HISTORY"; //$NON-NLS-1$
    private static final String STORE_HISTORY_SIZE = "HISTORY_SIZE"; //$NON-NLS-1$
    private static final String STORE_IGNORE_MESSAGES_FROM_ME = "STORE_IGNORE_MESSAGES_FROM_ME";
    private static final String STORE_IGNORE_SYSTEM_MESSAGES = "STORE_IGNORE_SYSTEM_MESSAGES";
    private static final String STORE_IS_REG_EX_SEARCH = "REG_EX_SEARCH"; //$NON-NLS-1$
    private static final String STORE_IS_WHOLE_WORD = "WHOLE_WORD"; //$NON-NLS-1$
    private Text channelsText;

    private boolean ignoreMessagesFromMe;
    private Button ignoreMessagesFromMeCheckbox;
    private boolean ignoreSystemMessages;
    private Button ignoreSystemMessagesCheckbox;
    private boolean isCaseSensitive;
    private Button isCaseSensitiveCheckbox;

    private boolean isFirstTime = true;
    private Button isRegExCheckbox;

    private boolean isRegExSearch;

    private boolean isWholeWord;
    private Button isWholeWordCheckbox;
    private Text nicksText;
    private ContentAssistCommandAdapter patterFieldContentAssist;
    private Combo patternCombo;
    private List<IrcSearchPatternData> previousSearchPatterns = new ArrayList<IrcSearchPatternData>(HISTORY_SIZE);
    // private IrcChannelsSelector channelsSelector;
    private ISearchPageContainer searchPageContainer;

    private CLabel statusLabel;
    private TimeSpan timeSpan = TimeSpan.A_ANY_TIME;
    private List<Button> timeSpanButtons = new ArrayList<Button>();
    private SelectionListener timeSpanListener = new SelectionAdapter() {
        public void widgetSelected(SelectionEvent event) {
            Button button = ((Button) event.widget);
            timeSpan = (TimeSpan) button.getData();
        }
    };

    private void addMessageControls(Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setFont(parent.getFont());
        GridLayout layout = new GridLayout(4, false);
        group.setLayout(layout);

        /* Ignore label */
        Label ignoreLabel = new Label(group, SWT.LEAD);
        ignoreLabel.setText(IrcUiMessages.IrcSearchPage_ignore);
        ignoreLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        ignoreLabel.setFont(group.getFont());

        /* Ignore system messages */
        ignoreSystemMessagesCheckbox = new Button(group, SWT.CHECK);
        ignoreSystemMessagesCheckbox.setText(IrcUiMessages.IrcSearchPage_ignoreSystemMessages);
        ignoreSystemMessagesCheckbox.setSelection(ignoreSystemMessages);
        ignoreSystemMessagesCheckbox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                ignoreSystemMessages = ignoreSystemMessagesCheckbox.getSelection();
            }
        });
        ignoreSystemMessagesCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        ignoreSystemMessagesCheckbox.setFont(group.getFont());

        /* Ignore messages from me */
        ignoreMessagesFromMeCheckbox = new Button(group, SWT.CHECK);
        ignoreMessagesFromMeCheckbox.setText(IrcUiMessages.IrcSearchPage_ignoreMessagesFromMe);
        ignoreMessagesFromMeCheckbox.setSelection(ignoreMessagesFromMe);
        ignoreMessagesFromMeCheckbox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                ignoreMessagesFromMe = ignoreMessagesFromMeCheckbox.getSelection();
            }
        });
        ignoreMessagesFromMeCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        ignoreMessagesFromMeCheckbox.setFont(group.getFont());
        /* place holder */
        new Label(group, SWT.LEAD);

        /* From nick label */
        Label fromNickLabel = new Label(group, SWT.LEAD);
        fromNickLabel.setText(IrcUiMessages.IrcSearchPage_fromNick);
        fromNickLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        fromNickLabel.setFont(group.getFont());

        /* From nick(s) */
        nicksText = new Text(group, SWT.SINGLE | SWT.BORDER);
        nicksText.setFont(group.getFont());
        GridData data = new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1);
        data.widthHint = convertWidthInCharsToPixels(30);
        nicksText.setLayoutData(data);

        Label fromNickAssist = new Label(group, SWT.LEAD);
        fromNickAssist.setText(IrcUiMessages.IrcSearchPage_fromNickAssist);
        fromNickAssist.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        fromNickAssist.setFont(group.getFont());

        /* In Channels label */
        Label inChannelsLabel = new Label(group, SWT.LEAD);
        inChannelsLabel.setText(IrcUiMessages.IrcSearchPage_inChannels);
        inChannelsLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        inChannelsLabel.setFont(group.getFont());

        /* In Channel(s) */
        channelsText = new Text(group, SWT.SINGLE | SWT.BORDER);
        channelsText.setFont(group.getFont());
        data = new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1);
        data.widthHint = convertWidthInCharsToPixels(30);
        channelsText.setLayoutData(data);

        Label inChannelsAssist = new Label(group, SWT.LEAD);
        inChannelsAssist.setText(IrcUiMessages.IrcSearchPage_inChannelsAssist);
        inChannelsAssist.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        inChannelsAssist.setFont(group.getFont());

        Label separator = new Label(group, SWT.NONE);
        separator.setVisible(false);
        data = new GridData(GridData.FILL, GridData.FILL, false, false, 4, 1);
        data.heightHint = convertHeightInCharsToPixels(1) / 3;
        separator.setLayoutData(data);

        /* Message time label */
        Label timeLabel = new Label(group, SWT.LEAD);
        timeLabel.setText(IrcUiMessages.IrcSearchPage_messageTime);
        timeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 4, 1));
        timeLabel.setFont(group.getFont());

        /* predefined time span radio buttons */
        for (TimeSpan timeSpan : TimeSpan.values()) {
            createTimeSpanRadio(timeSpan, group);
        }
        timeSpanButtons.get(TimeSpan.A_ANY_TIME.ordinal()).setSelection(true);

    }

    private void addTextPatternControls(Composite group) {
        // grid layout with 2 columns

        // Info text
        Label label = new Label(group, SWT.LEAD);
        label.setText(IrcUiMessages.IrcSearchPage_containingText_text);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
        label.setFont(group.getFont());

        // Pattern combo
        patternCombo = new Combo(group, SWT.SINGLE | SWT.BORDER);
        // Not done here to prevent page from resizing
        // fPattern.setItems(getPreviousSearchPatterns());
        patternCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                handleWidgetSelected();
                updateOKStatus();
            }
        });
        // add some listeners for regex syntax checking
        patternCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateOKStatus();
            }
        });
        patternCombo.setFont(group.getFont());
        GridData data = new GridData(GridData.FILL, GridData.FILL, true, false, 1, 2);
        data.widthHint = convertWidthInCharsToPixels(50);
        patternCombo.setLayoutData(data);

        ComboContentAdapter contentAdapter = new ComboContentAdapter();
        FindReplaceDocumentAdapterContentProposalProvider findProposer = new FindReplaceDocumentAdapterContentProposalProvider(
                true);
        patterFieldContentAssist = new ContentAssistCommandAdapter(patternCombo, contentAdapter, findProposer,
                ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, new char[0], true);
        patterFieldContentAssist.setEnabled(isRegExSearch);

        isCaseSensitiveCheckbox = new Button(group, SWT.CHECK);
        isCaseSensitiveCheckbox.setText(IrcUiMessages.IrcSearchPage_caseSensitive);
        isCaseSensitiveCheckbox.setSelection(isCaseSensitive);
        isCaseSensitiveCheckbox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                isCaseSensitive = isCaseSensitiveCheckbox.getSelection();
            }
        });
        isCaseSensitiveCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        isCaseSensitiveCheckbox.setFont(group.getFont());

        // RegEx checkbox
        isRegExCheckbox = new Button(group, SWT.CHECK);
        isRegExCheckbox.setText(IrcUiMessages.IrcSearchPage_regularExpression);
        isRegExCheckbox.setSelection(isRegExSearch);

        isRegExCheckbox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                isRegExSearch = isRegExCheckbox.getSelection();
                updateOKStatus();

                writeConfiguration();
                patterFieldContentAssist.setEnabled(isRegExSearch);
                isWholeWordCheckbox.setEnabled(!isRegExSearch);
            }
        });
        isRegExCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 2));
        isRegExCheckbox.setFont(group.getFont());

        // Text line which explains the special characters
        statusLabel = new CLabel(group, SWT.LEAD);
        statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 2));
        statusLabel.setFont(group.getFont());
        statusLabel.setAlignment(SWT.LEFT);
        statusLabel.setText(IrcUiMessages.IrcSearchPage_containingText_hint);

        // Whole Word checkbox
        isWholeWordCheckbox = new Button(group, SWT.CHECK);
        isWholeWordCheckbox.setText(IrcUiMessages.IrcSearchPage_wholeWord);
        isWholeWordCheckbox.setSelection(isWholeWord);
        isWholeWordCheckbox.setEnabled(!isRegExSearch);
        isWholeWordCheckbox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                isWholeWord = isWholeWordCheckbox.getSelection();
            }
        });
        isWholeWordCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        isWholeWordCheckbox.setFont(group.getFont());

    }

    public void createControl(Composite parent) {
        initializeDialogUnits(parent);
        readConfiguration();

        Composite result = new Composite(parent, SWT.NONE);
        result.setFont(parent.getFont());
        GridLayout layout = new GridLayout(2, false);
        result.setLayout(layout);

        addTextPatternControls(result);

        addMessageControls(result);

        setControl(result);
        Dialog.applyDialogFont(result);
    }

    public FileTextSearchScope createTextSearchScope() throws CoreException {
        return getAllScope();
    }

    private void createTimeSpanRadio(TimeSpan timeSpan, Composite group) {
        Button r = new Button(group, SWT.RADIO);
        r.setText(timeSpan.getLabel());
        r.setFont(group.getFont());
        r.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 4, 1));
        r.addSelectionListener(timeSpanListener);
        r.setData(timeSpan);
        timeSpanButtons.add(r);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.DialogPage#dispose()
     */
    public void dispose() {
        writeConfiguration();
        super.dispose();
    }

    private IrcSearchPatternData findInPrevious(String pattern) {
        for (IrcSearchPatternData element : previousSearchPatterns) {
            if (pattern.equals(element.textPattern)) {
                return element;
            }
        }
        return null;
    }

    private FileTextSearchScope getAllScope() throws CoreException {
        List<IResource> resources = new ArrayList<IResource>();
        for (IrcAccount account : IrcModel.getInstance().getSearchableAccounts()) {
            for (AbstractIrcChannel channel : account.getSearchableChannels()) {
                for (IFile log : channel.listSearchableLogFiles()) {
                    resources.add(log);
                }
            }
        }
        IResource[] arr = (IResource[]) resources.toArray(new IResource[resources.size()]);
        return FileTextSearchScope.newSearchScope(arr, getExtensions(), false);
    }

    private ISearchPageContainer getContainer() {
        return searchPageContainer;
    }

    /**
     * Returns the page settings for this Text search page.
     *
     * @return the page settings to be used
     */
    @SuppressWarnings("restriction")
    private IDialogSettings getDialogSettings() {
        return org.eclipse.search.internal.ui.SearchPlugin.getDefault().getDialogSettingsSection(PAGE_NAME);
    }

    @SuppressWarnings("unused")
    private FileTextSearchScope getEnclosingProjectScope() {
        String[] enclosingProjectName = getContainer().getSelectedProjectNames();
        if (enclosingProjectName == null) {
            return FileTextSearchScope.newWorkspaceScope(getExtensions(), false);
        }

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IResource[] res = new IResource[enclosingProjectName.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = root.getProject(enclosingProjectName[i]);
        }

        return FileTextSearchScope.newSearchScope(res, getExtensions(), false);
    }

    private String[] getExtensions() {
        return new String[] { "*" }; // channelsSelector.getChannelNames();
    }

    /**
     * Return search pattern data and update previous searches. An existing
     * entry will be updated.
     *
     * @return the search pattern data
     */
    private IrcSearchPatternData getPatternData() {
        IrcSearchPatternData match = findInPrevious(patternCombo.getText());
        if (match != null) {
            previousSearchPatterns.remove(match);
        }
        match = new IrcSearchPatternData(patternCombo.getText(), isCaseSensitive(), isRegExCheckbox.getSelection(),
                isWholeWordCheckbox.getSelection(), ignoreSystemMessagesCheckbox.getSelection(),
                ignoreMessagesFromMeCheckbox.getSelection(), nicksText.getText(), channelsText.getText(), timeSpan,
                getExtensions(), getContainer().getSelectedScope(), getContainer().getSelectedWorkingSets());
        previousSearchPatterns.add(0, match);
        return match;
    }

    private String[] getPreviousSearchPatterns() {
        int size = previousSearchPatterns.size();
        String[] patterns = new String[size];
        for (int i = 0; i < size; i++)
            patterns[i] = ((IrcSearchPatternData) previousSearchPatterns.get(i)).textPattern;
        return patterns;
    }

    @SuppressWarnings("unused")
    private FileTextSearchScope getSelectedResourcesScope() {
        Set<IResource> resources = new HashSet<IResource>();
        ISelection sel = getContainer().getSelection();
        if (sel instanceof IStructuredSelection && !sel.isEmpty()) {
            Iterator<?> iter = ((IStructuredSelection) sel).iterator();
            while (iter.hasNext()) {
                Object curr = iter.next();
                if (curr instanceof IAdaptable) {
                    IResource resource = (IResource) ((IAdaptable) curr).getAdapter(IResource.class);
                    if (resource != null && resource.isAccessible()) {
                        resources.add(resource);
                    }
                }
            }
        } else if (getContainer().getActiveEditorInput() != null) {
            resources.add((IResource) getContainer().getActiveEditorInput().getAdapter(IFile.class));
        }
        IResource[] arr = (IResource[]) resources.toArray(new IResource[resources.size()]);
        return FileTextSearchScope.newSearchScope(arr, getExtensions(), false);
    }

    private ISelection getSelection() {
        return searchPageContainer.getSelection();
    }

    private void handleWidgetSelected() {
        int selectionIndex = patternCombo.getSelectionIndex();
        if (selectionIndex < 0 || selectionIndex >= previousSearchPatterns.size())
            return;

        IrcSearchPatternData patternData = (IrcSearchPatternData) previousSearchPatterns.get(selectionIndex);
        if (!patternCombo.getText().equals(patternData.textPattern))
            return;
        isCaseSensitiveCheckbox.setSelection(patternData.isCaseSensitive);
        isRegExSearch = patternData.isRegExSearch;
        isRegExCheckbox.setSelection(isRegExSearch);
        isWholeWord = patternData.isWholeWord;
        isWholeWordCheckbox.setSelection(isWholeWord);
        isWholeWordCheckbox.setEnabled(!isRegExSearch);
        patternCombo.setText(patternData.textPattern);
        patterFieldContentAssist.setEnabled(isRegExSearch);

        if (patternData.workingSets != null)
            getContainer().setSelectedWorkingSets(patternData.workingSets);
        else
            getContainer().setSelectedScope(patternData.scope);
    }

    @SuppressWarnings("unused")
    private boolean initializePatternControl() {
        ISelection selection = getSelection();
        if (selection instanceof ITextSelection && !selection.isEmpty() && ((ITextSelection) selection).getLength() > 0) {
            String text = ((ITextSelection) selection).getText();
            if (text != null) {
                if (isRegExSearch) {
                    patternCombo.setText(FindReplaceDocumentAdapter.escapeForRegExPattern(text));
                } else {
                    patternCombo.setText(insertEscapeChars(text));
                }
                return true;
            }
        }
        return false;
    }

    private String insertEscapeChars(String text) {
        if (text == null || text.equals("")) { //$NON-NLS-1$
            return ""; //$NON-NLS-1$
        }
        StringBuffer sbIn = new StringBuffer(text);
        BufferedReader reader = new BufferedReader(new StringReader(text));
        int lengthOfFirstLine = 0;
        try {
            lengthOfFirstLine = reader.readLine().length();
        } catch (IOException ex) {
            return ""; //$NON-NLS-1$
        }
        StringBuffer sbOut = new StringBuffer(lengthOfFirstLine + 5);
        int i = 0;
        while (i < lengthOfFirstLine) {
            char ch = sbIn.charAt(i);
            if (ch == '*' || ch == '?' || ch == '\\')
                sbOut.append("\\"); //$NON-NLS-1$
            sbOut.append(ch);
            i++;
        }
        return sbOut.toString();
    }

    private boolean isCaseSensitive() {
        return isCaseSensitiveCheckbox.getSelection();
    }

    private ISearchQuery newQuery() throws CoreException {
        IrcSearchPatternData data = getPatternData();
        return new IrcSearchQuery(data.textPattern, data.isWholeWord && !data.isRegExSearch, data,
                createTextSearchScope());
    }

    public boolean performAction() {
        try {
            NewSearchUI.runQueryInBackground(newQuery());
        } catch (CoreException e) {
            ErrorDialog.openError(getShell(), IrcUiMessages.IrcSearchPage_searchproblems_title,
                    IrcUiMessages.IrcSearchPage_searchproblems_message, e.getStatus());
            return false;
        }
        return true;
    }

    /**
     * Initializes itself from the stored page settings.
     */
    private void readConfiguration() {
        IDialogSettings s = getDialogSettings();
        isCaseSensitive = s.getBoolean(STORE_CASE_SENSITIVE);
        isRegExSearch = s.getBoolean(STORE_IS_REG_EX_SEARCH);
        isWholeWord = s.getBoolean(STORE_IS_WHOLE_WORD);
        ignoreMessagesFromMe = s.getBoolean(STORE_IGNORE_MESSAGES_FROM_ME);
        ignoreSystemMessages = s.getBoolean(STORE_IGNORE_SYSTEM_MESSAGES);

        try {
            int historySize = s.getInt(STORE_HISTORY_SIZE);
            for (int i = 0; i < historySize; i++) {
                IDialogSettings histSettings = s.getSection(STORE_HISTORY + i);
                if (histSettings != null) {
                    IrcSearchPatternData data = IrcSearchPatternData.create(histSettings);
                    if (data != null) {
                        previousSearchPatterns.add(data);
                    }
                }
            }
        } catch (NumberFormatException e) {
            /* ignore */
        }
    }

    /**
     * @see org.eclipse.search.ui.ISearchPage#setContainer(org.eclipse.search.ui.ISearchPageContainer)
     */
    public void setContainer(ISearchPageContainer container) {
        searchPageContainer = container;
    }

    /**
     * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
     */
    public void setVisible(boolean visible) {
        if (visible && patternCombo != null) {
            if (isFirstTime) {
                isFirstTime = false;
                // Set item and text here to prevent page from resizing
                patternCombo.setItems(getPreviousSearchPatterns());
                // searchInChannelsCombo.setItems(fPreviousExtensions);
                // if (fExtensions.getItemCount() == 0) {
                // loadFilePatternDefaults();
                // }
                // if (!initializePatternControl()) {
                // fPattern.select(0);
                //                    searchInChannelsCombo.setText("*"); //$NON-NLS-1$
                // handleWidgetSelected();
                // }
            }
            patternCombo.setFocus();
        }
        updateOKStatus();

        IEditorInput editorInput = getContainer().getActiveEditorInput();
        getContainer().setActiveEditorCanProvideScopeSelection(
                editorInput != null && editorInput.getAdapter(IFile.class) != null);

        super.setVisible(visible);
    }

    private void statusMessage(boolean error, String message) {
        statusLabel.setText(message);
        if (error)
            statusLabel.setForeground(JFaceColors.getErrorText(statusLabel.getDisplay()));
        else
            statusLabel.setForeground(null);
    }

    private void updateOKStatus() {
        boolean regexStatus = validateRegex();
        boolean hasFilePattern = true; // searchInChannelsCombo.getText().length()
                                       // > 0;
        getContainer().setPerformActionEnabled(regexStatus && hasFilePattern);
    }

    @SuppressWarnings("restriction")
    private boolean validateRegex() {
        if (isRegExCheckbox.getSelection()) {
            try {
                org.eclipse.search.internal.core.text.PatternConstructor.createPattern(patternCombo.getText(),
                        isCaseSensitive, true);
            } catch (PatternSyntaxException e) {
                String locMessage = e.getLocalizedMessage();
                int i = 0;
                while (i < locMessage.length() && "\n\r".indexOf(locMessage.charAt(i)) == -1) { //$NON-NLS-1$
                    i++;
                }
                statusMessage(true, locMessage.substring(0, i)); // only take
                                                                 // first line
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
        IDialogSettings s = getDialogSettings();
        s.put(STORE_CASE_SENSITIVE, isCaseSensitive);
        s.put(STORE_IS_REG_EX_SEARCH, isRegExSearch);
        s.put(STORE_IS_WHOLE_WORD, isWholeWord);
        s.put(STORE_IGNORE_MESSAGES_FROM_ME, ignoreMessagesFromMe);
        s.put(STORE_IGNORE_SYSTEM_MESSAGES, ignoreSystemMessages);

        int historySize = Math.min(previousSearchPatterns.size(), HISTORY_SIZE);
        s.put(STORE_HISTORY_SIZE, historySize);
        for (int i = 0; i < historySize; i++) {
            IDialogSettings histSettings = s.addNewSection(STORE_HISTORY + i);
            IrcSearchPatternData data = ((IrcSearchPatternData) previousSearchPatterns.get(i));
            data.store(histSettings);
        }
    }

}
