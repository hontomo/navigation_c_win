package com.honto.navigation_c_win;

public class HeadingDirection {
    final int WEIGHT_P = 2, WEIGHT_M = 1, WEIGHT_G = 2;
    final float H_COL = (float)(5.0 * Math.PI / 180.0) , H_MAG = (float)(2.0 * Math.PI / 180.0);
    final float MAGNETIC_VARIATION = 0.0f;

    public float[] LastDirection;
    private float LastH;
    private float HMagnet, HGyro;
    private float HMagnet_, HGyro_;
    private float h_t_ =0;

    HeadingDirection() {
        LastDirection = new float[StepEventDetection.LASTACC_STACK_NUM];
        for(int i = 0; i < StepEventDetection.LASTACC_STACK_NUM; i++){
            LastDirection[i] = 0.0f;
        }
        HMagnet = 0.0f;
        HGyro = -100.0f;
    }

    public void estimateDirection() {
        calcHMagnet();
        calcHGyro();
        // Log.d("h", "mag = " + HMagnet + ", gyro = " + HGyro);
        LastDirection[PastSensorSystem.StoreNum_] = decideDirection();
    }

    private void calcHMagnet() { // BUG: Magnetic in GCS cannot be obtained correctly
        HMagnet = (float) Math.atan2(-PastSensorSystem.MagneticGCS_[PastSensorSystem.StoreNum_][2], PastSensorSystem.MagneticGCS_[PastSensorSystem.StoreNum_][1]);
        //if(PastSensorSystem.StoreNum_!=0)
        // HMagnet_ = (float) Math.atan2(-PastSensorSystem.MagneticGCS_[PastSensorSystem.StoreNum_-1][2], PastSensorSystem.MagneticGCS_[PastSensorSystem.StoreNum_-1][1]);
        //論文(10) (h_decline)
    }

    private void calcHGyro() {
        float[] corrected_velo = new float[3];
        for (int i = 0; i < 3; i++)
            corrected_velo[i] = PastSensorSystem.AngularRateGCS_[PastSensorSystem.StoreNum_][i] - 0;
        float[] gravity_vector = new float[3];
        for (int i = 0; i < 3; i++)
            gravity_vector[i] = ManageSensorSystem.RotationMat_[6 + i] * StepEventDetection.LastGravity;
        float velo_gcs = corrected_velo[0] * gravity_vector[0] + corrected_velo[1] * gravity_vector[1] + corrected_velo[2] * gravity_vector[2];
        velo_gcs = velo_gcs / (float) Math.sqrt(Math.pow(gravity_vector[0], 2) + Math.pow(gravity_vector[1], 2) + Math.pow(gravity_vector[2], 2));//論文(14)

        if (HGyro == -100.0f) {
            if(HMagnet != -0.0f) {
                HGyro = HMagnet;
            }
        }else{
            HGyro -= velo_gcs * (float)(PastSensorSystem.StepTime_)/1000;
        }//論文(16)
    }

    private float decideDirection() {
        // In the paper of SmartPDR, heading direction is computed with gyro and ""magnetometer""
        //実装できなかったやつ
       /* float h_cor = Math.abs(HMagnet-HGyro);
        float h_mag = Math.abs(HMagnet-HMagnet_);
        int h_cor_t = 5,h_mag_t=2;
        float h_t=0;
        if(h_cor >h_cor_t){
            if(h_mag>h_mag_t){
                h_t = (h_t_+HGyro)/2;
                //System.out.println("h_t,h_t-1:"+h_t+","+h_t_);
                h_t_ = h_t;
                return h_t;
            }
            else{
                h_t = h_t_;
                //System.out.println("h_t,h_t-1:"+h_t+","+h_t_);
                h_t_ = h_t;
                return h_t;
            }
        }
        else{
            if(h_mag>h_mag_t){
               h_t = (HMagnet+2*HGyro)/3;
                //System.out.println("h_t,h_t-1:"+h_t+","+h_t_);
                h_t_=h_t;
                return h_t;
            }
            else{
                h_t = (2*h_t_+HMagnet+2*HGyro)/5;
                //System.out.println("h_t,h_t-1:"+h_t+","+h_t_);
                h_t_=h_t;
                return h_t;
            }
        }*/
        return HGyro;//(2*HGyro+1*HMagnet)/3;//HGyro
    }

    public void updateDirectionListTrue() {
        int j = PastSensorSystem.StoreNum_ - StepEventDetection.LPF_PAR_ - 2* StepEventDetection.COMPARISON_SAMPLE_NUM_;
        for(int i = 0; i < StepEventDetection.LASTACC_STACK_NUM; i++){
            if(i <= StepEventDetection.LPF_PAR_ + 2* StepEventDetection.COMPARISON_SAMPLE_NUM_){
                LastDirection[i] = LastDirection[i+j];
            }
            else{
                LastDirection[i] = 0.0f;
            }
        }
    }

    public void updateDirectionListFalse() {
        if(PastSensorSystem.StoreNum_ == StepEventDetection.LASTACC_STACK_NUM-1){
            System.arraycopy(LastDirection, 1, LastDirection, 0, StepEventDetection.LASTACC_STACK_NUM - 1);
        }
    }

}
