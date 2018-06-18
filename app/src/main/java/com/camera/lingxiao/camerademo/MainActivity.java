package com.camera.lingxiao.camerademo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    private String[] perms = {Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO};
    public static final int RC_CAMERA_AND_LOCATION = 1;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private int mCamerId = 0;
    private Camera mCamera;
    private ImageView shutterImg,changeImg;
    private CircleImageView localImg;
    private Button recorderBtn;
    private CameraUtil mCameraUtil;
    private Uri localImgUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSurfaceView = findViewById(R.id.surfaceView);
        localImg = findViewById(R.id.iv_local);
        shutterImg = findViewById(R.id.iv_shutter);
        changeImg = findViewById(R.id.iv_change);
        recorderBtn = findViewById(R.id.bt_recode);
        mHolder = mSurfaceView.getHolder();
        methodRequiresTwoPermission();


        changeImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mCameraUtil){
                    mCameraUtil.changeCamera(mHolder);
                }
            }
        });

        localImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null == localImgUri){
                    return;
                }
                //打开指定的一张照片
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(localImgUri, "image/*");
                startActivity(intent);
            }
        });

        shutterImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shutterImg.setEnabled(false);
                if (null != mCameraUtil){
                    mCameraUtil
                            .takePicture(System.currentTimeMillis() + ".jpg",
                                    "CameraDemo");
                }
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        //同意授权
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        //拒绝授权
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).setRationale("请同意相机权限").setTitle("提示").build().show();
        }
    }

    private void methodRequiresTwoPermission() {
        if (EasyPermissions.hasPermissions(this, perms)) {
            if (checkCameraHardware(this)){
                mHolder.addCallback(new SurfaceCallback());
            }else {
                Toast.makeText(this,"没有相机硬件",Toast.LENGTH_SHORT).show();
            }
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, "需要同意权限",
                    RC_CAMERA_AND_LOCATION, perms);
        }
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isRecorder;
    private class SurfaceCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(final SurfaceHolder holder) {
            try {
                mCamera = Camera.open(mCamerId);
                mCameraUtil = new CameraUtil(mCamera,mCamerId);
                mCameraUtil.setPicTakenListener(new CameraUtil.PictureTakenCallBack() {
                    @Override
                    public void onPictureTaken(String result, File file) {
                        if (result.isEmpty()){
                            localImgUri = Uri.fromFile(file);
                            Toast.makeText(getApplicationContext(),"保存成功",Toast.LENGTH_SHORT).show();
                            localImg.setImageURI(localImgUri);
                        }else {
                            Toast.makeText(getApplicationContext(),result,Toast.LENGTH_SHORT).show();
                        }
                        shutterImg.setEnabled(true);
                    }
                });
                mCameraUtil.setPreviewCallback(new CameraUtil.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {

                    }
                });

                recorderBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (null != mCameraUtil){
                            if (isRecorder){
                                //如果是正在录制 就点击停止录制 不往后走了
                                mCameraUtil.stopRecorder();
                                recorderBtn.setText("录制");
                                return;
                            }
                            isRecorder = mCameraUtil.initRecorder(Environment
                                    .getExternalStorageDirectory()
                                    .getAbsolutePath()+"/CameraDemo/"
                                    +System.currentTimeMillis() + ".mp4",holder
                            );

                            if (isRecorder){
                                LogUtil.i("正在录制");
                                recorderBtn.setText("停止录制");
                            }
                        }
                    }
                });
            }catch (Exception e){
                LogUtil.i("摄像头被占用");
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            try {
                mCameraUtil
                        .initCamera(mSurfaceView.getWidth(),
                                mSurfaceView.getHeight(),
                                MainActivity.this);
                mCamera.setPreviewDisplay(holder);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (null != mCameraUtil){
                mCameraUtil.stopRecorder();
                mCameraUtil.stopPreview();
            }
        }
    }
}
