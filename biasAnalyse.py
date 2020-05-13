import numpy as np

gyro = np.genfromtxt('gyro.txt', delimiter=" ")

gyro_x = gyro[:, 0]
gyro_y = gyro[:, 1]
gyro_z = gyro[:, 2]

gyro_x_bias = np.mean(gyro_x)
gyro_y_bias = np.mean(gyro_y)
gyro_z_bias = np.mean(gyro_z)

print(str(gyro_x_bias) + ' ' + str(gyro_x_bias) + ' ' + str(gyro_z_bias))

gyro_x_noise = np.var(gyro_x)
gyro_y_noise = np.var(gyro_y)
gyro_z_noise = np.var(gyro_z)

print(str(gyro_x_noise) + ' ' + str(gyro_x_noise) + ' ' + str(gyro_z_noise))

accelerometer = np.genfromtxt('accelerometer.txt', delimiter=" ")

accelerometer_x = accelerometer[:, 0]
accelerometer_y = accelerometer[:, 1]
accelerometer_z = accelerometer[:, 2] + 9.81

accelerometer_x_bias = np.mean(accelerometer_x)
accelerometer_y_bias = np.mean(accelerometer_y)
accelerometer_z_bias = np.mean(accelerometer_z)

print(str(accelerometer_x_bias) + ' ' +
      str(accelerometer_x_bias) + ' ' + str(accelerometer_z_bias))

accelerometer_x_noise = np.var(accelerometer_x)
accelerometer_y_noise = np.var(accelerometer_y)
accelerometer_z_noise = np.var(accelerometer_z)

print(str(accelerometer_x_noise) + ' ' +
      str(accelerometer_x_noise) + ' ' + str(accelerometer_z_noise))
