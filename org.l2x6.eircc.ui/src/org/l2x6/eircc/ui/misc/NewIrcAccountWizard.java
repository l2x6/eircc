/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.misc;

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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.l2x6.eircc.core.IrcException;
import org.l2x6.eircc.core.model.InitialIrcAccount;
import org.l2x6.eircc.core.model.InitialIrcAccount.InitialIrcAccountField;
import org.l2x6.eircc.core.model.IrcAccount;
import org.l2x6.eircc.core.model.IrcModel;
import org.l2x6.eircc.core.model.resource.IrcResourceException;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.IrcUiMessages;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class NewIrcAccountWizard extends Wizard implements INewWizard {
    // the list selection page
    private static class AccountPage extends WizardPage implements ModifyListener {
        private static final int COLUMN_COUNT = 2;

        public static final String ID = "AccountPage";

        @SuppressWarnings("unused")
        private Button autoConnectCheckbox;
        @SuppressWarnings("unused")
        private Text hostText;
        private boolean isFirstCheck;
        private Text labelText;
        @SuppressWarnings("unused")
        private Object nameText;
        @SuppressWarnings("unused")
        private Text nickText;
        @SuppressWarnings("unused")
        private Text passwordText;
        private Text portText;
        private InitialIrcAccount result;
        @SuppressWarnings("unused")
        private Text socksProxyHostText;
        private Text socksProxyPortText;
        @SuppressWarnings("unused")
        private Text usernameText;

        @SuppressWarnings("unused")
        private Button useSslCheckbox;

        AccountPage() {
            super(ID, IrcUiMessages.AccountPage_title, null);
        }

        private Button createCheckboxField(Composite composite, DataBindingContext ctx,
                InitialIrcAccountField ircAccountField) {
            Button button = new Button(composite, SWT.CHECK);
            button.setText(ircAccountField.getLabel());
            GdBuilder.defaults(button).hSpan(COLUMN_COUNT).apply();
            ctx.bindValue(WidgetProperties.selection().observe(button),
                    PojoProperties.value(InitialIrcAccount.class, ircAccountField.name()).observe(result));
            return button;
        }

        @Override
        public void createControl(Composite parent) {
            initializeDialogUnits(parent);

            Composite composite = new Composite(parent, SWT.NONE);
            GlFactory.defaults().numColumns(COLUMN_COUNT).defaultMargins().applyTo(composite);
            composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
            setControl(composite);

            result = EirccUi.getDefault().getModel().proposeNextAccount();
            DataBindingContext ctx = new DataBindingContext();

            labelText = createTextField(composite, ctx, InitialIrcAccountField.label);
            labelText.setFocus();
            hostText = createTextField(composite, ctx, InitialIrcAccountField.host);
            portText = createTextField(composite, ctx, InitialIrcAccountField.port);
            portText.addVerifyListener(new NumericTextVerifier());

            usernameText = createTextField(composite, ctx, InitialIrcAccountField.username);
            passwordText = createTextField(composite, ctx, InitialIrcAccountField.password);
            nameText = createTextField(composite, ctx, InitialIrcAccountField.name);
            nickText = createTextField(composite, ctx, InitialIrcAccountField.preferedNick);

            useSslCheckbox = createCheckboxField(composite, ctx, InitialIrcAccountField.ssl);
            autoConnectCheckbox = createCheckboxField(composite, ctx, InitialIrcAccountField.autoConnect);

            socksProxyHostText = createTextField(composite, ctx, InitialIrcAccountField.socksProxyHost);
            socksProxyPortText = createTextField(composite, ctx, InitialIrcAccountField.socksProxyPort);
            socksProxyPortText.addVerifyListener(new NumericTextVerifier());

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
        private Text createTextField(Composite composite, DataBindingContext ctx, InitialIrcAccountField ircAccountField) {
            createLabel(composite, ircAccountField.getLabel());
            Text textControl = new Text(composite, SWT.SINGLE | SWT.BORDER);
            GdBuilder.defaults(textControl).apply();
            textControl.addModifyListener(this);

            Binding binding = ctx.bindValue(WidgetProperties.text(SWT.Modify).observe(textControl), PojoProperties
                    .value(InitialIrcAccount.class, ircAccountField.name()).observe(result));
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
            IrcModel ircModel = EirccUi.getDefault().getModel();

            try {
                IrcAccount newAccount = result.freeze();

                ircModel.addAccount(newAccount);

                if (newAccount.isAutoConnect()) {
                    EirccUi.getController().connect(newAccount);
                }
            } catch (IrcException | IrcResourceException e) {
                e.printStackTrace();
                setErrorMessage(e.getLocalizedMessage());
                return false;
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
