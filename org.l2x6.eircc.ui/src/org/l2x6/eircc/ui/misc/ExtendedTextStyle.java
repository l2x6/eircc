/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.misc;

import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.TextStyle;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class ExtendedTextStyle extends TextStyle {
    public static class ExtendedTextStyler extends Styler {
        private final ExtendedTextStyle sourceStyle;

        /**
         * @param textStyle
         */
        public ExtendedTextStyler(ExtendedTextStyle sourceStyle) {
            super();
            this.sourceStyle = sourceStyle;
        }

        /**
         * @see org.eclipse.jface.viewers.StyledString.Styler#applyStyles(org.eclipse.swt.graphics.TextStyle)
         */
        @Override
        public void applyStyles(TextStyle targetStyle) {
            targetStyle.background = sourceStyle.background;
            targetStyle.borderColor = sourceStyle.borderColor;
            targetStyle.borderStyle = sourceStyle.borderStyle;
            targetStyle.font = sourceStyle.font;
            targetStyle.foreground = sourceStyle.foreground;
            targetStyle.background = sourceStyle.background;
            targetStyle.underline = sourceStyle.underline;
            targetStyle.underlineColor = sourceStyle.underlineColor;
            targetStyle.underlineStyle = sourceStyle.underlineStyle;
            targetStyle.strikeout = sourceStyle.strikeout;
            targetStyle.strikeoutColor = sourceStyle.strikeoutColor;
            targetStyle.borderStyle = sourceStyle.borderStyle;
            targetStyle.borderColor = sourceStyle.borderColor;
            targetStyle.metrics = sourceStyle.metrics;
            targetStyle.rise = sourceStyle.rise;
        }

    }

    public int fontStyle = SWT.NONE;

    private Styler styler;

    /**
     *
     */
    public ExtendedTextStyle() {
        super();
    }

    public ExtendedTextStyle(Color foreground) {
        super();
        this.foreground = foreground;
    }

    public ExtendedTextStyle(Color foreground, int fontStyle) {
        super();
        this.foreground = foreground;
        this.fontStyle = fontStyle;
    }

    /**
     * @param font
     * @param foreground
     * @param background
     */
    public ExtendedTextStyle(Font font, Color foreground, Color background) {
        super(font, foreground, background);
    }

    /**
     * @param fontStyle
     */
    public ExtendedTextStyle(int fontStyle) {
        super();
        this.fontStyle = fontStyle;
    }

    /**
     * @param style
     */
    public ExtendedTextStyle(TextStyle style) {
        super(style);
    }

    public StyleRange createRange(int offset, int length) {
        StyleRange range = new StyleRange(this);
        range.fontStyle = this.fontStyle;
        range.start = offset;
        range.length = length;
        return range;
    }

    public Styler getStyler() {
        if (styler == null) {
            styler = new ExtendedTextStyler(this);
        }
        return styler;
    }
}
