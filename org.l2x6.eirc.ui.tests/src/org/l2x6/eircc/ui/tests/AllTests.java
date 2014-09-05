/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.l2x6.eircc.core.model.search.tests.ModelSearchSuite;

/**
 * All tests wrapper.
 *
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ ModelSearchSuite.class })
public class AllTests {
}
