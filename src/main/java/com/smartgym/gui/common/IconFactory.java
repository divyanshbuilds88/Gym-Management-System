package com.smartgym.gui.common;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/** Draws all menu icons with Java2D, so the project needs no image files. */
public final class IconFactory {

    private IconFactory() { }

    public static Icon get(final String name, final int size, final Color color) {
        return new Icon() {                                       // anonymous inner class implementing an interface
            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(Math.max(1.5f, size / 11f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                draw(g2, name, size);
                g2.dispose();
            }
        };
    }

    private static void person(Graphics2D g2, float s, float ox, float oy, float k) {
        AffineTransform old = g2.getTransform();
        g2.translate(ox, oy);
        g2.scale(k, k);
        g2.fill(new Ellipse2D.Float(s * .33f, s * .08f, s * .34f, s * .34f));
        g2.fill(new Arc2D.Float(s * .16f, s * .5f, s * .68f, s * .8f, 0, 180, Arc2D.PIE));
        g2.setTransform(old);
    }

    private static void draw(Graphics2D g2, String name, float s) {
        switch (name) {
            case "dashboard":
                g2.fill(new RoundRectangle2D.Float(s * .08f, s * .08f, s * .38f, s * .38f, 4, 4));
                g2.fill(new RoundRectangle2D.Float(s * .54f, s * .08f, s * .38f, s * .38f, 4, 4));
                g2.fill(new RoundRectangle2D.Float(s * .08f, s * .54f, s * .38f, s * .38f, 4, 4));
                g2.fill(new RoundRectangle2D.Float(s * .54f, s * .54f, s * .38f, s * .38f, 4, 4));
                break;
            case "members":
                person(g2, s, 0, 0, 1f);
                break;
            case "trainers":
                person(g2, s, -s * .12f, s * .04f, .85f);
                person(g2, s, s * .3f, s * .1f, .85f);
                break;
            case "membership":
                g2.draw(new RoundRectangle2D.Float(s * .08f, s * .2f, s * .84f, s * .6f, 5, 5));
                g2.fill(new Rectangle2D.Float(s * .08f, s * .36f, s * .84f, s * .13f));
                g2.drawLine((int) (s * .2f), (int) (s * .68f), (int) (s * .45f), (int) (s * .68f));
                break;
            case "workout":
                g2.drawLine((int) (s * .22f), (int) (s * .5f), (int) (s * .78f), (int) (s * .5f));
                g2.fill(new RoundRectangle2D.Float(s * .06f, s * .3f, s * .12f, s * .4f, 3, 3));
                g2.fill(new RoundRectangle2D.Float(s * .2f, s * .2f, s * .12f, s * .6f, 3, 3));
                g2.fill(new RoundRectangle2D.Float(s * .68f, s * .2f, s * .12f, s * .6f, 3, 3));
                g2.fill(new RoundRectangle2D.Float(s * .82f, s * .3f, s * .12f, s * .4f, 3, 3));
                break;
            case "schedule":
                g2.draw(new RoundRectangle2D.Float(s * .1f, s * .18f, s * .8f, s * .72f, 6, 6));
                g2.fill(new Rectangle2D.Float(s * .1f, s * .18f, s * .8f, s * .2f));
                g2.drawLine((int) (s * .3f), (int) (s * .06f), (int) (s * .3f), (int) (s * .26f));
                g2.drawLine((int) (s * .7f), (int) (s * .06f), (int) (s * .7f), (int) (s * .26f));
                g2.fill(new Rectangle2D.Float(s * .24f, s * .54f, s * .16f, s * .16f));
                g2.fill(new Rectangle2D.Float(s * .52f, s * .54f, s * .16f, s * .16f));
                break;
            case "attendance":
                g2.draw(new Ellipse2D.Float(s * .1f, s * .1f, s * .8f, s * .8f));
                g2.drawLine((int) (s * .5f), (int) (s * .5f), (int) (s * .5f), (int) (s * .26f));
                g2.drawLine((int) (s * .5f), (int) (s * .5f), (int) (s * .68f), (int) (s * .58f));
                break;
            case "payments":
                g2.draw(new RoundRectangle2D.Float(s * .06f, s * .24f, s * .88f, s * .52f, 6, 6));
                g2.fill(new Ellipse2D.Float(s * .36f, s * .36f, s * .28f, s * .28f));
                g2.fill(new Ellipse2D.Float(s * .16f, s * .46f, s * .08f, s * .08f));
                g2.fill(new Ellipse2D.Float(s * .76f, s * .46f, s * .08f, s * .08f));
                break;
            case "reports":
                g2.fill(new RoundRectangle2D.Float(s * .12f, s * .55f, s * .2f, s * .35f, 3, 3));
                g2.fill(new RoundRectangle2D.Float(s * .4f, s * .3f, s * .2f, s * .6f, 3, 3));
                g2.fill(new RoundRectangle2D.Float(s * .68f, s * .12f, s * .2f, s * .78f, 3, 3));
                break;
            case "profile":
                g2.draw(new Ellipse2D.Float(s * .05f, s * .05f, s * .9f, s * .9f));
                person(g2, s, s * .25f, s * .2f, .5f);
                break;
            case "logout":
                Path2D door = new Path2D.Float();
                door.moveTo(s * .45f, s * .14f);
                door.lineTo(s * .14f, s * .14f);
                door.lineTo(s * .14f, s * .86f);
                door.lineTo(s * .45f, s * .86f);
                g2.draw(door);
                g2.drawLine((int) (s * .36f), (int) (s * .5f), (int) (s * .86f), (int) (s * .5f));
                g2.drawLine((int) (s * .68f), (int) (s * .32f), (int) (s * .86f), (int) (s * .5f));
                g2.drawLine((int) (s * .68f), (int) (s * .68f), (int) (s * .86f), (int) (s * .5f));
                break;
            case "refresh":
                g2.draw(new Arc2D.Float(s * .16f, s * .16f, s * .68f, s * .68f, 40, 270, Arc2D.OPEN));
                Path2D head = new Path2D.Float();
                head.moveTo(s * .86f, s * .14f);
                head.lineTo(s * .86f, s * .42f);
                head.lineTo(s * .58f, s * .34f);
                head.closePath();
                g2.fill(head);
                break;
            case "check":
                Path2D tick = new Path2D.Float();
                tick.moveTo(s * .2f, s * .52f);
                tick.lineTo(s * .42f, s * .74f);
                tick.lineTo(s * .8f, s * .28f);
                g2.draw(tick);
                break;
            default:
                g2.fill(new Ellipse2D.Float(s * .25f, s * .25f, s * .5f, s * .5f));
                break;
        }
    }
}
