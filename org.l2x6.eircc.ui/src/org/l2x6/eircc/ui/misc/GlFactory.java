/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.misc;

import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public final class GlFactory {

    /**
     * Copies the given GridLayout instance
     *
     * @param l
     *            layout to copy
     * @return a new GridLayout
     */
    public static GridLayout copyLayout(GridLayout l) {
        GridLayout result = new GridLayout(l.numColumns, l.makeColumnsEqualWidth);
        result.horizontalSpacing = l.horizontalSpacing;
        result.marginBottom = l.marginBottom;
        result.marginHeight = l.marginHeight;
        result.marginLeft = l.marginLeft;
        result.marginRight = l.marginRight;
        result.marginTop = l.marginTop;
        result.marginWidth = l.marginWidth;
        result.verticalSpacing = l.verticalSpacing;

        return result;
    }

    /**
     * Creates a factory that creates copies of the given layout.
     *
     * @param l
     *            layout to copy
     * @return a new GridLayoutFactory instance that creates copies of the given
     *         layout
     */
    public static GlFactory createFrom(GridLayout l) {
        return new GlFactory(copyLayout(l));
    }

    /**
     * Creates a GridLayoutFactory that creates GridLayouts with no margins and
     * default dialog spacing.
     *
     * <p>
     * Initial values are:
     * </p>
     *
     * <ul>
     * <li>numColumns(1)</li>
     * <li>margins(0,0)</li>
     * <li>extendedMargins(0,0,0,0)</li>
     * <li>spacing(LayoutConstants.getSpacing())</li>
     * <li>equalWidth(false)</li>
     * </ul>
     *
     * @return a GridLayoutFactory that creates GridLayouts as though created
     *         with their default constructor
     * @see #swtDefaults
     */
    public static GlFactory defaults() {
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        Point defaultSpacing = LayoutConstants.getSpacing();
        layout.horizontalSpacing = defaultSpacing.x;
        layout.verticalSpacing = defaultSpacing.y;
        return new GlFactory(layout);
    }

    /**
     * Creates a GridLayoutFactory that creates GridLayouts with the default SWT
     * values.
     *
     * <p>
     * Initial values are:
     * </p>
     *
     * <ul>
     * <li>numColumns(1)</li>
     * <li>margins(5,5)</li>
     * <li>extendedMargins(0,0,0,0)</li>
     * <li>spacing(5,5)</li>
     * <li>equalWidth(false)</li>
     * </ul>
     *
     * @return a GridLayoutFactory that creates GridLayouts as though created
     *         with their default constructor
     * @see #fillDefaults
     */
    public static GlFactory swtDefaults() {
        return new GlFactory(new GridLayout());
    }

    /**
     * Template layout. The factory will create copies of this layout.
     */
    private GridLayout l;

    /**
     * Creates a new GridLayoutFactory that will create copies of the given
     * layout.
     *
     * @param l
     *            layout to copy
     */
    private GlFactory(GridLayout l) {
        this.l = l;
    }

    /**
     * Creates a new GridLayout and attaches it to the given composite. Does not
     * create the GridData of any of the controls in the composite.
     *
     * @param c
     *            composite whose layout will be set
     * @see #generateLayout
     * @see #create
     * @see GlFactory
     */
    public void applyTo(Composite c) {
        c.setLayout(copyLayout(l));
    }

    /**
     * Creates a copy of the reciever.
     *
     * @return a copy of the reciever
     */
    public GlFactory copy() {
        return new GlFactory(create());
    }

    /**
     * Creates a new GridLayout, and initializes it with values from the
     * factory.
     *
     * @return a new initialized GridLayout.
     * @see #applyTo
     */
    public GridLayout create() {
        return copyLayout(l);
    }

    public GlFactory defaultMargins() {
        return margins(LayoutConstants.getMargins());
    }

    /**
     * Sets whether the columns should be forced to be equal width
     *
     * @param equal
     *            true iff the columns should be forced to be equal width
     * @return this
     */
    public GlFactory equalWidth(boolean equal) {
        l.makeColumnsEqualWidth = equal;
        return this;
    }

    /**
     * Sets the margins for layouts created with this factory. The margins
     * specify the number of pixels of horizontal and vertical margin that will
     * be placed along the left, right, top, and bottom edges of the layout.
     * Note that thes margins will be added to the ones specified by
     * {@link #margins(int, int)}.
     *
     * @param left
     *            left margin size (pixels)
     * @param right
     *            right margin size (pixels)
     * @param top
     *            top margin size (pixels)
     * @param bottom
     *            bottom margin size (pixels)
     * @return this
     * @see #spacing(Point)
     * @see #spacing(int, int)
     *
     * @since 3.3
     */
    public GlFactory extendedMargins(int left, int right, int top, int bottom) {
        l.marginLeft = left;
        l.marginRight = right;
        l.marginTop = top;
        l.marginBottom = bottom;
        return this;
    }

    /**
     * Sets the margins for layouts created with this factory. The margins
     * specify the number of pixels of horizontal and vertical margin that will
     * be placed along the left, right, top, and bottom edges of the layout.
     * Note that thes margins will be added to the ones specified by
     * {@link #margins(int, int)}.
     *
     * <code><pre>
     *     // Construct a GridLayout whose left, right, top, and bottom
     *     // margin sizes are 10, 5, 0, and 15 respectively
     * 
     *     Rectangle margins = Geometry.createDiffRectangle(10,5,0,15);
     *     GridLayoutFactory.fillDefaults().extendedMargins(margins).applyTo(composite1);
     * </pre></code>
     *
     * @param differenceRect
     *            rectangle which, when added to the client area of the layout,
     *            returns the outer area of the layout. The x and y values of
     *            the rectangle correspond to the position of the bounds of the
     *            layout with respect to the client area. They should be
     *            negative. The width and height correspond to the relative size
     *            of the bounds of the layout with respect to the client area,
     *            and should be positive.
     * @return this
     * @see #spacing(Point)
     * @see #spacing(int, int)
     *
     * @since 3.3
     */
    public GlFactory extendedMargins(Rectangle differenceRect) {
        l.marginLeft = -differenceRect.x;
        l.marginTop = -differenceRect.y;
        l.marginBottom = differenceRect.y + differenceRect.height;
        l.marginRight = differenceRect.x + differenceRect.width;
        return this;
    }

    /**
     * Sets the margins for layouts created with this factory. The margins
     * specify the number of pixels of horizontal and vertical margin that will
     * be placed along the left/right and top/bottom edges of the layout. Note
     * that thes margins will be added to the ones specified by
     * {@link #extendedMargins(int, int, int, int)}.
     *
     * @param width
     *            margin width (pixels)
     * @param height
     *            margin height (pixels)
     * @return this
     * @see #spacing(Point) * @see #spacing(int, int)
     */
    public GlFactory margins(int width, int height) {
        l.marginWidth = width;
        l.marginHeight = height;
        return this;
    }

    /**
     * Sets the margins for layouts created with this factory. The margins are
     * the distance between the outer cells and the edge of the layout.
     *
     * @param margins
     *            margin size (pixels)
     * @return this
     * @see #spacing(Point)
     * @see #spacing(int, int)
     */
    public GlFactory margins(Point margins) {
        l.marginWidth = margins.x;
        l.marginHeight = margins.y;
        return this;
    }

    /**
     * Sets the number of columns in the layout
     *
     * @param numColumns
     *            number of columns in the layout
     * @return this
     */
    public GlFactory numColumns(int numColumns) {
        l.numColumns = numColumns;
        return this;
    }

    /**
     * Sets the spacing for layouts created with this factory. The spacing is
     * the distance between cells within the layout.
     *
     * @param hSpacing
     *            horizontal spacing (pixels)
     * @param vSpacing
     *            vertical spacing (pixels)
     * @return this
     * @see #margins(Point)
     * @see #margins(int, int)
     */
    public GlFactory spacing(int hSpacing, int vSpacing) {
        l.horizontalSpacing = hSpacing;
        l.verticalSpacing = vSpacing;
        return this;
    }

    /**
     * Sets the spacing for layouts created with this factory. The spacing is
     * the distance between cells within the layout.
     *
     * @param spacing
     *            space between controls in the layout (pixels)
     * @return this
     * @see #margins(Point)
     * @see #margins(int, int)
     */
    public GlFactory spacing(Point spacing) {
        l.horizontalSpacing = spacing.x;
        l.verticalSpacing = spacing.y;
        return this;
    }

}
