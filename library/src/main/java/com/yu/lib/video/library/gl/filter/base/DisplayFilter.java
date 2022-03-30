package com.yu.lib.video.library.gl.filter.base;

public class DisplayFilter extends BaseFilter {

    public DisplayFilter() {
        super(ShaderData.DISPLAY_VERTEX_SHADER, ShaderData.DISPLAY_FRAGMENT_SHADER);
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
