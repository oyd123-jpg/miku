package com.example.myapplication111;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;

import com.example.myapplication111.databinding.ActivityMainBinding;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    static {
        System.loadLibrary("myapplication111");
    }

    private static final float ACCEL_THRESHOLD = 20f;
    private static final float GYRO_THRESHOLD = 5f;
    private static final long COOLDOWN_MS = 800;

    private ActivityMainBinding binding;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;

    private MediaPlayer normalPlayer;
    private MediaPlayer plusPlayer;
    private long lastTriggerTime;
    private int punchCount;

    private volatile float cachedAccelMag;
    private volatile float cachedGyroMag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        normalPlayer = MediaPlayer.create(this, R.raw.hajimi);
        plusPlayer = MediaPlayer.create(this, R.raw.hajimi_plus);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (normalPlayer != null) {
            normalPlayer.release();
            normalPlayer = null;
        }
        if (plusPlayer != null) {
            plusPlayer.release();
            plusPlayer = null;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        float magnitude = (float) Math.sqrt(x * x + y * y + z * z);

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            cachedAccelMag = magnitude;
            binding.accelText.setText(String.format(Locale.US,
                    "加速度: X=%.1f  Y=%.1f  Z=%.1f  | 合=%.1f m/s²", x, y, z, magnitude));
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            cachedGyroMag = magnitude;
            binding.gyroText.setText(String.format(Locale.US,
                    "陀螺仪: X=%.2f  Y=%.2f  Z=%.2f  | 合=%.2f rad/s", x, y, z, magnitude));
        }

        checkPunch();
    }

    private void checkPunch() {
        long now = System.currentTimeMillis();

        if (cachedAccelMag > ACCEL_THRESHOLD
                && cachedGyroMag > GYRO_THRESHOLD
                && now - lastTriggerTime > COOLDOWN_MS) {

            lastTriggerTime = now;
            punchCount++;

            if (punchCount % 3 == 0) {
                binding.punchText.setText("砰！！！");
                if (plusPlayer != null) {
                    plusPlayer.seekTo(0);
                    plusPlayer.start();
                }
            } else {
                binding.punchText.setText("砰！");
                if (normalPlayer != null) {
                    normalPlayer.seekTo(0);
                    normalPlayer.start();
                }
            }

            int displayCount = punchCount % 3;
            if (displayCount == 0) displayCount = 3;
            binding.counterText.setText(String.format(Locale.US,
                    "第 %d/3 拳", displayCount));

            if (punchCount >= 1000) {
                punchCount %= 3;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public native String stringFromJNI();
}
