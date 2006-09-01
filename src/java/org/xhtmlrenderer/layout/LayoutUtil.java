/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci, Torbj�rn Gannholm
 * Copyright (c) 2005 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.xhtmlrenderer.layout;

import java.util.List;

import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.render.FloatedBlockBox;
import org.xhtmlrenderer.render.LineBox;
import org.xhtmlrenderer.render.MarkerData;

public class LayoutUtil {

    public static boolean layoutAbsolute(
            LayoutContext c, LineBox currentLine, BlockBox box) {
        boolean result = true;
        
        MarkerData markerData = c.getCurrentMarkerData();
        c.setCurrentMarkerData(null);
        
        box.setContainingBlock(c.getLayer().getMaster());
        box.setStaticEquivalent(currentLine);
        
        // If printing, don't layout until we know where its going
        if (! c.isPrint()) {
            if (! box.getStyle().isAlternateFlow()) {
                box.layout(c);
            } else {
                result = false;
            }
        } else {
            c.pushLayer(box);
            c.getLayer().setRequiresLayout(true);
            c.getLayer().setLayoutData(
                    new AbsoluteContentLayoutData(box, c.getCurrentStyle()));
            c.popLayer();
        }
        
        c.setCurrentMarkerData(markerData);
        
        return result;
    }
    
    public static FloatLayoutResult layoutFloated(
            final LayoutContext c, LineBox currentLine, FloatedBlockBox block, 
            int avail, List pendingFloats) {
        FloatLayoutResult result = new FloatLayoutResult();
        
        MarkerData markerData = c.getCurrentMarkerData();
        c.setCurrentMarkerData(null);
    
        block.setContainingBlock(currentLine.getParent());
        block.setContainingLayer(currentLine.getContainingLayer());
        
        if (pendingFloats != null) {
            block.y = currentLine.y + block.getMarginFromPrevious();
        } else {
            block.y = currentLine.y + currentLine.height;
        }
        
        block.calcInitialCanvasLocation(c);
        
        int initialY = block.y;
        
        block.layout(c);
        
        c.getBlockFormattingContext().floatBox(c, (FloatedBlockBox) block);

        if (pendingFloats != null && 
                (pendingFloats.size() > 0 || block.getWidth() > avail)) {
            block.reset(c);
            result.setPending(true);
        } else {
            if (c.isPrint()) {
                positionFloatOnPage(c, currentLine, block, initialY != block.y);
                c.getRootLayer().ensureHasPage(c, block);
            }
        }
        
        result.setBlock(block);
        c.setCurrentMarkerData(markerData);
        
        return result;
    }

    private static void positionFloatOnPage(
            final LayoutContext c, LineBox currentLine, FloatedBlockBox block, 
            boolean movedVertically) {
        boolean clearedPage = false;
        int clearDelta = 0;
        
        if (block.getStyle().isForcePageBreakBefore() || 
                (block.getStyle().isAvoidPageBreakInside() && 
                        block.crossesPageBreak(c))) {
            clearDelta = block.moveToNextPage(c);
            clearedPage = true;
            block.calcCanvasLocation();
            block.reset(c);
            block.setContainingLayer(currentLine.getContainingLayer());
            block.layout(c);
            c.getBlockFormattingContext().floatBox(c, (FloatedBlockBox) block);
        }
        
        if ((movedVertically || 
                    (block.getStyle().isAvoidPageBreakInside() && block.crossesPageBreak(c))) && 
                ! block.getStyle().isForcePageBreakBefore()) {
            if (clearedPage) {
                block.y -= clearDelta;
                block.calcCanvasLocation();
            }
            block.reset(c);
            block.setContainingLayer(currentLine.getContainingLayer());
            block.layout(c);
            c.getBlockFormattingContext().floatBox(c, (FloatedBlockBox) block);
        }
    }
}
