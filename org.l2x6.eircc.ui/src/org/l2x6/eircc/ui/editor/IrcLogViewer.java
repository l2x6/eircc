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

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcLogViewer extends SourceViewer {
    private class ViewerPaintListener implements PaintListener {

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

            for (Integer Y : horizontalLines) {
                int y = Y.intValue();
                if (y < textWidget.getClientArea().height) {
                    int x1 = timeLineX + timeColumnMargin + 1;
                    int x2 = clientArea.width - timeColumnMargin - 1;
                    if (x1 < x2) {
                        gc.drawLine(x1, y, x2, y);
                    }
                }
            }
        }
    }

    public static final int TEXT_OFFSET = "10:00:00 ".length();

    /**
     * y coordinates in the client area of {@link #getTextWidget()} where
     * horizontal lines should be drawn.
     */
    private final List<Integer> horizontalLines = new ArrayList<Integer>();

    /**
     * The content of log file as opposed to the presented channel log that is
     * maintained in {@link TextViewer#fDocument}
     */
    private IDocument rawDocument;

    private IAnnotationModel rawModel;

    // private final StyledText textWidget;

    // private final TextViewer viewer;;

    private final StyledText textWidget;

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
        super(parent, verticalRuler, overviewRuler, showAnnotationsOverview, styles | SWT.WRAP);
        this.setEditable(false);
        this.textWidget = this.getTextWidget();
        setDocument(new Document());
    }

    /**
     *
     */
    public void addHorizontalLine() {
        IDocument doc = getDocument();
        if (doc != null) {
            int length = doc.getLength();
            if (length > 0) {
                int lastOffset = length - 1;
                Point lastCharLocation = textWidget.getLocationAtOffset(lastOffset);
                int lastLineHeight = textWidget.getLineHeight(lastOffset);
                int y = lastCharLocation.y + lastLineHeight;
                if (horizontalLines.isEmpty() || horizontalLines.get(horizontalLines.size() - 1).intValue() != y) {
                    horizontalLines.add(Integer.valueOf(y));
                }
            }
        }
    }

    public void clear() {
        IDocument doc = this.getDocument();
        if (doc != null) {
            doc.set("");
        }
        horizontalLines.clear();
    }

    @Override
    public void configure(SourceViewerConfiguration configuration) {
        super.configure(configuration);
        // IrcLogEditorConfiguration logEditorConfiguration =
        // (IrcLogEditorConfiguration) configuration;

        GC gc = new GC(textWidget);
        ViewerPaintListener paintListener = new ViewerPaintListener(gc);
        gc.dispose();

        textWidget.setTabStops(paintListener.getTabStops());
        textWidget.setWrapIndent(paintListener.getFirstTabStop());
        textWidget.addPaintListener(paintListener);

    }

    public IDocument getRawDocument() {
        return rawDocument;
    }

    public IAnnotationModel getRawModel() {
        return rawModel;
    }

    public boolean isAtBottom() {
        ScrollBar verticalBar = textWidget.getVerticalBar();
        return !verticalBar.isVisible() || verticalBar.getMaximum() == verticalBar.getSelection() + verticalBar.getThumb();
    }

    public boolean isEmpty() {
        IDocument doc = this.getDocument();
        return doc == null || doc.getLength() == 0;
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

    /**
     * @param document
     * @param model
     */
    public void setRawDocument(IDocument document, IAnnotationModel model) {
        this.rawDocument = document;
        this.rawModel = model;
    }

}
