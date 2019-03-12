package com.honto.navigation_c_win;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

public class SubSubActivity extends AppCompatActivity implements SensorEventListener {

    Bitmap Input,Passage;
    private static String LOCAL_FILE = "log.txt";
    private SensorManager SensorManaged;
    public ManageSensorSystem SensorInput;
    private MapManagement MapManaged;
    Button PInitBtn;
    Switch LogSwitch;
    ImageView MapImage;
    DisplayPosition ShowPosition;

    boolean isPInit, isHaveStartPosition, LogFlag;
    int TotalStep;
    float TotalMovex, TotalMovey;

    int StepCount;
    float MoveLength;
    float LastAcceleration = 0.0f;
    long LastTime_ms;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub_sub);

        Intent intent = getIntent();
        Uri passUri = intent.getParcelableExtra("pass_uri");
        Uri mapUri = intent.getParcelableExtra("input_uri");
        try {
            Passage = MediaStore.Images.Media.getBitmap(getContentResolver(), passUri);
            Input = MediaStore.Images.Media.getBitmap(getContentResolver(), mapUri);

            /***************nexus to asus de tigau***********/
            if(Input.getWidth() > Input.getHeight()) {
                // 回転マトリックス作成（90度回転）
                Matrix mat = new Matrix();
                mat.postRotate(90);
                // 回転したビットマップを作成
                Input = Bitmap.createBitmap(Input, 0, 0, Input.getWidth(), Input.getHeight(), mat, true);
                //Passage = Bitmap.createBitmap(Input, 0, 0, Passage.getWidth(), Passage.getHeight(), mat, true);
            }
            /**********************************************/
        } catch (IOException e) {
            e.printStackTrace();
        }
        SensorManaged = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        PInitBtn = (Button)findViewById(R.id.button);
        isPInit = false;
        LogSwitch = (Switch)findViewById(R.id.logswitch);
        LogFlag = false;
        MapManaged = new MapManagement(this);
        MapManaged.setmap(Input,Passage);

        isHaveStartPosition = false;
        MapImage = (ImageView)findViewById(R.id.map);
        this.ShowPosition = (DisplayPosition)findViewById(R.id.position);
        SensorInput = new ManageSensorSystem();
        StepCount = 0;
        initializePastSensor();
    }

    protected void initializePastSensor() {
        PastSensorSystem.AccelerateGCS_ = new float[StepEventDetection.LASTACC_STACK_NUM][3];
        PastSensorSystem.MagneticGCS_ = new float[StepEventDetection.LASTACC_STACK_NUM][3];
        PastSensorSystem.AngularRateGCS_ = new float[StepEventDetection.LASTACC_STACK_NUM][3];
        PastSensorSystem.StoreNum_ = 0;
        for (int i = 0; i < StepEventDetection.LASTACC_STACK_NUM; i++) {
            for (int j = 0; j < 3; j++) {
                PastSensorSystem.AccelerateGCS_[i][j] = 0.00f;
                PastSensorSystem.MagneticGCS_[i][j] = 0.00f;
                PastSensorSystem.AngularRateGCS_[i][j] = 0.00f;
            }
        }
    }

    @Override
    protected void onStart(){
        super.onStart();
        mapDisplaySetup();
    }
    protected void mapDisplaySetup(){MapImage.setImageBitmap(MapManaged.MapImageName);}

    @Override
    protected void onResume(){
        super.onResume();
        SensorManaged.registerListener(this, SensorManaged.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManaged.SENSOR_DELAY_UI);
        SensorManaged.registerListener(this, SensorManaged.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManaged.SENSOR_DELAY_UI);
        SensorManaged.registerListener(this, SensorManaged.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManaged.SENSOR_DELAY_UI);

        MapManaged.initializePositionInfo();

        PInitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initBtnPushProcess();
            }
        });
        LogSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isPInit) LogSwitch.setChecked(LogFlag);
                else LogFlag = !LogFlag;
            }
        });
        LastTime_ms = System.currentTimeMillis();
    }

    protected void initBtnPushProcess(){
        if(isPInit){ // End of particle initialize

            PInitBtn.setText("Start");

            if(LogFlag) {
                writeLogToLocalFile(getNowDate(),LOCAL_FILE);
                writeLogToLocalFile(String.valueOf(TotalStep) + "\t" + String.valueOf(TotalMovex) + "\t" + String.valueOf(TotalMovey),LOCAL_FILE);
            }

            // Display information of particle initialize (not helpful)
            TextView t1 = (TextView) findViewById(R.id.t_d1);
            TextView t2 = (TextView) findViewById(R.id.t_d2);
            TextView t3 = (TextView) findViewById(R.id.t_d3);
            t1.setText(String.valueOf(TotalStep));
            MapManaged.RealLengthBetweenClicks = (float) Math.sqrt(Math.pow(TotalMovex, 2) + Math.pow(TotalMovey, 2));
            t2.setText(String.valueOf(MapManaged.RealLengthBetweenClicks));
            MapManaged.RealThetaBetweenClicks = (float) Math.atan2(TotalMovey, TotalMovex);
            t3.setText(String.valueOf(MapManaged.RealThetaBetweenClicks));

            isHaveStartPosition = false;
            StepCount = 0; MoveLength = 0.0f;

        }else{ // Start of particle initialize

            PInitBtn.setText("Stop");
            if(LogFlag) writeLogToLocalFile("\n",LOCAL_FILE);
            TotalStep = 0; TotalMovex = 0.0f; TotalMovey = 0.0f;
            ShowPosition.initialize();

        }
        isPInit = !isPInit;
    }


    @Override
    protected void onPause(){
        super.onPause();
        if(SensorManaged != null) SensorManaged.unregisterListener(this);
    }

    float last_StepDirection=0;
    @Override
    public void onSensorChanged(SensorEvent event){
        //センサーが変化したときの処理
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {//加速度センサの値
            SensorInput.AccelerateLCS = Arrays.copyOf(event.values, 3);
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {//磁気センサの値
            SensorInput.MagneticLCS = Arrays.copyOf(event.values, 3);
        }
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {//ジャイロセンサの値
            SensorInput.AngularRateLCS = Arrays.copyOf(event.values, 3);
        }

        if(LastAcceleration != SensorInput.AccelerateLCS[2]) {

            long nowtime = System.currentTimeMillis();
            PastSensorSystem.StepTime_ = (int)( nowtime - LastTime_ms);
            LastTime_ms = nowtime;

            SensorInput.calcSensorInGCS();

            if(isPInit){
                if (SensorInput.isStepTime()){
                    TotalStep++;
                    TotalMovex += SensorInput.StepLength * (float) Math.cos(SensorInput.StepDirection);
                    TotalMovey += SensorInput.StepLength * (float) Math.sin(SensorInput.StepDirection);
                }
            }else{
                if (SensorInput.isStepTime()) {

                    StepCount++;
                    MoveLength += SensorInput.StepLength;
                    TotalStep++;
                    TotalMovex += SensorInput.StepLength * (float) Math.cos(SensorInput.StepDirection);
                    TotalMovey += SensorInput.StepLength * (float) Math.sin(SensorInput.StepDirection);

                    //Log.d("step", "l = " + SensorInput.StepLength + ", h = " + SensorInput.StepDirection);

                    /******************20181018*******turn detectionしようとした*/
                    /*
                    //Log.d("step", "l = " + last_StepDirection + ", h = " + SensorInput.StepDirection);
                    float Direction_change =SensorInput.StepDirection - last_StepDirection;
                    if (LogFlag){
                        writeLogToLocalFile(getNowDate(), "direction.txt");
                        writeLogToLocalFile(String.valueOf(StepCount)+"\t"+String.valueOf(last_StepDirection) + "\t"
                                + String.valueOf(SensorInput.StepDirection) +"\t" + Direction_change,"direction.txt") ;
                    }
                    last_StepDirection = SensorInput.StepDirection;
                    Log.d("change","directionchange : " + Direction_change);
                    /****************/


                    if (MapManaged.isParticleInitialized) {

                        MapManaged.updateParticles(SensorInput.StepLength, SensorInput.StepDirection);

                        // Pick up randomly and show 50 particles
                        ShowPosition.DisplayedParticleList.clear();
                        Random r = new Random(System.currentTimeMillis());
                        for (int i = 0; i < 50; i++) {
                            PointF p = MapManaged.ParticleList.get(r.nextInt(2000)).position;
                            ShowPosition.DisplayedParticleList.add(new PointF(p.x - (float) MapManaged.MapImageViewRectGlobal.left, p.y - (float) MapManaged.MapImageViewRectGlobal.top));
                        }
                        ShowPosition.updatePositionList(MapManaged.NowPosition);
                        ShowPosition.updatePositionDisplay();

                        if(LogFlag) writeLogToLocalFile(String.valueOf(StepCount) + "\t" + String.valueOf(SensorInput.StepLength) + "\t" + String.valueOf(SensorInput.StepDirection),LOCAL_FILE);
                        if (LogFlag)writeLogToLocalFile(String.valueOf(StepCount) + "\t"+ String.valueOf(MapManaged.getMpp()),"mpp.txt");//20181016 update
                    }
                }

                TextView t1 = (TextView) findViewById(R.id.t_u1); t1.setText(String.valueOf(StepCount));
                TextView t2 = (TextView) findViewById(R.id.t_u2); t2.setText(String.valueOf(MoveLength));
                TextView t3 = (TextView) findViewById(R.id.t_u3); t3.setText(String.valueOf(SensorInput.StepDirection));

            }
            LastAcceleration = SensorInput.AccelerateLCS[2];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor event, int accuracy){}

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Display display = getWindowManager().getDefaultDisplay();
        MapManaged.setMapSize();
        MapManaged.setDisplaySize(display);
        GetDisplayedImageSize();
        MapImage.getGlobalVisibleRect(MapManaged.MapImageViewRectGlobal);
        MapManaged.calcMapRectGlobal();
    }

    private void GetDisplayedImageSize(){
        float scaleX = (float) MapImage.getWidth() / (float) MapManaged.MapSize.x;
        float scaleY = (float) MapImage.getHeight() / (float) MapManaged.MapSize.y;
        float scale = Math.min(scaleX, scaleY);
        float width = scale * MapManaged.MapSize.x;
        float height = scale * MapManaged.MapSize.y;
        MapManaged.MapSizeDisplayed.set((int) width, (int) height);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if((event.getAction() == MotionEvent.ACTION_UP) && ShowPosition.TapedPosition2.x != -1) { /* Calibration */

            PointF f = new PointF(); f.set(event.getX(),event.getY());
            PointF f2 = MapManaged.transformViewToImage(f);
            MapManaged.TapedPosition1_view.x = MapManaged.TapedPosition2_view.x; MapManaged.TapedPosition1_view.y = MapManaged.TapedPosition2_view.y;
            MapManaged.TapedPosition2_view.x = f.x; MapManaged.TapedPosition2_view.y = f.y;

            MapManaged.RealLengthBetweenClicks = (float) Math.sqrt(Math.pow(TotalMovex, 2) + Math.pow(TotalMovey, 2));
            MapManaged.RealThetaBetweenClicks = (float) Math.atan2(TotalMovey, TotalMovex);
            MapManaged.initializeParticles();

            TotalStep = 0; TotalMovex = 0.0f; TotalMovey = 0.0f;

            if(LogFlag) writeLogToLocalFile( "-1\t" + String.valueOf(f2.x/2) + "\t" + String.valueOf(f2.y/2),LOCAL_FILE);
            // why f2.x/2  "/2" ?

        }

        if((event.getAction() == MotionEvent.ACTION_UP) && isPInit){

            if(isHaveStartPosition){
                MapManaged.TapedPosition2_view.x = event.getX();
                MapManaged.TapedPosition2_view.y = event.getY();
                initBtnPushProcess();
                ShowPosition.TapedPosition2.x = (int) MapManaged.TapedPosition2_view.x- MapManaged.MapImageViewRectGlobal.left;
                ShowPosition.TapedPosition2.y = (int) MapManaged.TapedPosition2_view.y- MapManaged.MapImageViewRectGlobal.top;
                MapManaged.initializeParticles();

                /***changed mpp.txt mpp log **/
                System.out.println(MapManaged.getMpp());
                if(LogFlag) {
                    writeLogToLocalFile("\n"+getNowDate(), "mpp.txt");
                    writeLogToLocalFile(String.valueOf(MapManaged.getMpp()) , "mpp.txt");
                }
                /****/

                // Pick up randomly and show 50 particles
                ShowPosition.DisplayedParticleList.clear();
                Random r = new Random(System.currentTimeMillis());
                for (int i = 0; i < 50; i++) {
                    PointF p = MapManaged.ParticleList.get(r.nextInt(500)).position;
                    ShowPosition.DisplayedParticleList.add(new PointF(p.x - (float) MapManaged.MapImageViewRectGlobal.left, p.y - (float) MapManaged.MapImageViewRectGlobal.top));
                }
                ShowPosition.updatePositionList(MapManaged.NowPosition);
                ShowPosition.updatePositionDisplay();

            }else{ // Beginning of particle initialization

                MapManaged.TapedPosition1_view.x = event.getX();
                MapManaged.TapedPosition1_view.y = event.getY();
                isHaveStartPosition = true;
                ShowPosition.TapedPosition1.x = (int) MapManaged.TapedPosition1_view.x- MapManaged.MapImageViewRectGlobal.left;
                ShowPosition.TapedPosition1.y = (int) MapManaged.TapedPosition1_view.y- MapManaged.MapImageViewRectGlobal.top;
                ShowPosition.updatePositionDisplay();

            }
        }

        return true;
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/

    // from http://qiita.com/zuccyi/items/d9c185588a5628837137
    private static String getNowDate(){
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        return df.format(date);
    }

    // from http://qiita.com/tyfkda/items/7c07594ee6a7bc56dbc3

    /**
     *  When you want to obtain data of step events, you write the below commands in "Terminal"
     *
     *      adb shell
     *      run-as com.honto.navigation_c_win
     *      cat files/log.txt
     *
     *  The terminal cannot show too long data (I think it can show about 1200~1300 lines at most)
     */
    private void writeLogToLocalFile(String message,String file) {
        try {
            OutputStream out = openFileOutput(file, MODE_APPEND);
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(out,"UTF-8"));
            writer.append(message + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
