package com.honto.navigation_c_win;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;

public class DisplayPosition extends View {
    final int AMOUNT_OF_DISPLAYED_POSITION = 15;

    private Paint PathPaint, TapedPaint, ParticlePaint;
    public Point Position;
    ArrayList<PointF> PositionList;
    public ArrayList<PointF> DisplayedParticleList;
    public Point TapedPosition1, TapedPosition2;

    public DisplayPosition(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.PathPaint = new Paint();
        this.PathPaint.setColor(Color.rgb(255, 0, 0));
        this.PathPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.TapedPaint = new Paint();
        this.TapedPaint.setColor(Color.rgb(50, 50, 50));
        this.TapedPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.ParticlePaint = new Paint();
        this.ParticlePaint.setColor(Color.rgb(255, 100, 100));
        this.ParticlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        initialize();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        if(TapedPosition1.x != -1) canvas.drawCircle(TapedPosition1.x, TapedPosition1.y, 8, TapedPaint);
        if(TapedPosition2.x != -1) canvas.drawCircle(TapedPosition2.x, TapedPosition2.y, 8, TapedPaint);
        for(PointF point : DisplayedParticleList){
            canvas.drawCircle(point.x, point.y, 1, ParticlePaint);
        }
        int m = 0;
        for(Iterator<PointF> i = PositionList.iterator(); i.hasNext(); ){
            PointF posi = i.next(); m++;
            PathPaint.setColor(Color.argb(17 * m, 255, 0, 0));
            canvas.drawCircle(posi.x, posi.y, 5, ParticlePaint);
        }
    }

    public void updatePositionDisplay(){
        invalidate();//描画関数
    }

    public void initialize(){
        Position = new Point(0,0);
        PositionList = new ArrayList<>();
        TapedPosition1 = new Point(-1,-1);
        TapedPosition2 = new Point(-1,-1);
        DisplayedParticleList = new ArrayList<>();
    }

    public void updatePositionList(PointF point){
        PointF p = new PointF();
        if(PositionList.size() > AMOUNT_OF_DISPLAYED_POSITION) PositionList.remove(0);
        p.set(point);
        PositionList.add(p);
    }
}
