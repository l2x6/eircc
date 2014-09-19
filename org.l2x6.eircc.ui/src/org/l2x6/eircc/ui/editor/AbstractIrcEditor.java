/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IFindReplaceTargetExtension;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ISelectionValidator;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.hyperlink.HyperlinkManager;
import org.eclipse.jface.text.revisions.RevisionInformation;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.OverviewRuler;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IKeyBindingService;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.INavigationLocation;
import org.eclipse.ui.INavigationLocationProvider;
import org.eclipse.ui.IPersistableEditor;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.ui.internal.texteditor.EditPosition;
import org.eclipse.ui.internal.texteditor.NLSUtility;
import org.eclipse.ui.internal.texteditor.TextEditorPlugin;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;
import org.eclipse.ui.texteditor.FindNextAction;
import org.eclipse.ui.texteditor.FindReplaceAction;
import org.eclipse.ui.texteditor.GotoAnnotationAction;
import org.eclipse.ui.texteditor.GotoLineAction;
import org.eclipse.ui.texteditor.IAbstractTextEditorHelpContextIds;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.texteditor.IReadOnlyDependent;
import org.eclipse.ui.texteditor.IStatusField;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.ITextEditorExtension;
import org.eclipse.ui.texteditor.ITextEditorExtension4;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;
import org.eclipse.ui.texteditor.IncrementalFindAction;
import org.eclipse.ui.texteditor.KeyBindingSupportForAssistant;
import org.eclipse.ui.texteditor.MarkerAnnotationPreferences;
import org.eclipse.ui.texteditor.MoveLinesAction;
import org.eclipse.ui.texteditor.RecenterAction;
import org.eclipse.ui.texteditor.ResourceAction;
import org.eclipse.ui.texteditor.ShowWhitespaceCharactersAction;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.texteditor.TextSelectionNavigationLocation;
import org.l2x6.eircc.ui.IrcUiMessages;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public abstract class AbstractIrcEditor extends EditorPart implements ITextEditor, IReusableEditor, ITextEditorExtension,
ITextEditorExtension4, INavigationLocationProvider, IPersistableEditor {

    /**
     * Internal implementation class for a change listener.
     *
     * @since 3.0
     */
    protected abstract class AbstractSelectionChangedListener implements ISelectionChangedListener {

        /**
         * Installs this selection changed listener with the given selection
         * provider. If the selection provider is a post selection provider,
         * post selection changed events are the preferred choice, otherwise
         * normal selection changed events are requested.
         *
         * @param selectionProvider
         *            the selection provider
         */
        public void install(ISelectionProvider selectionProvider) {
            if (selectionProvider == null)
                return;

            if (selectionProvider instanceof IPostSelectionProvider) {
                IPostSelectionProvider provider = (IPostSelectionProvider) selectionProvider;
                provider.addPostSelectionChangedListener(this);
            } else {
                selectionProvider.addSelectionChangedListener(this);
            }
        }

        /**
         * Removes this selection changed listener from the given selection
         * provider.
         *
         * @param selectionProvider
         *            the selection provider
         */
        public void uninstall(ISelectionProvider selectionProvider) {
            if (selectionProvider == null)
                return;

            if (selectionProvider instanceof IPostSelectionProvider) {
                IPostSelectionProvider provider = (IPostSelectionProvider) selectionProvider;
                provider.removePostSelectionChangedListener(this);
            } else {
                selectionProvider.removeSelectionChangedListener(this);
            }
        }
    }

    /**
     * Representation of action activation codes.
     */
    static class ActionActivationCode {

        /** The action id. */
        public String fActionId;
        /** The character. */
        public char fCharacter;
        /** The key code. */
        public int fKeyCode = -1;
        /** The state mask. */
        public int fStateMask = SWT.DEFAULT;

        /**
         * Creates a new action activation code for the given action id.
         *
         * @param actionId
         *            the action id
         */
        public ActionActivationCode(String actionId) {
            fActionId = actionId;
        }

        /**
         * Returns <code>true</code> if this activation code matches the given
         * verify event.
         *
         * @param event
         *            the event to test for matching
         * @return whether this activation code matches <code>event</code>
         */
        public boolean matches(VerifyEvent event) {
            return (event.character == fCharacter && (fKeyCode == -1 || event.keyCode == fKeyCode) && (fStateMask == SWT.DEFAULT || event.stateMask == fStateMask));
        }
    }

    /**
     * Internal key verify listener for triggering action activation codes.
     */
    class ActivationCodeTrigger implements VerifyKeyListener {

        /** Indicates whether this trigger has been installed. */
        private boolean fIsInstalled = false;
        /**
         * The key binding service to use.
         *
         * @since 2.0
         */
        private IKeyBindingService fKeyBindingService;

        /**
         * Installs this trigger on the editor's text widget.
         *
         * @since 2.0
         */
        public void install() {
            if (!fIsInstalled) {

                if (logViewer instanceof ITextViewerExtension) {
                    ITextViewerExtension e = (ITextViewerExtension) logViewer;
                    e.prependVerifyKeyListener(this);
                } else {
                    StyledText text = logViewer.getTextWidget();
                    text.addVerifyKeyListener(this);
                }

                fKeyBindingService = getEditorSite().getKeyBindingService();
                fIsInstalled = true;
            }
        }

        /**
         * Registers the given action for key activation.
         *
         * @param action
         *            the action to be registered
         * @since 2.0
         */
        public void registerActionForKeyActivation(IAction action) {
            if (fIsInstalled && action.getActionDefinitionId() != null)
                fKeyBindingService.registerAction(action);
        }

        /**
         * Sets the key binding scopes for this editor.
         *
         * @param keyBindingScopes
         *            the key binding scopes
         * @since 2.1
         */
        public void setScopes(String[] keyBindingScopes) {
            if (keyBindingScopes != null && keyBindingScopes.length > 0)
                fKeyBindingService.setScopes(keyBindingScopes);
        }

        /**
         * Uninstalls this trigger from the editor's text widget.
         *
         * @since 2.0
         */
        public void uninstall() {
            if (fIsInstalled) {

                if (logViewer instanceof ITextViewerExtension) {
                    ITextViewerExtension e = (ITextViewerExtension) logViewer;
                    e.removeVerifyKeyListener(this);
                } else if (logViewer != null) {
                    StyledText text = logViewer.getTextWidget();
                    if (text != null && !text.isDisposed())
                        text.removeVerifyKeyListener(fActivationCodeTrigger);
                }

                fIsInstalled = false;
                fKeyBindingService = null;
            }
        }

        /**
         * The given action is no longer available for key activation
         *
         * @param action
         *            the action to be unregistered
         * @since 2.0
         */
        public void unregisterActionFromKeyActivation(IAction action) {
            if (fIsInstalled && action.getActionDefinitionId() != null)
                fKeyBindingService.unregisterAction(action);
        }

        /*
         * @see VerifyKeyListener#verifyKey(org.eclipse.swt.events.VerifyEvent)
         */
        public void verifyKey(VerifyEvent event) {

            ActionActivationCode code = null;
            int size = fActivationCodes.size();
            for (int i = 0; i < size; i++) {
                code = (ActionActivationCode) fActivationCodes.get(i);
                if (code.matches(event)) {
                    IAction action = getAction(code.fActionId);
                    if (action != null) {

                        if (action instanceof IUpdate)
                            ((IUpdate) action).update();

                        if (!action.isEnabled() && action instanceof IReadOnlyDependent) {
                            IReadOnlyDependent dependent = (IReadOnlyDependent) action;
                            boolean writable = dependent.isEnabled(true);
                            if (writable) {
                                event.doit = false;
                                return;
                            }
                        } else if (action.isEnabled()) {
                            event.doit = false;
                            action.run();
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Internal interface for a cursor listener. I.e. aggregation of mouse and
     * key listener.
     *
     * @since 2.0
     */
    interface ICursorListener extends MouseListener, KeyListener {
    }

    /**
     * Data structure for the position label value.
     */
    private static class PositionLabelValue {

        public int fValue;

        public String toString() {
            return String.valueOf(fValue);
        }
    }

    /**
     * This selection listener allows the SelectionProvider to implement
     * {@link ISelectionValidator}.
     *
     * @since 3.0
     */
    private class SelectionListener extends AbstractSelectionChangedListener implements IDocumentListener {

        private IDocument fDocument;
        private Object fPostSelection = INVALID_SELECTION;

        /*
         * @see
         * org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged
         * (org.eclipse.jface.text.DocumentEvent)
         *
         * @since 3.0
         */
        public synchronized void documentAboutToBeChanged(DocumentEvent event) {
            fPostSelection = INVALID_SELECTION;
        }

        /*
         * @see
         * org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse
         * .jface.text.DocumentEvent)
         *
         * @since 3.0
         */
        public void documentChanged(DocumentEvent event) {
        }

        /*
         * @see org.eclipse.ui.texteditor.AbstractTextEditor.
         * AbstractSelectionChangedListener
         * #install(org.eclipse.jface.viewers.ISelectionProvider)
         *
         * @since 3.0
         */
        public void install(ISelectionProvider selectionProvider) {
            super.install(selectionProvider);

            if (selectionProvider != null)
                selectionProvider.addSelectionChangedListener(this);
        }

        public synchronized boolean isValid(ISelection selection) {
            return fPostSelection != INVALID_SELECTION && fPostSelection == selection;
        }

        /*
         * @see
         * org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged
         * (org.eclipse.jface.viewers.SelectionChangedEvent)
         */
        public synchronized void selectionChanged(SelectionChangedEvent event) {
            fPostSelection = event.getSelection();
        }

        public void setDocument(IDocument document) {
            if (fDocument != null)
                fDocument.removeDocumentListener(this);

            fDocument = document;
            if (fDocument != null)
                fDocument.addDocumentListener(this);
        }

        /*
         * @see org.eclipse.ui.texteditor.AbstractTextEditor.
         * AbstractSelectionChangedListener
         * #uninstall(org.eclipse.jface.viewers.ISelectionProvider)
         *
         * @since 3.0
         */
        public void uninstall(ISelectionProvider selectionProvider) {
            if (selectionProvider != null)
                selectionProvider.removeSelectionChangedListener(this);

            if (fDocument != null) {
                fDocument.removeDocumentListener(this);
                fDocument = null;
            }
            super.uninstall(selectionProvider);
        }
    }

    /**
     * Editor specific selection provider which wraps the source viewer's
     * selection provider.
     *
     * @since 3.4 protected, was added in 2.1 as private class
     */
    protected class SelectionProvider implements IPostSelectionProvider, ISelectionValidator {

        /*
         * @see org.eclipse.jface.text.IPostSelectionProvider#
         * addPostSelectionChangedListener
         * (org.eclipse.jface.viewers.ISelectionChangedListener)
         *
         * @since 3.0
         */
        public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
            if (logViewer != null) {
                if (logViewer.getSelectionProvider() instanceof IPostSelectionProvider) {
                    IPostSelectionProvider provider = (IPostSelectionProvider) logViewer.getSelectionProvider();
                    provider.addPostSelectionChangedListener(listener);
                }
            }
        }

        /*
         * @see
         * org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener
         * (ISelectionChangedListener)
         */
        public void addSelectionChangedListener(ISelectionChangedListener listener) {
            if (logViewer != null)
                logViewer.getSelectionProvider().addSelectionChangedListener(listener);
        }

        /*
         * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
         */
        public ISelection getSelection() {
            return doGetSelection();
        }

        /*
         * @see org.eclipse.jface.text.IPostSelectionValidator#isValid()
         *
         * @since 3.0
         */
        public boolean isValid(ISelection postSelection) {
            return fSelectionListener != null && fSelectionListener.isValid(postSelection);
        }

        /*
         * @see org.eclipse.jface.text.IPostSelectionProvider#
         * removePostSelectionChangedListener
         * (org.eclipse.jface.viewers.ISelectionChangedListener)
         *
         * @since 3.0
         */
        public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
            if (logViewer != null) {
                if (logViewer.getSelectionProvider() instanceof IPostSelectionProvider) {
                    IPostSelectionProvider provider = (IPostSelectionProvider) logViewer.getSelectionProvider();
                    provider.removePostSelectionChangedListener(listener);
                }
            }
        }

        /*
         * @see org.eclipse.jface.viewers.ISelectionProvider#
         * removeSelectionChangedListener(ISelectionChangedListener)
         */
        public void removeSelectionChangedListener(ISelectionChangedListener listener) {
            if (logViewer != null)
                logViewer.getSelectionProvider().removeSelectionChangedListener(listener);
        }

        /*
         * @see
         * org.eclipse.jface.viewers.ISelectionProvider#setSelection(ISelection)
         */
        public void setSelection(ISelection selection) {
            doSetSelection(selection);
        }
    }

    /**
     * Internal text listener for updating all content dependent actions. The
     * updating is done asynchronously.
     */
    class TextListener implements ITextListener, ITextInputListener {

        /** Display used for posting the updater code. */
        private Display fDisplay;

        /**
         * Has the runnable been posted?
         *
         * @since 3.0
         */
        private boolean fIsRunnablePosted = false;
        /**
         * The editor's last edit position
         *
         * @since 3.0
         */
        private Position fLocalLastEditPosition;
        /** The posted updater code. */
        private Runnable fRunnable = new Runnable() {
            public void run() {
                fIsRunnablePosted = false;

                if (logViewer != null) {
                    updateContentDependentActions();

                    // remember the last edit position
                    if (isDirty() && fUpdateLastEditPosition) {
                        fUpdateLastEditPosition = false;
                        ISelection sel = getSelectionProvider().getSelection();
                        IEditorInput input = getEditorInput();
                        IDocument document = getDocumentProvider().getDocument(input);

                        if (fLocalLastEditPosition != null) {
                            document.removePosition(fLocalLastEditPosition);
                            fLocalLastEditPosition = null;
                        }

                        if (sel instanceof ITextSelection && !sel.isEmpty()) {
                            ITextSelection s = (ITextSelection) sel;
                            fLocalLastEditPosition = new Position(s.getOffset(), s.getLength());
                            try {
                                document.addPosition(fLocalLastEditPosition);
                            } catch (BadLocationException ex) {
                                fLocalLastEditPosition = null;
                            }
                        }

                        IEditorSite editorSite = getEditorSite();
                        if (editorSite instanceof MultiPageEditorSite)
                            editorSite = ((MultiPageEditorSite) editorSite).getMultiPageEditor().getEditorSite();
                        TextEditorPlugin.getDefault().setLastEditPosition(
                                new EditPosition(input, editorSite.getId(), fLocalLastEditPosition));
                    }
                }
            }
        };
        /**
         * Should the last edit position be updated?
         *
         * @since 3.0
         */
        private boolean fUpdateLastEditPosition = false;

        /*
         * @see
         * org.eclipse.jface.text.ITextInputListener#inputDocumentAboutToBeChanged
         * (org.eclipse.jface.text.IDocument, org.eclipse.jface.text.IDocument)
         */
        public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
            if (oldInput != null && fLocalLastEditPosition != null) {
                oldInput.removePosition(fLocalLastEditPosition);
                fLocalLastEditPosition = null;
            }
        }

        /*
         * @see
         * org.eclipse.jface.text.ITextInputListener#inputDocumentChanged(org
         * .eclipse.jface.text.IDocument, org.eclipse.jface.text.IDocument)
         */
        public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
        }

        /*
         * @see ITextListener#textChanged(TextEvent)
         */
        public void textChanged(TextEvent event) {

            /*
             * Also works for text events which do not base on a DocumentEvent.
             * This way, if the visible document of the viewer changes, all
             * content dependent actions are updated as well.
             */

            if (fDisplay == null)
                fDisplay = getSite().getShell().getDisplay();

            if (event.getDocumentEvent() != null)
                fUpdateLastEditPosition = true;

            if (!fIsRunnablePosted) {
                fIsRunnablePosted = true;
                fDisplay.asyncExec(fRunnable);
            }
        }
    }

    private static final Object INVALID_SELECTION = new Object();

    /** The width of the vertical ruler. */
    protected static final int VERTICAL_RULER_WIDTH = 12;

    /**
     * Returns the minimal region of the given source viewer's document that
     * completely comprises everything that is visible in the viewer's widget.
     *
     * @param viewer
     *            the viewer go return the coverage for
     * @return the minimal region of the source viewer's document comprising the
     *         contents of the viewer's widget
     * @since 2.1
     */
    protected static final IRegion getCoverage(ISourceViewer viewer) {
        if (viewer instanceof ITextViewerExtension5) {
            ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
            return extension.getModelCoverage();
        }
        return viewer.getVisibleRegion();
    }

    /**
     * Tells whether the given region is visible in the given source viewer.
     *
     * @param viewer
     *            the source viewer
     * @param offset
     *            the offset of the region
     * @param length
     *            the length of the region
     * @return <code>true</code> if visible
     * @since 2.1
     */
    protected static final boolean isVisible(ISourceViewer viewer, int offset, int length) {
        if (viewer instanceof ITextViewerExtension5) {
            ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
            IRegion overlap = extension.modelRange2WidgetRange(new Region(offset, length));
            return overlap != null;
        }
        return viewer.overlapsWithVisibleRegion(offset, length);
    }

    /**
     * Returns the offset of the given source viewer's document that corresponds
     * to the given widget offset or <code>-1</code> if there is no such offset.
     *
     * @param viewer
     *            the source viewer
     * @param widgetOffset
     *            the widget offset
     * @return the corresponding offset in the source viewer's document or
     *         <code>-1</code>
     * @since 2.1
     */
    protected static final int widgetOffset2ModelOffset(ISourceViewer viewer, int widgetOffset) {
        if (viewer instanceof ITextViewerExtension5) {
            ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
            return extension.widgetOffset2ModelOffset(widgetOffset);
        }
        return widgetOffset + viewer.getVisibleRegion().getOffset();
    }

    /** The actions registered with the editor. */
    private Map fActions = new HashMap(10);

    private List fActivationCodes = new ArrayList(2);

    /** The verify key listener for activation code triggering. */
    private ActivationCodeTrigger fActivationCodeTrigger = new ActivationCodeTrigger();
    /**
     * Helper for accessing annotation from the perspective of this editor.
     *
     * <p>
     * This field should not be referenced by subclasses. It is
     * <code>protected</code> for API compatibility reasons and will be made
     * <code>private</code> soon. Use {@link #getAnnotationAccess()} instead.
     * </p>
     */
    protected IAnnotationAccess fAnnotationAccess;
    private MarkerAnnotationPreferences fAnnotationPreferences;

    /**
     * The editor's background color.
     *
     * @since 2.0
     */
    private Color fBackgroundColor;

    /** The position label value of the current column. */
    private final PositionLabelValue fColumnLabel = new PositionLabelValue();

    private SourceViewerConfiguration fConfiguration;

    /** The actions marked as content dependent. */
    private List fContentActions = new ArrayList(5);

    private ICursorListener fCursorListener;
    /** The editor's context menu id. */
    private String fEditorContextMenuId;

    /**
     * The error message shown in the status line in case of failed information
     * look up.
     */
    protected final String fErrorLabel = IrcUiMessages.Editor_statusline_error_label;
    /**
     * The find scope's highlight color.
     *
     * @since 2.0
     */
    private Color fFindScopeHighlightColor;

    /** The editor's font. */
    private Font fFont;
    private Color fForegroundColor;

    /**
     * Key binding support for the quick assist assistant.
     *
     * @since 3.5
     */
    private KeyBindingSupportForAssistant fKeyBindingSupportForContentAssistant;
    private final PositionLabelValue fLineLabel = new PositionLabelValue();

    /** Context menu listener. */
    private IMenuListener fMenuListener;
    private IOverviewRuler fOverviewRuler;
    private final String fPositionLabelPattern = IrcUiMessages.Editor_statusline_position_pattern;
    /** The arguments for the position label pattern. */
    private final Object[] fPositionLabelPatternArguments = new Object[] { fLineLabel, fColumnLabel };

    /** The editor's preference store. */
    private IPreferenceStore fPreferenceStore;

    /**
     * The actions marked as property dependent.
     *
     * @since 2.0
     */
    private List fPropertyActions = new ArrayList(5);
    /** The editor's range indicator. */
    private Annotation fRangeIndicator;
    /** The actions marked as selection dependent. */
    private List fSelectionActions = new ArrayList(5);
    /**
     * The editor's selection background color.
     *
     * @since 3.0
     */
    private Color fSelectionBackgroundColor;

    /** Selection changed listener. */
    private ISelectionChangedListener fSelectionChangedListener;
    /**
     * The editor's selection foreground color.
     *
     * @since 3.0
     */
    private Color fSelectionForegroundColor;
    /**
     * The editor's selection listener.
     *
     * @since 3.0
     */
    private SelectionListener fSelectionListener;

    private SelectionProvider fSelectionProvider = new SelectionProvider();

    /** The editor's presentation mode. */
    private boolean fShowHighlightRangeOnly;

    protected IrcLogViewer logViewer;
    /**
     * Helper for managing the decoration support of this editor's viewer.
     *
     * <p>
     * This field should not be referenced by subclasses. It is
     * <code>protected</code> for API compatibility reasons and will be made
     * <code>private</code> soon. Use
     * {@link #getSourceViewerDecorationSupport(ISourceViewer)} instead.
     * </p>
     */
    protected SourceViewerDecorationSupport fSourceViewerDecorationSupport;

    /**
     * The actions marked as state dependent.
     *
     * @since 2.0
     */
    private List fStateActions = new ArrayList(5);
    /**
     * The map of the editor's status fields.
     *
     * @since 2.0
     */
    private Map fStatusFields;

    /** The text context menu to be disposed. */
    private Menu fTextContextMenu;

    /** The editor's text listener. */
    private TextListener fTextListener = new TextListener();

    /**
     *
     */
    public AbstractIrcEditor() {
        super();
        fAnnotationPreferences = EditorsPlugin.getDefault().getMarkerAnnotationPreferences();
    }

    /**
     * Convenience method to add the action installed under the given action id
     * to the given menu.
     *
     * @param menu
     *            the menu to add the action to
     * @param actionId
     *            the id of the action to be added
     */
    protected final void addAction(IMenuManager menu, String actionId) {
        IAction action = getAction(actionId);
        if (action != null) {
            if (action instanceof IUpdate)
                ((IUpdate) action).update();
            menu.add(action);
        }
    }

    /**
     * Convenience method to add the action installed under the given action id
     * to the specified group of the menu.
     *
     * @param menu
     *            the menu to add the action to
     * @param group
     *            the group in the menu
     * @param actionId
     *            the id of the action to add
     */
    protected final void addAction(IMenuManager menu, String group, String actionId) {
        IAction action = getAction(actionId);
        if (action != null) {
            if (action instanceof IUpdate)
                ((IUpdate) action).update();

            IMenuManager subMenu = menu.findMenuUsingPath(group);
            if (subMenu != null)
                subMenu.add(action);
            else
                menu.appendToGroup(group, action);
        }
    }

    /**
     * @see org.eclipse.ui.texteditor.ITextEditorExtension#addRulerContextMenuListener(org.eclipse.jface.action.IMenuListener)
     */
    @Override
    public void addRulerContextMenuListener(IMenuListener listener) {
    }

    /**
     * Adjusts the highlight range so that at least the specified range is
     * highlighted.
     * <p>
     * Subclasses may re-implement this method.
     * </p>
     *
     * @param offset
     *            the offset of the range which at least should be highlighted
     * @param length
     *            the length of the range which at least should be highlighted
     */
    protected void adjustHighlightRange(int offset, int length) {
        if (logViewer == null)
            return;

        if (logViewer instanceof ITextViewerExtension5) {
            ITextViewerExtension5 extension = (ITextViewerExtension5) logViewer;
            extension.exposeModelRange(new Region(offset, length));
        } else if (!isVisible(logViewer, offset, length)) {
            logViewer.resetVisibleRegion();
        }
    }

    /**
     * @see org.eclipse.ui.texteditor.ITextEditor#close(boolean)
     */
    @Override
    public void close(final boolean save) {
        Display display = getSite().getShell().getDisplay();
        display.asyncExec(new Runnable() {
            public void run() {
                if (logViewer != null)
                    getSite().getPage().closeEditor(AbstractIrcEditor.this, save);
            }
        });
    }

    /**
     * Configures the decoration support for this editor's source viewer.
     * Subclasses may override this method, but should call their superclass'
     * implementation at some point.
     *
     * @param support
     *            the decoration support to configure
     */
    protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support) {

        Iterator e = fAnnotationPreferences.getAnnotationPreferences().iterator();
        while (e.hasNext())
            support.setAnnotationPreference((AnnotationPreference) e.next());

        support.setCursorLinePainterPreferenceKeys(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE,
                AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE_COLOR);
        // support.setMarginPainterPreferenceKeys(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN,
        // AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLOR,
        // AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN);
        // support.setSymbolicFontName(getFontPropertyPreferenceKey());
    }

    /**
     * Creates this editor's standard actions and connects them with the global
     * workbench actions.
     * <p>
     * Subclasses may extend.
     * </p>
     */
    protected void createActions() {

        ResourceAction action;

        setAction(IWorkbenchCommandConstants.EDIT_COPY, null);
        action = new TextOperationAction(IrcUiMessages.getBundleForConstructedKeys(),
                "Editor.Copy.", this, ITextOperationTarget.COPY, true); //$NON-NLS-1$
        action.setHelpContextId(IAbstractTextEditorHelpContextIds.COPY_ACTION);
        action.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);
        setAction(ITextEditorActionConstants.COPY, action);

        action = new TextOperationAction(IrcUiMessages.getBundleForConstructedKeys(),
                "Editor.SelectAll.", this, ITextOperationTarget.SELECT_ALL, true); //$NON-NLS-1$
        action.setHelpContextId(IAbstractTextEditorHelpContextIds.SELECT_ALL_ACTION);
        action.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_SELECT_ALL);
        setAction(ITextEditorActionConstants.SELECT_ALL, action);

        action = new TextOperationAction(IrcUiMessages.getBundleForConstructedKeys(),
                "Editor.Print.", this, ITextOperationTarget.PRINT, true); //$NON-NLS-1$
        action.setHelpContextId(IAbstractTextEditorHelpContextIds.PRINT_ACTION);
        action.setActionDefinitionId(IWorkbenchCommandConstants.FILE_PRINT);
        setAction(ITextEditorActionConstants.PRINT, action);

        action = new FindReplaceAction(IrcUiMessages.getBundleForConstructedKeys(), "Editor.FindReplace.", this); //$NON-NLS-1$
        action.setHelpContextId(IAbstractTextEditorHelpContextIds.FIND_ACTION);
        action.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE);
        setAction(ITextEditorActionConstants.FIND, action);

        action = new FindNextAction(IrcUiMessages.getBundleForConstructedKeys(), "Editor.FindNext.", this, true); //$NON-NLS-1$
        action.setHelpContextId(IAbstractTextEditorHelpContextIds.FIND_NEXT_ACTION);
        action.setActionDefinitionId(IWorkbenchActionDefinitionIds.FIND_NEXT);
        setAction(ITextEditorActionConstants.FIND_NEXT, action);

        action = new FindNextAction(IrcUiMessages.getBundleForConstructedKeys(), "Editor.FindPrevious.", this, false); //$NON-NLS-1$
        action.setHelpContextId(IAbstractTextEditorHelpContextIds.FIND_PREVIOUS_ACTION);
        action.setActionDefinitionId(IWorkbenchActionDefinitionIds.FIND_PREVIOUS);
        setAction(ITextEditorActionConstants.FIND_PREVIOUS, action);

        action = new IncrementalFindAction(IrcUiMessages.getBundleForConstructedKeys(),
                "Editor.FindIncremental.", this, true); //$NON-NLS-1$
        action.setHelpContextId(IAbstractTextEditorHelpContextIds.FIND_INCREMENTAL_ACTION);
        action.setActionDefinitionId(IWorkbenchActionDefinitionIds.FIND_INCREMENTAL);
        setAction(ITextEditorActionConstants.FIND_INCREMENTAL, action);

        action = new IncrementalFindAction(IrcUiMessages.getBundleForConstructedKeys(),
                "Editor.FindIncrementalReverse.", this, false); //$NON-NLS-1$
        action.setHelpContextId(IAbstractTextEditorHelpContextIds.FIND_INCREMENTAL_REVERSE_ACTION);
        action.setActionDefinitionId(IWorkbenchActionDefinitionIds.FIND_INCREMENTAL_REVERSE);
        setAction(ITextEditorActionConstants.FIND_INCREMENTAL_REVERSE, action);

        action = new GotoLineAction(IrcUiMessages.getBundleForConstructedKeys(), "Editor.GotoLine.", this); //$NON-NLS-1$
        action.setHelpContextId(IAbstractTextEditorHelpContextIds.GOTO_LINE_ACTION);
        action.setActionDefinitionId(ITextEditorActionDefinitionIds.LINE_GOTO);
        setAction(ITextEditorActionConstants.GOTO_LINE, action);

        action = new MoveLinesAction(IrcUiMessages.getBundleForConstructedKeys(),
                "Editor.MoveLinesUp.", this, getSourceViewer(), true, false); //$NON-NLS-1$
        action.setHelpContextId(IAbstractTextEditorHelpContextIds.MOVE_LINES_ACTION);
        action.setActionDefinitionId(ITextEditorActionDefinitionIds.MOVE_LINES_UP);
        setAction(ITextEditorActionConstants.MOVE_LINE_UP, action);

        action = new MoveLinesAction(IrcUiMessages.getBundleForConstructedKeys(),
                "Editor.MoveLinesDown.", this, getSourceViewer(), false, false); //$NON-NLS-1$
        action.setHelpContextId(IAbstractTextEditorHelpContextIds.MOVE_LINES_ACTION);
        action.setActionDefinitionId(ITextEditorActionDefinitionIds.MOVE_LINES_DOWN);
        setAction(ITextEditorActionConstants.MOVE_LINE_DOWN, action);

        action = new MoveLinesAction(IrcUiMessages.getBundleForConstructedKeys(),
                "Editor.CopyLineUp.", this, getSourceViewer(), true, true); //$NON-NLS-1$
        action.setHelpContextId(IAbstractTextEditorHelpContextIds.COPY_LINES_ACTION);
        action.setActionDefinitionId(ITextEditorActionDefinitionIds.COPY_LINES_UP);
        setAction(ITextEditorActionConstants.COPY_LINE_UP, action);

        action = new MoveLinesAction(IrcUiMessages.getBundleForConstructedKeys(),
                "Editor.CopyLineDown.", this, getSourceViewer(), false, true); //$NON-NLS-1$
        action.setHelpContextId(IAbstractTextEditorHelpContextIds.COPY_LINES_ACTION);
        action.setActionDefinitionId(ITextEditorActionDefinitionIds.COPY_LINES_DOWN);
        setAction(ITextEditorActionConstants.COPY_LINE_DOWN, action);

        action = new GotoAnnotationAction(this, true);
        setAction(ITextEditorActionConstants.NEXT, action);
        action = new GotoAnnotationAction(this, false);
        setAction(ITextEditorActionConstants.PREVIOUS, action);

        action = new RecenterAction(IrcUiMessages.getBundleForConstructedKeys(), "Editor.Recenter.", this); //$NON-NLS-1$
        action.setHelpContextId(IAbstractTextEditorHelpContextIds.RECENTER_ACTION);
        action.setActionDefinitionId(ITextEditorActionDefinitionIds.RECENTER);
        setAction(ITextEditorActionConstants.RECENTER, action);

        action = new ShowWhitespaceCharactersAction(IrcUiMessages.getBundleForConstructedKeys(),
                "Editor.ShowWhitespaceCharacters.", this, getPreferenceStore()); //$NON-NLS-1$
        action.setHelpContextId(IAbstractTextEditorHelpContextIds.SHOW_WHITESPACE_CHARACTERS_ACTION);
        action.setActionDefinitionId(ITextEditorActionDefinitionIds.SHOW_WHITESPACE_CHARACTERS);
        setAction(ITextEditorActionConstants.SHOW_WHITESPACE_CHARACTERS, action);

        action = new TextOperationAction(IrcUiMessages.getBundleForConstructedKeys(),
                "Editor.OpenHyperlink.", this, HyperlinkManager.OPEN_HYPERLINK, true); //$NON-NLS-1$;
        action.setHelpContextId(IAbstractTextEditorHelpContextIds.OPEN_HYPERLINK_ACTION);
        action.setActionDefinitionId(ITextEditorActionDefinitionIds.OPEN_HYPERLINK);
        setAction(ITextEditorActionConstants.OPEN_HYPERLINK, action);

        PropertyDialogAction openProperties = new PropertyDialogAction(new IShellProvider() {
            public Shell getShell() {
                return getSite().getShell();
            }
        }, new ISelectionProvider() {
            public void addSelectionChangedListener(ISelectionChangedListener listener) {
            }

            public ISelection getSelection() {
                return new StructuredSelection(getEditorInput());
            }

            public void removeSelectionChangedListener(ISelectionChangedListener listener) {
            }

            public void setSelection(ISelection selection) {
            }
        });
        openProperties.setActionDefinitionId(IWorkbenchCommandConstants.FILE_PROPERTIES);
        setAction(ITextEditorActionConstants.PROPERTIES, openProperties);

        markAsContentDependentAction(ITextEditorActionConstants.UNDO, true);
        markAsContentDependentAction(ITextEditorActionConstants.REDO, true);
        markAsContentDependentAction(ITextEditorActionConstants.FIND, true);
        markAsContentDependentAction(ITextEditorActionConstants.FIND_NEXT, true);
        markAsContentDependentAction(ITextEditorActionConstants.FIND_PREVIOUS, true);
        markAsContentDependentAction(ITextEditorActionConstants.FIND_INCREMENTAL, true);
        markAsContentDependentAction(ITextEditorActionConstants.FIND_INCREMENTAL_REVERSE, true);

        markAsSelectionDependentAction(ITextEditorActionConstants.CUT, true);
        markAsSelectionDependentAction(ITextEditorActionConstants.COPY, true);
        markAsSelectionDependentAction(ITextEditorActionConstants.PASTE, true);
        markAsSelectionDependentAction(ITextEditorActionConstants.DELETE, true);
        markAsSelectionDependentAction(ITextEditorActionConstants.SHIFT_RIGHT, true);
        markAsSelectionDependentAction(ITextEditorActionConstants.SHIFT_RIGHT_TAB, true);
        markAsSelectionDependentAction(ITextEditorActionConstants.UPPER_CASE, true);
        markAsSelectionDependentAction(ITextEditorActionConstants.LOWER_CASE, true);

        markAsPropertyDependentAction(ITextEditorActionConstants.UNDO, true);
        markAsPropertyDependentAction(ITextEditorActionConstants.REDO, true);
        markAsPropertyDependentAction(ITextEditorActionConstants.REVERT_TO_SAVED, true);
        markAsPropertyDependentAction(ITextEditorActionConstants.SAVE, true);

        markAsStateDependentAction(ITextEditorActionConstants.UNDO, true);
        markAsStateDependentAction(ITextEditorActionConstants.REDO, true);
        markAsStateDependentAction(ITextEditorActionConstants.CUT, true);
        markAsStateDependentAction(ITextEditorActionConstants.PASTE, true);
        markAsStateDependentAction(ITextEditorActionConstants.DELETE, true);
        markAsStateDependentAction(ITextEditorActionConstants.SHIFT_RIGHT, true);
        markAsStateDependentAction(ITextEditorActionConstants.SHIFT_RIGHT_TAB, true);
        markAsStateDependentAction(ITextEditorActionConstants.SHIFT_LEFT, true);
        markAsStateDependentAction(ITextEditorActionConstants.FIND, true);
        markAsStateDependentAction(ITextEditorActionConstants.DELETE_LINE, true);
        markAsStateDependentAction(ITextEditorActionConstants.DELETE_LINE_TO_BEGINNING, true);
        markAsStateDependentAction(ITextEditorActionConstants.DELETE_LINE_TO_END, true);
        markAsStateDependentAction(ITextEditorActionConstants.MOVE_LINE_UP, true);
        markAsStateDependentAction(ITextEditorActionConstants.MOVE_LINE_DOWN, true);
        markAsStateDependentAction(ITextEditorActionConstants.CUT_LINE, true);
        markAsStateDependentAction(ITextEditorActionConstants.CUT_LINE_TO_BEGINNING, true);
        markAsStateDependentAction(ITextEditorActionConstants.CUT_LINE_TO_END, true);

        setActionActivationCode(ITextEditorActionConstants.SHIFT_RIGHT_TAB, '\t', -1, SWT.NONE);
        setActionActivationCode(ITextEditorActionConstants.SHIFT_LEFT, '\t', -1, SWT.SHIFT);
    }

    /**
     * Creates the annotation access for this editor.
     *
     * @return the created annotation access
     */
    protected IAnnotationAccess createAnnotationAccess() {
        return new DefaultMarkerAnnotationAccess();
    }

    /**
     * Creates a color from the information stored in the given preference
     * store. Returns <code>null</code> if there is no such information
     * available.
     *
     * @param store
     *            the store to read from
     * @param key
     *            the key used for the lookup in the preference store
     * @param display
     *            the display used create the color
     * @return the created color according to the specification in the
     *         preference store
     * @since 2.0
     */
    private Color createColor(IPreferenceStore store, String key, Display display) {

        RGB rgb = null;

        if (store.contains(key)) {

            if (store.isDefault(key))
                rgb = PreferenceConverter.getDefaultColor(store, key);
            else
                rgb = PreferenceConverter.getColor(store, key);

            if (rgb != null)
                return new Color(display, rgb);
        }

        return null;
    }

    /**
     * Creates the listener on this editor's context menus.
     *
     * @return the created menu listener
     * @since 3.4
     */
    protected IMenuListener createContextMenuListener() {
        return new IMenuListener() {
            public void menuAboutToShow(IMenuManager menu) {
                String id = menu.getId();
                if (getEditorContextMenuId().equals(id)) {
                    setFocus();
                    editorContextMenuAboutToShow(menu);
                }
            }
        };
    }

    /**
     * @see org.eclipse.ui.INavigationLocationProvider#createEmptyNavigationLocation()
     */
    @Override
    public INavigationLocation createEmptyNavigationLocation() {
        return new TextSelectionNavigationLocation(this, false);
    }

    /**
     * @see org.eclipse.ui.INavigationLocationProvider#createNavigationLocation()
     */
    @Override
    public INavigationLocation createNavigationLocation() {
        return new TextSelectionNavigationLocation(this, true);
    }

    protected IOverviewRuler createOverviewRuler(ISharedTextColors sharedColors) {
        IOverviewRuler ruler = new OverviewRuler(getAnnotationAccess(), VERTICAL_RULER_WIDTH, sharedColors);

        Iterator<?> e = fAnnotationPreferences.getAnnotationPreferences().iterator();
        while (e.hasNext()) {
            AnnotationPreference preference = (AnnotationPreference) e.next();
            if (preference.contributesToHeader())
                ruler.addHeaderAnnotationType(preference.getAnnotationType());
        }
        return ruler;
    }

    public void createPartControl(Composite parent) {

        int styles = SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION;
        this.logViewer = createSourceViewer(parent, styles);
        this.logViewer.configure(fConfiguration);

        if (fRangeIndicator != null)
            this.logViewer.setRangeIndicator(fRangeIndicator);

        logViewer.addTextListener(fTextListener);
        logViewer.addTextInputListener(fTextListener);
        getSelectionProvider().addSelectionChangedListener(getSelectionChangedListener());

        initializeViewerFont(logViewer);
        initializeViewerColors(logViewer);
        initializeFindScopeColor(logViewer);

        StyledText styledText = logViewer.getTextWidget();

        styledText.addMouseListener(getCursorListener());
        styledText.addKeyListener(getCursorListener());

        // Disable orientation switching until we fully support it.
        styledText.addListener(SWT.OrientationChange, new Listener() {
            public void handleEvent(Event event) {
                event.doit = false;
            }
        });

        String id = fEditorContextMenuId != null ? fEditorContextMenuId
                : org.eclipse.ui.texteditor.AbstractTextEditor.DEFAULT_EDITOR_CONTEXT_MENU_ID;

        MenuManager manager = new MenuManager(id, id);
        manager.setRemoveAllWhenShown(true);
        manager.addMenuListener(getContextMenuListener());
        fTextContextMenu = manager.createContextMenu(styledText);

        // comment this line if using gestures, above.
        styledText.setMenu(fTextContextMenu);

        if (fEditorContextMenuId != null)
            getEditorSite().registerContextMenu(fEditorContextMenuId, manager, getSelectionProvider(),
                    isEditorInputIncludedInContextMenu());

        getEditorSite().registerContextMenu(org.eclipse.ui.texteditor.AbstractTextEditor.COMMON_EDITOR_CONTEXT_MENU_ID,
                manager, getSelectionProvider(), false);

        if (fEditorContextMenuId == null)
            fEditorContextMenuId = org.eclipse.ui.texteditor.AbstractTextEditor.DEFAULT_EDITOR_CONTEXT_MENU_ID;

        getSite().setSelectionProvider(getSelectionProvider());

        fSelectionListener = new SelectionListener();
        fSelectionListener.install(getSelectionProvider());
        fSelectionListener.setDocument(getDocumentProvider().getDocument(getEditorInput()));

        initializeActivationCodeTrigger();

        // createNavigationActions();
        createActions();

        initializeSourceViewer(getEditorInput());

    }

    protected IrcLogViewer createSourceViewer(Composite parent, int styles) {
        fAnnotationAccess = getAnnotationAccess();
        fOverviewRuler = createOverviewRuler(getSharedColors());

        IrcLogViewer result = new IrcLogViewer(parent, null, getOverviewRuler(), isOverviewRulerVisible(), styles);
        // ensure decoration support has been created and configured.
        getSourceViewerDecorationSupport(result);

        return result;
    }

    /**
     * /** Disposes of the non-shared font.
     *
     * @since 3.5
     */
    private void disposeFont() {
        if (fFont != null) {
            fFont.dispose();
            fFont = null;
        }
    }

    /**
     * Returns the current selection.
     *
     * @return ISelection
     * @since 2.1
     */
    protected ISelection doGetSelection() {
        ISelectionProvider sp = null;
        if (logViewer != null)
            sp = logViewer.getSelectionProvider();
        return (sp == null ? null : sp.getSelection());
    }

    /**
     * @see org.eclipse.ui.texteditor.ITextEditor#doRevertToSaved()
     */
    @Override
    public void doRevertToSaved() {
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#doSaveAs()
     */
    @Override
    public void doSaveAs() {
    }

    /**
     * Sets the given selection.
     *
     * @param selection
     *            the selection
     * @since 2.1
     */
    protected void doSetSelection(ISelection selection) {
        if (selection instanceof ITextSelection) {
            ITextSelection textSelection = (ITextSelection) selection;
            selectAndReveal(textSelection.getOffset(), textSelection.getLength());
        }
    }

    /**
     * Sets up this editor's context menu before it is made visible.
     * <p>
     * Subclasses may extend to add other actions.
     * </p>
     *
     * @param menu
     *            the menu
     */
    protected void editorContextMenuAboutToShow(IMenuManager menu) {

        menu.add(new Separator(ITextEditorActionConstants.GROUP_UNDO));
        menu.add(new GroupMarker(ITextEditorActionConstants.GROUP_SAVE));
        menu.add(new Separator(ITextEditorActionConstants.GROUP_COPY));
        menu.add(new Separator(ITextEditorActionConstants.GROUP_PRINT));
        menu.add(new Separator(ITextEditorActionConstants.GROUP_EDIT));
        menu.add(new Separator(ITextEditorActionConstants.GROUP_FIND));
        menu.add(new Separator(IWorkbenchActionConstants.GROUP_ADD));
        menu.add(new Separator(ITextEditorActionConstants.GROUP_REST));
        menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        addAction(menu, ITextEditorActionConstants.GROUP_COPY, ITextEditorActionConstants.COPY);
    }

    /**
     * Returns the activation code registered for the specified action.
     *
     * @param actionID
     *            the action id
     * @return the registered activation code or <code>null</code> if no code
     *         has been installed
     */
    private ActionActivationCode findActionActivationCode(String actionID) {
        int size = fActivationCodes.size();
        for (int i = 0; i < size; i++) {
            ActionActivationCode code = (ActionActivationCode) fActivationCodes.get(i);
            if (actionID.equals(code.fActionId))
                return code;
        }
        return null;
    }

    /**
     * Returns the annotation closest to the given range respecting the given
     * direction. If an annotation is found, the annotations current position is
     * copied into the provided annotation position.
     *
     * @param offset
     *            the region offset
     * @param length
     *            the region length
     * @param forward
     *            <code>true</code> for forwards, <code>false</code> for
     *            backward
     * @param annotationPosition
     *            the position of the found annotation
     * @return the found annotation
     * @since 3.2
     */
    protected Annotation findAnnotation(final int offset, final int length, boolean forward, Position annotationPosition) {

        Annotation nextAnnotation = null;
        Position nextAnnotationPosition = null;
        Annotation containingAnnotation = null;
        Position containingAnnotationPosition = null;
        boolean currentAnnotation = false;

        IDocument document = getDocumentProvider().getDocument(getEditorInput());
        int endOfDocument = document.getLength();
        int distance = Integer.MAX_VALUE;

        IAnnotationModel model = getDocumentProvider().getAnnotationModel(getEditorInput());
        Iterator e = model.getAnnotationIterator();
        while (e.hasNext()) {
            Annotation a = (Annotation) e.next();
            if (!isNavigationTarget(a))
                continue;

            Position p = model.getPosition(a);
            if (p == null)
                continue;

            if (forward && p.offset == offset || !forward && p.offset + p.getLength() == offset + length) {// ||
                                                                                                           // p.includes(offset))
                                                                                                           // {
                if (containingAnnotation == null
                        || (forward && p.length >= containingAnnotationPosition.length || !forward
                                && p.length >= containingAnnotationPosition.length)) {
                    containingAnnotation = a;
                    containingAnnotationPosition = p;
                    currentAnnotation = p.length == length;
                }
            } else {
                int currentDistance = 0;

                if (forward) {
                    currentDistance = p.getOffset() - offset;
                    if (currentDistance < 0)
                        currentDistance = endOfDocument + currentDistance;

                    if (currentDistance < distance || currentDistance == distance
                            && p.length < nextAnnotationPosition.length) {
                        distance = currentDistance;
                        nextAnnotation = a;
                        nextAnnotationPosition = p;
                    }
                } else {
                    currentDistance = offset + length - (p.getOffset() + p.length);
                    if (currentDistance < 0)
                        currentDistance = endOfDocument + currentDistance;

                    if (currentDistance < distance || currentDistance == distance
                            && p.length < nextAnnotationPosition.length) {
                        distance = currentDistance;
                        nextAnnotation = a;
                        nextAnnotationPosition = p;
                    }
                }
            }
        }
        if (containingAnnotationPosition != null && (!currentAnnotation || nextAnnotation == null)) {
            annotationPosition.setOffset(containingAnnotationPosition.getOffset());
            annotationPosition.setLength(containingAnnotationPosition.getLength());
            return containingAnnotation;
        }
        if (nextAnnotationPosition != null) {
            annotationPosition.setOffset(nextAnnotationPosition.getOffset());
            annotationPosition.setLength(nextAnnotationPosition.getLength());
        }

        return nextAnnotation;
    }

    /**
     * @see org.eclipse.ui.texteditor.ITextEditor#getAction(java.lang.String)
     */
    @Override
    public IAction getAction(String actionId) {
        Assert.isNotNull(actionId);
        IAction action = (IAction) fActions.get(actionId);

        // if (action == null) {
        // action= findContributedAction(actionId);
        // if (action != null)
        // setAction(actionId, action);
        // }

        return action;
    }

    /**
     * Returns the annotation access.
     *
     * @return the annotation access
     */
    protected IAnnotationAccess getAnnotationAccess() {
        if (fAnnotationAccess == null)
            fAnnotationAccess = createAnnotationAccess();
        return fAnnotationAccess;
    }

    /**
     * Creates and returns the listener on this editor's context menus.
     *
     * @return the menu listener
     */
    protected final IMenuListener getContextMenuListener() {
        if (fMenuListener == null)
            fMenuListener = createContextMenuListener();
        return fMenuListener;
    }

    /**
     * Returns this editor's "cursor" listener to be installed on the editor's
     * source viewer. This listener is listening to key and mouse button events.
     * It triggers the updating of the status line by calling
     * <code>handleCursorPositionChanged()</code>.
     *
     * @return the listener
     * @since 2.0
     */
    protected final ICursorListener getCursorListener() {
        if (fCursorListener == null) {
            fCursorListener = new ICursorListener() {

                public void keyPressed(KeyEvent e) {
                    handleCursorPositionChanged();
                }

                public void keyReleased(KeyEvent e) {
                }

                public void mouseDoubleClick(MouseEvent e) {
                }

                public void mouseDown(MouseEvent e) {
                }

                public void mouseUp(MouseEvent e) {
                    handleCursorPositionChanged();
                }
            };
        }
        return fCursorListener;
    }

    /**
     * Returns a description of the cursor position.
     *
     * @return a description of the cursor position
     * @since 2.0
     */
    protected String getCursorPosition() {

        if (logViewer == null)
            return fErrorLabel;

        StyledText styledText = logViewer.getTextWidget();
        int caret = widgetOffset2ModelOffset(logViewer, styledText.getCaretOffset());
        IDocument document = logViewer.getDocument();

        if (document == null)
            return fErrorLabel;

        try {

            int line = document.getLineOfOffset(caret);

            int lineOffset = document.getLineOffset(line);
            int tabWidth = styledText.getTabs();
            int column = 0;
            for (int i = lineOffset; i < caret; i++)
                if ('\t' == document.getChar(i))
                    column += tabWidth - (tabWidth == 0 ? 0 : column % tabWidth);
                else
                    column++;

            fLineLabel.fValue = line + 1;
            fColumnLabel.fValue = column + 1;
            return NLSUtility.format(fPositionLabelPattern, fPositionLabelPatternArguments);

        } catch (BadLocationException x) {
            return fErrorLabel;
        }
    }

    /**
     * @see org.eclipse.ui.texteditor.ITextEditor#getDocumentProvider()
     */
    @Override
    public IDocumentProvider getDocumentProvider() {
        return IrcChannelDocumentProvider.getInstance();
    }

    /**
     * Returns the editor's context menu id. May return <code>null</code> before
     * the editor's part has been created.
     *
     * @return the editor's context menu id which may be <code>null</code>
     */
    protected final String getEditorContextMenuId() {
        return fEditorContextMenuId;
    }

    /*
     * @see ITextEditor#getHighlightRange()
     */
    public IRegion getHighlightRange() {
        if (logViewer == null)
            return null;

        if (fShowHighlightRangeOnly)
            return getCoverage(logViewer);

        return logViewer.getRangeIndication();
    }

    /**
     * Returns the overview ruler.
     *
     * @return the overview ruler
     */
    protected IOverviewRuler getOverviewRuler() {
        if (fOverviewRuler == null)
            fOverviewRuler = createOverviewRuler(getSharedColors());
        return fOverviewRuler;
    }

    /**
     * Returns this editor's preference store or <code>null</code> if none has
     * been set.
     *
     * @return this editor's preference store which may be <code>null</code>
     */
    protected final IPreferenceStore getPreferenceStore() {
        return fPreferenceStore;
    }

    /**
     * Returns this editor's selection changed listener to be installed on the
     * editor's source viewer.
     *
     * @return the listener
     */
    protected final ISelectionChangedListener getSelectionChangedListener() {
        if (fSelectionChangedListener == null) {
            fSelectionChangedListener = new ISelectionChangedListener() {

                private Display fDisplay;

                private Runnable fRunnable = new Runnable() {
                    public void run() {
                        // check whether editor has not been disposed yet
                        if (logViewer != null && logViewer.getDocument() != null) {
                            handleCursorPositionChanged();
                            updateSelectionDependentActions();
                        }
                    }
                };

                public void selectionChanged(SelectionChangedEvent event) {
                    if (fDisplay == null)
                        fDisplay = getSite().getShell().getDisplay();
                    if (Display.getCurrent() == fDisplay)
                        fRunnable.run();
                    else
                        fDisplay.asyncExec(fRunnable);
                }
            };
        }

        return fSelectionChangedListener;
    }

    /**
     * @see org.eclipse.ui.texteditor.ITextEditor#getSelectionProvider()
     */
    @Override
    public ISelectionProvider getSelectionProvider() {
        return fSelectionProvider;
    }

    protected ISharedTextColors getSharedColors() {
        return EditorsPlugin.getDefault().getSharedTextColors();
    }

    public IrcLogViewer getSourceViewer() {
        return logViewer;
    }

    /**
     * Returns the source viewer decoration support.
     *
     * @param viewer
     *            the viewer for which to return a decoration support
     * @return the source viewer decoration support
     */
    protected SourceViewerDecorationSupport getSourceViewerDecorationSupport(ISourceViewer viewer) {
        if (fSourceViewerDecorationSupport == null) {
            fSourceViewerDecorationSupport = new SourceViewerDecorationSupport(viewer, getOverviewRuler(),
                    getAnnotationAccess(), getSharedColors());
            configureSourceViewerDecorationSupport(fSourceViewerDecorationSupport);
        }
        return fSourceViewerDecorationSupport;
    }

    /**
     * Returns the current status field for the given status category.
     *
     * @param category
     *            the status category
     * @return the current status field for the given status category
     * @since 2.0
     */
    protected IStatusField getStatusField(String category) {
        if (category != null && fStatusFields != null)
            return (IStatusField) fStatusFields.get(category);
        return null;
    }

    /**
     * Jumps to the next annotation according to the given direction.
     *
     * @param forward
     *            <code>true</code> if search direction is forward,
     *            <code>false</code> if backward
     * @return the selected annotation or <code>null</code> if none
     * @see #isNavigationTarget(Annotation)
     * @see #findAnnotation(int, int, boolean, Position)
     * @since 3.2
     */
    public Annotation gotoAnnotation(boolean forward) {
        ITextSelection selection = (ITextSelection) getSelectionProvider().getSelection();
        Position position = new Position(0, 0);
        Annotation annotation = findAnnotation(selection.getOffset(), selection.getLength(), forward, position);
        setStatusLineErrorMessage(null);
        setStatusLineMessage(null);

        if (annotation != null) {
            selectAndReveal(position.getOffset(), position.getLength());
            setStatusLineMessage(annotation.getText());
        }
        return annotation;
    }

    /**
     * Handles a potential change of the cursor position. Subclasses may extend.
     *
     * @since 2.0
     */
    protected void handleCursorPositionChanged() {
        updateStatusField(ITextEditorActionConstants.STATUS_CATEGORY_INPUT_POSITION);
    }

    /**
     * Initializes the activation code trigger.
     *
     * @since 2.1
     */
    private void initializeActivationCodeTrigger() {
        // fActivationCodeTrigger.install();
        // fActivationCodeTrigger.setScopes(fKeyBindingScopes);
    }

    /**
     * Initializes the background color used for highlighting the document
     * ranges defining search scopes.
     *
     * @param viewer
     *            the viewer to initialize
     * @since 2.0
     */
    private void initializeFindScopeColor(ISourceViewer viewer) {

        IPreferenceStore store = getPreferenceStore();
        if (store != null) {

            StyledText styledText = viewer.getTextWidget();

            Color color = createColor(store, org.eclipse.ui.texteditor.AbstractTextEditor.PREFERENCE_COLOR_FIND_SCOPE,
                    styledText.getDisplay());

            IFindReplaceTarget target = viewer.getFindReplaceTarget();
            if (target != null && target instanceof IFindReplaceTargetExtension)
                ((IFindReplaceTargetExtension) target).setScopeHighlightColor(color);

            if (fFindScopeHighlightColor != null)
                fFindScopeHighlightColor.dispose();

            fFindScopeHighlightColor = color;
        }
    }

    /**
     * Initializes the editor's source viewer based on the given editor input.
     *
     * @param input
     *            the editor input to be used to initialize the source viewer
     */
    private void initializeSourceViewer(IEditorInput input) {

        IDocumentProvider documentProvider = getDocumentProvider();
        IAnnotationModel model = documentProvider.getAnnotationModel(input);
        IDocument document = documentProvider.getDocument(input);

        if (document != null) {
            logViewer.setDocument(document, model);
            logViewer.setEditable(isEditable());
            logViewer.showAnnotations(model != null);
        }

        // if (fElementStateListener instanceof IElementStateListenerExtension)
        // {
        // boolean isStateValidated= false;
        // if (documentProvider instanceof IDocumentProviderExtension)
        // isStateValidated=
        // ((IDocumentProviderExtension)documentProvider).isStateValidated(input);
        //
        // IElementStateListenerExtension extension=
        // (IElementStateListenerExtension) fElementStateListener;
        // extension.elementStateValidationChanged(input, isStateValidated);
        // }

        // if (fInitialCaret == null)
        // fInitialCaret= fSourceViewer.getTextWidget().getCaret();

        // if (fIsOverwriting)
        // fSourceViewer.getTextWidget().invokeAction(ST.TOGGLE_OVERWRITE);
        // handleInsertModeChanged();

        // if (isTabsToSpacesConversionEnabled())
        // installTabsToSpacesConverter();

        // if (fSourceViewer instanceof ITextViewerExtension8) {
        // IPreferenceStore store= getPreferenceStore();
        // EnrichMode mode= store != null ?
        // convertEnrichModePreference(store.getInt(PREFERENCE_HOVER_ENRICH_MODE))
        // : EnrichMode.AFTER_DELAY;
        // ((ITextViewerExtension8)fSourceViewer).setHoverEnrichMode(mode);
        // }
    }

    /**
     * Initializes the fore- and background colors of the given viewer for both
     * normal and selected text.
     *
     * @param viewer
     *            the viewer to be initialized
     * @since 2.0
     */
    protected void initializeViewerColors(ISourceViewer viewer) {

        IPreferenceStore store = getPreferenceStore();
        if (store != null) {

            StyledText styledText = viewer.getTextWidget();

            // ----------- foreground color --------------------
            Color color = store
                    .getBoolean(org.eclipse.ui.texteditor.AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT) ? null
                    : createColor(store, org.eclipse.ui.texteditor.AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND,
                            styledText.getDisplay());
            styledText.setForeground(color);

            if (fForegroundColor != null)
                fForegroundColor.dispose();

            fForegroundColor = color;

            // ---------- background color ----------------------
            color = store
                    .getBoolean(org.eclipse.ui.texteditor.AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT) ? null
                    : createColor(store, org.eclipse.ui.texteditor.AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND,
                            styledText.getDisplay());
            styledText.setBackground(color);

            if (fBackgroundColor != null)
                fBackgroundColor.dispose();

            fBackgroundColor = color;

            // ----------- selection foreground color --------------------
            color = store
                    .getBoolean(org.eclipse.ui.texteditor.AbstractTextEditor.PREFERENCE_COLOR_SELECTION_FOREGROUND_SYSTEM_DEFAULT) ? null
                    : createColor(store,
                            org.eclipse.ui.texteditor.AbstractTextEditor.PREFERENCE_COLOR_SELECTION_FOREGROUND,
                            styledText.getDisplay());
            styledText.setSelectionForeground(color);

            if (fSelectionForegroundColor != null)
                fSelectionForegroundColor.dispose();

            fSelectionForegroundColor = color;

            // ---------- selection background color ----------------------
            color = store
                    .getBoolean(org.eclipse.ui.texteditor.AbstractTextEditor.PREFERENCE_COLOR_SELECTION_BACKGROUND_SYSTEM_DEFAULT) ? null
                    : createColor(store,
                            org.eclipse.ui.texteditor.AbstractTextEditor.PREFERENCE_COLOR_SELECTION_BACKGROUND,
                            styledText.getDisplay());
            styledText.setSelectionBackground(color);

            if (fSelectionBackgroundColor != null)
                fSelectionBackgroundColor.dispose();

            fSelectionBackgroundColor = color;
        }
    }

    /**
     * Initializes the given viewer's font.
     *
     * @param viewer
     *            the viewer
     * @since 2.0
     */
    private void initializeViewerFont(ISourceViewer viewer) {

        boolean isSharedFont = true;
        Font font = null;
        String symbolicFontName = JFaceResources.DIALOG_FONT;

        if (symbolicFontName != null)
            font = JFaceResources.getFont(symbolicFontName);

        if (font == null)
            font = JFaceResources.getDialogFont();

        if (!font.equals(logViewer.getTextWidget().getFont())) {
            setFont(viewer, font);

            disposeFont();
            if (!isSharedFont)
                fFont = font;
        } else if (!isSharedFont) {
            font.dispose();
        }
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#isDirty()
     */
    @Override
    public boolean isDirty() {
        return false;
    }

    /**
     * @see org.eclipse.ui.texteditor.ITextEditor#isEditable()
     */
    @Override
    public boolean isEditable() {
        return false;
    }

    /**
     * Tells whether the editor input should be included when adding object
     * contributions to this editor's context menu.
     * <p>
     * This implementation always returns <code>true</code>.
     * </p>
     *
     * @return <code>true</code> if the editor input should be considered
     * @since 3.2
     */
    protected boolean isEditorInputIncludedInContextMenu() {
        return true;
    }

    /**
     * @see org.eclipse.ui.texteditor.ITextEditorExtension#isEditorInputReadOnly()
     */
    @Override
    public boolean isEditorInputReadOnly() {
        return true;
    }

    protected boolean isLineNumberRulerVisible() {
        return false;
    }

    /**
     * Returns whether the given annotation is configured as a target for the
     * "Go to Next/Previous Annotation" actions.
     * <p>
     * Per default every annotation is a target.
     * </p>
     *
     * @param annotation
     *            the annotation
     * @return <code>true</code> if this is a target, <code>false</code>
     *         otherwise
     * @since 3.2
     */
    protected boolean isNavigationTarget(Annotation annotation) {
        return true;
    }

    protected boolean isOverviewRulerVisible() {
        return true;
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
     */
    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    /**
     * Marks or unmarks the given action to be updated on content changes.
     *
     * @param actionId
     *            the action id
     * @param mark
     *            <code>true</code> if the action is content dependent
     */
    public void markAsContentDependentAction(String actionId, boolean mark) {
        Assert.isNotNull(actionId);
        if (mark) {
            if (!fContentActions.contains(actionId))
                fContentActions.add(actionId);
        } else
            fContentActions.remove(actionId);
    }

    /**
     * Marks or unmarks the given action to be updated on property changes.
     *
     * @param actionId
     *            the action id
     * @param mark
     *            <code>true</code> if the action is property dependent
     * @since 2.0
     */
    public void markAsPropertyDependentAction(String actionId, boolean mark) {
        Assert.isNotNull(actionId);
        if (mark) {
            if (!fPropertyActions.contains(actionId))
                fPropertyActions.add(actionId);
        } else
            fPropertyActions.remove(actionId);
    }

    /**
     * Marks or unmarks the given action to be updated on text selection
     * changes.
     *
     * @param actionId
     *            the action id
     * @param mark
     *            <code>true</code> if the action is selection dependent
     */
    public void markAsSelectionDependentAction(String actionId, boolean mark) {
        Assert.isNotNull(actionId);
        if (mark) {
            if (!fSelectionActions.contains(actionId))
                fSelectionActions.add(actionId);
        } else
            fSelectionActions.remove(actionId);
    }

    /**
     * Marks or unmarks the given action to be updated on state changes.
     *
     * @param actionId
     *            the action id
     * @param mark
     *            <code>true</code> if the action is state dependent
     * @since 2.0
     */
    public void markAsStateDependentAction(String actionId, boolean mark) {
        Assert.isNotNull(actionId);
        if (mark) {
            if (!fStateActions.contains(actionId))
                fStateActions.add(actionId);
        } else
            fStateActions.remove(actionId);
    }

    /**
     * Writes a check mark of the given situation into the navigation history.
     *
     * @since 2.1
     */
    protected void markInNavigationHistory() {
        getSite().getPage().getNavigationHistory().markLocation(this);
    }

    /**
     * @see org.eclipse.ui.texteditor.ITextEditor#removeActionActivationCode(java.lang.String)
     */
    @Override
    public void removeActionActivationCode(String actionId) {
        Assert.isNotNull(actionId);
        ActionActivationCode code = findActionActivationCode(actionId);
        if (code != null)
            fActivationCodes.remove(code);
    }

    /**
     * @see org.eclipse.ui.texteditor.ITextEditorExtension#removeRulerContextMenuListener(org.eclipse.jface.action.IMenuListener)
     */
    @Override
    public void removeRulerContextMenuListener(IMenuListener listener) {
    }

    /*
     * @see ITextEditor#resetHighlightRange
     */
    public void resetHighlightRange() {
        if (logViewer == null)
            return;

        if (fShowHighlightRangeOnly)
            logViewer.resetVisibleRegion();
        else
            logViewer.removeRangeIndication();
    }

    /**
     * @see org.eclipse.ui.IPersistableEditor#restoreState(org.eclipse.ui.IMemento)
     */
    @Override
    public void restoreState(IMemento memento) {
    }

    /**
     * @see org.eclipse.ui.IPersistable#saveState(org.eclipse.ui.IMemento)
     */
    @Override
    public void saveState(IMemento memento) {
    }

    /*
     * @see ITextEditor#selectAndReveal(int, int)
     */
    public void selectAndReveal(int start, int length) {
        selectAndReveal(start, length, start, length);
    }

    /**
     * Selects and reveals the specified ranges in this text editor.
     *
     * @param selectionStart
     *            the offset of the selection
     * @param selectionLength
     *            the length of the selection
     * @param revealStart
     *            the offset of the revealed range
     * @param revealLength
     *            the length of the revealed range
     * @since 3.0
     */
    protected void selectAndReveal(int selectionStart, int selectionLength, int revealStart, int revealLength) {
        if (logViewer == null)
            return;

        ISelection selection = getSelectionProvider().getSelection();
        if (selection instanceof ITextSelection) {
            ITextSelection textSelection = (ITextSelection) selection;
            if (textSelection.getOffset() != 0 || textSelection.getLength() != 0)
                markInNavigationHistory();
        }

        StyledText widget = logViewer.getTextWidget();
        widget.setRedraw(false);
        {
            adjustHighlightRange(revealStart, revealLength);
            logViewer.revealRange(revealStart, revealLength);

            logViewer.setSelectedRange(selectionStart, selectionLength);

            markInNavigationHistory();
        }
        widget.setRedraw(true);
    }

    /**
     * @see org.eclipse.ui.texteditor.ITextEditor#setAction(java.lang.String,
     *      org.eclipse.jface.action.IAction)
     */
    @Override
    public void setAction(String actionID, IAction action) {
        Assert.isNotNull(actionID);
        if (action == null) {
            action = (IAction) fActions.remove(actionID);
            if (action != null)
                fActivationCodeTrigger.unregisterActionFromKeyActivation(action);
        } else {
            if (action.getId() == null)
                action.setId(actionID); // make sure the action ID has been set
            fActions.put(actionID, action);
            fActivationCodeTrigger.registerActionForKeyActivation(action);
        }
    }

    /**
     * @see ITextEditor#setActionActivationCode(String, char, int, int)
     */
    public void setActionActivationCode(String actionID, char activationCharacter, int activationKeyCode,
            int activationStateMask) {

        Assert.isNotNull(actionID);

        ActionActivationCode found = findActionActivationCode(actionID);
        if (found == null) {
            found = new ActionActivationCode(actionID);
            fActivationCodes.add(found);
        }

        found.fCharacter = activationCharacter;
        found.fKeyCode = activationKeyCode;
        found.fStateMask = activationStateMask;
    }

    /**
     * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
     */
    @Override
    public void setFocus() {
        logViewer.setFocus();
    }

    /**
     * Sets the font for the given viewer sustaining selection and scroll
     * position.
     *
     * @param sourceViewer
     *            the source viewer
     * @param font
     *            the font
     * @since 2.0
     */
    private void setFont(ISourceViewer sourceViewer, Font font) {
        if (sourceViewer.getDocument() != null) {

            ISelectionProvider provider = sourceViewer.getSelectionProvider();
            ISelection selection = provider.getSelection();
            int topIndex = sourceViewer.getTopIndex();

            StyledText styledText = sourceViewer.getTextWidget();
            Control parent = styledText;
            if (sourceViewer instanceof ITextViewerExtension) {
                ITextViewerExtension extension = (ITextViewerExtension) sourceViewer;
                parent = extension.getControl();
            }

            parent.setRedraw(false);

            styledText.setFont(font);

            provider.setSelection(selection);
            sourceViewer.setTopIndex(topIndex);

            if (parent instanceof Composite) {
                Composite composite = (Composite) parent;
                composite.layout(true);
            }

            parent.setRedraw(true);

        } else {

            StyledText styledText = sourceViewer.getTextWidget();
            styledText.setFont(font);

        }
    }

    /*
     * @see ITextEditor#setHighlightRange(int, int, boolean)
     */
    public void setHighlightRange(int offset, int length, boolean moveCursor) {
        if (logViewer == null)
            return;

        if (fShowHighlightRangeOnly) {
            if (moveCursor)
                logViewer.setVisibleRegion(offset, length);
        } else {
            IRegion rangeIndication = logViewer.getRangeIndication();
            if (rangeIndication == null || offset != rangeIndication.getOffset()
                    || length != rangeIndication.getLength())
                logViewer.setRangeIndication(offset, length, moveCursor);
        }
    }

    @Override
    public void setInput(IEditorInput input) {
        super.setInput(input);
    }

    /**
     * Sets this editor's preference store. This method must be called before
     * the editor's control is created.
     *
     * @param store
     *            the preference store or <code>null</code> to remove the
     *            preference store
     */
    protected void setPreferenceStore(IPreferenceStore store) {
        // if (fPreferenceStore != null) {
        // fPreferenceStore.removePropertyChangeListener(fPropertyChangeListener);
        // fPreferenceStore.removePropertyChangeListener(fFontPropertyChangeListener);
        // }

        fPreferenceStore = store;

        // if (fPreferenceStore != null) {
        // fPreferenceStore.addPropertyChangeListener(fPropertyChangeListener);
        // fPreferenceStore.addPropertyChangeListener(fFontPropertyChangeListener);
        // }
    }

    /**
     * Sets this editor's source viewer configuration used to configure its
     * internal source viewer. This method must be called before the editor's
     * control is created. If not, this editor uses a
     * <code>SourceViewerConfiguration</code>.
     *
     * @param configuration
     *            the source viewer configuration object
     */
    protected void setSourceViewerConfiguration(SourceViewerConfiguration configuration) {
        Assert.isNotNull(configuration);
        fConfiguration = configuration;
    }

    /**
     * @see org.eclipse.ui.texteditor.ITextEditorExtension#setStatusField(org.eclipse.ui.texteditor.IStatusField,
     *      java.lang.String)
     */
    @Override
    public void setStatusField(IStatusField field, String category) {
        Assert.isNotNull(category);
        if (field != null) {

            if (fStatusFields == null)
                fStatusFields = new HashMap(3);

            fStatusFields.put(category, field);
            updateStatusField(category);

        } else if (fStatusFields != null)
            fStatusFields.remove(category);

        // if (fIncrementalFindTarget != null &&
        // ITextEditorActionConstants.STATUS_CATEGORY_FIND_FIELD.equals(category))
        // fIncrementalFindTarget.setStatusField(field);
    }

    /**
     * Sets the given message as error message to this editor's status line.
     *
     * @param message
     *            message to be set
     * @since 3.2
     */
    protected void setStatusLineErrorMessage(String message) {
        IEditorStatusLine statusLine = (IEditorStatusLine) getAdapter(IEditorStatusLine.class);
        if (statusLine != null)
            statusLine.setMessage(true, message, null);
    }

    /**
     * Sets the given message as message to this editor's status line.
     *
     * @param message
     *            message to be set
     * @since 3.2
     */
    protected void setStatusLineMessage(String message) {
        IEditorStatusLine statusLine = (IEditorStatusLine) getAdapter(IEditorStatusLine.class);
        if (statusLine != null)
            statusLine.setMessage(false, message, null);
    }

    /*
     * @see ITextEditor#showHighlightRangeOnly(boolean)
     */
    public void showHighlightRangeOnly(boolean showHighlightRangeOnly) {
        fShowHighlightRangeOnly = showHighlightRangeOnly;
    }

    /**
     * @see org.eclipse.ui.texteditor.ITextEditorExtension4#showRevisionInformation(org.eclipse.jface.text.revisions.RevisionInformation,
     *      java.lang.String)
     */
    @Override
    public void showRevisionInformation(RevisionInformation info, String quickDiffProviderId) {
    }

    /*
     * @see ITextEditor#showsHighlightRangeOnly()
     */
    public boolean showsHighlightRangeOnly() {
        return fShowHighlightRangeOnly;
    }

    /**
     * Updates the specified action by calling <code>IUpdate.update</code> if
     * applicable.
     *
     * @param actionId
     *            the action id
     */
    private void updateAction(String actionId) {
        Assert.isNotNull(actionId);
        if (fActions != null) {
            IAction action = (IAction) fActions.get(actionId);
            if (action instanceof IUpdate)
                ((IUpdate) action).update();
        }
    }

    /**
     * Updates all content dependent actions.
     */
    protected void updateContentDependentActions() {
        if (fContentActions != null) {
            Iterator e = fContentActions.iterator();
            while (e.hasNext())
                updateAction((String) e.next());
        }
    }

    /**
     * Updates all property dependent actions.
     *
     * @since 2.0
     */
    protected void updatePropertyDependentActions() {
        if (fPropertyActions != null) {
            Iterator e = fPropertyActions.iterator();
            while (e.hasNext())
                updateAction((String) e.next());
        }
    }

    /**
     * Updates all selection dependent actions.
     */
    protected void updateSelectionDependentActions() {
        if (fSelectionActions != null) {
            Iterator e = fSelectionActions.iterator();
            while (e.hasNext())
                updateAction((String) e.next());
        }
    }

    /**
     * Updates all state dependent actions.
     *
     * @since 2.0
     */
    protected void updateStateDependentActions() {
        if (fStateActions != null) {
            Iterator e = fStateActions.iterator();
            while (e.hasNext())
                updateAction((String) e.next());
        }
    }

    /**
     * Updates the status fields for the given category.
     *
     * @param category
     *            the category
     * @since 2.0
     */
    protected void updateStatusField(String category) {

        if (category == null)
            return;

        IStatusField field = getStatusField(category);
        if (field != null) {

            String text = null;

            if (ITextEditorActionConstants.STATUS_CATEGORY_INPUT_POSITION.equals(category))
                text = getCursorPosition();
            else if (ITextEditorActionConstants.STATUS_CATEGORY_ELEMENT_STATE.equals(category))
                text = IrcUiMessages.Editor_statusline_state_readonly_label;

            field.setText(text == null ? fErrorLabel : text);
        }
    }

}
