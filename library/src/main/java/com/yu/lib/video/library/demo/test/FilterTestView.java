package com.yu.lib.video.library.demo.test;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yu.lib.video.library.R;
import com.yu.lib.video.library.gl.BeautyParam;
import com.yu.lib.video.library.YuVideoManager;
import com.yu.lib.video.library.gl.filter.beauty.TestFilter;

public class FilterTestView extends FrameLayout {

    public FilterTestView(@NonNull Context context) {
        super(context);
        init();
    }

    public FilterTestView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.yu_video_view_fiter_test, this);
    }

    public void init(final YuVideoManager render) {
        ((CheckBox) findViewById(R.id.f1)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                render.getFilterApi().setBeautyParam(new BeautyParam().setGaussLevel(isChecked? 1f: 0f));
            }
        });
        ((CheckBox) findViewById(R.id.f2)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                render.getFilterApi().setBeautyParam(new BeautyParam().setComplexionLevel(isChecked? 1f: 0f));
            }
        });
        ((CheckBox) findViewById(R.id.f3)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                render.getFilterApi().setSingleFilter(isChecked? new TestFilter(buttonView.getContext()): null);
            }
        });
    }
}
