package com.acase.pcj.screenrecorddemo;import android.Manifest;import android.app.Activity;import android.content.Context;import android.content.Intent;import android.content.pm.PackageManager;import android.hardware.Camera;import android.media.projection.MediaProjection;import android.media.projection.MediaProjectionManager;import android.os.Build;import android.os.Bundle;import android.os.Environment;import android.support.annotation.RequiresApi;import android.util.DisplayMetrics;import android.view.SurfaceHolder;import android.view.SurfaceView;import android.view.View;import android.widget.ImageView;import android.widget.TextView;import android.widget.Toast;import java.io.File;import java.util.ArrayList;import java.util.List;@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)public class MainActivity extends Activity implements View.OnClickListener {    private SurfaceView mCamareView;    private SurfaceHolder mSurfaceHolder;    private Camera mCamera;    private final static int CAMERA_ID = 0;    private TextView mTime;    private ImageView mRecordBtn;    private boolean mIsRecording = false;    private boolean mIsSufaceCreated = false;    // 权限请求相关    private static final String[] ALL_PERMISSIONS = new String[]{            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA,            Manifest.permission.RECORD_AUDIO,   Manifest.permission.READ_EXTERNAL_STORAGE};    private static final int REQUEST_CODE_ASK_ALL_PERMISSIONS = 154;    private boolean mIsDenyAllPermission = false;    //  屏幕录制相关    private MediaProjection mMediaProject;    private MediaProjectionManager mProjectManager;    private static final int REQUEST_CODE = 10086;    private ScreenRecordManager recordVideo;    private int mScreenDensity;    @Override    protected void onCreate(Bundle savedInstanceState) {        super.onCreate(savedInstanceState);        setContentView(R.layout.activity_main);        initView();    }    @Override    protected void onResume() {        super.onResume();        // 请求权限        requestAllPermissions(REQUEST_CODE_ASK_ALL_PERMISSIONS);        startCamare();    }    @Override    protected void onPause() {        super.onPause();        if (mIsRecording) {            stopRecording();        }        if (recordVideo != null) {            recordVideo = null;        }        // 释放相机资源        releaseCamare();    }    private void initView() {        mCamareView = findViewById(R.id.camera_preview);        mTime = findViewById(R.id.timestamp);        mRecordBtn = findViewById(R.id.record_shutter);        mRecordBtn.setOnClickListener(this);        mSurfaceHolder = mCamareView.getHolder();        mSurfaceHolder.addCallback(mSurfaceCallback);        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);    }    private void startCreenManager() {        // 获取服务        mProjectManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);        Intent captureIntent = mProjectManager.createScreenCaptureIntent();        startActivityForResult(captureIntent, REQUEST_CODE);    }    private void startCamare() {        // 保证只有一个Camera对象        if (mCamera != null || !mIsSufaceCreated) {            return;        }        mCamera = Camera.open(CAMERA_ID);        Camera.Parameters parameters = mCamera.getParameters();        Camera.Size size = getBestPreviewSize(1280, 720, parameters);        if (size != null) {            parameters.setPreviewSize(size.width, size.height);        }        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);        parameters.setPreviewFrameRate(20);        //设置相机预览方向        mCamera.setDisplayOrientation(90);        mCamera.setParameters(parameters);        try {            mCamera.setPreviewDisplay(mSurfaceHolder);        } catch (Exception e) {            e.printStackTrace();        }        mCamera.startPreview();    }    private SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {        @Override        public void surfaceDestroyed(SurfaceHolder holder) {            mIsSufaceCreated = false;        }        @Override        public void surfaceCreated(SurfaceHolder holder) {            mIsSufaceCreated = true;        }        @Override        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {            startCamare();        }    };    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {        Camera.Size result = null;        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {            if (size.width <= width && size.height <= height) {                if (result == null) {                    result = size;                } else {                    int resultArea = result.width * result.height;                    int newArea = size.width * size.height;                    if (newArea > resultArea) {                        result = size;                    }                }            }        }        return result;    }    private void releaseCamare() {        //释放Camera对象        if (mCamera != null) {            try {                mCamera.setPreviewDisplay(null);            } catch (Exception e) {                e.printStackTrace();            }            mCamera.stopPreview();            mCamera.release();            mCamera = null;        }    }    @Override    public void onClick(View v) {        if (v.getId() == mRecordBtn.getId()) {            if (mIsRecording) {                stopRecording();            } else              //  startCreenManager();            startRecording();        }    }    // 停止录制    private void stopRecording() {        mIsRecording = false;        mRecordBtn.setImageDrawable(getResources().getDrawable(R.mipmap.recording_shutter));        recordVideo.stopRecording();    }    // 开始录制    private void startRecording() {        mIsRecording = true;        mRecordBtn.setImageDrawable(getResources().getDrawable(R.mipmap.recording_shutter_hl));        recordVideo = ScreenRecordManager.getInstance();        getScreenBaseInfo();        recordVideo.setScreenDensity(mScreenDensity);        recordVideo.initMediaRecorder(mCamera, getCachePath(), 1280, 720, mMediaProject);    }    /**     * 获取屏幕相关数据     */    private void getScreenBaseInfo() {        DisplayMetrics metrics = new DisplayMetrics();        getWindowManager().getDefaultDisplay().getMetrics(metrics);        mScreenDensity = metrics.densityDpi;    }    // 创建文件夹    public static String getCachePath() {        String path = null;        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {            path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator                    + "AR/Camera";        }        File dir = new File(path);        if (!dir.exists()) {            dir.mkdirs();        }        return path + File.separator + "VID_" + System.currentTimeMillis() + ".mp4";    }    /**     * 权限请求相关     *     * @param requestCode     * @param permissions     * @param grantResults     */    @Override    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {        if (requestCode == REQUEST_CODE_ASK_ALL_PERMISSIONS) {            mIsDenyAllPermission = false;            for (int i = 0; i < permissions.length; i++) {                if (i >= grantResults.length || grantResults[i] == PackageManager.PERMISSION_DENIED) {                    mIsDenyAllPermission = true;                    break;                }            }            if (mIsDenyAllPermission) {                Toast.makeText(MainActivity.this, "缺少权限，请检查！", Toast.LENGTH_SHORT).show();                finish();            }        }    }    /**     * 请求权限     *     * @param requestCode     */    private void requestAllPermissions(int requestCode) {        try {            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {                List<String> permissionsList = getRequestPermissions(this);                if (permissionsList.size() == 0) {                    return;                }                if (!mIsDenyAllPermission) {                    requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),                            requestCode);                }            }        } catch (Exception e) {            e.printStackTrace();        }    }    private static List<String> getRequestPermissions(Activity activity) {        List<String> permissionsList = new ArrayList();        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {            for (String permission : ALL_PERMISSIONS) {                if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {                    permissionsList.add(permission);                }            }        }        return permissionsList;    }    @Override    protected void onActivityResult(int requestCode, int resultCode, Intent data) {        super.onActivityResult(requestCode, resultCode, data);        if (requestCode == REQUEST_CODE) {            mMediaProject = mProjectManager.getMediaProjection(resultCode, data);            Toast.makeText(this, "请求屏幕录制权限成功开始录制", Toast.LENGTH_SHORT).show();            startRecording();        }    }}