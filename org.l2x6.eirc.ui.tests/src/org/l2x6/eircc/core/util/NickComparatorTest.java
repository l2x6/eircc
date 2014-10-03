/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.util;


import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class NickComparatorTest {
    @Test
    public void testIsSameUser() {
        Assert.assertTrue(NickComparator.isSameUser(null, null));
        Assert.assertTrue(NickComparator.isSameUser("", ""));
        Assert.assertTrue(NickComparator.isSameUser("joe", "joe"));
        Assert.assertTrue(NickComparator.isSameUser("joe", "joe1"));
        Assert.assertTrue(NickComparator.isSameUser("joe", "joe12"));
        Assert.assertTrue(NickComparator.isSameUser("joe", "joe_"));
        Assert.assertTrue(NickComparator.isSameUser("joe", "joe__"));
        Assert.assertTrue(NickComparator.isSameUser("joe_", "joe1"));
        Assert.assertTrue(NickComparator.isSameUser("joe__", "joe12"));
        Assert.assertTrue(NickComparator.isSameUser("joe_", "joe_"));
        Assert.assertTrue(NickComparator.isSameUser("joe12", "joe__"));

        Assert.assertFalse(NickComparator.isSameUser(null, ""));
        Assert.assertFalse(NickComparator.isSameUser("", null));
        Assert.assertFalse(NickComparator.isSameUser(null, "joe"));
        Assert.assertFalse(NickComparator.isSameUser("joe", null));
        Assert.assertFalse(NickComparator.isSameUser("joe", "jim"));
        Assert.assertFalse(NickComparator.isSameUser("joe_", "jim1"));
    }

    @Test
    public void testBaseNick() {
        Assert.assertNull(NickComparator.getBaseNick(null));
        Assert.assertEquals("", NickComparator.getBaseNick(""));
        Assert.assertEquals("joe", NickComparator.getBaseNick("joe"));
        Assert.assertEquals("joe", NickComparator.getBaseNick("joe1"));
        Assert.assertEquals("joe", NickComparator.getBaseNick("joe22"));
        Assert.assertEquals("joe", NickComparator.getBaseNick("joe_"));
        Assert.assertEquals("joe", NickComparator.getBaseNick("joe__"));
    }

}
