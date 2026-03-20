package org.atypical.carabassa.core.model.impl;

import org.atypical.carabassa.core.model.BoundingBox;

public class BoundingBoxImpl implements BoundingBox {

    private int minX;
    private int minY;
    private int width;
    private int height;

    public BoundingBoxImpl() {
        super();
    }

    public BoundingBoxImpl(int minX, int minY, int width, int height) {
        super();
        this.minX = minX;
        this.minY = minY;
        this.width = width;
        this.height = height;
    }

    @Override
    public int getMinX() {
        return minX;
    }

    public void setMinX(int minX) {
        this.minX = minX;
    }

    @Override
    public int getMinY() {
        return minY;
    }

    public void setMinY(int minY) {
        this.minY = minY;
    }

    @Override
    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + height;
        result = prime * result + minX;
        result = prime * result + minY;
        result = prime * result + width;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BoundingBoxImpl other = (BoundingBoxImpl) obj;
        if (height != other.height)
            return false;
        if (minX != other.minX)
            return false;
        if (minY != other.minY)
            return false;
        if (width != other.width)
            return false;
        return true;
    }

}
