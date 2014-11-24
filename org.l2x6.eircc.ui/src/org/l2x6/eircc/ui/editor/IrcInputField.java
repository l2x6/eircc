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

import org.eclipse.core.commands.IHandler;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionInfo;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.TextViewerUndoManager;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.l2x6.eircc.core.IrcException;
import org.l2x6.eircc.core.model.AbstractIrcChannel;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.prefs.IrcPreferences;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcInputField implements FocusListener, DisposeListener {

    private static class PriorizeExpression extends Expression {

        private Control focusControl;

        public PriorizeExpression(Control control) {
            focusControl = control;
        }

        @Override
        public void collectExpressionInfo(ExpressionInfo info) {
            info.markDefaultVariableAccessed();
            info.addVariableNameAccess(ISources.ACTIVE_SHELL_NAME);
            info.addVariableNameAccess(ISources.ACTIVE_WORKBENCH_WINDOW_NAME);
        }

        @Override
        public EvaluationResult evaluate(IEvaluationContext context)
                throws CoreException {
            if (Display.getCurrent() != null && focusControl.isFocusControl()) {
                return EvaluationResult.TRUE;
            }
            return EvaluationResult.FALSE;
        }
    }
    private ContentAssistant contentAssistant;
    private final IrcEditor editor;
    private final List<IHandlerActivation> handlerActivations = new ArrayList<IHandlerActivation>();
    private final IHandlerService handlerService;
    private VerifyKeyListener inputWidgetListener = new VerifyKeyListener() {

        @Override
        public void verifyKey(VerifyEvent e) {
            switch (e.keyCode) {
            case SWT.CR:
            case SWT.LF:
            case SWT.KEYPAD_CR:
                if (e.stateMask == 0) {
                    try {
                        sendMessage();
                        e.doit = false;
                    } catch (Exception e1) {
                        EirccUi.log(e1);
                    }
                }
                break;
            case SWT.SPACE:
                if ((e.stateMask & SWT.CTRL) == SWT.CTRL) {
                    try {
                        contentAssistant.showPossibleCompletions();
                    } catch (Exception e1) {
                        EirccUi.log(e1);
                    }
                }
                break;
            default:
                break;
            }

        }
    };

    private final PriorizeExpression priorizeExpression;
    private final TextViewer textViewer;

    /**
     *
     */
    public IrcInputField(Composite parent, IrcEditor editor) {
        this.editor = editor;
        this.textViewer = new TextViewer(parent, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL);
        textViewer.setDocument(new Document());
        textViewer.appendVerifyKeyListener(inputWidgetListener);
        textViewer.setUndoManager(new TextViewerUndoManager(25));
        textViewer.activatePlugins();

        this.priorizeExpression = new PriorizeExpression(textViewer.getTextWidget());

        contentAssistant = new ContentAssistant();
        contentAssistant.setContentAssistProcessor(new IrcContentAssistProcessor(editor),
                IDocument.DEFAULT_CONTENT_TYPE);
        IrcPreferences prefs = IrcPreferences.getInstance();
        contentAssistant.enablePrefixCompletion(prefs.getEditorAutoPrefixCompletion());
        contentAssistant.enableAutoInsert(prefs.getEditorAutoInsert());
        contentAssistant.install(textViewer);

        StyledText textWidget = textViewer.getTextWidget();
        textWidget.addFocusListener(this);
        textWidget.addDisposeListener(this);

        IWorkbenchWindow window = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow();
        handlerService = (IHandlerService) window
                .getService(IHandlerService.class);

        if (textViewer.getTextWidget().isFocusControl()) {
            activateContext();
        }

    }

    /**
     * Add handlers, if they haven't already been added.
     */
    protected void activateContext() {
        if (handlerActivations.isEmpty()) {
            activateHandler(ITextOperationTarget.UNDO, IWorkbenchCommandConstants.EDIT_UNDO);
            activateHandler(ITextOperationTarget.REDO, IWorkbenchCommandConstants.EDIT_REDO);
        }
    }

    /**
     * Add a single handler.
     *
     * @param operation
     * @param commandId
     */
    protected void activateHandler(int operation, String commandId) {
        IHandler actionHandler = createActionHandler(operation, commandId);
        IHandlerActivation handlerActivation = handlerService.activateHandler(commandId, actionHandler, priorizeExpression);
        handlerActivations.add(handlerActivation);
    }

    /**
     * Create an {@link IHandler} that delegates the given {@code operation} to the text viewer.
     *
     * @param operation
     * @param actionDefinitionId
     * @return a new {@link IHandler}
     */
    private IHandler createActionHandler(final int operation, String actionDefinitionId) {
        Action action = new Action() {
            @Override
            public void run() {
                if (textViewer.canDoOperation(operation)) {
                    textViewer.doOperation(operation);
                }
            }
        };
        action.setActionDefinitionId(actionDefinitionId);
        return new ActionHandler(action);
    }

    protected void deactivateContext() {
        if (!handlerActivations.isEmpty()) {
            for (IHandlerActivation activation : handlerActivations) {
                handlerService.deactivateHandler(activation);
                activation.getHandler().dispose();
            }
            handlerActivations.clear();
        }
    }

   /**
     * @see org.eclipse.swt.events.FocusListener#focusGained(org.eclipse.swt.events.FocusEvent)
     */
    @Override
    public void focusGained(FocusEvent e) {
        activateContext();
    }

    /**
     * @see org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
     */
    @Override
    public void focusLost(FocusEvent e) {
        deactivateContext();
    }

    /**
     * @return
     */
    public boolean isVisible() {
        return textViewer.getControl().isVisible();
    }

    private void sendMessage() throws IrcException {
        String text = textViewer.getDocument().get();
        if (text.length() > 0) {
            /* remove the trailing whitespace */
            int end = text.length() - 1;
            for (; end >= 0; end--) {
                if (!Character.isWhitespace(text.charAt(end))) {
                    break;
                }
            }
            end++;

            if (end < text.length()) {
                text = text.substring(0, end);
            }
            if (text.length() > 0) {
                AbstractIrcChannel channel = editor.getChannel();
                EirccUi.getController().postMessage(channel, text);
                textViewer.getDocument().set("");
            }
        }
    }

    /**
     *
     */
    public void setFocus() {
        textViewer.getControl().setFocus();
    }

    /**
     * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
     */
    @Override
    public void widgetDisposed(DisposeEvent e) {
        deactivateContext();
    }
}
