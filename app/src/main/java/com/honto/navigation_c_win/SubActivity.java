package com.honto.navigation_c_win;


import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.imgproc.Imgproc.INTER_AREA;
import static org.opencv.imgproc.Imgproc.INTER_NEAREST;
import static org.opencv.imgproc.Imgproc.resize;


public class SubActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        //System.loadLibrary("image");
    }

    Button button, button2;
    ImageView imageView;
    Uri mapUri;
    Bitmap bitmap;
    int gFlag = -1; //Grabcut Flag

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);
        button = (Button)findViewById(R.id.buttonPanel);
        button2 = (Button)findViewById(R.id.buttonPanel2);

        Intent intent = getIntent();
        mapUri = intent.getParcelableExtra("Map");
        imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(gFlag ==-1) {
                    getContour(v, event);
                }
                else {
                    passagewayTouch(v, event);

                }

                return false;
            }
        });
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mapUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        /***************nexus to asus de tigau***********/
        if(bitmap.getWidth() > bitmap.getHeight()) {
            // 回転マトリックス作成（90度回転）
            Matrix mat = new Matrix();
            mat.postRotate(90);
            // 回転したビットマップを作成
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mat, true);

        }
        imageView.setImageBitmap(bitmap);
    }

    int x,y;
    ArrayList<Point> point = new ArrayList<Point>();

    int contour_count = 0;
    Bitmap tmp ;
    public void getContour(View v,MotionEvent event) {
        String action = "";
        Mat input = new Mat();
        if(contour_count==0) {
            tmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        }

        Matrix matrix = new Matrix();
        imageView.getImageMatrix().invert(matrix);
        matrix.postTranslate(imageView.getScrollX(),imageView.getScrollY());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                action = "ACTION_DOWN";
                float[] point_ex = {event.getX(),event.getY()};
                matrix.mapPoints(point_ex);
                x = (int)point_ex[0];y=(int)point_ex[1];

                point.add(new Point(x, y));
                Utils.bitmapToMat(tmp, input);
                Imgproc.ellipse(input, new Point(x, y), new Size(18, 18), 0, 0, 360, new Scalar(250, 0, 0, 55), -1);
                Imgproc.cvtColor(input, input, Imgproc.COLOR_RGB2RGBA, 4);
                Utils.matToBitmap(input, tmp);
                contour_count ++;
                imageView.setImageBitmap(null);
                imageView.setImageDrawable(null);
                imageView.setImageBitmap(tmp);
                break;
        }
    }

    Mat Passage_img;
    int passage_count = 0;
    public void passagewayTouch(View v, MotionEvent event) {
        String action = "";
        Matrix matrix = new Matrix();
        imageView.getImageMatrix().invert(matrix);
        matrix.postTranslate(imageView.getScrollX(),imageView.getScrollY());
        if(passage_count==0) {
            Passage_img = new Mat(label_img.rows(), label_img.cols(), CvType.CV_8UC1, new Scalar(0));
        }
        Mat gb_tmp = gb_img.clone();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                action = "ACTION_DOWN";

                float[] point_ex = {event.getX(),event.getY()};
                matrix.mapPoints(point_ex);
                x = (int)point_ex[0];y=(int)point_ex[1];

                long passageAddr = Passage_img.getNativeObjAddr();
                long labelAddr = label_img.getNativeObjAddr();
                long gbAddr = gb_tmp.getNativeObjAddr();

                //Touchしたところを通路領域にする
                PassageWaySegment(passageAddr,labelAddr,gbAddr,x,y);
                passage_count++;

                Imgproc.cvtColor(gb_tmp,gb_tmp,Imgproc.COLOR_RGB2RGBA,4);
                Bitmap dst = Bitmap.createBitmap(gb_tmp.cols(),gb_tmp.rows(),Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(gb_tmp,dst);

                imageView.setImageBitmap(dst);

                break;
        }
    }

    public Mat makeMaskImage(ArrayList<Point> point, List<MatOfPoint>contours_list){
        MatOfPoint contours =new MatOfPoint();
        contours.fromList(point);
        point.clear();
        contours_list.add(contours);
        Mat dst=new Mat(new Size(bitmap.getWidth(),bitmap.getHeight()), CV_8UC1,new Scalar(Imgproc.GC_BGD));
        Mat mask=new Mat(new Size(bitmap.getWidth(),bitmap.getHeight()), CV_8UC1,new Scalar(0));
        Scalar color= new Scalar(Imgproc.GC_PR_FGD);
        Imgproc.drawContours(dst, contours_list, -1, color,-1);
        Imgproc.drawContours(mask, contours_list, -1, new Scalar(255), -1);
        contours_list.clear();

        return mask;
    }

    List<MatOfPoint> contours_list = new ArrayList<>();
    Mat gb_img,label_img;
    @Override
    public void onResume(){
        super.onResume();

        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                /*GrabCut*/
                Mat mask = makeMaskImage(point,contours_list);
                gb_img = new Mat();
                Utils.bitmapToMat(bitmap,gb_img);
                long inputAddr = gb_img.getNativeObjAddr();
                long maskAddr = mask.getNativeObjAddr();
                GrabCut(inputAddr,maskAddr);
                gFlag=1;

                label_img = gb_img.clone();
                long labelAddr = label_img.getNativeObjAddr();
                ImageSegmentation(labelAddr);

                Imgproc.resize(gb_img,gb_img,label_img.size(),1,1,Imgproc.INTER_AREA);
                Imgproc.cvtColor(gb_img,gb_img,Imgproc.COLOR_RGB2RGBA,4);
                Bitmap dst = Bitmap.createBitmap(gb_img.cols(),gb_img.rows(),Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(gb_img,dst);
                imageView.setImageBitmap(dst);
            }

        });

        button2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){

                resize(Passage_img,Passage_img,new Size(bitmap.getWidth(),bitmap.getHeight()),1,1,INTER_NEAREST);
                Bitmap pass = Bitmap.createBitmap(Passage_img.cols(),Passage_img.rows(),Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(Passage_img,pass);
                saveImageExternal(pass);

                Intent intent2 = new Intent(SubActivity.this,SubSubActivity.class);
                intent2.putExtra("input_uri",mapUri);
                intent2.putExtra("pass_uri",passUri);
                startActivity(intent2);
            }
        });

    }

    Uri passUri;
    private void saveImageExternal(Bitmap bmp){

        //外部ストレージへのアクセスを確認する
        if (!isExternalStorageWritable()) {
            Log.i("saveImageExternal", "External Storage Not Writable.");
            return;
        }
        //パスを取得する
        String storagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
        String fileName = "PassageWay.jpg";

        //保存先のディレクトリがなければ作成する
        File file = new File(storagePath);
        try {
            if (!file.exists()) file.mkdir();
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        //ファイルを外部ストレージに保存して、ギャラリーに追加する
        file = new File(storagePath, fileName);
        passUri = Uri.fromFile(file);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            addImageToGallery(file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
    private void addImageToGallery(String filePath) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.MediaColumns.DATA, filePath);

            getApplicationContext().getContentResolver()
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native void GrabCut(long inputAddr, long maskAddr);
    public native void ImageSegmentation(long labelAddr);
    public native void PassageWaySegment(long passageAddr,long labelAddr,long gbAddr,int x, int y);
}
