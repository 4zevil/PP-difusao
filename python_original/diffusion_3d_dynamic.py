# Random 3D displacements of N particles after S steps.
# Change numPart, numSteps and sigma to see the time evolution of the distribution

import numpy as np
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
plt.rcParams["font.family"] = "Times New Roman"
plt.rcParams["font.size"] = 12

# Number of particles and steps
numPart = 120
numSteps = 100
timeVect = np.arange(1, numSteps+1)

# Number of standard deviations to plot, that wrap the particles
N_Sigma = 3

# Generation of the "numPart" trajectories with "numSteps" displacements. Vector with +1/-1 values
randVect_X = np.cumsum((-1 + 2 * np.round(0.01 * np.random.randint(0, 100, size=(numPart, numSteps)))), axis=1)
randVect_Y = np.cumsum((-1 + 2 * np.round(0.01 * np.random.randint(0, 100, size=(numPart, numSteps)))), axis=1)
randVect_Z = np.cumsum((-1 + 2 * np.round(0.01 * np.random.randint(0, 100, size=(numPart, numSteps)))), axis=1)
maxV_XYZ = 1.2 * np.max(np.array([randVect_X, randVect_Y, randVect_Z]))

# Position mean for x, y and z
randMean_X = np.sum(randVect_X, axis=0) / numPart
randMean_Y = np.sum(randVect_Y, axis=0) / numPart
randMean_Z = np.sum(randVect_Z, axis=0) / numPart

# Mean of the squares of the positions
randSquareMean_X = np.sqrt(np.sum(randVect_X**2, axis=0) / numPart)
randSquareMean_Y = np.sqrt(np.sum(randVect_Y**2, axis=0) / numPart)
randSquareMean_Z = np.sqrt(np.sum(randVect_Z**2, axis=0) / numPart)

# Build up the unit sphere that encloses the N-Sigma portion of particles
phi = np.linspace(0, np.pi, 50)
theta = np.linspace(0, 2 * np.pi, 50)
phi, theta = np.meshgrid(phi, theta)
Xs = np.sin(phi) * np.cos(theta)
Ys = np.sin(phi) * np.sin(theta)
Zs = np.cos(phi)

# Create the plot and configure the view
fig = plt.figure()
ax = fig.add_subplot(111, projection='3d')
plt.tight_layout()

# Plot particle positions along time...
for indLoop in range(numSteps):
    ax.clear()
    sigma_x = randSquareMean_X[indLoop]
    sigma_y = randSquareMean_Y[indLoop]
    sigma_z = randSquareMean_Z[indLoop]
    x_mean = randMean_X[indLoop]
    y_mean = randMean_Y[indLoop]
    z_mean = randMean_Z[indLoop]

    # Plot the particles and the N-Sigma sphere
    ax.scatter(randVect_X[:, indLoop], randVect_Y[:, indLoop], randVect_Z[:, indLoop], marker='o', c='k', s=8)
    ax.plot_surface(x_mean + N_Sigma * sigma_x * Xs, y_mean + N_Sigma * sigma_y * Ys, z_mean + N_Sigma * sigma_z * Zs, alpha=0.2, color='b')

    # Fix plot limits and labels
    ax.set_xlim(-maxV_XYZ, maxV_XYZ)
    ax.set_ylim(-maxV_XYZ, maxV_XYZ)
    ax.set_zlim(-maxV_XYZ, maxV_XYZ)
    ax.set_xlabel('δx')
    ax.set_ylabel('δy')
    ax.set_zlabel('δz')
    ax.view_init(elev=20, azim=-170)
    ax.set_title(r'$\bf{Diffusion\ in\ 3D:}$ ' + str(numPart) + ' particles during ' + str(numSteps) + ' steps', y=0.96)
    plt.draw()
    plt.pause(0.1)

plt.show(block=False)
plt.pause(2)
