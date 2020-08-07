package cc.uphoton.rawcamera.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Properties;

import cc.uphoton.rawcamera.R;
import cc.uphoton.rawcamera.utils.Const;

import static cc.uphoton.rawcamera.utils.Const.SETTING_DEPTH_WEI;


public class SettingActivity extends AppCompatActivity {
    private SharedPreferences sp;
    private Button mJGGButton;
    private Button mFGButton;
    private EditText mJGGValueEditText;
    private EditText mJGGPWMEditText;
    private EditText mFGValueEditText;
    private EditText mFGPWMEditText;
    private RadioGroup mRadioGroup;
    private RadioButton mRadioBtn16;
    private RadioButton mRadioBtn32;
    private RadioButton mRadioBtn128;
    private RadioButton mRadioBtn512;
    private RadioButton mRadioBtn64;
    private RadioButton mRadioBtn0;
    private RadioButton mRadioBtn1;
    private int mDepthWei;
    private Button mSelectRefButton;
    private String picGalleryPath;
    private Uri galleryUri;
    private TextView mRefPath;
    private static final int IMAGE_REQUEST_CODE = 0;

    private String jggValue;
    private String jggPWM;
    private String fgValue;
    private String fgPWM;

    private RadioGroup mLightRadioGroup;
    private RadioButton mJGGRadioButton;
    private RadioButton mFGRadioButton;
    private EditText mValueEditText;
    private EditText mPWMEditText;
    private Button mLightSettingButton;

    private RadioGroup mISORadioGroup;
    private RadioButton mAutoRadioBtn;
    private RadioButton mISO400RadioBtn;
    private RadioButton mISO200RadioBtn;
    private RadioButton mISO800RadioBtn;
    private RadioButton mISO1600RadioBtn;
    private EditText mRangeEditText;
    private EditText mExposureEditText;
    private EditText mBrightnessEditText;
    private EditText mContrastEditText;

    private RadioGroup mSaveFormatRadioGroup;
    private RadioButton mJPEGRadioBtn;
    private RadioButton mRawRadioBtn;

    private Float reference_depth;
    private int disparity_period_DOE;
    private Float depth_range_min;
    private Float depth_range_max;
    private Float roi_depth_range_0;
    private Float roi_depth_range_1;
    private Float face_filter_ratio;
    private int match_win_size;
    private String match_win_size_str;
    private String hole_win_radius_str;
    private int hole_win_radius;
    private Boolean horizontal_flip;
    private int ir_image_width;
    private int ir_image_height;
    private float focal_length;
    private float base_width;
    private float pixel_size;
    private int[] pixelsRef;
    private Properties proper;
    private boolean isFirstRun;
    private RadioGroup mPZRadioGroup;
    private RadioButton mRbtnTrue;
    private RadioButton mRbtnFalse;


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(SettingActivity.this, MainActivity.class);
                startActivity(intent);
                this.finish(); // back button
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_setting_haixin);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        sp = getSharedPreferences("sp", MODE_PRIVATE);
        initView();
        Log.e("lzh", "曝光参数===" + sp.getString(Const.EXPOSURE_VALUE, Const.EXPOSURE_VALUE_DEFAULT));
        if (sp.getBoolean(Const.PEI_ZHUN, true)) {
            mRbtnTrue.setChecked(true);
            mRbtnFalse.setChecked(false);
        } else {
            mRbtnTrue.setChecked(false);
            mRbtnFalse.setChecked(true);
        }
        mPZRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_true) {
                    mRbtnTrue.setChecked(true);
                    mRbtnFalse.setChecked(false);
                    sp.edit().putBoolean(Const.PEI_ZHUN, true).commit();
                } else if (checkedId == R.id.rb_false) {
                    mRbtnTrue.setChecked(false);
                    mRbtnFalse.setChecked(true);
                    sp.edit().putBoolean(Const.PEI_ZHUN, false).commit();
                }

            }
        });
        mExposureEditText.setText(sp.getString(Const.EXPOSURE_VALUE, Const.EXPOSURE_VALUE_DEFAULT));
        mExposureEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                int mExposureValue;
                if (mExposureEditText.getText() == null || mExposureEditText.getText().toString().isEmpty()) {
                    mExposureValue = 0;
                } else {
                    if (Integer.parseInt(v.getText().toString()) > 12) {
                        Toast.makeText(SettingActivity.this, "请输入0-12内的数值", Toast.LENGTH_SHORT).show();
                        mExposureEditText.setText(12 + "");
                    } else if (Integer.parseInt(v.getText().toString()) < -12) {
                        Toast.makeText(SettingActivity.this, "请输入-12-12内的数值", Toast.LENGTH_SHORT).show();
                        mExposureEditText.setText(0 + "");
                    }

                    mExposureValue = Integer.parseInt(mExposureEditText.getText().toString());
                }
                sp.edit().putString(Const.EXPOSURE_VALUE, mExposureValue + "").commit();
                return false;
            }
        });
        mBrightnessEditText.setText(sp.getString(Const.BRIGHTNESS, Const.BRIGHTNESS_VALUE_DEFAULT));
        mBrightnessEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                int mBrightnessValue;
                if (mBrightnessEditText.getText() == null || mBrightnessEditText.getText().toString().isEmpty()) {
                    mBrightnessValue = 0;
                } else {
                    if (Integer.parseInt(v.getText().toString()) > 255) {
                        Toast.makeText(SettingActivity.this, "请输入-255-255内的数值", Toast.LENGTH_SHORT).show();
                        mBrightnessEditText.setText(255 + "");
                    } else if (Integer.parseInt(v.getText().toString()) < -255) {
                        Toast.makeText(SettingActivity.this, "请输入-255-255内的数值", Toast.LENGTH_SHORT).show();
                        mBrightnessEditText.setText(-255 + "");
                    }

                    mBrightnessValue = Integer.parseInt(mBrightnessEditText.getText().toString());
                }
                sp.edit().putString(Const.BRIGHTNESS, mBrightnessValue + "").commit();
                return false;
            }
        });
        mContrastEditText.setText(sp.getString(Const.CONSTRAST, Const.CONSTRAST_VALUE_DEFAULT));
        mContrastEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                int mContrastValue;
                if (mContrastEditText.getText() == null || mContrastEditText.getText().toString().isEmpty()) {
                    mContrastValue = 0;
                } else {
                    if (Integer.parseInt(v.getText().toString()) > 255) {
                        Toast.makeText(SettingActivity.this, "请输入-255-255内的数值", Toast.LENGTH_SHORT).show();
                        mContrastEditText.setText(255 + "");
                    } else if (Integer.parseInt(v.getText().toString()) < -255) {
                        Toast.makeText(SettingActivity.this, "请输入-255-255内的数值", Toast.LENGTH_SHORT).show();
                        mContrastEditText.setText(-255 + "");
                    }
                    mContrastValue = Integer.parseInt(mContrastEditText.getText().toString());
                }
                sp.edit().putString(Const.CONSTRAST, mContrastValue + "").commit();
                return false;
            }
        });

    }


    private void initView() {
        mPZRadioGroup = findViewById(R.id.mRadioGroup111);
        mRbtnTrue = findViewById(R.id.rb_true);
        mRbtnFalse = findViewById(R.id.rb_false);
//        getFragmentManager().beginTransaction().replace(R.id.prefs_frame, new MyPreferenceFragment()).commit();
        mSaveFormatRadioGroup = findViewById(R.id.rg_save_format);
        mJPEGRadioBtn = findViewById(R.id.rb_jpg);
        mRawRadioBtn = findViewById(R.id.rb_raw);
        String saveFormat = sp.getString(Const.SAVE_FORMAR, Const.JPEG_FORMAR);
        if (Const.JPEG_FORMAR.equals(saveFormat)) {
            mJPEGRadioBtn.setChecked(true);
            mRawRadioBtn.setChecked(false);
        } else {
            mJPEGRadioBtn.setChecked(false);
            mRawRadioBtn.setChecked(true);
        }
        mSaveFormatRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup groupConst, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_jpg:
                        mJPEGRadioBtn.setChecked(true);
                        mRawRadioBtn.setChecked(false);
                        sp.edit().putString(Const.SAVE_FORMAR, Const.JPEG_FORMAR).commit();
                        break;
                    case R.id.rb_raw:
                        mJPEGRadioBtn.setChecked(false);
                        mRawRadioBtn.setChecked(true);
                        sp.edit().putString(Const.SAVE_FORMAR, Const.RAW_SENSOR_FORMAR).commit();
                        break;
                }
            }
        });
        mRangeEditText = findViewById(R.id.et_colormapParams_range);
        mExposureEditText = findViewById(R.id.et_exposure);
        mBrightnessEditText = findViewById(R.id.et_bright);
        mContrastEditText = findViewById(R.id.et_Contrast);
        mISORadioGroup = findViewById(R.id.rg_iso);
        mAutoRadioBtn = findViewById(R.id.rb_auto);
        mISO400RadioBtn = findViewById(R.id.rb_iso_400);
        mISO200RadioBtn = findViewById(R.id.rb_iso_200);
        mISO800RadioBtn = findViewById(R.id.rb_iso_800);
        mISO1600RadioBtn = findViewById(R.id.rb_iso_1600);

        mLightRadioGroup = findViewById(R.id.mLightRadioGroup);
        mJGGRadioButton = findViewById(R.id.rb_jgg);
        mFGRadioButton = findViewById(R.id.rb_fg);
        mValueEditText = findViewById(R.id.et_value);
        mPWMEditText = findViewById(R.id.et_pwm);
        mLightSettingButton = findViewById(R.id.btn_light_setting);

        String mISOValue = sp.getString(Const.ISO_VALUE, Const.ISO_AUTO);
        if ("auto".equals(mISOValue)) {
            mAutoRadioBtn.setChecked(true);
            mISO400RadioBtn.setChecked(false);
            mISO200RadioBtn.setChecked(false);
            mISO800RadioBtn.setChecked(false);
            mISO1600RadioBtn.setChecked(false);
        } else if ("400".equals(mISOValue)) {
            mAutoRadioBtn.setChecked(false);
            mISO400RadioBtn.setChecked(true);
            mISO200RadioBtn.setChecked(false);
            mISO800RadioBtn.setChecked(false);
            mISO1600RadioBtn.setChecked(false);
        } else if ("200".equals(mISOValue)) {
            mAutoRadioBtn.setChecked(false);
            mISO400RadioBtn.setChecked(false);
            mISO200RadioBtn.setChecked(true);
            mISO800RadioBtn.setChecked(false);
            mISO1600RadioBtn.setChecked(false);
        } else if ("800".equals(mISOValue)) {
            mAutoRadioBtn.setChecked(false);
            mISO400RadioBtn.setChecked(false);
            mISO200RadioBtn.setChecked(false);
            mISO800RadioBtn.setChecked(true);
            mISO1600RadioBtn.setChecked(false);

        } else if ("1600".equals(mISOValue)) {
            mAutoRadioBtn.setChecked(false);
            mISO400RadioBtn.setChecked(false);
            mISO200RadioBtn.setChecked(false);
            mISO800RadioBtn.setChecked(false);
            mISO1600RadioBtn.setChecked(true);
        }
        mISORadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_auto:
                        mAutoRadioBtn.setChecked(true);
                        mISO400RadioBtn.setChecked(false);
                        mISO200RadioBtn.setChecked(false);
                        mISO800RadioBtn.setChecked(false);
                        mISO1600RadioBtn.setChecked(false);
                        sp.edit().putString(Const.ISO_VALUE, Const.ISO_AUTO).commit();
                        break;
                    case R.id.rb_iso_400:
                        mAutoRadioBtn.setChecked(false);
                        mISO400RadioBtn.setChecked(true);
                        mISO200RadioBtn.setChecked(false);
                        mISO800RadioBtn.setChecked(false);
                        mISO1600RadioBtn.setChecked(false);
                        sp.edit().putString(Const.ISO_VALUE, Const.ISO_400).commit();
                        break;
                    case R.id.rb_iso_200:
                        mAutoRadioBtn.setChecked(false);
                        mISO400RadioBtn.setChecked(false);
                        mISO200RadioBtn.setChecked(true);
                        mISO800RadioBtn.setChecked(false);
                        mISO1600RadioBtn.setChecked(false);
                        sp.edit().putString(Const.ISO_VALUE, Const.ISO_200).commit();
                        break;
                    case R.id.rb_iso_800:
                        mAutoRadioBtn.setChecked(false);
                        mISO400RadioBtn.setChecked(false);
                        mISO200RadioBtn.setChecked(false);
                        mISO800RadioBtn.setChecked(true);
                        mISO1600RadioBtn.setChecked(false);
                        sp.edit().putString(Const.ISO_VALUE, Const.ISO_800).commit();
                        break;
                    case R.id.rb_iso_1600:
                        mAutoRadioBtn.setChecked(false);
                        mISO400RadioBtn.setChecked(false);
                        mISO200RadioBtn.setChecked(false);
                        mISO800RadioBtn.setChecked(false);
                        mISO1600RadioBtn.setChecked(true);
                        sp.edit().putString(Const.ISO_VALUE, Const.ISO_1600).commit();
                        break;
                    default:
                        break;
                }
            }
        });

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


        mSelectRefButton = findViewById(R.id.bt_select_ref);
        mRefPath = findViewById(R.id.tv_ref_path);
        String refPath = sp.getString(Const.REF_PATH, null);
        if (refPath != null && !refPath.isEmpty()) {
            mRefPath.setText(refPath);
            Bitmap mBitmapRef = BitmapFactory.decodeFile(refPath);
            int w = mBitmapRef.getWidth();
            int h = mBitmapRef.getHeight();
            pixelsRef = new int[w * h];
            mBitmapRef.getPixels(pixelsRef, 0, w, 0, 0, w, h);
        }
        mSelectRefButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //从文件夹选择参考图片
                pickPhoto();
            }
        });

        mRadioGroup = findViewById(R.id.mRadioGroup);
        mRadioBtn16 = findViewById(R.id.rb_16);
        mRadioBtn32 = findViewById(R.id.rb_32);
        mRadioBtn64 = findViewById(R.id.rb_64);
        mRadioBtn128 = findViewById(R.id.rb_128);
        mRadioBtn512 = findViewById(R.id.rb_512);
        mRadioBtn0 = findViewById(R.id.rb_0);
        mRadioBtn1 = findViewById(R.id.rb_1);
        mJGGButton = findViewById(R.id.jgg_btn);
        mFGButton = findViewById(R.id.fg_btn);
        mJGGValueEditText = findViewById(R.id.et_value_jgg);
        mJGGPWMEditText = findViewById(R.id.et_pwm_jgg);
        mFGValueEditText = findViewById(R.id.et_value_fg);
        mFGPWMEditText = findViewById(R.id.et_pwm_fg);
        jggValue = sp.getString(Const.JGG_FZ, "0");
        jggPWM = sp.getString(Const.JGG_DL, "0");
        fgValue = sp.getString(Const.FG_FZ, "0");
        fgPWM = sp.getString(Const.FG_DL, "0");
        if (jggValue.equals("000")) {
            jggValue = "0";
        }
        if (jggPWM.equals("000")) {
            jggPWM = "0";
        }
        if (fgValue.equals("000")) {
            fgValue = "0";
        }
        if (fgPWM.equals("000")) {
            fgPWM = "0";
        }
        mJGGValueEditText.setText(jggValue);
        mJGGPWMEditText.setText(jggPWM);
        mFGValueEditText.setText(fgValue);
        mFGPWMEditText.setText(fgPWM);
        //初始化深度图算法的位数
        mDepthWei = sp.getInt(SETTING_DEPTH_WEI, 128);
        if (mDepthWei == 16) {
            mRadioBtn16.setChecked(true);
        } else if (mDepthWei == 32) {
            mRadioBtn32.setChecked(true);
        } else if (mDepthWei == 64) {
            mRadioBtn64.setChecked(true);
        } else if (mDepthWei == 128) {
            mRadioBtn128.setChecked(true);
        } else if (mDepthWei == 512) {
            mRadioBtn512.setChecked(true);
        } else if (mDepthWei == 0) {
            mRadioBtn0.setChecked(true);
        } else if (mDepthWei == -1) {
            mRadioBtn1.setChecked(true);
        }
        final int range_vga = sp.getInt(Const.RANGE_VALUE_VGA, 10);
        final int range_roi = sp.getInt(Const.RANGE_VALUE_ROI, 3);
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_16:
                        sp.edit().putInt(SETTING_DEPTH_WEI, 16).commit();
                        mRadioBtn16.setChecked(true);
                        mRangeEditText.setText(range_roi + "");
                        break;
                    case R.id.rb_32:
                        sp.edit().putInt(SETTING_DEPTH_WEI, 32).commit();
                        mRadioBtn32.setChecked(true);
                        mRangeEditText.setText(range_roi + "");

                        break;
                    case R.id.rb_64:
                        sp.edit().putInt(SETTING_DEPTH_WEI, 64).commit();
                        mRadioBtn64.setChecked(true);
                        mRangeEditText.setText(range_roi + "");


                        break;
                    case R.id.rb_128:
                        sp.edit().putInt(SETTING_DEPTH_WEI, 128).commit();
                        mRadioBtn128.setChecked(true);
                        mRangeEditText.setText(range_roi + "");


                        break;
                    case R.id.rb_512:
                        sp.edit().putInt(SETTING_DEPTH_WEI, 512).commit();
                        mRadioBtn512.setChecked(true);
                        mRangeEditText.setText(range_roi + "");


                        break;
                    case R.id.rb_0:
                        sp.edit().putInt(SETTING_DEPTH_WEI, 0).commit();
                        mRadioBtn0.setChecked(true);
                        mRangeEditText.setText(range_roi + "");


                        break;
                    case R.id.rb_1:
                        sp.edit().putInt(SETTING_DEPTH_WEI, -1).commit();
                        mRadioBtn1.setChecked(true);
                        mRangeEditText.setText(range_vga + "");

                        break;
                    default:
                        sp.edit().putInt(SETTING_DEPTH_WEI, 0).commit();
                        mRadioBtn0.setChecked(true);
                        mRangeEditText.setText(range_roi + "");


                        break;

                }
            }
        });
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
                        Toast.makeText(SettingActivity.this, "最大峰值电流为2500ma", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(SettingActivity.this, "请输入数字", Toast.LENGTH_SHORT).show();
                }


                if (value.getBytes().length == 1) {
                    value = "000" + value;
                } else if (value.getBytes().length == 2) {
                    value = "00" + value;
                } else if (value.getBytes().length == 3) {
                    value = "0" + value;
                }

                if (pwm == null || pwm.isEmpty()) {
                    pwm = "0";
                    mPWMEditText.setText("0");
                }
                if (Integer.parseInt(pwm) > 100) {
                    pwm = "100";
                    mPWMEditText.setText("100");
                    Toast.makeText(SettingActivity.this, "最大电流占空比为100", Toast.LENGTH_SHORT).show();
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
                if (mJGGRadioButton.isChecked()) {
                    sp.edit().putBoolean(Const.JGG_BTN_IS_CHECKED, true).commit();
                } else {
                    sp.edit().putBoolean(Const.JGG_BTN_IS_CHECKED, false).commit();
                }
                isSetted = true;
                Toast.makeText(SettingActivity.this, "set success", Toast.LENGTH_SHORT).show();
            }
        });

        //设置结构光
        mJGGButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String jggValue = mJGGValueEditText.getText().toString();
                String jggPWM = mJGGPWMEditText.getText().toString();

                if (jggValue == null || jggValue.isEmpty()) {
                    jggValue = "0";
                    mJGGValueEditText.setText("0");
                }
                try {
                    if (Integer.parseInt(jggValue) > 400) {
                        jggValue = "400";
                        mJGGValueEditText.setText("400");
                        Toast.makeText(SettingActivity.this, "最大峰值电流为400ma", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(SettingActivity.this, "请输入数字", Toast.LENGTH_SHORT).show();
                }


                if (jggValue.getBytes().length == 1) {
                    jggValue = "00" + jggValue;
                } else if (jggValue.getBytes().length == 2) {
                    jggValue = "0" + jggValue;
                }

                if (jggPWM == null || jggPWM.isEmpty()) {
                    jggPWM = "0";
                    mJGGPWMEditText.setText("0");
                }
                if (Integer.parseInt(jggPWM) > 100) {
                    jggPWM = "100";
                    mJGGPWMEditText.setText("100");
                    Toast.makeText(SettingActivity.this, "最大电流占空比为100", Toast.LENGTH_SHORT).show();
                }
                if (jggPWM.getBytes().length == 1) {
                    jggPWM = "00" + jggPWM;
                } else if (jggPWM.getBytes().length == 2) {
                    jggPWM = "0" + jggPWM;
                }
                String jggStr = "1" + jggValue + jggPWM + "0";

                sp.edit().putString(Const.JGG_FZ, jggValue).commit();
                sp.edit().putString(Const.JGG_DL, jggPWM).commit();
                new ConnectionThread(jggStr).start();
                sp.edit().putString(Const.Interface_Value, jggStr).commit();
                isSetted = true;


            }
        });
        //设置泛光
        mFGButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fgValue = mFGValueEditText.getText().toString();
                String fgPWM = mFGPWMEditText.getText().toString();
                if (fgValue == null || fgValue.isEmpty()) {
                    fgValue = "0";
                    mFGValueEditText.setText("0");
                }
                if (Integer.parseInt(fgValue) > 400) {
                    fgValue = "400";
                    mFGValueEditText.setText("400");
                    Toast.makeText(SettingActivity.this, "最大峰值电流为400ma", Toast.LENGTH_SHORT).show();
                }

                if (fgValue.getBytes().length == 1) {
                    fgValue = "00" + fgValue;
                } else if (fgValue.getBytes().length == 2) {
                    fgValue = "0" + fgValue;
                }

                if (fgPWM == null || fgPWM.isEmpty()) {
                    fgPWM = "0";
                    mFGPWMEditText.setText("0");
                }
                if (Integer.parseInt(fgPWM) > 100) {
                    fgPWM = "100";
                    mFGPWMEditText.setText("100");
                    Toast.makeText(SettingActivity.this, "最大电流占空比为100", Toast.LENGTH_SHORT).show();
                }
                if (fgPWM.getBytes().length == 1) {
                    fgPWM = "00" + fgPWM;
                } else if (fgPWM.getBytes().length == 2) {
                    fgPWM = "0" + fgPWM;
                }

                sp.edit().putString(Const.FG_FZ, fgValue).commit();
                sp.edit().putString(Const.FG_DL, fgPWM).commit();
                String fgStr = "2" + fgValue + fgPWM + "0";
                new ConnectionThread(fgStr).start();
                sp.edit().putString(Const.Interface_Value, fgStr).commit();
                isSetted = true;

            }
        });
    }


    private boolean isSetted = false;
    private String receiverMsg;
    private Handler mHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle receiveBundle = msg.getData();
            receiverMsg = receiveBundle.getString("data");
            if (receiverMsg != null && isSetted) {
                Toast.makeText(SettingActivity.this, receiverMsg, Toast.LENGTH_SHORT).show();
                isSetted = false;
            }
        }
    };

    private void pickPhoto() {
        Intent intentFromGallery = new Intent();
        intentFromGallery.setType("image/*"); // 设置文件类型
        intentFromGallery
                .setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intentFromGallery,
                IMAGE_REQUEST_CODE);
    }

    private Socket soc;
    private String messageRecv;
    private static String IP_ADDRESS = "127.0.0.1";
    private static int PORT = 8666;

    //新建一个子线程，实现socket通信
    public class ConnectionThread extends Thread {
        String message = null;

        public ConnectionThread(String msg) {
            message = msg;
            Log.e("lzh", "---lzh-message--" + message);
        }

        @Override
        public void run() {

            try {
                Socket soc = new Socket();
                SocketAddress endpoint = new InetSocketAddress(IP_ADDRESS, PORT);
                try {
                    soc.connect(endpoint, 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                    Bundle bundle = new Bundle();
                    bundle.putString("data", "connect inteface time out");
                    Message message = new Message();
                    message.setData(bundle);
                    mHandle.sendMessage(message);
                }
                OutputStream output = soc.getOutputStream();
                output.write(message.getBytes());
                output.flush();
                int i = 0;
                while (true) {
                    i++;
                    if (i > 500) {
                        break;
                    }
                    InputStream input = soc.getInputStream();
                    // simply for java.util.ArrayList
                    int count = 0;
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
                    } else {
                        Log.e("lzh", "count==0");
                    }
                }
                soc.close();
            } catch (IOException e) {
                Log.e(getClass().getName(), e.getMessage());
            }


        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
      /*  if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case IMAGE_REQUEST_CODE:
                    try {
                        galleryUri = data.getData();
                        String uriStr = galleryUri.toString();
                        //3399:content://com.android.providers.media.documents/document/image:1238
                        //4507:content://com.android.externalstorage.documents/document/primary:DetectDemoNew/resize_ir.jpg
                        //4503:content://media/external/images/media/698
                        if (uriStr.contains("%3A") || uriStr.contains("%2F")) {
                            uriStr = uriStr.replace("%3A", ":");
                            uriStr = uriStr.replace("%2F", "/");
                        }
                        String refName = uriStr.substring(uriStr.lastIndexOf("/"));
                        String path = Utils.getPath(SettingActivity.this, Uri.parse(uriStr));

                        if (path != null && !(path.endsWith(".jpg") || path.endsWith(".bmp") || path.endsWith(".png"))) {
                            path = path + refName;
                        }
                        picGalleryPath = path;
                        mRefPath.setText(picGalleryPath);
                        if (picGalleryPath != null) {
                            sp.edit().putString(Const.REF_PATH, picGalleryPath).commit();
                            //预处理参考图
                            Bitmap mBitmapRef = BitmapFactory.decodeFile(picGalleryPath);
                            int w = mBitmapRef.getWidth();
                            int h = mBitmapRef.getHeight();
                            pixelsRef = new int[w * h];
                            mBitmapRef.getPixels(pixelsRef, 0, w, 0, 0, w, h);
                            if (sp.getInt(SETTING_DEPTH_WEI, 128) == -1) {
                                PreprocessVGA(pixelsRef, focal_length, base_width, pixel_size, reference_depth, disparity_period_DOE, depth_range_min,
                                        depth_range_max, horizontal_flip);
                            } else {
                                PreprocessROI(pixelsRef, ir_image_width, ir_image_height, focal_length, base_width, pixel_size, reference_depth, disparity_period_DOE, depth_range_min,
                                        depth_range_max, horizontal_flip, roi_depth_range_0, roi_depth_range_1, face_filter_ratio);

                            }
                            Toast.makeText(SettingActivity.this, "参考图设置成功" + picGalleryPath, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(SettingActivity.this, "参考图设置失败", Toast.LENGTH_SHORT).show();
                        String refPath = sp.getString(Const.REF_PATH, null);
                        if (refPath != null && !refPath.isEmpty()) {
                            mRefPath.setText(refPath);
                        }
                    }
                    break;
                default:
                    break;
            }
        }*/
    }

    @Override
    protected void onPause() {
        super.onPause();
//        new ConnectionThread("20000000").start();
//        SystemClock.sleep(100);
//        new ConnectionThread("10000000").start();
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("lzh", "mExposureEditText--" + mExposureEditText.getText().toString());
        int mExposureValue = Integer.parseInt(mExposureEditText.getText().toString());
        sp.edit().putString(Const.EXPOSURE_VALUE, mExposureValue + "").commit();
        int mBrightnessValue = Integer.parseInt(mBrightnessEditText.getText().toString());
        sp.edit().putString(Const.BRIGHTNESS, mBrightnessValue + "").commit();
        int mContrastValue = Integer.parseInt(mContrastEditText.getText().toString());
        sp.edit().putString(Const.CONSTRAST, mContrastValue + "").commit();

        int rangeValueVGA = 10, rangeValueROI = 3;
        mDepthWei = sp.getInt(SETTING_DEPTH_WEI, 128);
        if (mRangeEditText.getText() == null || mRangeEditText.getText().toString().isEmpty()) {
            if (mDepthWei == -1) {
                rangeValueVGA = 10;
            } else {
                rangeValueROI = 3;
            }
        } else {
            if (mDepthWei == -1) {
                rangeValueVGA = Integer.parseInt(mRangeEditText.getText().toString());
            } else {
                rangeValueVGA = 10;
            }
            if (mDepthWei != -1) {
                rangeValueROI = Integer.parseInt(mRangeEditText.getText().toString());
            } else {
                rangeValueROI = 3;
            }
        }
        sp.edit().putInt(Const.RANGE_VALUE_VGA, rangeValueVGA).commit();
        sp.edit().putInt(Const.RANGE_VALUE_ROI, rangeValueROI).commit();
//        if (pixelsRef != null) {
//            if (sp.getInt(SETTING_DEPTH_WEI, 128) == -1) {
//                PreprocessVGA(pixelsRef, focal_length, base_width, pixel_size, reference_depth, disparity_period_DOE, depth_range_min,
//                        depth_range_max, horizontal_flip);
//            } else {
//                PreprocessROI(pixelsRef, ir_image_width, ir_image_height, focal_length, base_width, pixel_size, reference_depth, disparity_period_DOE, depth_range_min,
//                        depth_range_max, horizontal_flip, roi_depth_range_0, roi_depth_range_1, face_filter_ratio);
//
//            }
//        }

    }
}
