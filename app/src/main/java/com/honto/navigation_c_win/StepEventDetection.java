package com.honto.navigation_c_win;

public class StepEventDetection {
    final float HPF_PAR = 0.95f;
    static final int LPF_PAR_ = 2;
    static final int COMPARISON_SAMPLE_NUM_ = 4; // COMPARISON_SAMPLE_NUM > LPF_PAR
    final float PEAK_THR = 0.3f;
    final float PP_THR = 0.6f;

    static public float LastGravity = 0.0f;
    public float[] LastAccelerationZ, LastAccelerationLPF;
    static final int LASTACC_STACK_NUM = 50 + COMPARISON_SAMPLE_NUM_;

    StepEventDetection() {
        LastAccelerationZ = new float[LASTACC_STACK_NUM];
        LastAccelerationLPF = new float[LASTACC_STACK_NUM];
        for(int i = 0; i < LASTACC_STACK_NUM; i++){
            LastAccelerationZ[i] = 0.0f;
            LastAccelerationLPF[i] = 0.0f;
        }
    }

    public boolean judgeStep() {

        LastAccelerationZ[PastSensorSystem.StoreNum_] = calcZAxisAcceleration(PastSensorSystem.AccelerateGCS_[PastSensorSystem.StoreNum_][2]);//論文(4)(5)
        if(PastSensorSystem.StoreNum_ >= LPF_PAR_) LastAccelerationLPF[PastSensorSystem.StoreNum_ - LPF_PAR_] = accelerationLowPassFilter();//論文(6) a_step

        if(PastSensorSystem.StoreNum_ >= 2* COMPARISON_SAMPLE_NUM_ + LPF_PAR_) {
            boolean s = stepTimeJudgement();
            /*if(!s){
                if(PastSensorSystem.StoreNum == LASTACC_STACK_NUM-1){
                    System.arraycopy(last_accelerationZ, 1, last_accelerationZ, 0, LASTACC_STACK_NUM-1);
                    System.arraycopy(last_accelerationLPF, 1, last_accelerationLPF, 0, LASTACC_STACK_NUM-1);
                }else{
                    PastSensorSystem.StoreNum++;
                }
            }*/
            return s;
        }else{
            PastSensorSystem.StoreNum_++;
            return false;
        }
    }

    private float calcZAxisAcceleration(float acc_z) {
        float g;
        if(LastGravity > 1.0f) {
            g = LastGravity * HPF_PAR + (1.00f - HPF_PAR) * acc_z;
        }else{
            g = acc_z;
        }
        LastGravity = g;
        return (acc_z - g);
    }

    private float accelerationLowPassFilter() {
        float a = 0.0f;
        int n = 0;
        for(int i = PastSensorSystem.StoreNum_ - 2 * LPF_PAR_; i <= PastSensorSystem.StoreNum_; i++){
            if(i >= 0){
                n++;
                a += LastAccelerationZ[i];
            }
        }
        return a/(float)n;
    }

    boolean stepTimeJudgement() {
        //t_peak,t_pp,t_slopeのjudge
        /** t_peak */
        if (LastAccelerationLPF[PastSensorSystem.StoreNum_ - COMPARISON_SAMPLE_NUM_ - LPF_PAR_] < PEAK_THR) return false;//0.3
        for (int i = PastSensorSystem.StoreNum_ - LPF_PAR_ - 2* COMPARISON_SAMPLE_NUM_; i <= PastSensorSystem.StoreNum_ - LPF_PAR_; i++) {
            if (i != PastSensorSystem.StoreNum_ - LPF_PAR_ - COMPARISON_SAMPLE_NUM_ && LastAccelerationLPF[PastSensorSystem.StoreNum_ - LPF_PAR_ - COMPARISON_SAMPLE_NUM_] < LastAccelerationLPF[i])
                return false;
        }//論文(7a)

        /** t_pp */
        float pp_before = 0.0f, pp_after = 0.0f;
        for (int i = 0; i < COMPARISON_SAMPLE_NUM_; i++) {
            if (pp_before < Math.abs(LastAccelerationLPF[PastSensorSystem.StoreNum_ - LPF_PAR_ - COMPARISON_SAMPLE_NUM_] - LastAccelerationLPF[PastSensorSystem.StoreNum_ - LPF_PAR_ - COMPARISON_SAMPLE_NUM_ - 1 - i]))
                pp_before = Math.abs(LastAccelerationLPF[PastSensorSystem.StoreNum_ - LPF_PAR_ - COMPARISON_SAMPLE_NUM_] - LastAccelerationLPF[PastSensorSystem.StoreNum_ - LPF_PAR_ - COMPARISON_SAMPLE_NUM_ - 1 - i]);
            if (pp_after < Math.abs(LastAccelerationLPF[PastSensorSystem.StoreNum_ - LPF_PAR_ - COMPARISON_SAMPLE_NUM_] - LastAccelerationLPF[PastSensorSystem.StoreNum_ - LPF_PAR_ - COMPARISON_SAMPLE_NUM_ + 1 + i]))
                pp_after = Math.abs(LastAccelerationLPF[PastSensorSystem.StoreNum_ - LPF_PAR_ - COMPARISON_SAMPLE_NUM_] - LastAccelerationLPF[PastSensorSystem.StoreNum_ - LPF_PAR_ - COMPARISON_SAMPLE_NUM_ + 1 + i]);
        }
        if (pp_before < PP_THR || pp_after < PP_THR) return false;//0.6
        //論文(7b)

        /** t_slope */
        float slope_before = 0.0f, slope_after = 0.0f;
        for (int i = 0; i < COMPARISON_SAMPLE_NUM_; i++) {
            slope_before += LastAccelerationLPF[PastSensorSystem.StoreNum_ - LPF_PAR_ - 2* COMPARISON_SAMPLE_NUM_ + i + 1] - LastAccelerationLPF[PastSensorSystem.StoreNum_ - LPF_PAR_ - 2* COMPARISON_SAMPLE_NUM_ + i];
            slope_after += LastAccelerationLPF[PastSensorSystem.StoreNum_ - LPF_PAR_ - COMPARISON_SAMPLE_NUM_ + i + 1] - LastAccelerationLPF[PastSensorSystem.StoreNum_ - LPF_PAR_ - COMPARISON_SAMPLE_NUM_ + i];
        }
        if (slope_after > 0 || slope_before < 0) return false;
        //論文(7c)
        return true;

    }

    public int detectValley() {
        int valley = COMPARISON_SAMPLE_NUM_;
        for(int i = COMPARISON_SAMPLE_NUM_ +1; i <= PastSensorSystem.StoreNum_; i++){
            if(LastAccelerationLPF[valley] > LastAccelerationLPF[i]) valley = i;
        }
        return valley;
    }

    public void updateAccelerateListTrue() {
        int j = PastSensorSystem.StoreNum_ - LPF_PAR_ - 2* COMPARISON_SAMPLE_NUM_;
        for(int i = 0; i < LASTACC_STACK_NUM; i++){
            if(i <= LPF_PAR_ + 2* COMPARISON_SAMPLE_NUM_){
                LastAccelerationLPF[i] = LastAccelerationLPF[i+j];
                LastAccelerationZ[i] = LastAccelerationZ[i+j];
            }
            else{
                LastAccelerationLPF[i] = 0.0f;
                LastAccelerationZ[i] = 0.0f;
            }
        }
        //PastSensorSystem.StoreNum = 2*COMPARISON_SAMPLE_NUM+LPF_PAR+1;
    }

    public void updateAccelerateListFalse() {
        if(PastSensorSystem.StoreNum_ == LASTACC_STACK_NUM-1){
            System.arraycopy(LastAccelerationZ, 1, LastAccelerationZ, 0, LASTACC_STACK_NUM - 1);
            System.arraycopy(LastAccelerationLPF, 1, LastAccelerationLPF, 0, LASTACC_STACK_NUM - 1);
        }
    }

    public float stepLengthEstimation(int valley){
        float a_pp = LastAccelerationLPF[PastSensorSystem.StoreNum_] - LastAccelerationLPF[valley];
        //if(LastAccelerationLPF[PastSensorSystem.StoreNum_] < 3.23f){
            return (float)(1.479 * Math.sqrt(Math.sqrt(a_pp)) - 1.259);
        //}else{
        //    return (float)(1.131 * Math.log(a_pp) + 0.159);
        //}
    }


}
