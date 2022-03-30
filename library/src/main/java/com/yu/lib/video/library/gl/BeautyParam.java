package com.yu.lib.video.library.gl;

public class BeautyParam {
    private float gaussLevel = 0.5f; // 0-1
    private float complexionLevel = 0.5f;  // 0-1

    public BeautyParam setGaussLevel(float gaussLevel) {
        this.gaussLevel = gaussLevel;
        return this;
    }

    public BeautyParam setComplexionLevel(float complexionLevel) {
        this.complexionLevel = complexionLevel;
        return this;
    }

    public float getGaussLevel() {
        return gaussLevel;
    }

    public float getComplexionLevel() {
        return complexionLevel;
    }
}
