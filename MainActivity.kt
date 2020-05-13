package com.example.tiltmesure

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    lateinit var sensorManager: SensorManager
    lateinit var accelerometer: Sensor
    lateinit var gyroscope: Sensor

    var accelerometerCount: Int = 0
    var gyroCount: Int = 0
    val accelerometerData: ArrayList<FloatArray> = ArrayList()
    val gyroData: ArrayList<FloatArray> = ArrayList()
    val sampleSize = 1000

    var accelerometerBias = -1.0
    var accelerometerNoise = -1.0
    val gyroBias = -1.0
    val gyroNoise = -1.0

    val accelerometerTilt: ArrayList<Double> = ArrayList()
    val gyroTilt: ArrayList<Double> = ArrayList()
    var initAccelerometerTimeStamp: Long = -1
    var initGyroTimeStamp: Long = -1
    var lastGyroTimeStamp: Long = -1

    val secondToNano: Double = 1000000000.0
    val fiveMin: Long = secondToNano.toLong() * 60 * 5

    var initTilt: Double = -1.0
    var combined: Boolean = false
    val alpha: Double = 0.3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL)

        gyroTilt.add(0.0)
    }

    fun recordAccelerometer(values: FloatArray) {
        accelerometerData.add(values)
        accelerometerCount++
        logProcess(accelerometerCount, "accelerometer")
        if (accelerometerCount > sampleSize) {
            Log.d("save-Data", "start saving data for accelerometer")
            saveData("accelerometer", accelerometerData)
            sensorManager.unregisterListener(this, accelerometer)
        }
    }

    fun recordGyro(values: FloatArray) {
        gyroData.add(values)
        gyroCount++
        logProcess(gyroCount, "gyro")
        if (gyroCount > sampleSize) {
            Log.d("save-Data", "start saving data for gyro")
            saveData("gyro", gyroData)
            sensorManager.unregisterListener(this, gyroscope)
        }
    }

    fun logProcess(count: Int, name: String) {
        if (count % 10 == 0)
            Log.d("progress report", "$name counted $count datapoints")
    }

    fun saveData(filename: String, source: ArrayList<FloatArray>) {
        //        val sd = File(Environment.getExternalStorageDirectory())
        val path = applicationContext.getExternalFilesDir(null)
        Log.d("filepath", "the path saving to is ${path?.absolutePath}")
        val file = File(path, "$filename.txt")
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()
        file.printWriter().use { out ->
            source.forEach {
                out.println("${it[0]} ${it[1]} ${it[2]}")
            }
        }
    }

    fun accTiltMesure(values: FloatArray, timeStamp: Long) {
        val normG = sqrt(values[0].pow(2) + values[1].pow(2) + values[2].pow(2))
        for (i in values.indices) {
            values[i] = values[i] / normG
        }
        val inclination = Math.toDegrees(acos(values[2]).toDouble())
        if (initAccelerometerTimeStamp == (-1).toLong())
            initAccelerometerTimeStamp = timeStamp

        accelerometerTilt.add(inclination)
        logProcess(accelerometerTilt.size, "accelerometer")
//        changeText("inclination: $inclination \n")

        if (timeStamp - initAccelerometerTimeStamp > fiveMin) {
            Log.d("accelerometer", "time is up for 5 min")
            sensorManager.unregisterListener(this, accelerometer)
            saveTiltData("accelerometer_tilt", accelerometerTilt)
        }
    }

    fun gyroTiltMesure(values: FloatArray, timeStamp: Long) {
        if (initGyroTimeStamp == (-1).toLong()) {
            initGyroTimeStamp = timeStamp
            lastGyroTimeStamp = timeStamp
        }
//        Log.d("gyro", "${values[0]} ${values[1]} ${values[2]} ${(timeStamp - lastGyroTimeStamp) / secondToNano}")
        val rotation = Math.toDegrees(values[0].toDouble() * ((timeStamp - lastGyroTimeStamp) / secondToNano))
        lastGyroTimeStamp = timeStamp
        val inclination = rotation + gyroTilt[gyroTilt.size - 1]
        gyroTilt.add(inclination)
        logProcess(gyroTilt.size, "gyro")
//        changeText(("inclination: $inclination"))

        if (timeStamp - initGyroTimeStamp > fiveMin) {
            Log.d("gyro", "time is up for 5 min")
            sensorManager.unregisterListener(this, gyroscope)
            saveTiltData("gyro_tilt", gyroTilt)
        }
    }

    fun combinedTiltMesure(type: Int, values: FloatArray, timeStamp: Long, saveFile: Boolean) {
        if (type == Sensor.TYPE_GYROSCOPE) {
            if (initTilt != -1.0 && !combined) {
                if (initGyroTimeStamp == (-1).toLong() || lastGyroTimeStamp == (-1).toLong()) {
                    initGyroTimeStamp = timeStamp
                    lastGyroTimeStamp = timeStamp
                }
                val rotation = Math.toDegrees(values[0].toDouble() * ((timeStamp - lastGyroTimeStamp) / secondToNano))
                lastGyroTimeStamp = timeStamp
                val lastIndex = gyroTilt.size - 1
                val inclination = rotation + gyroTilt[lastIndex - 1]
                gyroTilt[lastIndex] += inclination * (1 - alpha)
                combined = !combined
//                Log.d("gyro", "added inclination $inclination and current rotation $rotation")
            }

        } else {
            val normG = sqrt(values[0].pow(2) + values[1].pow(2) + values[2].pow(2))
            for (i in values.indices) {
                values[i] = values[i] / normG
            }
            if (initGyroTimeStamp == (-1).toLong()) {
                initGyroTimeStamp = timeStamp
            }
            val inclination = Math.toDegrees(acos(values[2]).toDouble())
            if (initTilt == -1.0) {
                initTilt = inclination
                gyroTilt.add(initTilt)
                gyroTilt.add(initTilt * alpha)
//                Log.d("accelerometer", "init with $initTilt")
            } else if (combined) {
                gyroTilt.add(inclination * alpha)
                combined = !combined
//                Log.d("accelerometer", "added $inclination")
            }
        }
        if (combined) {
            changeText(("inclination: ${gyroTilt[gyroTilt.size - 1]}"))
        }

//        logProcess(gyroTilt.size, "gyro")

        if (saveFile && timeStamp - initGyroTimeStamp > fiveMin) {
            sensorManager.unregisterListener(this)
            saveTiltData("combined_tilt", gyroTilt)
        }

    }

    fun saveTiltData(filename: String, source: ArrayList<Double>) {
        //        val sd = File(Environment.getExternalStorageDirectory())
        val path = applicationContext.getExternalFilesDir(null)
        Log.d("filepath", "the path saving to is ${path?.absolutePath}")
        val file = File(path, "$filename.txt")
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()
        file.printWriter().use { out ->
            source.forEach {
                out.println("$it")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {

        // get sensor name and data
        val values = event!!.values.copyOf()
        val time = event.timestamp
//        Log.i("sensor report", "${event.sensor.name} gives out ${values[0]}, ${values[1]}, ${values[2]}")

        // part 1 get data
//        when (event.sensor.type) {
//            Sensor.TYPE_ACCELEROMETER -> recordAccelerometer(values)
//            Sensor.TYPE_GYROSCOPE -> recordGyro(values)
//            else -> throw IllegalStateException("the sensorName is equal to neither gyro or accelerometer")
//        }

        // part 2 tilt mesure
//        when (event.sensor.type) {
//            Sensor.TYPE_ACCELEROMETER -> accTiltMesure(values.copyOf(), time)
//            Sensor.TYPE_GYROSCOPE -> gyroTiltMesure(values.copyOf(), time)
//            else -> throw IllegalStateException("the sensorName is equal to neither gyro or accelerometer")
//        }

        // part 2 combined tilt
//        when (event.sensor.type) {
//            Sensor.TYPE_ACCELEROMETER -> combinedTiltMesure(Sensor.TYPE_ACCELEROMETER, values.copyOf(), time, true)
//            Sensor.TYPE_GYROSCOPE -> combinedTiltMesure(Sensor.TYPE_GYROSCOPE, values.copyOf(), time, true)
//            else -> throw IllegalStateException("the sensorName is equal to neither gyro or accelerometer")
//        }

        // part 3
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> combinedTiltMesure(Sensor.TYPE_ACCELEROMETER, values.copyOf(), time, false)
            Sensor.TYPE_GYROSCOPE -> combinedTiltMesure(Sensor.TYPE_GYROSCOPE, values.copyOf(), time, false)
            else -> throw IllegalStateException("the sensorName is equal to neither gyro or accelerometer")
        }

    }

    fun changeText(text: String) {
        if (text != "no") {
            displayBox.text = text
        }
    }
}

