package com.maddox.navigon;

import java.util.Collection;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class MapView extends View
{
    protected Context context;

    // wymiary widoku
    protected int viewWidth, viewHeight;

    //dostarczanie kafelkow do widoku
    protected TilesProvider tileProvider;

    //zarzadzanie kafelkami
    protected TilesManager tileManager;

    protected Paint fontPaint;
    protected Paint bitmapPaint = new Paint();
    protected Paint circlePaint = new Paint();

    // centrowanie na ten punkt (ETI) gdy nie ma gps
    protected PointD seekLocation = new PointD(18.612363, 54.371676);
    // lokalizacja gps
    protected Location gpsLocation = null;
    // jezeli prawda seekLoc = gpsLoc
    protected boolean autoFollow = false;

    protected Bitmap positionMarker;

    protected PointD lastTouchPos = new PointD(-1, -1);

    protected PointD tochPoin = new PointD(0,0);

    public MapView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        viewWidth = viewHeight = -1;

        if (!isInEditMode())
        {
            int zoomLevel = 0;
            TypedArray arr = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MapView, 0,0);
            try
            {
                zoomLevel = arr.getInt(R.styleable.MapView_zoomLevel, 0);
                Drawable d = arr.getDrawable(R.styleable.MapView_marker);
                if (d != null) positionMarker = ((BitmapDrawable) d).getBitmap();
            }
            finally
            {
                arr.recycle();
            }

            tileManager = new TilesManager(256, viewWidth, viewHeight);
            tileManager.setZoom(zoomLevel);
        }

        initPaints();
    }

    public MapView(Context context, int viewWidth, int viewHeight, TilesProvider tilesProvider, Bitmap positionMarker)
    {
        super(context);
        this.context = context;

        this.tileProvider = tilesProvider;

        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;

        this.positionMarker = positionMarker;

        //wymiary kafelka to 256 podawany jako parametr bo s¹ mozliwe rozne wielkosci
        tileManager = new TilesManager(256, viewWidth, viewHeight);

        //inicjalizacja paintow
        initPaints();

        // pobieranie kafelek
        fetchTiles();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        // startowe wymiary podawane z konstruktora
        if (viewWidth != -1 && viewHeight != -1)
        {
            //ustawianie odpowiednich wymiarow
            setMeasuredDimension(viewWidth, viewHeight);
        }
        // widok wziety z xmla.
        else
        {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    void initPaints()
    {
        // tekst
        fontPaint = new Paint();
        fontPaint.setColor(Color.DKGRAY);
        fontPaint.setShadowLayer(1, 1, 1, Color.BLACK);
        fontPaint.setTextSize(20);

        circlePaint.setARGB(70, 170, 170, 80);
        circlePaint.setAntiAlias(true);
    }

    void fetchTiles()
    {
        // update menadzera zeby wiedzia³ gdzie jest teraz srodek
        tileManager.setLocation(seekLocation.x, seekLocation.y);

        // pobranie 4 rogow jakie potrzebujemy
        Rect visibleRegion = tileManager.getVisibleRegion();

        if (tileProvider == null) return;

       //zamowienie na konkretne kafelki dany region o danym zoomie
       tileProvider.fetchTiles(visibleRegion, tileManager.getZoom());
    }

    public void setTilesProvider(TilesProvider tilesProvider)
    {
        this.tileProvider = tilesProvider;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        if (this.isInEditMode())
        {
            canvas.drawARGB(255, 255, 255, 255);

            String str = "MapView: Design Mode";

            //ustawienie tekstu
            int xPos = (int) ((canvas.getWidth() / 2f) - fontPaint.measureText(str) / 2f);
            int yPos = (int) ((canvas.getHeight() / 2) - ((fontPaint.descent() + fontPaint.ascent()) / 2));

            canvas.drawText(str, xPos, yPos, fontPaint);
            return;
        }

        //nie ma kafelkow
        if (tileProvider == null)
        {
            canvas.drawARGB(255, 250, 111, 103);

            String str = "TilesProvider not set";

            int xPos = (int) ((canvas.getWidth() / 2f) - fontPaint.measureText(str) / 2f);
            int yPos = (int) ((canvas.getHeight() / 2) - ((fontPaint.descent() + fontPaint.ascent()) / 2));

            canvas.drawText(str, xPos, yPos, fontPaint);

            return;
        }

        // szary
        canvas.drawARGB(255, 100, 100, 100);

        //znormalizowany pixel œrodka 0,0 - 1,0
        PointD pixRatio = TilesManager.calcRatio(seekLocation.x, seekLocation.y);

        // normalny pixel œrodka
        int mapWidth = tileManager.mapSize() * 256;
        Point pix = new Point((int) (pixRatio.x * mapWidth), (int) (pixRatio.y * mapWidth));

		//ustalanie polozenia kafelkow
        Point offset = new Point((int) (pix.x - viewWidth / 2f), (int) (pix.y - viewHeight / 2f));

        drawTiles(canvas, offset);

        drawMarker(canvas, offset);
    }

    void drawTiles(Canvas canvas, Point offset)
    {
        Collection<Tile> tilesList = tileProvider.getTiles().values();

        //petla po wszystkich kafelkach
        for (Tile tile : tilesList)
        {

            int tileSize = tileManager.getTileSize();
            long tileX = tile.x * tileSize;
            long tileY = tile.y * tileSize;

            long finalX = tileX - offset.x;
            long finalY = tileY - offset.y;

            //rysowanie
            canvas.drawBitmap(tile.img, finalX, finalY, bitmapPaint);
        }
    }

    void drawMarker(Canvas canvas, Point offset)
    {
        if (gpsLocation != null)
        {
            Point markerPos = tileManager.lonLatToPixelXY(gpsLocation.getLongitude(), gpsLocation.getLatitude());

            int markerX = markerPos.x - offset.x;
            int markerY = markerPos.y - offset.y;

            if (positionMarker != null)
            {
                canvas.drawBitmap(positionMarker, markerX - positionMarker.getWidth() / 2, markerY - positionMarker.getHeight() / 2,
                        bitmapPaint);
            }
            //wcielo grafike rysuj kó³ko
            else
            {
                canvas.drawCircle(markerX, markerY, 10, bitmapPaint);
            }

            // ile metrow na 1 px
            float ground = (float) tileManager.calcGroundResolution(gpsLocation.getLatitude());

            float rad = gpsLocation.getAccuracy() / ground;

            canvas.drawCircle(markerX, markerY, rad, circlePaint);

            // info tekstowe
            int pen = 1;
            canvas.drawText("lon:" + (tochPoin.x*(-1)), 0, 20 * pen++, fontPaint);
            canvas.drawText("lat:" + (tochPoin.y*(-1)), 0, 20 * pen++, fontPaint);
//            canvas.drawText("alt:" + gpsLocation.getAltitude(), 0, 20 * pen++, fontPaint);
            canvas.drawText("Zoom:" + tileManager.getZoom(), 0, 20 * pen++, fontPaint);
            canvas.drawText("\n10 px to: " + String.valueOf(ground*10), 0, 20 * pen++, fontPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN)
        {
            lastTouchPos.x = (int) event.getX();
            lastTouchPos.y = (int) event.getY();
            Point center = new Point(getWidth() / 2, getHeight() / 2);
            Point diff = new Point(center.x - (int) lastTouchPos.x, center.y - (int) lastTouchPos.y);
            Point centerGlobal = tileManager.lonLatToPixelXY(seekLocation.x, seekLocation.y);
            centerGlobal.x -= diff.x;
            centerGlobal.y -= diff.y;

            PointD geoPoint = tileManager.pixelXYToLonLat((int) centerGlobal.y, (int) centerGlobal.x);
            tochPoin = geoPoint;

            return true;
        }
        else if (action == MotionEvent.ACTION_MOVE)
        {
            autoFollow = false;
            PointD current = new PointD(event.getX(), event.getY());

            // o ile przesuniecie w px
            PointD diff = new PointD(current.x - lastTouchPos.x, current.y - lastTouchPos.y);

            Point pixels1 = tileManager.lonLatToPixelXY(seekLocation.x, seekLocation.y);

            Point pixels2 = new Point(pixels1.x - (int) diff.x, pixels1.y - (int) diff.y);

            PointD newSeek = tileManager.pixelXYToLonLat((int) pixels2.x, (int) pixels2.y);

            seekLocation = newSeek;

            fetchTiles();
            invalidate();

            lastTouchPos.x = current.x;
            lastTouchPos.y = current.y;

            return true;
        }

        return super.onTouchEvent(event);
    }


    public void refresh()
    {
        fetchTiles();
        invalidate();
    }

    public void postRefresh()
    {
        fetchTiles();
        postInvalidate();
    }

    public void followMarker()
    {
        if (gpsLocation != null)
        {
            seekLocation.x = gpsLocation.getLongitude();
            seekLocation.y = gpsLocation.getLatitude();
            autoFollow = true;

            fetchTiles();
            invalidate();
        }
    }

    public void zoomIn()
    {
        tileManager.zoomIn();
        onMapZoomChanged();
    }

    public void zoomOut()
    {
        tileManager.zoomOut();
        onMapZoomChanged();
    }

    protected void onMapZoomChanged()
    {
        if (tileProvider != null) tileProvider.clear();

        fetchTiles();
        invalidate();
    }

    public Location getGpsLocation()
    {
        return gpsLocation;
    }

    public PointD getSeekLocation()
    {
        return seekLocation;
    }

    public void setSeekLocation(double longitude, double latitude)
    {
        seekLocation.x = longitude;
        seekLocation.y = latitude;
    }

    public void setGpsLocation(double longitude, double latitude)
    {
        if (gpsLocation == null) gpsLocation = new Location("");
        gpsLocation.setLongitude(longitude);
        gpsLocation.setLatitude(latitude);

        if (autoFollow) followMarker();

    }

    public int getZoom()
    {
        return tileManager.getZoom();
    }

    public void setZoom(int zoom)
    {
        tileManager.setZoom(zoom);
        onMapZoomChanged();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);

        if (!this.isInEditMode())
        {
            this.viewWidth = getWidth();
            this.viewHeight = getHeight();

            tileManager.setDimensions(viewWidth, viewHeight);

            refresh();
        }
    }
}