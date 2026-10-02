# Random 2D displacements of N particles after S steps. 
# Change numPart and numSteps and sigma and run the code many times to observe the effect on the trajectories.

import numpy as np
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
from mpl_toolkits.mplot3d import art3d
plt.rcParams["font.family"] = "Times New Roman"
plt.rcParams["font.size"] = 12

# Simulation Parameters. Number of particles and steps
numPart = 150
numSteps = 500
startPlot = 50

# Number of standard deviations to plot
N_Sigma = 2

# Generate random vectors 
randVect_X = np.zeros((numPart, numSteps))
randVect_Y = np.zeros((numPart, numSteps))
randVect_Zero = np.zeros((numPart, numSteps))
timeVect = np.arange(1, numSteps + 1)

# Generation of "numPart" trajectories with "numSteps" displacements. Vector with +1/-1 values 
randVect_X = np.cumsum((-1 + 2 * np.round(0.01 * np.random.randint(0, 100, (numPart, numSteps)))).astype(int), axis=1)
randVect_Y = np.cumsum((-1 + 2 * np.round(0.01 * np.random.randint(0, 100, (numPart, numSteps)))).astype(int), axis=1)
maxV_X = 1.25 * max(randVect_X.flatten())
maxV_Y = 1.25 * max(randVect_Y.flatten())

# Position mean for x and y
randMean_X = np.sum(randVect_X, axis=0) / numPart
randMean_Y = np.sum(randVect_Y, axis=0) / numPart

# Mean of the squares of the positions
randSquareMean_X = np.sqrt(np.sum(randVect_X ** 2, axis=0) / numPart)
randSquareMean_Y = np.sqrt(np.sum(randVect_Y ** 2, axis=0) / numPart)

# Representation of the positions of all the particles over time ...
hF3 = plt.figure()
axP3 = hF3.add_subplot(111, projection='3d')
axP3.view_init(20, 30)
plt.tight_layout()

# Inset showing particles in 2D
left, bottom, width, height = [0.21, 0.5, 0.2, 0.2]
axInset = hF3.add_axes([left, bottom, width, height], zorder=1)

# Creation of an XY grid to represent a Gaussian function
AltoG = 2 * np.pi * numPart
xVals = np.linspace(-maxV_X, maxV_X)
yVals = np.linspace(-maxV_Y, maxV_Y)
Xga, Yga = np.meshgrid(xVals, yVals)

# Loop that updates the positions as time passes...
for indLoop in range(startPlot, numSteps, 10):
    # Values needed inside the loop
    xPos_N = randVect_X[:, indLoop]
    yPos_N = randVect_Y[:, indLoop]
    sigma_x = randSquareMean_X[indLoop]
    sigma_y = randSquareMean_Y[indLoop]
    x_mean = randMean_X[indLoop]
    y_mean = randMean_Y[indLoop]

    # Now paint the Gaussian that explains the distribution with time
    Zga = (AltoG * np.exp(-((Xga - x_mean) ** 2 / (2 * sigma_x ** 2)) - ((Yga - y_mean) ** 2 /(2 * sigma_y ** 2)))) / (2 * np.pi * sigma_x * sigma_y)
    axP3.clear()
    hS = axP3.plot_surface(Xga, Yga, Zga, cmap='jet', linewidth=0, antialiased=False, alpha=0.4)
    axP3.view_init(20, 30)
    axP3.set_xlim(min(xVals), max(xVals))
    axP3.set_ylim(min(yVals), max(yVals))

    # Plot 2D positions of particles in the z=0 plane of the 3D figure
    axP3.scatter(xPos_N, yPos_N, np.zeros(numPart), marker='.', s=10)
    axP3.set_zlim([0, 0.6])  # Fixed Z Scale

    # Draw a circle with a radius of N sigma (using the Matplotlib add_patch function with a side of 2*R)
    circle = plt.Circle((x_mean, y_mean), N_Sigma * sigma_x, linestyle='--', linewidth=2, edgecolor='k', facecolor='none')
    axP3.add_patch(circle)
    art3d.pathpatch_2d_to_3d(circle, z=0, zdir="z")

    # Plot all particles in the inset Plot ...
    axInset.clear()
    axInset.scatter(xPos_N, yPos_N, marker='.', s=6)
    axInset.set_xlim(1.2*min(xVals), 1.2*max(xVals))
    axInset.set_ylim(1.2*min(yVals), 1.2*max(yVals))
    axInset.tick_params(axis='both', which='both', bottom=False, top=False, left=False, labelbottom=False, labelleft=False)

    # Plot the N-Sigma circle to show the expected distribution of the particles
    circle = plt.Circle((x_mean, y_mean), N_Sigma * sigma_x, linestyle='--', linewidth=2, edgecolor='k', facecolor='none')
    axInset.add_patch(circle)

    axP3.set_title(r'$\bf{Diffusion\ in\ 2D:}$ ' + str(numPart) + ' particles during ' + str(numSteps) + ' steps', y=0.96)
    plt.draw()
    plt.pause(0.2)

plt.show(block=False)
plt.pause(2)
