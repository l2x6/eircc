/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui;

import java.text.MessageFormat;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.l2x6.eircc.core.IrcController;
import org.l2x6.eircc.core.IrcException;
import org.l2x6.eircc.core.model.IrcAccount;
import org.l2x6.eircc.core.model.IrcAccount.IrcAccountField;
import org.l2x6.eircc.core.model.IrcModel;
import org.l2x6.eircc.ui.utils.GdBuilder;
import org.l2x6.eircc.ui.utils.GlFactory;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class NewIrcAccountWizard extends Wizard implements INewWizard {
    // the list selection page
    private static class AccountPage extends WizardPage implements ModifyListener {
        private static final int COLUMN_COUNT = 2;

        public static final String ID = "AccountPage";

        private Button autoConnectCheckbox;
        private Text hostText;
        private boolean isFirstCheck;
        private Text labelText;
        private Object nameText;
        private Text nickText;
        private Text passwordText;
        private Text portText;
        private IrcAccount result;
        private Text usernameText;

        private Button useSslCheckbox;

        AccountPage() {
            super(ID, IrcUiMessages.AccountPage_title, null);
        }

        private Button createCheckboxField(Composite composite, DataBindingContext ctx, IrcAccountField ircAccountField) {
            Button button = new Button(composite, SWT.CHECK);
            button.setText(ircAccountField.getLabel());
            GdBuilder.defaults(button).hSpan(COLUMN_COUNT).apply();
            ctx.bindValue(WidgetProperties.selection().observe(button),
                    PojoProperties.value(IrcAccount.class, ircAccountField.name()).observe(result));
            return button;
        }

        @Override
        public void createControl(Composite parent) {
            initializeDialogUnits(parent);

            Composite composite = new Composite(parent, SWT.NONE);
            GlFactory.defaults().numColumns(COLUMN_COUNT).defaultMargins().applyTo(composite);
            composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
            setControl(composite);

            result = IrcModel.getInstance().proposeNextAccount();
            DataBindingContext ctx = new DataBindingContext();

            labelText = createTextField(composite, ctx, IrcAccountField.label);
            labelText.setFocus();
            hostText = createTextField(composite, ctx, IrcAccountField.host);
            portText = createTextField(composite, ctx, IrcAccountField.port);
            portText.addVerifyListener(new VerifyListener() {
                @Override
                public void verifyText(VerifyEvent e) {
                    final String oldText = portText.getText();
                    final String newS = oldText.substring(0, e.start) + e.text + oldText.substring(e.end);
                    try {
                        Integer.parseInt(newS);
                    } catch (final NumberFormatException numberFormatException) {
                        e.doit = false;
                    }
                }
            });

            usernameText = createTextField(composite, ctx, IrcAccountField.username);
            passwordText = createTextField(composite, ctx, IrcAccountField.password);
            nameText = createTextField(composite, ctx, IrcAccountField.name);
            nickText = createTextField(composite, ctx, IrcAccountField.preferedNick);

            useSslCheckbox = createCheckboxField(composite, ctx, IrcAccountField.ssl);
            autoConnectCheckbox = createCheckboxField(composite, ctx, IrcAccountField.autoConnect);

            ctx.updateTargets();

            WizardPageSupport.create(this, ctx);

        }

        /**
         * @param composite
         * @param text
         */
        private void createLabel(Composite composite, String text) {
            Label label = new Label(composite, SWT.NONE);
            label.setText(text);
            GdBuilder.defaults(label).apply();
        }

        /**
         * @param composite
         * @param ctx
         * @param ircAccountField
         * @param accountPage_label
         * @return
         */
        private Text createTextField(Composite composite, DataBindingContext ctx, IrcAccountField ircAccountField) {
            createLabel(composite, ircAccountField.getLabel());
            Text textControl = new Text(composite, SWT.SINGLE | SWT.BORDER);
            GdBuilder.defaults(textControl).apply();
            textControl.addModifyListener(this);

            Binding binding = ctx.bindValue(WidgetProperties.text(SWT.Modify).observe(textControl), PojoProperties
                    .value(IrcAccount.class, ircAccountField.name()).observe(result));
            ControlDecorationSupport.create(binding, SWT.TOP | SWT.LEFT);
            return textControl;
        }

        /**
         * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
         */
        @Override
        public void modifyText(ModifyEvent e) {
            validateInput();
        }

        /**
         * @return
         */
        public boolean performFinish() {
            IrcModel ircModel = IrcModel.getInstance();
            ircModel.addAccount(result);

            if (result.isAutoConnect()) {
                try {
                    IrcController.getInstance().connect(result);
                } catch (IrcException e) {
                    setErrorMessage(e.getLocalizedMessage());
                    return false;
                }
            }
            return true;
        }

        /**
         * validates the current input of the page to determine if the finish
         * button can be enabled
         */
        private void validateInput() {
            String errorMessage = null;
            String newText = labelText.getText();

            if (newText.isEmpty()) {
                if (isFirstCheck) {
                    setPageComplete(false);
                    isFirstCheck = false;
                    return;
                }
                errorMessage = MessageFormat.format(IrcUiMessages.Error_Input_in_field_x_required,
                        IrcUiMessages.Account_Label);
            }
            isFirstCheck = false;
            setErrorMessage(errorMessage);
            setPageComplete(errorMessage == null);
        }

    }

    public static final String ID = "org.l2x6.eircc.ui.NewIrcAccountWizard";

    private AccountPage accountPage;

    /**
     * Creates the wizard's pages lazily.
     */
    @Override
    public void addPages() {
        accountPage = new AccountPage();
        addPage(accountPage);
    }

    /**
     * Initializes the wizard.
     *
     * @param aWorkbench
     *            the workbench
     * @param currentSelection
     *            the current selectio
     */
    public void init(IWorkbench aWorkbench, IStructuredSelection currentSelection) {

        setWindowTitle(IrcUiMessages.AccountPage_title);
        // setDefaultPageImageDescriptor(WorkbenchImages
        // .getImageDescriptor(IWorkbenchGraphicConstants.IMG_WIZBAN_EXPORT_WIZ));
        setNeedsProgressMonitor(true);
    }

    /**
     * Subclasses must implement this <code>IWizard</code> method to perform any
     * special finish processing for their wizard.
     */
    @Override
    public boolean performFinish() {
        return accountPage.performFinish();
    }

}
