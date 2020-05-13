import numpy as np
import matplotlib.pyplot as plt

accelerometer = np.genfromtxt('accelerometer_tilt.txt')
gyro = np.genfromtxt('gyro_tilt.txt')
combined = np.genfromtxt('combined_tilt.txt')


def plotting(data, title):
    plt.xlabel("sample in 5 minutes")
    plt.ylabel("tilt")
    plt.plot(data)
    plt.title(title)
    plt.grid()
    plt.show()


plotting(accelerometer, "accelerometer_tilt")
plotting(gyro, "gyro_tilt")
plotting(combined, "combined_tilt")
