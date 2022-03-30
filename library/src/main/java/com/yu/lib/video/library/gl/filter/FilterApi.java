package com.yu.lib.video.library.gl.filter;

import com.yu.lib.video.library.gl.BeautyParam;
import com.yu.lib.video.library.gl.filter.base.BaseFilter;

public interface FilterApi {
    void setBeautyParam(BeautyParam beautyParam);
    void addFilter(BaseFilter filter);
    void setSingleFilter(BaseFilter filter);
}
