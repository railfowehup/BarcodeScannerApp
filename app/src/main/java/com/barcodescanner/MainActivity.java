package com.barcodescanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MainActivity - 高速条码扫码器主界面
 * 使用 CameraX + ML Kit 实现快速条码识别
 */
public class MainActivity extends AppCompatActivity implements BarcodeAnalyzer.BarcodeCallback {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private PreviewView previewView;
    private CardView resultCard;
    private TextView resultFormat;
    private TextView resultValue;
    private Button btnContinue;
    private FloatingActionButton btnFlashlight;
    private View flashlightContainer;
    private TextView flashlightLabel;
    private View scanLine;
    private TextView hintText;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Camera camera;
    private BarcodeAnalyzer barcodeAnalyzer;
    private ExecutorService analysisExecutor;
    private boolean isFlashlightOn = false;

    // 扫码成功音效
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initSound();
        startScanLineAnimation();

        // 检查相机权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        }

        // 继续扫描按钮
        btnContinue.setOnClickListener(v -> resumeScanning());

        // 闪光灯按钮
        btnFlashlight.setOnClickListener(v -> toggleFlashlight());
    }

    private void initViews() {
        previewView = findViewById(R.id.previewView);
        resultCard = findViewById(R.id.resultCard);
        resultFormat = findViewById(R.id.resultFormat);
        resultValue = findViewById(R.id.resultValue);
        btnContinue = findViewById(R.id.btnContinue);
        btnFlashlight = findViewById(R.id.btnFlashlight);
        flashlightContainer = findViewById(R.id.flashlightContainer);
        flashlightLabel = findViewById(R.id.flashlightLabel);
        scanLine = findViewById(R.id.scanLine);
        hintText = findViewById(R.id.hintText);
    }

    private void initSound() {
        try {
            // 使用系统默认通知音作为扫码提示音
            mediaPlayer = MediaPlayer.create(this,
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(1.0f, 1.0f);
            }
        } catch (Exception ignored) {
            // 如果无法加载音效，静默处理
        }
    }

    /**
     * 扫描线动画 - 上下循环移动
     */
    private void startScanLineAnimation() {
        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0f,
                Animation.RELATIVE_TO_PARENT, 0f,
                Animation.RELATIVE_TO_PARENT, 0f,
                Animation.RELATIVE_TO_PARENT, 1f
        );
        animation.setDuration(2000);
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);
        scanLine.startAnimation(animation);
    }

    /**
     * 启动 CameraX 相机
     */
    private void startCamera() {
        analysisExecutor = Executors.newSingleThreadExecutor();
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCamera(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                Toast.makeText(this, "相机启动失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCamera(@NonNull ProcessCameraProvider cameraProvider) {
        // 预览
        Preview preview = new Preview.Builder()
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // 选择后置摄像头
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // 图像分析 - 降低分辨率提高帧率
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(new android.util.Size(1280, 720)) // 720p 足够识别，帧率更高
                .build();

        // 创建条码分析器
        barcodeAnalyzer = new BarcodeAnalyzer(this);
        imageAnalysis.setAnalyzer(analysisExecutor, barcodeAnalyzer);

        // 解绑旧用例
        cameraProvider.unbindAll();

        // 绑定生命周期
        camera = cameraProvider.bindToLifecycle(
                (LifecycleOwner) this,
                cameraSelector,
                preview,
                imageAnalysis
        );
    }

    /**
     * 条码识别回调 - 主线程执行
     */
    @Override
    public void onBarcodeDetected(String format, String value) {
        runOnUiThread(() -> {
            // 暂停扫描
            barcodeAnalyzer.setScanning(false);

            // 震动反馈
            vibrate();

            // 播放声音
            playBeep();

            // 显示结果
            resultFormat.setText("格式: " + format);
            resultValue.setText(value);
            resultCard.setVisibility(View.VISIBLE);
            hintText.setText("✅ 识别成功");
            hintText.setTextColor(Color.GREEN);
        });
    }

    /**
     * 继续扫描
     */
    private void resumeScanning() {
        resultCard.setVisibility(View.GONE);
        hintText.setText("将条形码对准框内");
        hintText.setTextColor(Color.WHITE);
        barcodeAnalyzer.setScanning(true);
    }

    /**
     * 切换闪光灯 - 使用 CameraX 的 CameraControl API
     */
    private void toggleFlashlight() {
        if (camera == null) {
            Toast.makeText(this, "相机未就绪，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (isFlashlightOn) {
                camera.getCameraControl().enableTorch(false);
                isFlashlightOn = false;
                btnFlashlight.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                android.graphics.Color.parseColor("#555555")));
                flashlightLabel.setText("闪光灯");
                flashlightLabel.setTextColor(android.graphics.Color.WHITE);
            } else {
                camera.getCameraControl().enableTorch(true);
                isFlashlightOn = true;
                btnFlashlight.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                android.graphics.Color.parseColor("#FFD600")));
                flashlightLabel.setText("💡 已开启");
                flashlightLabel.setTextColor(android.graphics.Color.parseColor("#FFD600"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "闪光灯控制失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 震动反馈
     */
    private void vibrate() {
        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
            vibrator = vm.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        }
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(200);
            }
        }
    }

    /**
     * 播放提示音
     */
    private void playBeep() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.seekTo(0);
                }
                mediaPlayer.start();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "需要相机权限才能扫码", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (barcodeAnalyzer != null) {
            barcodeAnalyzer.close();
        }
        if (analysisExecutor != null && !analysisExecutor.isShutdown()) {
            analysisExecutor.shutdown();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
