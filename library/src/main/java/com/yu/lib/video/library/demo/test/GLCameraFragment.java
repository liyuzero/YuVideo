package com.yu.lib.video.library.demo.test;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.fragment.app.Fragment;

import com.yu.lib.video.library.R;
import com.yu.lib.video.library.utils.VLog;
import com.yu.lib.video.library.camera.OnCameraPreviewCallBack;
import com.yu.lib.video.library.camera.YuCameraManager;
import com.yu.lib.video.library.gl.BeautyParam;
import com.yu.lib.video.library.gl.OnYuVideoListener;
import com.yu.lib.video.library.YuVideoManager;
import com.yu.lib.video.library.gl.render.YuVideoParam;

import org.jetbrains.annotations.NotNull;

public class GLCameraFragment extends Fragment {
    private ContentLoadingProgressBar mProgressBar;
    private YuVideoManager mVideoManager;
    private RecordButton mRecordButton;
    //视频录制配置参数
    private YuVideoParam mYuVideoParam;
    private SurfaceTexture mSurfaceTexture;

    private boolean mIsInit;

    @Nullable
    @Override
    @SuppressLint("InflateParams")
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.yu_video_fragment_gl_camera, null);
        loadView(view);
        return view;
    }

    private void loadView(@NotNull View view) {
        mVideoManager = new YuVideoManager(view.getContext(), true);
        mProgressBar = view.findViewById(R.id.progress);
        mProgressBar.show();
        TextureView textureView = view.findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(mOnTextureListener);

        FilterTestView testView = view.findViewById(R.id.filerTestView);
        testView.init(mVideoManager);

        mRecordButton = view.findViewById(R.id.recordButton);
        mRecordButton.setEnabled(false);
        mRecordButton.setOnRecordStateChangedListener(mOnRecordStateListener);

        final Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed()) {
            return;
        }

        YuVideoParam param = mYuVideoParam = new YuVideoParam();
        //美颜参数
        param.setBeautyParam(new BeautyParam());
        //录制参数
        YuVideoParam.RecordParam recordParam = new YuVideoParam.RecordParam(new YuVideoParam.RecordParam.VideoParam(),
                new YuVideoParam.RecordParam.AudioParam());
        recordParam.setSegmentRecord(true, view.getContext().getCacheDir().getAbsolutePath() + "/YuRecord"); //分段录制开启
        recordParam.setOutputFilePath(Environment.getExternalStorageDirectory().getAbsolutePath() + "/temp.mp4");
        recordParam.getVideoParam().setAsync(false);
        param.setRecordParam(recordParam);

        YuCameraManager.Companion.getInstance().setPreviewCallBack(new OnCameraPreviewCallBack() {
            @Override
            public void releaseComplete() {
                //渲染、录制线程 和 相机线程不在一块，如果openGL先release，会导致texture被回收，无效化，
                // 这时候会报：BufferQueue has been abandoned，(测试不会引起崩溃)，所以openGL改为在此处释放
                if (mVideoManager != null) {
                    mVideoManager.release();
                }
            }

            @Override
            public void onPreviewSuggestSizeCallBack(int width, int height) {
                if (mVideoManager != null) {
                    mVideoManager.onVideoSizeChanged(width, height);
                }
            }

            @Override
            public void onPreviewFrame(@NotNull byte[] data, @NotNull Camera camera, int width, int height) {
                //此处返回的是原始相机数据，所以不能取此处数据进行录制等操作
            }
        });

        View sureView = view.findViewById(R.id.sure);
        sureView.setVisibility(View.VISIBLE);
        sureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //分段录制结束
                mVideoManager.stopRecord();
            }
        });
    }

    private final TextureView.SurfaceTextureListener mOnTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mSurfaceTexture = surface;
            mVideoManager.onSurfaceChanged(width, height);

            startPreView();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            mSurfaceTexture = surface;

            mVideoManager.onSurfaceChanged(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private void startPreView() {
        if(mIsInit || mSurfaceTexture == null) {
            return;
        }
        mIsInit = true;
        mVideoManager.init(mYuVideoParam, mSurfaceTexture, YuVideoManager.SCALE_TYPE_CENTER_CROP,
                new OnYuVideoListener() {

                    @Override
                    public void onRenderTextureInitComplete(boolean isInitSuccess) {
                        if(getActivity() == null) {
                            return;
                        }
                        if(isInitSuccess) {
                            //mVideoManager.getRenderSurfaceTexture()为阻塞方法, 不过此处回调时已经解了
                            YuCameraManager.Companion.getInstance().openCamera(getActivity(),
                                    mVideoManager.getRenderSurfaceTexture(), false);
                            YuCameraManager.Companion.getInstance().startPreView();
                            mRecordButton.setEnabled(true);
                        } else {
                            if(getContext() != null) {
                                Toast.makeText(getContext(), "初始化失败", Toast.LENGTH_SHORT).show();
                            }
                            mProgressBar.post(new Runnable() {
                                @Override
                                public void run() {
                                    mProgressBar.hide();
                                }
                            });
                        }
                    }

                    @Override
                    public void onRenderStart() {
                        mProgressBar.post(new Runnable() {
                            @Override
                            public void run() {
                                mProgressBar.hide();
                            }
                        });
                    }

                    @Override
                    public void onRecordSegmentFinish(int segmentIndex, long segmentDuration, long curTotalDuration) {
                        VLog.d("hehe", "onRecordSegmentFinish：" + segmentIndex + "  ==  " + curTotalDuration);
                        Toast.makeText(getContext(), "录制小段完成 " + segmentIndex, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onRecordFinish(String outputFilePath, long duration) {
                        VLog.d("hehe", "onRecordFinish：" + duration);
                        Toast.makeText(getContext(), "录制全部完成："+ outputFilePath + " \n 时长" + duration, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onRecordFail() {
                        VLog.d("hehe", "onRecordFinishFail：fail");
                        Toast.makeText(getContext(), "录制失败！！", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onRecordDeleteSegment(boolean isSuccess, int index, int count) {

                    }

                    @Override
                    public void onRecordCallBack(int segmentIndex, long curDuration) {
                        VLog.d("hehe", "onRecordCallBack：" + curDuration);
                    }
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //退出页面后，就不应该占用Camera了
        YuCameraManager.Companion.getInstance().release();
    }

    /*-------------------------------------------- 录制部分 --------------------------------------------*/

    private final RecordButton.OnRecordStateChangedListener mOnRecordStateListener = new RecordButton.OnRecordStateChangedListener() {
        long startTime;

        @Override
        public boolean onRecordStart() {
            startTime = System.currentTimeMillis();
            mVideoManager.startRecord();
            return true;
        }

        @Override
        public void onRecordStop() {
            if(getContext() != null) {
                VLog.d("hehe", "按下录制时间：" + (System.currentTimeMillis() - startTime));
            }
            mVideoManager.stopSegmentRecord();
        }

        @Override
        public void onZoom(float percentage) {

        }
    };
}
