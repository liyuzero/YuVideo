package com.yu.lib.video.library.gl.filter.beauty;

import android.content.Context;

import com.yu.lib.video.library.R;
import com.yu.lib.video.library.gl.filter.base.BaseFilter;

//自测用
public class TestFilter extends BaseFilter {

    public TestFilter(Context context) {
        super(context, R.raw.fragment_test);
    }

    @Override
    public boolean setupProgram(int programId) {
        return true;
    }

    @Override
    public void onDrawBegin() {
    }

    @Override
    public void onDrawEnd() {

    }
}
