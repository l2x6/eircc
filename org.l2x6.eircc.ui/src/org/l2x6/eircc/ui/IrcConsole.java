/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.l2x6.eircc.core.client.TrafficLoggerFactory;
import org.l2x6.eircc.core.model.IrcAccount;
import org.schwering.irc.lib.TrafficLogger;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcConsole implements TrafficLoggerFactory {
    private static final IrcConsole INSTANCE = new IrcConsole();


    public static IrcConsole getInstance() {
        return INSTANCE;
    }

    public static class ConsoleTrafficLogger implements TrafficLogger {
        private final MessageConsoleStream consoleStream;
        private MessageConsole console;
        /**
         * @param consoleStream
         */
        public ConsoleTrafficLogger(MessageConsole console) {
            super();
            this.consoleStream = console.newMessageStream();
            this.console = console;
        }

        /**
         * @see org.schwering.irc.lib.TrafficLogger#in(java.lang.String)
         */
        @Override
        public void in(String line) {
            reveal();
            consoleStream.println("> "+ line);
        }

        /**
         * @see org.schwering.irc.lib.TrafficLogger#out(java.lang.String)
         */
        @Override
        public void out(String line) {
            reveal();
            consoleStream.println("< "+ line);
        }

        public void uiReveal() {
            IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (win != null) {
                IWorkbenchPage page = win.getActivePage();
                if (page != null) {
                    try {
                        IConsoleView view = (IConsoleView) page.showView(IConsoleConstants.ID_CONSOLE_VIEW);
                        view.display(console);
                    } catch (PartInitException e) {
                        EirccUi.log(e);
                    }
                }
            }
        }

        public void reveal() {
            if (Display.getCurrent() == null) {
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        uiReveal();
                    }
                });
            } else {
                uiReveal();
            }
        }
    }
    /**
     * @see org.l2x6.eircc.core.client.TrafficLoggerFactory#createTrafficLogger(org.l2x6.eircc.core.model.IrcAccount)
     */
    @Override
    public TrafficLogger createTrafficLogger(IrcAccount account) {
        String consoleName = account.getLabel() + IrcUiMessages.Console_Account_Log;
        MessageConsole console = getOrCreateConsole(consoleName);
        return new ConsoleTrafficLogger(console);
    }

    private MessageConsole getOrCreateConsole(String name) {
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();
        for (int i = 0; i < existing.length; i++)
           if (name.equals(existing[i].getName()))
              return (MessageConsole) existing[i];
        MessageConsole myConsole = new MessageConsole(name, null);
        conMan.addConsoles(new IConsole[]{myConsole});
        return myConsole;
     }

}
