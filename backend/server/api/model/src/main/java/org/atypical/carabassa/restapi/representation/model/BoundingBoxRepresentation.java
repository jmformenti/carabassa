package org.atypical.carabassa.restapi.representation.model;

public class BoundingBoxRepresentation {

    private int minX;
    private int minY;
    private int width;
    private int height;

    public BoundingBoxRepresentation() {
        super();
    }

    public BoundingBoxRepresentation(int minX, int minY, int width, int height) {
        super();
        this.minX = minX;
        this.minY = minY;
        this.width = width;
        this.height = height;
    }

    public int getMinX() {
        return minX;
    }

    public void setMinX(int minX) {
        this.minX = minX;
    }

    public int getMinY() {
        return minY;
    }

    public void setMinY(int minY) {
        this.minY = minY;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

}
