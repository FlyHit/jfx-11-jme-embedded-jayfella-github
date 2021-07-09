package com.jayfella.jfx.embedded.jfx;

import javafx.geometry.Rectangle2D;

public class FrameUtil {
    public static Rectangle2D getDirtyRectangle(byte[] current, byte[] previous, int width, int height) {
        int minX = 0, minY = 0, maxX = 0, maxY = 0;
        // calculate minX
        search:
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (isPixelDirty(current, previous, i, j, width)) {
                    minX = i;
                    break search;
                }
            }
        }

        // calculate maxX
        if (minX == width - 1) {
            return null;
        } else {
            search:
            for (int i = width - 1; i > minX; i--) {
                for (int j = 0; j < height; j++) {
                    if (isPixelDirty(current, previous, i, j, width)) {
                        maxX = i;
                        break search;
                    }
                }
            }
        }

        // calculate minY
        search:
        for (int j = 0; j < height; j++) {
            for (int i = minX; i <= maxX; i++) {
                if (isPixelDirty(current, previous, i, j, width)) {
                    minY = j;
                    break search;
                }
            }
        }

        // calculate maxY
        if (minY == height - 1) {
            return null;
        } else {
            search:
            for (int j = height - 1; j > minY; j--) {
                for (int i = minX; i <= maxX; i++) {
                    if (isPixelDirty(current, previous, i, j, width)) {
                        maxY = j;
                        break search;
                    }
                }
            }
        }

        if (maxX - minX == 0 || maxY - minY == 0) {
            return null;
        }

        return new Rectangle2D(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static boolean isPixelDirty(byte[] current, byte[] previous, int x, int y, int width) {
        int index = 4 * (y * width + x);
        return current[index] != previous[index] ||
                current[index + 1] != previous[index + 1] ||
                current[index + 2] != previous[index + 2] ||
                current[index + 3] != previous[index + 3];
    }
}
