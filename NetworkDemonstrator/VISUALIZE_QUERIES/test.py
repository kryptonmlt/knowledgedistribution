import matplotlib.pyplot as plt
from itertools import cycle
lines = ["-","--","-.",":"]
linecycler = cycle(lines)
plt.figure()
for i in range(10):
    x = range(i,i+10)
    plt.plot(range(10),x,next(linecycler),label="")
plt.show()