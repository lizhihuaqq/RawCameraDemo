package cc.uphoton.rawcamera.activity;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cc.uphoton.rawcamera.R;
import cc.uphoton.rawcamera.utils.Const;
import cc.uphoton.rawcamera.utils.Utils;

import static android.bluetooth.BluetoothHidDeviceAppQosSettings.MAX;
import static cc.uphoton.rawcamera.utils.Const.REF_RAW_PATH;
import static cc.uphoton.rawcamera.utils.Utils.getBytesByFile;

public class MainActivity extends Activity {
    static {
        System.loadLibrary("native-lib");
    }

    private static final SparseIntArray ORIENTATION = new SparseIntArray();

    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    private String mCameraIdIR;
    private Size mPreviewSizeIR;
    private Size mCaptureSizeIR;
    private HandlerThread mCameraThreadIR;
    private String mCameraIdRGB;
    private Size mPreviewSizeRGB;
    private Size mCaptureSizeRGB;
    private HandlerThread mCameraThreadRGB;
    private Handler mCameraHandlerIR;
    private Handler mCameraHandlerRGB;
    private CameraDevice mCameraDeviceIR;
    private CameraDevice mCameraDeviceRGB;
    private TextureView mTextureViewIR;
    private TextureView mTextureViewRGB;
    private ImageReader mImageReaderIR;
    private ImageReader mImageReaderRGB;
    private CaptureRequest.Builder mCaptureRequestBuilderIR;
    private CaptureRequest.Builder mCaptureRequestBuilderRGB;
    private CaptureRequest mCaptureRequestIR;
    private CaptureRequest mCaptureRequestRGB;

    private CameraCaptureSession mCameraCaptureSessionIR;
    private CameraCaptureSession mCameraCaptureSessionRGB;

    private Button mCollectBtn;
    private TextView mPicCountTextView;

    private RadioGroup mLightRadioGroup;
    private RadioButton mJGGRadioButton;
    private RadioButton mFGRadioButton;
    private EditText mValueEditText;
    private EditText mPWMEditText;
    private Button mLightSettingButton;
    private static SharedPreferences sp;


    private String jggValue;
    private String jggPWM;
    private String fgValue;
    private String fgPWM;
    private boolean isFirstRun;
    private String mISOValue;
    private String mExposureValue;
    private Button mSettingBtn;
    private static String timeStamp;
    private static ProgressDialog mProgressDialog = null;
    private long starttime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (new File(Const.REF_RAW_PATH).exists()) {
            new PreprocessTask().execute(REF_RAW_PATH);
        } else {
            Toast.makeText(MainActivity.this, "没有raw参考图", Toast.LENGTH_SHORT).show();
        }
        setContentView(R.layout.activity_main);
        if (new File(Const.YML_PATH).exists()) {
            PreprocessRemappedRGB2IR(Const.YML_PATH);
        }
        sp = getSharedPreferences("sp", MODE_PRIVATE);
        mSettingBtn = findViewById(R.id.setting);
        mSettingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intent);
                finish();
            }
        });
        mLightRadioGroup = findViewById(R.id.mLightRadioGroup);
        mJGGRadioButton = findViewById(R.id.rb_jgg);
        mFGRadioButton = findViewById(R.id.rb_fg);
        mValueEditText = findViewById(R.id.et_value);
        mPWMEditText = findViewById(R.id.et_pwm);
        mLightSettingButton = findViewById(R.id.btn_light_setting);
        mTextureViewIR = (TextureView) this.findViewById(R.id.textureviewIR);
        mTextureViewRGB = (TextureView) this.findViewById(R.id.textureviewRGB);
        mCollectBtn = findViewById(R.id.photoButton);
        mPicCountTextView = findViewById(R.id.tv_count);
        mCollectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              /*  mProgressDialog = ProgressDialog.show(MainActivity.this,
                        "", "正在采图并计算", true, true);
                picCount = 0;
                isClicked = 0;
                timeStamp = null;
                takePicture();*/
                if (!isStart) {
                    mCollectBtn.setText("点击可停止采集");
                    mPicCountTextView.setText("0");
                    Toast.makeText(MainActivity.this, "开始采集", Toast.LENGTH_SHORT).show();
                    isStart = true;
                    picCount = 0;
                    isClicked = 0;
                    timeStamp = null;
                    takePicture();
                } else {
                    isStart = false;
                    mCollectBtn.setText("点击开始采集");
                    Toast.makeText(MainActivity.this, "停止采集", Toast.LENGTH_SHORT).show();
                }
            }
        });
//        initData();
    }

    private static class PreprocessTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... files) {
            byte[] refBytes = getBytesByFile(files[0]);
            PreprocessRaw(refBytes);
            return null;
        }
    }

    private void initData() {
        new ConnectionThread("10400050").start();
        sp.edit().putBoolean(Const.JGG_BTN_IS_CHECKED, true).commit();
        sp.edit().putString(Const.FZ_DIANL, "400").commit();
        sp.edit().putString(Const.ZHAN_KONG_BI, "50").commit();
        if (sp.getBoolean(Const.JGG_BTN_IS_CHECKED, true)) {
            mJGGRadioButton.setChecked(true);
            mFGRadioButton.setChecked(false);
        } else {
            mJGGRadioButton.setChecked(false);
            mFGRadioButton.setChecked(true);
        }
        String Value = sp.getString(Const.FZ_DIANL, "0");
        String PWM = sp.getString(Const.ZHAN_KONG_BI, "0");
        mValueEditText.setText(Value);
        mPWMEditText.setText(PWM);

        mLightRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_jgg:
                        //泛光选中
                        mJGGRadioButton.setChecked(true);
                        mFGRadioButton.setChecked(false);
                        break;
                    case R.id.rb_fg:
                        //结构光选中
                        mJGGRadioButton.setChecked(false);
                        mFGRadioButton.setChecked(true);
                        break;
                }
            }
        });
        mLightSettingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = mValueEditText.getText().toString();
                String pwm = mPWMEditText.getText().toString();
                if (value == null || value.isEmpty()) {
                    value = "0";
                    mValueEditText.setText("0");
                }

                try {
                    if (Integer.parseInt(value) > 2500) {
                        value = "2500";
                        mValueEditText.setText("2500");
                        Toast.makeText(MainActivity.this, "最大峰值电流为2.5A", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "请输入数字", Toast.LENGTH_SHORT).show();
                }

                if (value.getBytes().length == 1) {
                    value = "00" + value;
                } else if (value.getBytes().length == 2) {
                    value = "0" + value;
                }

                if (pwm == null || pwm.isEmpty()) {
                    pwm = "0";
                    mPWMEditText.setText("0");
                }
                if (Integer.parseInt(pwm) > 100) {
                    pwm = "100";
                    mPWMEditText.setText("100");
                    Toast.makeText(MainActivity.this, "最大电流占空比为100", Toast.LENGTH_SHORT).show();
                }
                if (pwm.getBytes().length == 1) {
                    pwm = "00" + pwm;
                } else if (pwm.getBytes().length == 2) {
                    pwm = "0" + pwm;
                }
                String jggStr;
                if (mJGGRadioButton.isChecked()) {
                    jggStr = "1" + value + pwm + "0";
                } else {
                    jggStr = "2" + value + pwm + "0";
                }
                sp.edit().putString(Const.FZ_DIANL, value).commit();
                sp.edit().putString(Const.ZHAN_KONG_BI, pwm).commit();
//                new ConnectionThread(jggStr).start();
                sp.edit().putString(Const.Interface_Value, jggStr).commit();
                new ConnectionThread(jggStr).start();
                if (mJGGRadioButton.isChecked()) {
                    sp.edit().putBoolean(Const.JGG_BTN_IS_CHECKED, true).commit();
                } else {
                    sp.edit().putBoolean(Const.JGG_BTN_IS_CHECKED, false).commit();
                }
                Toast.makeText(MainActivity.this, "set success", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onResume() {
        super.onResume();
        jggValue = sp.getString(Const.JGG_FZ, "0");
        jggPWM = sp.getString(Const.JGG_DL, "0");
        fgValue = sp.getString(Const.FG_FZ, "0");
        fgPWM = sp.getString(Const.FG_DL, "0");
        isFirstRun = sp.getBoolean(Const.APP_IS_First_RUN, true);
        if (isFirstRun) {
//            isFirstRun = false;
            sp.edit().putBoolean(Const.APP_IS_First_RUN, false).commit();
            new ConnectionThread("10400050").start();
            sp.edit().putString(Const.JGG_FZ, "400").commit();
            sp.edit().putString(Const.JGG_DL, "050").commit();
            sp.edit().putString(Const.FG_FZ, "000").commit();
            sp.edit().putString(Const.FG_DL, "000").commit();
            sp.edit().putString(Const.Interface_Value, "10400050").commit();
            sp.edit().putString(Const.FZ_DIANL, "400").commit();
            sp.edit().putString(Const.ZHAN_KONG_BI, "050").commit();
            sp.edit().putBoolean(Const.JGG_BTN_IS_CHECKED, true).commit();
        } else {
            String interface_value = sp.getString(Const.Interface_Value, "0");
            new ConnectionThread(interface_value).start();
        }
        mISOValue = sp.getString(Const.ISO_VALUE, Const.ISO_AUTO);
        mExposureValue = sp.getString(Const.EXPOSURE_VALUE, Const.EXPOSURE_VALUE_DEFAULT);
        Log.e("", "mISOValue--" + mISOValue);
        startCameraThread();

        if (!(mTextureViewIR.isAvailable()) && !(mTextureViewRGB.isAvailable())) {
            mTextureViewIR.setSurfaceTextureListener(mTextureListenerIR);
            mTextureViewRGB.setSurfaceTextureListener(mTextureListenerRGB);
        } else {
            startPreviewIR(false);
            startPreviewRGB();
        }

    }

    private void startCameraThread() {
        mCameraThreadIR = new HandlerThread("CameraThreadIR");
        mCameraThreadIR.start();
        mCameraHandlerIR = new Handler(mCameraThreadIR.getLooper());
        mCameraThreadRGB = new HandlerThread("CameraThreadRGB");
        mCameraThreadRGB.start();
        mCameraHandlerRGB = new Handler(mCameraThreadRGB.getLooper());
    }


    private TextureView.SurfaceTextureListener mTextureListenerIR = new TextureView.SurfaceTextureListener() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        //当mTextureListener准备好后会回调该方法
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //当SurefaceTexture可用的时候，设置相机参数并打开相机
            setupCameraIR(width, height);
            openCameraIR();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
    private TextureView.SurfaceTextureListener mTextureListenerRGB = new TextureView.SurfaceTextureListener() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        //当mTextureListener准备好后会回调该方法
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //当SurefaceTexture可用的时候，设置相机参数并打开相机
            setupCameraRGB(width, height);
            openCameraRGB();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    /**
     * 设置相机参数
     *
     * @param width
     * @param height
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setupCameraIR(int width, int height) {
        //获取摄像头的管理者CameraManager
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            //遍历所有摄像头
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);//获取该摄像头的特性参数

//                Log.e("lzh", "范围--" + characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE));
                StreamConfigurationMap map1 = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                //此处默认打开后置摄像头

                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK)
                    continue;

                //获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                assert map != null;
                //根据TextureView的尺寸设置预览尺寸
                mPreviewSizeIR = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                //获取相机支持的最大拍照尺寸
                mCaptureSizeIR = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new Comparator<Size>() {
                    @Override
                    public int compare(Size lhs, Size rhs) {
                        return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getHeight() * rhs.getWidth());
                    }
                });
                //此ImageReader用于拍照所需
                setupImageReaderIR();
                mCameraIdIR = cameraId;
                break;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setupCameraRGB(int width, int height) {
        //获取摄像头的管理者CameraManager
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            //遍历所有摄像头
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);//获取该摄像头的特性参数
//                Log.e("lzh", "范围--" + characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE));
                StreamConfigurationMap map1 = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                //此处默认打开后置摄像头
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT)
                    continue;
                Range<Long> range = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
                // range==[13175, 863370925]
                //获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                assert map != null;
                //根据TextureView的尺寸设置预览尺寸
                mPreviewSizeRGB = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);

               /* for(int i=0;i<map.getOutputSizes(SurfaceTexture.class).length;i++){
                    Log.e("lzh","mPreviewSizeRGB==="+map.getOutputSizes(SurfaceTexture.class)[i]);
                }*/
                //获取相机支持的最大拍照尺寸
                mCaptureSizeRGB = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new Comparator<Size>() {
                    @Override
                    public int compare(Size lhs, Size rhs) {
                        return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getHeight() * rhs.getWidth());
                    }
                });
                //此ImageReader用于拍照所需
                setupImageReaderRGB();
                mCameraIdRGB = cameraId;
                break;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //选择sizeMap中大于并且最接近width和height的size
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Size getOptimalSize(Size[] sizeMap, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return sizeMap[0];
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openCameraIR() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            configureTextureViewTransform(mTextureViewIR.getWidth(), mTextureViewIR.getHeight(), mPreviewSizeIR, mTextureViewIR, 3);
            manager.openCamera(mCameraIdIR, mStateCallbackIR, mCameraHandlerIR);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCameraRGB() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            configureTextureViewTransform(mTextureViewRGB.getWidth(), mTextureViewRGB.getHeight(), mPreviewSizeRGB, mTextureViewRGB, 0);
            manager.openCamera(mCameraIdRGB, mStateCallbackRGB, mCameraHandlerRGB);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraDevice.StateCallback mStateCallbackIR = new CameraDevice.StateCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDeviceIR = camera;
            startPreviewIR(false);
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDeviceIR = null;
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDeviceIR = null;
        }
    };
    private CameraDevice.StateCallback mStateCallbackRGB = new CameraDevice.StateCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDeviceRGB = camera;
            startPreviewRGB();
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDeviceRGB = null;
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDeviceRGB = null;
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startPreviewIR(boolean isJPEG) {
        if (mCameraDeviceIR != null) {
            SurfaceTexture mSurfaceTexture = mTextureViewIR.getSurfaceTexture();
            mSurfaceTexture.setDefaultBufferSize(mPreviewSizeIR.getWidth(), mPreviewSizeIR.getHeight());
            Surface previewSurfaceIR = new Surface(mSurfaceTexture);
            try {
                mCaptureRequestBuilderIR = mCameraDeviceIR.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mCaptureRequestBuilderIR.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(manager.getCameraIdList()[0]);//获取该摄像头的特性参数
                Range[] fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                mCaptureRequestBuilderIR.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRanges[fpsRanges.length - 1]);
                mCaptureRequestBuilderIR.addTarget(previewSurfaceIR);
                if (isJPEG) {
                    mImageReaderIR = ImageReader.newInstance(480, 640,
                            ImageFormat.JPEG, 2);
                } else {
                    mImageReaderIR = ImageReader.newInstance(1080, 1280,
                            ImageFormat.RAW_SENSOR, 2);
                }

                mImageReaderIR.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        mCameraHandlerIR.post(new imageSaverIR(reader.acquireNextImage(), sp));
                    }
                }, mCameraHandlerIR);
                mCameraDeviceIR.createCaptureSession(Arrays.asList(previewSurfaceIR, mImageReaderIR.getSurface()), new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(CameraCaptureSession session) {
                        try {
                            mCaptureRequestBuilderIR.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, Integer.parseInt(mExposureValue));
                            mCaptureRequestIR = mCaptureRequestBuilderIR.build();
                            mCameraCaptureSessionIR = session;
                            mCameraCaptureSessionIR.setRepeatingRequest(mCaptureRequestIR, null, mCameraHandlerIR);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(CameraCaptureSession session) {

                    }
                }, mCameraHandlerIR);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

    }

    public void startPreviewRGB() {
        SurfaceTexture mSurfaceTextureRGB = mTextureViewRGB.getSurfaceTexture();
        mSurfaceTextureRGB.setDefaultBufferSize(mPreviewSizeRGB.getWidth(), mPreviewSizeRGB.getHeight());
        Surface previewSurfaceRGB = new Surface(mSurfaceTextureRGB);
        try {
            mCaptureRequestBuilderRGB = mCameraDeviceRGB.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilderRGB.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            mCaptureRequestBuilderRGB.addTarget(previewSurfaceRGB);
            mCameraDeviceRGB.createCaptureSession(Arrays.asList(previewSurfaceRGB, mImageReaderRGB.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
//                        mCaptureRequestBuilderRGB.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, Integer.parseInt(mExposureValue));
//                        mCaptureRequestBuilderRGB.set(CaptureRequest.SENSOR_EXPOSURE_TIME, Long.parseLong("200000000"));
//                        mCaptureRequestBuilderRGB.set(CaptureRequest.SENSOR_SENSITIVITY, 160);
                        mCaptureRequestRGB = mCaptureRequestBuilderRGB.build();
                        mCameraCaptureSessionRGB = session;
                        mCameraCaptureSessionRGB.setRepeatingRequest(mCaptureRequestRGB, null, mCameraHandlerRGB);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, mCameraHandlerRGB);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void takePicture() {
        starttime = Long.parseLong(Utils.getStringDate());
        lockFocus();
    }

    private List<CaptureRequest> builderList = new ArrayList<CaptureRequest>();

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void lockFocus() {
        try {
            mCaptureRequestBuilderIR.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            mCameraCaptureSessionIR.capture(mCaptureRequestBuilderIR.build(), mCaptureCallbackIR, mCameraHandlerIR);
            mCaptureRequestBuilderRGB.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            mCameraCaptureSessionRGB.capture(mCaptureRequestBuilderRGB.build(), mCaptureCallbackRGB, mCameraHandlerRGB);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.CaptureCallback mCaptureCallbackIR = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            captureIR();
        }
    };
    private CameraCaptureSession.CaptureCallback mCaptureCallbackRGB = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            captureRGB();
        }
    };
    private static int CODE_TAKE_LED = 1000;
    private static int CODE_TAKE_LED_PIC = 1001;
    private static int CODE_TAKE_PIC_FINISH_ALL = 1002;
    private static int CODE_TAKE_PIC_New = 1003;
    private static int isClicked = 111;
    private static boolean isStart = false;

    private int picCount = 0;
    private Handler mHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
//            &&isTakeIR
            if (msg.what == CODE_TAKE_LED) {

                String interface_value = sp.getString(Const.Interface_Value, "0");
                StringBuilder sb = new StringBuilder(interface_value);
                sb.replace(0, 1, "2");
                new ConnectionThread(sb.toString()).start();
                mc = new MyCount(50, 50);
                mc.start();
            } else if (msg.what == CODE_TAKE_LED_PIC) {
                startPreviewIR(true);
                mc = new MyCount(50, 50);
                mc.start();
            } else if (msg.what == CODE_TAKE_PIC_FINISH_ALL) {
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
                if (mPicCountTextView != null) {
                    mPicCountTextView.setText(picCount + "");
                }
                startPreviewIR(false);
//                Toast.makeText(MainActivity.this,"计算完成",Toast.LENGTH_SHORT).show();
                Log.e("lzh", "picCount==" + picCount);
                long peoid = Long.parseLong(Utils.getStringDate()) - starttime;
                Log.e("lzh", "总耗时==" + peoid);
                mHandle.sendEmptyMessageDelayed(CODE_TAKE_PIC_New, 1000);
            } else if (msg.what == CODE_TAKE_PIC_New) {
                if (isStart) {
                    isClicked = 0;
                    takePicture();
                }
            }
        }
    };
    private MyCount mc;
    private static startCount mcStart;

    private class MyCount extends CountDownTimer {
        public MyCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            //倒计时后进行拍照保存；
            mc.cancel();
            try {
                mCaptureRequestBuilderIR.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
                mCameraCaptureSessionIR.capture(mCaptureRequestBuilderIR.build(), mCaptureCallbackIR, mCameraHandlerIR);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onTick(long millisUntilFinished) {

        }

    }

    private class startCount extends CountDownTimer {
        public startCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            //倒计时后进行拍照保存；
            mc.cancel();
            takePicture();
        }

        @Override
        public void onTick(long millisUntilFinished) {

        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void captureIR() {
        try {
            final CaptureRequest.Builder mCaptureBuilder = mCameraDeviceIR.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            mCaptureBuilder.addTarget(mImageReaderIR.getSurface());
            mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(1));
            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    if (isClicked == 0) {
                        isClicked = 1;
                        picCount--;
                    }
                    if (isClicked == 1) {
                        mHandle.sendEmptyMessage(CODE_TAKE_LED);
                    } else if (isClicked == 2) {
                        mHandle.sendEmptyMessage(CODE_TAKE_LED_PIC);
                    } else if (isClicked == 100) {
                        picCount++;
                        Log.e("lzh", "picCount==" + picCount);
                        String interface_value = sp.getString(Const.Interface_Value, "0");
                        new ConnectionThread(interface_value).start();
//                        startPreviewIR(false);
                        mHandle.sendEmptyMessage(CODE_TAKE_PIC_FINISH_ALL);
                        unlockFocusIR();
                    }

                }
            };

            mCameraCaptureSessionIR.stopRepeating();
            mCameraCaptureSessionIR.capture(mCaptureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void captureRGB() {
        try {
            final CaptureRequest.Builder mCaptureBuilder = mCameraDeviceRGB.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            mCaptureBuilder.addTarget(mImageReaderRGB.getSurface());
            mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(0));
            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
//                    Toast.makeText(getApplicationContext(), "拍照成功", Toast.LENGTH_SHORT).show();
                    unlockFocusRGB();
//                    mHandle.sendEmptyMessage(CODE_TAKE_PIC_FINISH);
                }
            };
            mCameraCaptureSessionRGB.stopRepeating();
            mCameraCaptureSessionRGB.capture(mCaptureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)


    public void unlockFocusIR() {
        try {
            mCaptureRequestBuilderIR.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
//            mCameraCaptureSessionIR.capture(mCaptureRequestBuilderIR.build(), null, mCameraHandlerIR);
//            mCameraCaptureSessionIR.setRepeatingRequest(mCaptureRequestIR, null, mCameraHandlerIR);
        } catch (Exception e) {
            Log.e("lzh", "unlockFocusIR--Exception--" + e.toString());
            e.printStackTrace();
        }
    }

    public void unlockFocusRGB() {
        try {
            mCaptureRequestBuilderRGB.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mCameraCaptureSessionRGB.capture(mCaptureRequestBuilderRGB.build(), null, mCameraHandlerRGB);
            mCameraCaptureSessionRGB.setRepeatingRequest(mCaptureRequestRGB, null, mCameraHandlerRGB);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Socket soc;
    private String messageRecv;
    private static String IP_ADDRESS = "127.0.0.1";
    private static int PORT = 8666;

    //新建一个子线程，实现socket通信
    class ConnectionThread extends Thread {
        String message = null;

        public ConnectionThread(String msg) {
            Log.e("lzh", "msg--" + msg);
            message = msg;
        }

        @Override
        public void run() {

            try {
                Socket soc = new Socket();
                SocketAddress endpoint = new InetSocketAddress(IP_ADDRESS, PORT);
                try {
                    soc.connect(endpoint, 200);
//                    soc.setSoTimeout(1000);
                } catch (Exception e) {
//                    soc.close();
                    Log.e("lzh", "结构光error--" + e.toString());
                    e.printStackTrace();
                    Bundle bundle = new Bundle();
                    bundle.putString("data", "set failed");
                    Message message = new Message();
                    message.setData(bundle);
                    mHandle.sendMessage(message);
                }
                OutputStream output;
                try {
                    output = soc.getOutputStream();
                    soc.setSoTimeout(100);
                    output.write(message.getBytes());
                    output.flush();
                } catch (Exception e) {
                    Log.e("lzh", "Exception222--" + e.toString());
                    e.printStackTrace();
                    Bundle bundle = new Bundle();
                    bundle.putString("data", "set failed");
                    Message message = new Message();
                    message.setData(bundle);
                    mHandle.sendMessage(message);
                }

                int i = 0;
                boolean isSocketConnect = true;
                while (isSocketConnect) {
                    i++;
                    if (i > 500) {
                        break;
                    }
                    InputStream input;
                    int count = 0;
                    try {
                        input = soc.getInputStream();
                        soc.setSoTimeout(100);
                        if (count == 0) {
                            count = input.available();
                        }
                        if (count != 0) {
                            byte[] b = new byte[count];
                            int readed = input.read(b);
                            messageRecv = new String(b, 0, readed);
                            Bundle bundle = new Bundle();
                            bundle.putString("data", messageRecv);
                            Message message = new Message();
                            message.setData(bundle);
                            mHandle.sendMessage(message);
                            break;
                        }
                    } catch (Exception e) {
                        isSocketConnect = false;
                        Log.e("lzh", "Exception333--" + e.toString());
                        e.printStackTrace();
                        Bundle bundle2 = new Bundle();
                        bundle2.putString("data", "set failed");
                        Message message = new Message();
                        message.setData(bundle2);
                        mHandle.sendMessage(message);
                    }
                }
                soc.close();

            } catch (IOException e) {
                Log.e(getClass().getName(), e.getMessage());
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onPause() {
        super.onPause();
        new ConnectionThread("10000000").start();
        if (mCameraCaptureSessionIR != null) {
            mCameraCaptureSessionIR.close();
            mCameraCaptureSessionIR = null;
        }
        if (mCameraCaptureSessionRGB != null) {
            mCameraCaptureSessionRGB.close();
            mCameraCaptureSessionRGB = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPreviewIR();
        stopPreviewRGB();
    }

    public void stopPreviewIR() {
        if (mCameraCaptureSessionIR != null) {
            mCameraCaptureSessionIR.close();
            mCameraCaptureSessionIR = null;
        }

        if (mCameraDeviceIR != null) {
            mCameraDeviceIR.close();
            mCameraDeviceIR = null;
        }

        if (mImageReaderIR != null) {
            mImageReaderIR.close();
            mImageReaderIR = null;
        }

    }

    public void stopPreviewRGB() {
        if (mCameraCaptureSessionRGB != null) {
            mCameraCaptureSessionRGB.close();
            mCameraCaptureSessionRGB = null;
        }

        if (mCameraDeviceRGB != null) {
            mCameraDeviceRGB.close();
            mCameraDeviceRGB = null;
        }

        if (mImageReaderRGB != null) {
            mImageReaderRGB.close();
            mImageReaderRGB = null;
        }

    }

    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void setupImageReaderIR() {
        //2代表ImageReader中最多可以获取两帧图像流

      /*  if (Const.JPEG_FORMAR.equals(sp.getString(Const.SAVE_FORMAR, Const.JPEG_FORMAR))) {
            mImageReaderIR = ImageReader.newInstance(mCaptureSizeIR.getWidth(), mCaptureSizeIR.getHeight(),
                    ImageFormat.JPEG, 2);
        } else {
            mImageReaderIR = ImageReader.newInstance(mCaptureSizeIR.getWidth(), mCaptureSizeIR.getHeight(),
                    ImageFormat.RAW_SENSOR, 2);
        }*/
        mImageReaderIR = ImageReader.newInstance(1080, 1280,
                ImageFormat.RAW_SENSOR, 20);
        mImageReaderIR.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                mCameraHandlerIR.post(new imageSaverIR(reader.acquireNextImage(), sp));
            }
        }, mCameraHandlerIR);
    }

    private void setupImageReaderRGB() {
        //2代表ImageReader中最多可以获取两帧图像流

      /*  if (Const.JPEG_FORMAR.equals(sp.getString(Const.SAVE_FORMAR, Const.JPEG_FORMAR))) {
            mImageReaderIR = ImageReader.newInstance(mCaptureSizeIR.getWidth(), mCaptureSizeIR.getHeight(),
                    ImageFormat.JPEG, 2);
        } else {
            mImageReaderIR = ImageReader.newInstance(mCaptureSizeIR.getWidth(), mCaptureSizeIR.getHeight(),
                    ImageFormat.RAW_SENSOR, 2);
        }*/
        mImageReaderRGB = ImageReader.newInstance(1600, 1200,
                ImageFormat.JPEG, 2);

        mImageReaderRGB.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                mCameraHandlerRGB.post(new imageSaverRGB(reader.acquireNextImage(), sp));
            }
        }, mCameraHandlerRGB);
    }

    public static class imageSaverIR implements Runnable {

        private Image mImage;
        private SharedPreferences msp;

        public imageSaverIR(Image image, SharedPreferences sp) {
            mImage = image;
            msp = sp;
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            String path = Environment.getExternalStorageDirectory() + "/DetectDemo/RAWCamera";
            File mImageFile = new File(path);
            if (!mImageFile.exists()) {
                mImageFile.mkdirs();
            }

            String fileName = null;
            String preName;
            if (isClicked == 0) {

                timeStamp = Utils.getStringDate();
                //计算深度图
//                byte [] ir_data=Utils.getBytesByFile("/storage/emulated/0/DetectDemo/RAWCamera/IR_20200730110341585.raw");
//                new CalRawDepthTask().execute(ir_data);
                if (new File(Const.REF_RAW_PATH).exists()) {
                    byte[] timeData = timeStamp.getBytes();
                    byte yuvAndTimeData[][] = {data, timeData};
                    new CalRawDepthTask().execute(yuvAndTimeData);
                }

                preName = "/IR_";
                fileName = path + preName + timeStamp + ".raw";
                isClicked = 1;
            } else if (isClicked == 1) {
                byte[] timeData = timeStamp.getBytes();
                byte yuvAndTimeData[][] = {data, timeData};
                new AdjustBrightAndContrasTask().execute(yuvAndTimeData);
                preName = "/LED_";
                fileName = path + preName + timeStamp + ".raw";
                isClicked = 100;
//                Log.e("lzh", "存图--LED raw");

            } else if (isClicked == 2) {

                isClicked = 100;
                preName = "/srcLED_";
                fileName = path + preName + timeStamp + ".jpg";
            }
            FileOutputStream fos = null;
            if (fileName != null) {
                try {
                    fos = new FileOutputStream(fileName);
                    fos.write(data, 0, data.length);
                } catch (IOException e) {
                    Log.e("lzh", "IOException==" + e.toString());
                    e.printStackTrace();
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                            mImage.close();
                            fos = null;
                            mImage = null;
                            buffer.clear();
                        } catch (IOException e) {
                            Log.e("lzh", "close error--" + e.toString());
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
    }

    private static class AdjustBrightAndContrasTask extends AsyncTask<byte[], Void, Void> {
        @Override
        protected Void doInBackground(byte[]... bytes) {
            byte[] rawData = bytes[0];
            byte[] timeData = bytes[1];
            int brightness = Integer.parseInt(sp.getString(Const.BRIGHTNESS, Const.BRIGHTNESS_VALUE_DEFAULT));
            int contrast = Integer.parseInt(sp.getString(Const.CONSTRAST, Const.CONSTRAST_VALUE_DEFAULT));
//            Log.e("lzh", "brightness==" + brightness);
//            Log.e("lzh", "contrast==" + contrast);
            Log.e("lzh", "timeStamp-==" + new String(timeData));
            String ledJPEGpath = "/storage/emulated/0/DetectDemo/RAWCamera/LED_" + new String(timeData) + ".jpg";
            AdjustBrightnessAndContrast(rawData, brightness, contrast, ledJPEGpath);
            return null;
        }

    }

    private static class CalRawDepthTask extends AsyncTask<byte[], Void, Void> {
        @Override
        protected Void doInBackground(byte[]... bytes) {
            int iscompensate = 0;
            if (sp.getBoolean(Const.PEI_ZHUN, true)) {
                iscompensate = 1;
            } else {
                iscompensate = 0;
            }
            CalRawDepth(bytes[0], "/storage/emulated/0/DetectDemo/RAWCamera/DepthData_" + new String(bytes[1]) + ".raw", "/storage/emulated/0/DetectDemo/RAWCamera/Depth_" + new String(bytes[1]) + ".jpg", iscompensate);
            return null;
        }

    }

    public static class imageSaverRGB implements Runnable {

        private Image mImage;
        private SharedPreferences msp;

        public imageSaverRGB(Image image, SharedPreferences sp) {
            mImage = image;
            msp = sp;
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            String path = Environment.getExternalStorageDirectory() + "/DetectDemo/RAWCamera";
            File mImageFile = new File(path);
            if (!mImageFile.exists()) {
                mImageFile.mkdirs();
            }
//            String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String fileName;
            if (Const.JPEG_FORMAR.equals(msp.getString(Const.SAVE_FORMAR, Const.JPEG_FORMAR))) {
                fileName = path + "/src_RGB_" + timeStamp + ".jpg";
            } else {
                fileName = path + "/src_RGB_" + timeStamp + ".raw";
            }
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(fileName);
                fos.write(data, 0, data.length);
                //对齐RGB输出：
                String srcRGBPath = fileName;
                String desRGBPath = path + "/RGB_" + timeStamp + ".jpg";
                String paths[] = {srcRGBPath, desRGBPath};
                new RamapRGBTask().execute(paths);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("lzh", "RGB--Exception==" + e.toString());
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                        mImage.close();
                        buffer.clear();
                    } catch (IOException e) {
                        Log.e("lzh", "close--Exception==" + e.toString());
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    //输入的RGB为1200*1600 IR1080*1280
    private static class RamapRGBTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... paths) {
            if (new File(Const.YML_PATH).exists()) {
                long startTime = System.nanoTime();  //開始時間
                GetRemappedRGBImage2IRVGA(Const.YML_PATH, paths[0], paths[1], null, null);
                long periodTime = System.nanoTime() - startTime;
//                Log.e("lzh", "对齐耗时--" + periodTime / 1000000);
                Utils.deleteFile(paths[0]);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private void configureTextureViewTransform(int viewWidth, int viewHeight, Size mPreviewSize, TextureView mTextureView, int rotation) {
        if (null == mTextureView) {
            return;
        }
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        } else if (Surface.ROTATION_0 == rotation) {
        }
        mTextureView.setTransform(matrix);
    }

    //测试
    public static native String stringFromJNI();

    //Remap RGB to IR
    public static native void GetRemappedRGBImage2IRVGA(String ymlPath, String src_rgb_path, String des_rgb_path, String irPath, String mixPath);

    //预处理RGB和IR的remap
    public static native void PreprocessRemappedRGB2IR(String ymlPath);

    public static native void PreprocessRaw(byte[] ref_data);

    //计算raw IR 的深度图
    public static native void CalRawDepth(byte[] obj_data, String depth_data_path, String depth_imge_path, int compensate);

    //给LED RAW图进行调整亮度和对比度后并进行保存
    public static native void AdjustBrightnessAndContrast(byte[] obj_data, int brightness, int contrast, String adjust_led_path);
}

