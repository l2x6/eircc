/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.editor;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.l2x6.eircc.core.model.PlainIrcMessage;
import org.l2x6.eircc.ui.prefs.IrcPreferences;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcLogViewer extends SourceViewer {
    private static class ViewerPaintListener implements PaintListener {

        private final int firstTabStop;
        private final int[] tabStops;

        private final int timeColumnMargin;
        private final int timeColumnWith;
        private final int timeLineWidth = 1;
        private final int timeLineX;

        public ViewerPaintListener(GC gc) {
            int widestDigitWith = Integer.MIN_VALUE;
            int widestDigit = -1;
            for (int i = 0; i <= 9; i++) {
                int w = gc.textExtent(String.valueOf(i)).x;
                if (w > widestDigitWith) {
                    widestDigitWith = w;
                    widestDigit = i;
                }
            }
            String timeString = "" + widestDigit + widestDigit + ":" + widestDigit + widestDigit + ":" + widestDigit
                    + widestDigit;
            timeColumnWith = gc.textExtent(timeString).x;
            timeColumnMargin = gc.textExtent(" ").x / 2;
            int tabWidth = gc.textExtent("    ").x;
            tabStops = new int[2];
            firstTabStop = timeColumnMargin + timeColumnWith + timeColumnMargin + timeLineWidth + timeColumnMargin;
            tabStops[0] = firstTabStop;
            tabStops[1] = firstTabStop + tabWidth;

            timeLineX = timeColumnMargin + timeColumnWith + timeColumnMargin;
        }

        public int getFirstTabStop() {
            return firstTabStop;
        }

        public int[] getTabStops() {
            return tabStops;
        }

        @Override
        public void paintControl(PaintEvent e) {
            Rectangle clientArea = ((StyledText) e.widget).getClientArea();
            GC gc = e.gc;
            gc.setLineStyle(SWT.LINE_SOLID);
            gc.setLineWidth(timeLineWidth);
            gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            gc.drawLine(timeLineX, 0, timeLineX, clientArea.height);
        }
    }

    private final StyledText textWidget;

    // private final StyledText textWidget;

    // private final TextViewer viewer;;

    /**
     * @param parent
     * @param verticalRuler
     * @param overviewRuler
     * @param styles
     * @param showAnnotationsOverview
     * @param styles
     */
    public IrcLogViewer(Composite parent, IVerticalRuler verticalRuler, IOverviewRuler overviewRuler,
            boolean showAnnotationsOverview, int styles) {
        // SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL
        super(parent, verticalRuler, overviewRuler, showAnnotationsOverview, styles);
        this.setEditable(false);
        this.textWidget = this.getTextWidget();
    }

    public void appendMessage(PlainIrcMessage m) {
        if (this.getDocument() == null) {
            this.setDocument(new Document());
        }
        IrcPreferences prefs = IrcPreferences.getInstance();
        IrcDefaultMessageFormatter formatter = prefs.getFormatter(m);
        formatter.format(this, m);
    }

    public void clear() {
        IDocument doc = this.getDocument();
        if (doc != null) {
            doc.set("");
        }
    }

    @Override
    public void configure(SourceViewerConfiguration configuration) {
        super.configure(configuration);
        //IrcLogEditorConfiguration logEditorConfiguration = (IrcLogEditorConfiguration) configuration;

        GC gc = new GC(textWidget);
        ViewerPaintListener paintListener = new ViewerPaintListener(gc);
        gc.dispose();

        textWidget.setTabStops(paintListener.getTabStops());
        textWidget.setWrapIndent(paintListener.getFirstTabStop());
        textWidget.addPaintListener(paintListener);

    }

    /**
     * @return
     */
    public boolean isVisible() {
        return textWidget != null && textWidget.isVisible();
    }

    public void scrollToBottom() {
        textWidget.setTopIndex(textWidget.getLineCount() - 1);
    }

    public void setFocus() {
        textWidget.setFocus();
    }


}
