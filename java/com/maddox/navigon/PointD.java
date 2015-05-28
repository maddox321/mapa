/*
 * MapApp : Simple offline map application, made by Hisham Ghosheh for tutorial purposes only
 * Tutorial on my blog
 * http://ghoshehsoft.wordpress.com/2012/03/09/building-a-map-app-for-android/
 * 
 * Class tutorial:
 * http://ghoshehsoft.wordpress.com/2012/03/19/mapapp3-writing-a-tiles-manager/
 */

package com.maddox.navigon;

public class PointD
{
    public double x, y;

    public PointD(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    public PointD()
    {
        this(0, 0);
    }

    @Override
    public String toString()
    {
        return "(" + Double.toString(x) + "," + Double.toString(y) + ")";
    }
}