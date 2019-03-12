package com.honto.navigation_c_win;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        //System.loadLibrary("image");
    }

    //opencv library load
    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("tag", "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    private  Bitmap bitmap;
    private ImageView imageView;
    private Uri m_uri;
    private Uri resultUri;
    private static final int REQUEST_CHOOSER = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mOpenCVCallBack);
        setViews();
    }

    private void setViews(){
        Button button = (Button)findViewById(R.id.buttonPanel);//Gallery
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                showPicture();
            }
        });

        //SubActivityへの遷移
        Button button2 = (Button)findViewById(R.id.buttonPanel2);//Next
        button2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v2){
                Im_pro();
                Intent intent = new Intent(MainActivity.this, SubActivity.class);
                intent.putExtra("Map",resultUri);//Mapの受け渡し
                startActivity(intent);
            }
        });
    }

    private void showPicture() {
        //qiita.com/Yuki_Yamada/items/137d15a4e65ed2308787
        //"[android]ギャラリーもしくはカメラから画像を持ってくる"参照(2018/2/6)

        //カメラの起動Intentの用意
        File pathExternalPublicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        String filename = System.currentTimeMillis() + ".jpg";
        File capturedFile = new File(pathExternalPublicDir, filename);
        m_uri = Uri.fromFile(capturedFile);
        Intent intentCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, m_uri);

        // ギャラリー用のIntent作成
        Intent intentGallery = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intentGallery.addCategory(Intent.CATEGORY_OPENABLE);
        intentGallery.setType("image/jpeg");
        Intent intent = Intent.createChooser(intentCamera, "Select Action");
        intent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{intentGallery});
        startActivityForResult(intent, REQUEST_CHOOSER);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CHOOSER) {
            if (resultCode != RESULT_OK) {
                // キャンセル時
                return;
            }
            resultUri = (data != null ? data.getData() : m_uri);//条件式 ? 真の場合の文 : 偽の場合の文
            System.out.println("URI:" + resultUri);
            if (resultUri == null) {
                // 取得失敗
                Log.d("Error","Sippai yade!!");
                return;
            }
            // ギャラリーへスキャンを促す
            MediaScannerConnection.scanFile(
                    this,
                    new String[]{resultUri.getPath()},
                    new String[]{"image/jpeg"},
                    null
            );
            // 画像を設定
            imageView = (ImageView) findViewById(R.id.imageView);
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), resultUri);
                /***************nexus to asus de tigau***********/
                if(bitmap.getWidth() > bitmap.getHeight()) {
                    // 回転マトリックス作成（90度回転）
                    Matrix mat = new Matrix();
                    mat.postRotate(90);
                    // 回転したビットマップを作成
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mat, true);

                }
                imageView.setImageBitmap(bitmap);
                m_uri = resultUri;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


    private void Im_pro(){
        Mat inputMat = new Mat();
        Utils.bitmapToMat(bitmap,inputMat);

        long addr = inputMat.getNativeObjAddr();
        OutputImage(addr);

        Imgproc.cvtColor(inputMat,inputMat,Imgproc.COLOR_GRAY2RGBA, 4);
        Bitmap dst = Bitmap.createBitmap(inputMat.width(), inputMat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(inputMat, dst);

        imageView.setImageBitmap(bitmap);


    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native void OutputImage(long inputAddr);
}
