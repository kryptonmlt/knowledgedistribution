import matplotlib.pyplot as plt
from numpy.random import random
from mpl_toolkits.mplot3d import Axes3D
import numpy as np
import os

colors=['b', 'c', 'y', 'm', 'r']
for file in os.listdir(os.path.dirname(os.path.realpath(__file__))):
	if file.endswith(".csv"):
		XYZ = np.genfromtxt(file, delimiter=",",skip_header=1)
		ax = plt.subplot(111, projection='3d')
		ax.set_xlabel('K')
		ax.set_ylabel('KNN')
		ax.set_zlabel('Error')
		qde = XYZ[np.where(XYZ[:,3] == 0)]
		qd = XYZ[np.where(XYZ[:,3] == 1)]
		avg = XYZ[np.where(XYZ[:,3] == 2)]
		ideal = XYZ[np.where(XYZ[:,3] == 3)]
		base = XYZ[np.where(XYZ[:,3] == 4)]
		ax.plot(qde[:, 0], qde[:, 1], qde[:, 2], 'o', color=colors[0], label='Reliability Factor')
		ax.plot(qd[:, 0], qd[:, 1], qd[:, 2], 'o', color=colors[1], label='Distance')
		ax.plot(avg[:, 0], avg[:, 1], avg[:, 2], 'o', color=colors[2], label='Average')
		ax.plot(ideal[:, 0], ideal[:, 1], ideal[:, 2], 'o', color=colors[3], label='Ideal')
		ax.plot(base[:, 0], base[:, 1], base[:, 2], 'o', color=colors[4], label='BaseLine (100% messages)')
		plt.legend(loc='upper left', numpoints=1, ncol=1, fontsize=15)
		plt.title("")
		print "Showing",file
		plt.show()