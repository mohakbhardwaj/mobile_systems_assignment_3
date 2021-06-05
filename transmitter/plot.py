#!/usr/bin/env python
import matplotlib.pyplot as plt




xdata = [20, 40, 60, 80]

bit_error_rate_05 = [0.0, 0.1, 0.3, 0.1]
bit_error_rate_01 = [0.0, 0.1, 0.4, 0.2]
data_rate_05 = [10.0/30.0, 9.0/30.0, 7.0/30.0, 9.0/30.0]
data_rate_01 = [10.0/30.0, 9.0/30.0, 6.0/30.0, 8.0/30.0]


fig, ax = plt.subplots()

ax.plot(xdata, bit_error_rate_05, marker='o', label=r'$\Delta \alpha = 0.5$')
ax.plot(xdata, bit_error_rate_01, marker='o', label=r'$\Delta \alpha = 0.1$')
ax.set_xlabel('Distance (cm)')
ax.set_ylabel('Bit Error Rate')
ax.legend()


fig2, ax2 = plt.subplots()

ax2.plot(xdata, data_rate_05, marker='o', label=r'$\Delta \alpha = 0.5$')
ax2.plot(xdata, data_rate_01, marker='o', label=r'$\Delta \alpha = 0.1$')
ax2.set_xlabel('Distance (cm)')
ax2.set_ylabel('Data Rate')
ax2.legend()



plt.show()