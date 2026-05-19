package com.barcodescanner;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.nio.ByteBuffer;

/**
 * BarcodeAnalyzer - 使用 Google ML Kit 进行高速条码识别
 * 针对快速移动物体优化：降低分辨率、连续帧分析、去重
 */
public class BarcodeAnalyzer implements ImageAnalysis.Analyzer {

    private final BarcodeScanner barcodeScanner;
    private final BarcodeCallback callback;
    private long lastScanTime = 0;
    private String lastScannedCode = "";
    private static final long MIN_SCAN_INTERVAL = 2000; // 同一码2秒内不重复识别
    private boolean isScanning = true;

    public interface BarcodeCallback {
        void onBarcodeDetected(String format, String value);
    }

    public BarcodeAnalyzer(BarcodeCallback callback) {
        this.callback = callback;
        // 配置 ML Kit BarcodeScanner - 支持所有条码格式
        this.barcodeScanner = BarcodeScanning.getClient(
                new com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_ALL_FORMATS
                        )
                        .build()
        );
    }

    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (!isScanning) {
            imageProxy.close();
            return;
        }

        @SuppressWarnings("UnsafeOptInUsageError")
        InputImage inputImage;

        // 从 ImageProxy 获取图像数据
        if (imageProxy.getFormat() == android.graphics.ImageFormat.YUV_420_888
                || imageProxy.getFormat() == android.graphics.ImageFormat.NV21) {
            // 直接使用 YUV_420_888 格式
            inputImage = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
        } else {
            // 降级方案：从 ByteBuffer 转换
            ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            inputImage = InputImage.fromByteArray(
                    bytes,
                    imageProxy.getWidth(),
                    imageProxy.getHeight(),
                    imageProxy.getImageInfo().getRotationDegrees(),
                    InputImage.IMAGE_FORMAT_NV21
            );
        }

        barcodeScanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    if (!isScanning) return;

                    for (Barcode barcode : barcodes) {
                        String rawValue = barcode.getRawValue();
                        if (rawValue == null || rawValue.isEmpty()) continue;

                        long now = System.currentTimeMillis();
                        // 去重：同一码在短时间内不重复触发
                        if (rawValue.equals(lastScannedCode) && (now - lastScanTime) < MIN_SCAN_INTERVAL) {
                            continue;
                        }

                        lastScannedCode = rawValue;
                        lastScanTime = now;

                        String formatName = getBarcodeFormatName(barcode.getFormat());
                        if (callback != null) {
                            callback.onBarcodeDetected(formatName, rawValue);
                        }
                        break; // 只取第一个识别到的条码
                    }
                })
                .addOnCompleteListener(task -> imageProxy.close());
    }

    /**
     * 暂停/恢复扫描
     */
    public void setScanning(boolean scanning) {
        this.isScanning = scanning;
    }

    public boolean isScanning() {
        return isScanning;
    }

    /**
     * 获取条码格式名称
     */
    private String getBarcodeFormatName(int format) {
        switch (format) {
            case Barcode.FORMAT_EAN_13: return "EAN-13";
            case Barcode.FORMAT_EAN_8: return "EAN-8";
            case Barcode.FORMAT_UPC_A: return "UPC-A";
            case Barcode.FORMAT_UPC_E: return "UPC-E";
            case Barcode.FORMAT_CODE_39: return "Code 39";
            case Barcode.FORMAT_CODE_93: return "Code 93";
            case Barcode.FORMAT_CODE_128: return "Code 128";
            case Barcode.FORMAT_CODABAR: return "Codabar";
            case Barcode.FORMAT_ITF: return "ITF";
            case Barcode.FORMAT_QR_CODE: return "QR Code";
            case Barcode.FORMAT_DATA_MATRIX: return "Data Matrix";
            case Barcode.FORMAT_PDF417: return "PDF417";
            case Barcode.FORMAT_AZTEC: return "Aztec";
            default: return "未知格式";
        }
    }

    /**
     * 释放资源
     */
    public void close() {
        barcodeScanner.close();
    }
}
