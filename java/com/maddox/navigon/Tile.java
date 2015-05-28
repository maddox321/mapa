package com.maddox.navigon;

import android.graphics.Bitmap;

//kafelki
public class Tile
{
    public int x;
    public int y;
    public Bitmap img;

    public Tile(int x, int y, Bitmap img)
    {
        this.x = x;
        this.y = y;
        this.img = img;
    }
}