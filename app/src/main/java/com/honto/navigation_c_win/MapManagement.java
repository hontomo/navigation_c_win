package com.honto.navigation_c_win;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.view.Display;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MapManagement {
    final int PARTICLE_NUMBER = 2000;

    public Bitmap MapImageName, PassagewayMaskImage;
    Point MapSize, MapSizeDisplayed;
    public Rect MapRectGlobal, MapImageViewRectGlobal;
    private Point DisplaySize;

    public PointF TapedPosition1_view, TapedPosition2_view;
    public float RealLengthBetweenClicks, RealThetaBetweenClicks;

    public ArrayList<Particle> ParticleList;
    public boolean isParticleInitialized;
    private double Sigma_tap;
    public PointF NowPosition;
    Bitmap input_image;
    Bitmap passage_image;

    MapManagement(Context context){
        //MapImageName =BitmapFactory.decodeResource(context.getResources(), R.drawable.map1);
        //PassagewayMaskImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.map1m);
        MapSize = new Point();
        MapSizeDisplayed = new Point();
        MapRectGlobal = new Rect();
        MapImageViewRectGlobal = new Rect();
        DisplaySize = new Point();
        TapedPosition1_view = new PointF();
        TapedPosition2_view = new PointF();
        NowPosition = new PointF(0,0);
        initializePositionInfo();
        Sigma_tap = 20;
        isParticleInitialized = false;
    }

    public void setmap(Bitmap input,Bitmap passage){
        MapImageName=input;
        PassagewayMaskImage=passage;
    }
    public void initializePositionInfo(){
        TapedPosition1_view.x = -1.0f;
        TapedPosition1_view.y = -1.0f;
        TapedPosition2_view.x = -1.0f;
        TapedPosition2_view.y = -1.0f;
    }

    public void setMapSize(){
        MapSize.x = MapImageName.getWidth();
        MapSize.y = MapImageName.getHeight();
    }

    public void setDisplaySize(Display display){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Android 4.2~
            display.getRealSize(DisplaySize);

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            // Android 3.2~
            try {
                Method getRawWidth = Display.class.getMethod("getRawWidth");
                Method getRawHeight = Display.class.getMethod("getRawHeight");
                int width = (Integer) getRawWidth.invoke(display);
                int height = (Integer) getRawHeight.invoke(display);
                DisplaySize.set(width, height);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Sigma_tap = Math.min(DisplaySize.x, DisplaySize.y) / 100;
    }

    public PointF transformViewToImage(PointF view){
        PointF image_coordinate = new PointF();
        image_coordinate.x = (((float)MapSize.x/(float)(MapRectGlobal.right-MapRectGlobal.left))*(view.x-(float)MapRectGlobal.left));
        image_coordinate.y = (((float)MapSize.y/(float)(MapRectGlobal.bottom-MapRectGlobal.top))*(view.y-(float)MapRectGlobal.top));
        return image_coordinate;
    }

    public void calcMapRectGlobal(){
        MapRectGlobal.top = (MapImageViewRectGlobal.bottom - MapImageViewRectGlobal.top)/2 - MapSizeDisplayed.y/2 + MapImageViewRectGlobal.top;
        MapRectGlobal.bottom = (MapImageViewRectGlobal.bottom - MapImageViewRectGlobal.top)/2 + MapSizeDisplayed.y/2 + MapImageViewRectGlobal.top;
        MapRectGlobal.left = (MapImageViewRectGlobal.right - MapImageViewRectGlobal.left)/2 - MapSizeDisplayed.x/2 + MapImageViewRectGlobal.left;
        MapRectGlobal.right = (MapImageViewRectGlobal.right - MapImageViewRectGlobal.left)/2 + MapSizeDisplayed.x/2 + MapImageViewRectGlobal.left;
    }

    float mpp;//20180615 change
    public float getMpp(){
        return mpp;
    }
    public void initializeParticles(){
        ParticleList = new ArrayList<>();
        for(int i = 0; i < PARTICLE_NUMBER; i++){
            Random r = new Random(System.currentTimeMillis());
            PointF startclick_with_noiz = new PointF();
            PointF stopclick_with_noiz = new PointF();
            startclick_with_noiz.x = (float)(TapedPosition1_view.x);// + Sigma_tap * r.nextGaussian());
            startclick_with_noiz.y = (float)(TapedPosition1_view.y);// + Sigma_tap * r.nextGaussian());
            stopclick_with_noiz.x = (float)(TapedPosition2_view.x);// + Sigma_tap * r.nextGaussian());
            stopclick_with_noiz.y = (float)(TapedPosition2_view.y);// + Sigma_tap * r.nextGaussian());
            System.out.println("random:" + r.nextGaussian());
            System.out.println("maprect:"+MapImageViewRectGlobal);
            System.out.println("t_x,t_y=" + TapedPosition1_view + "and t_x,t_y" + TapedPosition2_view);
            PointF start_in_map, stop_in_map;
            start_in_map = transformViewToImage(startclick_with_noiz);
            stop_in_map = transformViewToImage(stopclick_with_noiz);
            float length_in_map = (float) Math.sqrt(Math.pow(start_in_map.x - stop_in_map.x, 2) + Math.pow(start_in_map.y - stop_in_map.y, 2));
            System.out.println("x,y=" + start_in_map+" and x,y=" + stop_in_map);

            mpp = RealLengthBetweenClicks / length_in_map;
            System.out.println(RealLengthBetweenClicks);
            float theta_in_map = (float) Math.atan2(stop_in_map.y - start_in_map.y, stop_in_map.x - start_in_map.x);
            float offset = RealThetaBetweenClicks - theta_in_map;

            /********Initial Parameter Change**********/
            /*nn

            HeadingDirection initialDirection = new HeadingDirection();
            initialDirection.estimateDirection();

            StepEventDetection initialStep = new StepEventDetection();
            boolean s = initialStep.judgeStep();
            int valley_num;
            if(s){
                valley_num = initialStep.detectValley();
            }
            else{
                valley_num = 53;
            }
            if(i==0){
                for (int j = 0; j<54 ; j++)
                    System.out.println("Sensor Direction : " + initialDirection.LastDirection[j]);
            }
            //up : -pi/2  right : 0 down : pi/2 left : pi
            theta_in_map = (float)-Math.PI/2;

            mpp = (float)0.01140;//0.0170;// real_nagasa /pixel_nagasa * (transform_youarehere_x/youarehere_x)
            //このプログラムの画像の表示がそのサイズを少し縮小しているから(2448*3264のときは2/3くらい)
            //mpp = (float)0.0219;3++
            // mpp = (float)0.0175;
            //System.out.println("Sigma_tap : " + Sigma_tap);
            //System.out.println("r.nextGaussian()" + r.nextGaussian());
            //System.out.println(RealThetaBetweenClicks+","+RealLengthBetweenClicks);
            offset = initialDirection.LastDirection[valley_num] - theta_in_map;
*/
            /*************/

            ParticleList.add(new Particle(stopclick_with_noiz, mpp, offset, 1));
            //if(stopclick_with_noiz.y > 1000) Log.d("log", "position = " + stopclick_with_noiz.x + ", " + stopclick_with_noiz.y);
        }
        NowPosition.set(TapedPosition2_view.x-(float)MapImageViewRectGlobal.left, TapedPosition2_view.y-(float)MapImageViewRectGlobal.top);
        isParticleInitialized = true;
    }

    public void updateParticles(float length, float direction){
        /** Particle Propagation & Particle Correction */
        int total_weight = 0,total_mpp=0;
        float x = 0.0f, y = 0.0f;
        float rate = (float)MapSizeDisplayed.x / (float)MapSize.x;
        Iterator<Particle> i = ParticleList.iterator();
        Random r = new Random(System.currentTimeMillis());
        while(i.hasNext()) {
            Particle p = i.next();
            //Log.d("position", "position = " + p.position.x + ", " + p.position.y);
            p.meterperpixel += p.meterperpixel * (float) r.nextGaussian() / 100.0f;
            p.offset += (float) ((2 * Math.PI / 180) * r.nextGaussian());
            p.position.x += length * (float) Math.cos(direction - p.offset + (2*Math.PI/180)*r.nextGaussian() ) / p.meterperpixel * rate;
            p.position.y += length * (float) Math.sin(direction - p.offset + (2*Math.PI/180)*r.nextGaussian() ) / p.meterperpixel * rate;
            PointF position_in_map = transformViewToImage(p.position);
            total_mpp += p.meterperpixel;
            if (Color.red(PassagewayMaskImage.getPixel((int) position_in_map.x, (int) position_in_map.y)) == 0) {
                i.remove();
            }else{
                x += p.position.x*p.weight;///p.position.x
                y += p.position.y*p.weight;///p.position.y
                total_weight += p.weight;

                System.out.println("weoght" + p.weight);
            }
        }
        int remain =  ParticleList.size();
        NowPosition.set(x / remain - (float) MapImageViewRectGlobal.left,y/remain-(float)MapImageViewRectGlobal.top);

        /** Re-Sampling */
        ArrayList<Particle> particles_old = new ArrayList<>();
        particles_old.addAll(ParticleList);
        ParticleList.clear();
        for(int a = 0; a < PARTICLE_NUMBER; a++) {
            /*
            int sampled_weight = 0, sampled_particle = r.nextInt(total_weight);
            Particle p = new Particle();
            Iterator<Particle> j = particles.iterator();
            while(sampled_weight <= sampled_particle){
                p = j.next();
                sampled_weight += p.weight;
            }
            p.weightlist.add(remain+a);
            */
            Particle p = particles_old.get(r.nextInt(remain));
            PointF new_p = new PointF();
            if (remain > 0.4 * PARTICLE_NUMBER) { new_p.x = p.position.x; new_p.y = p.position.y; }
            else{
                /** Particle Diffusion */
                Random r2 = new Random(System.currentTimeMillis());
                new_p.x = p.position.x + 30*(float)r2.nextGaussian();
                new_p.y = p.position.y + 30*(float)r2.nextGaussian();
            }
            float new_mpp =  p.meterperpixel;
            mpp = new_mpp;//20181016update
            float new_offset = p.offset;
            ParticleList.add(new Particle(new_p, new_mpp, new_offset, 1));
        }
        particles_old.clear();
        /*
        Particle p;
        Iterator<Particle> j = particles.iterator();
        for(int a = 0; a < remain; a++){
            p = j.next();
            p.weight += p.weightlist.size() + 1;
            for (Integer index : p.weightlist) {
                Particle q = particles.get(index);
                q.weight = p.weight;
            }
            p.weightlist.clear();
        }*/
    }

}

class Particle {
    public PointF position;
    public float meterperpixel;
    public float offset;
    public int weight;
    //public ArrayList<Integer> weightlist;

    Particle(){
        position = new PointF();
        //weightlist = new ArrayList<>();
    }

    Particle(PointF _position, float _meterperpixle, float _offset, int _weight){
        position = new PointF();
        position.set(_position);
        meterperpixel = _meterperpixle;
        offset = _offset;
        weight = _weight;
        //weightlist = new ArrayList<>();
    }
}

