package pl.krzysztofwojciechowski.ballmaze

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    private var sensorManager: SensorManager? = null
    private var lightSensor: Sensor? = null
    private var lightSensorListener: LightSensorListener? = null
    private var accelerometer: Sensor? = null
    private var accelerometerListener: AccelerometerListener? = null
    private var gameView: GameView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameView = GameView(this)
        setContentView(gameView)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        lightSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_LIGHT)
        lightSensorListener = LightSensorListener()

        accelerometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometerListener = AccelerometerListener()
    }


    override fun onResume() {
        super.onResume()
        sensorManager!!.registerListener(lightSensorListener, lightSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager!!.registerListener(accelerometerListener, accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager!!.unregisterListener(lightSensorListener)
        sensorManager!!.unregisterListener(accelerometerListener)
    }

    internal inner class LightSensorListener : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            gameView!!.handleLightSensor(event.values[0])
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            // We can't do much about this.
        }
    }

    internal inner class AccelerometerListener : SensorEventListener {
        var lastVal = 0f

        override fun onSensorChanged(event: SensorEvent) {
            // https://gamedev.stackexchange.com/a/73294
            lastVal += ACCEL_ALPHA * (event.values[0] - lastVal)
            gameView!!.handleAccelerometer(-lastVal * ACCEL_SENSITIVITY)
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            // Cannot handle meaningfully.
        }
    }
}