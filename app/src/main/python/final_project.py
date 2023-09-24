import numpy as np
from scipy import signal
import pandas as pd
from scipy.interpolate import interp1d

def heartrate(input):
    Gdata = np.array(input)
    Gdata = Gdata[100:2000]
    lens = len(Gdata)
    # bandpass
    fs = 100
    lowcut = 0.5
    highcut = 10
    low = lowcut/(fs/2)
    high = highcut/(fs/2)
    order = 6
    ACbuffer1, ACbuffer2 = signal.butter(
        order, [low, high], 'bandpass', analog=False)
    G = signal.filtfilt(ACbuffer1, ACbuffer2, Gdata, axis=0)
    # FFT
    cutsecond = 3
    cut = int(cutsecond/(1/fs))
    fftG = G[0:cut]
    Glen = len(fftG)
    Gfstep = fs/Glen
    f = np.linspace(0, (Glen-1)*Gfstep, Glen)
    GdataFFT = np.fft.fft(fftG)
    GdataFFT = abs(GdataFFT)/Glen
    G_fplot = f[0:int(Glen/2+1)]
    G_yplot = 2*GdataFFT[0:int(Glen/2+1)]
    Glocal = max(G_yplot)
    Gpeaks = signal.find_peaks(G_yplot, height=Glocal)
    Fmaximum = Gpeaks[0]

    # use window calculate heart rate---------------------------------------------------------------
    length = len(Gdata)
    s = length/fs
    w = ((1/float(G_fplot[Fmaximum]))*1.2)*(length/s)
    left = 0
    heart_rate = []
    storepeak = []
    IRstorepeak = []
    space = []
    key = 0  # 讓心率從第一個波峰開始算，而不是從第0筆資料
    averg = 0
    n = 2
    while left+w < length:
        # 綠光
        aa = left
        if (key == 0):
            wave = G[0:int(w)]
            highest = max(wave)
            ind = np.where(wave == highest)
            left = ind[0][0]
        elif (w > 2 and key == 1):
            wave = G[int(left+(w/2)):int(left+w)]
            highest = max(wave)
            ind = np.where(wave == highest)
            left = left+ind[0][0]+(w/2)
        storepeak.append(int(left))
        if (len(space) >= n):
            averg = space[n-1]-space[0]
            w = (averg/(n-1))*1.2
            for p in range(0, n-1):
                origin = space[p]
                space[p] = space[p+1]
                space[p+1] = origin
            space[n-1] = int(left)
        else:
            space.append(int(left))
        averg = 0
        bb = left
        if (key == 1):
            heart_rate_store = 60/((bb-aa)*(s/length))
            heart_rate.append(int(heart_rate_store))
        key = 1

    # 呼吸率------------------------------------------------------------------------
    q_u = np.zeros(G.shape)
    u_x = [0, ]
    u_y = [G[0], ]

    for i in range(0, len(storepeak)):
        u_x.append(storepeak[i])
        u_y.append(G[storepeak[i]])
    u_p = interp1d(u_x, u_y, kind='cubic', bounds_error=False, fill_value=0.0)
    for k in range(0, storepeak[len(storepeak)-1]):
        q_u[k] = u_p(k)

    rlowcut = 0.1
    rhighcut = 0.8
    rlow = rlowcut/(fs/2)
    rhigh = rhighcut/(fs/2)
    r_order = 3
    rb, ra = signal.butter(r_order, [rlow, rhigh], 'bandpass', analog=False)
    r = signal.filtfilt(rb, ra, q_u, axis=0)
    q_len = np.arange(1, len(q_u)+1)
    return heart_rate,  r
