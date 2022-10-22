package client;

import java.awt.*;
import java.io.Serializable;

public class Drawable implements Serializable {
    Shape shape;
    String text;
    Color color;
    Point point;

    public Drawable(Shape shape) {
        this.shape = shape;
    }

    public Drawable(String text) {
        this.text = text;
    }

    public void setShape(Shape shape) {
        this.shape = shape;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Shape getShape() {
        return shape;
    }

    public String getText() {
        return text;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Point getPoint() {
        return point;
    }

    public void setPoint(Point point) {
        this.point = point;
    }

    public void paint(Graphics2D g) {
        if(color != null) {
            g.setColor(color);
        }
        if(shape != null) {
            g.draw(shape);
        }
        if(text != null) {
            g.drawString(text, point.x, point.y);
        }
    }
}
