package red.lilu.zhinanzhen;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import red.lilu.zhinanzhen.databinding.ActivityMainBinding;

/**
 * https://developer.android.com/guide/topics/sensors/sensors_position#sensors-pos-orient
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {

  private static final String T = "调试";
  private ActivityMainBinding b;
  private SensorManager sensorManager;
  private final float[] accelerometerReading = new float[3];
  private final float[] magnetometerReading = new float[3];
  private final float[] rotationMatrix = new float[9];
  private final float[] orientationAngles = new float[3];

  // Compute the three orientation angles based on the most recent readings from
  // the device's accelerometer and magnetometer.
  public void updateOrientationAngles() {
    // https://developer.android.com/reference/android/hardware/SensorManager#getRotationMatrix(float%5B%5D,%20float%5B%5D,%20float%5B%5D,%20float%5B%5D)
    SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading);
    // https://developer.android.com/reference/android/hardware/SensorManager#getOrientation(float%5B%5D,%20float%5B%5D)
    SensorManager.getOrientation(rotationMatrix, orientationAngles);

    //以手机屏幕向上水平放置为例, 上侧顺时针旋转时的角度(0或360表示正北，90表示正东，正负180表示正南，-90表示正西)
    long azimuthDegrees = Math.round(Math.toDegrees(orientationAngles[0])); //-z
    //纠正为0到360度的值
    azimuthDegrees = (azimuthDegrees + 360) % 360;
    //以手机屏幕向上水平放置为例, 上下两侧翻转时的角度
    long pitchDegrees = Math.round(Math.toDegrees(orientationAngles[1])); //x
    //以手机屏幕向上水平放置为例, 左右两侧翻转时的角度
    long rollDegrees = Math.round(Math.toDegrees(orientationAngles[2])); //y

    if (rollDegrees < -45L && rollDegrees > -135L && Math.abs(pitchDegrees) < 45L) {
      //右侧朝天(横屏立起时以背面朝向为指向)
      azimuthDegrees += 90;
      if (azimuthDegrees >= 360) {
        azimuthDegrees -= 360;
      }
    } else if (rollDegrees > 45L && rollDegrees < 135L && Math.abs(pitchDegrees) < 45L) {
      //左侧朝天(横屏立起时以背面朝向为指向)
      azimuthDegrees -= 90;
      if (azimuthDegrees < 0) {
        azimuthDegrees += 360;
      }
    } else if (Math.abs(rollDegrees) > 100 && pitchDegrees <= -45) {
      //竖起时当pitch在-80左右时会出现指向倒置(上侧的感应会跳动,导致指向相反方向)
      //此处修正并不理想, 没有达到高德地图的稳定效果.
      azimuthDegrees += 180;
      if (azimuthDegrees >= 360) {
        azimuthDegrees -= 360;
      }
    }

    //更新UI
    b.text.setText(
        String.format("azimuth: %04d, pitch: %04d, roll: %04d", azimuthDegrees, pitchDegrees, rollDegrees)
    );
    b.image.setRotation(azimuthDegrees);
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
      System.arraycopy(event.values, 0, accelerometerReading,
          0, accelerometerReading.length);
    } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
      System.arraycopy(event.values, 0, magnetometerReading,
          0, magnetometerReading.length);
    }

    updateOrientationAngles();
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    //
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    b = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(b.getRoot());

    //传感器
    sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
  }

  @Override
  protected void onResume() {
    super.onResume();

    // Get updates from the accelerometer and magnetometer at a constant rate.
    // To make batch operations more efficient and reduce power consumption,
    // provide support for delaying updates to the application.
    //
    // In this example, the sensor reporting delay is small enough such that
    // the application receives an update before the system checks the sensor
    // readings again.
    Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    if (accelerometer != null) {
      sensorManager.registerListener(this, accelerometer,
          SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
    }
    Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    if (magneticField != null) {
      sensorManager.registerListener(this, magneticField,
          SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();

    sensorManager.unregisterListener(this);
  }
}
