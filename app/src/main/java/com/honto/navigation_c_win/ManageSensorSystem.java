package com.honto.navigation_c_win;

import android.hardware.SensorManager;

public class ManageSensorSystem {
    public float[] AccelerateLCS;
    public float[] MagneticLCS;
    public float[] AngularRateLCS;
    static public float[] RotationMat_;

    private StepEventDetection Step;
    private HeadingDirection Head;
    public float StepLength, StepDirection;

    ManageSensorSystem() {
        AccelerateLCS = new float[3];
        MagneticLCS = new float[3];
        AngularRateLCS = new float[3];
        RotationMat_ = new float[9];
        Step = new StepEventDetection();
        Head = new HeadingDirection();
        StepLength = 0;
    }

    public void calcSensorInGCS() {//グローバル軸への変換
        if(AccelerateLCS != null && MagneticLCS != null && AngularRateLCS != null) {
            float[] inR = new float[9];
            SensorManager.getRotationMatrix(inR, null, AccelerateLCS, MagneticLCS);
            SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Y, RotationMat_);
            //Log.d("m_local", "X = " + MagneticLCS[0] + ", Y = " + MagneticLCS[1] + ", Z = " + MagneticLCS[2]);
            float[] bbb = new float[3];
            float[] aaa = new float[3];
            bbb[0]=-(float)MagneticLCS[0];bbb[1]=(float)MagneticLCS[1];bbb[2]=(float)MagneticLCS[2];
            LSC2GCS(AccelerateLCS,PastSensorSystem.AccelerateGCS_[PastSensorSystem.StoreNum_]);
            LSC2GCS(MagneticLCS, PastSensorSystem.MagneticGCS_[PastSensorSystem.StoreNum_]);
            LSC2GCS(AngularRateLCS, PastSensorSystem.AngularRateGCS_[PastSensorSystem.StoreNum_]);
            //LSC2GCS(bbb, PastSensorSystem.MagneticGCS_[PastSensorSystem.StoreNum_]);
            // Log.d("m_global", "X = " + PastSensorSystem.MagneticGCS_[PastSensorSystem.StoreNum_][0] + ", Y = " + PastSensorSystem.MagneticGCS_[PastSensorSystem.StoreNum_][1] + ", Z = "
            //    + PastSensorSystem.MagneticGCS_[PastSensorSystem.StoreNum_][2]);

        }
    }

    private void LSC2GCS(float[] lcs_mat, float[] gcs_mat){
        if(RotationMat_ != null) {
            for (int i = 0; i < 3; i++) {
                gcs_mat[i] = RotationMat_[3 * i] * lcs_mat[0] + RotationMat_[3 * i + 1] * lcs_mat[1] + RotationMat_[3 * i + 2] * lcs_mat[2];
            }
        }
    }

    public boolean isStepTime(){
        boolean s = Step.judgeStep();//StepEventDetection.judgeStep
        Head.estimateDirection();//HeadDirection calcGyro
        if(s){
            int valley_num = Step.detectValley();
            StepLength = Step.stepLengthEstimation(valley_num);
            StepDirection = Head.LastDirection[valley_num];// * 180 / (float)Math.PI;
            updatePastSensorListTrue();
        }else{
            updatePastSensorListFalse();
        }
        return s;
    }

    private void updatePastSensorListTrue() {
        Step.updateAccelerateListTrue();
        Head.updateDirectionListTrue();
        int shift = PastSensorSystem.StoreNum_ - StepEventDetection.LPF_PAR_ - 2 * StepEventDetection.COMPARISON_SAMPLE_NUM_;
        for(int i = 0; i < StepEventDetection.LASTACC_STACK_NUM; i++){
            if(i <= StepEventDetection.LPF_PAR_ + 2* StepEventDetection.COMPARISON_SAMPLE_NUM_){
                PastSensorSystem.AccelerateGCS_[i][0] = PastSensorSystem.AccelerateGCS_[i+shift][0];
                PastSensorSystem.AccelerateGCS_[i][1] = PastSensorSystem.AccelerateGCS_[i+shift][1];
                PastSensorSystem.AccelerateGCS_[i][2] = PastSensorSystem.AccelerateGCS_[i+shift][2];
                PastSensorSystem.MagneticGCS_[i][0] = PastSensorSystem.MagneticGCS_[i+shift][0];
                PastSensorSystem.MagneticGCS_[i][1] = PastSensorSystem.MagneticGCS_[i+shift][1];
                PastSensorSystem.MagneticGCS_[i][2] = PastSensorSystem.MagneticGCS_[i+shift][2];
                PastSensorSystem.AngularRateGCS_[i][0] = PastSensorSystem.AngularRateGCS_[i+shift][0];
                PastSensorSystem.AngularRateGCS_[i][1] = PastSensorSystem.AngularRateGCS_[i+shift][1];
                PastSensorSystem.AngularRateGCS_[i][2] = PastSensorSystem.AngularRateGCS_[i+shift][2];
            }else{
                PastSensorSystem.AccelerateGCS_[i][0] = 0.0f;
                PastSensorSystem.AccelerateGCS_[i][1] = 0.0f;
                PastSensorSystem.AccelerateGCS_[i][2] = 0.0f;
                PastSensorSystem.MagneticGCS_[i][0] = 0.0f;
                PastSensorSystem.MagneticGCS_[i][1] = 0.0f;
                PastSensorSystem.MagneticGCS_[i][2] = 0.0f;
                PastSensorSystem.AngularRateGCS_[i][0] = 0.0f;
                PastSensorSystem.AngularRateGCS_[i][1] = 0.0f;
                PastSensorSystem.AngularRateGCS_[i][2] = 0.0f;
            }
        }
        PastSensorSystem.StoreNum_ = 2* StepEventDetection.COMPARISON_SAMPLE_NUM_ + StepEventDetection.LPF_PAR_ +1;
    }

    private void updatePastSensorListFalse() {
        Step.updateAccelerateListFalse();
        Head.updateDirectionListFalse();
        if(PastSensorSystem.StoreNum_ ==StepEventDetection.LASTACC_STACK_NUM-1){
            System.arraycopy(PastSensorSystem.AccelerateGCS_, 1, PastSensorSystem.AccelerateGCS_, 0, StepEventDetection.LASTACC_STACK_NUM - 1);
            System.arraycopy(PastSensorSystem.MagneticGCS_, 1, PastSensorSystem.MagneticGCS_, 0, StepEventDetection.LASTACC_STACK_NUM - 1);
            System.arraycopy(PastSensorSystem.AngularRateGCS_, 1, PastSensorSystem.AngularRateGCS_, 0, StepEventDetection.LASTACC_STACK_NUM - 1);
        }else{
            PastSensorSystem.StoreNum_++;
        }
    }

}
