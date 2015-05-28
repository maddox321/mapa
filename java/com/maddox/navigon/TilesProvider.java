package com.maddox.navigon;

import java.util.Hashtable;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

public class TilesProvider
{
    // baza z mapa
    protected SQLiteDatabase tilesDB;

    // do przechpwuwania kafelek hash - x:y
    protected Hashtable<String, Tile> tiles = new Hashtable<String, Tile>();

    public TilesProvider(String dbPath)
    {
        tilesDB = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
    }

    public void fetchTiles(Rect rect, int zoom)
    {
        // zapytanie do bazy
        String query = "SELECT x,y,image FROM tiles WHERE x >= " + rect.left + " AND x <= " + rect.right + " AND y >= " + rect.top
                + " AND y <=" + rect.bottom + " AND z == " + (17 - zoom);

        Cursor cursor;
        cursor = tilesDB.rawQuery(query, null);


        Hashtable<String, Tile> temp = new Hashtable<String, Tile>();

        //kursor bo otrzymanych rowach
        if (cursor.moveToFirst())
        {
            do
            {
                int x = cursor.getInt(0);
                int y = cursor.getInt(1);

                Tile tile = tiles.get(x + ":" + y);

                //nie ma w bazie
                if (tile == null)
                {
                    byte[] img = cursor.getBlob(2);

                    Bitmap tileBitmap = BitmapFactory.decodeByteArray(img, 0, img.length);

                    tile = new Tile(x, y, tileBitmap);
                }

                temp.put(x + ":" + y, tile);
            }
            while (cursor.moveToNext());

            tiles.clear();
            tiles = temp;
        }
    }

    public Hashtable<String, Tile> getTiles()
    {
        return tiles;
    }

    public void close()
    {
        tilesDB.close();
    }

    public void clear()
    {
        tiles.clear();
    }
}