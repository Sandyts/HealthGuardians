import numpy as np
from scipy import signal
import pandas as pd
from scipy.interpolate import interp1d


def heartratebloodoxygen(data):
    data = np.array(data)
    Gdata = data[100:2000]
    IRdata = data[2100:4000]
    Rdata = data[4100:6000]
    # bandpass
    fs = 100
    lowcut = 0.5
    highcut = 4
    low = lowcut/(fs/2)
    high = highcut/(fs/2)
    order = 5
    ACbuffer1, ACbuffer2 = signal.butter(
        order, [low, high], 'bandpass', analog=False)
    Rac = signal.filtfilt(ACbuffer1, ACbuffer2, Rdata, axis=0)
    IRac = signal.filtfilt(ACbuffer1, ACbuffer2, IRdata, axis=0)
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
    w = ((1/float(G_fplot[Fmaximum]))*1.25)*(length/s)
    PPI = []
    left = 0
    left2 = 0
    left3 = 0
    heart_rate = []
    storepeak = []
    Rstorepeak = []
    IRstorepeak = []
    space = []
    key = 0  # 讓心率從第一個波峰開始算，而不是從第0筆資料
    averg = 0
    n = 10
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
        # 紅光波峰
        if (left2+w < length):
            #left2 = aa
            if (key == 0):
                wave2 = Rac[:int(w)]
                highest2 = max(wave2)
                ind2 = np.where(wave2 == highest2)
                left2 = ind2[0][0]
            elif (w > 2 and key == 1):
                wave2 = Rac[int(left2+(w/2)):int(left2+w)]
                highest2 = max(wave2)
                ind2 = np.where(wave2 == highest2)
                left2 = left2+ind2[0][0]+(w/2)
            Rstorepeak.append(int(left2))
        # 紅外光波峰
        if (left3+w < length):
            #left3 = aa
            if (key == 0):
                wave3 = IRac[:int(w)]
                highest3 = max(wave3)
                ind3 = np.where(wave3 == highest3)
                left3 = ind3[0][0]
            elif (w > 2 and key == 1):
                wave3 = IRac[int(left3+(w/2)):int(left3+w)]
                highest3 = max(wave3)
                ind3 = np.where(wave3 == highest3)
                left3 = left3+ind3[0][0]+(w/2)
            IRstorepeak.append(int(left3))
        # 把波峰取n個,然後將n個波峰形成的視窗平均成新的w
        if (len(space) >= n):
            averg = space[n-1]-space[0]
            w = (averg/(n-1))*1.25
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
            PPI.append((bb-aa)*(s/length))
            heart_rate_store = 60/((bb-aa)*(s/length))
            heart_rate.append(int(heart_rate_store))
        key = 1
    x = 5
    newheart_rate = []
    # 判斷最大跟最小的心率刪掉,把剩下的平均
    for i in range(0, heart_rate.count(max(heart_rate))):
        heart_rate.remove(max(heart_rate))
    for i in range(0, heart_rate.count(min(heart_rate))):
        heart_rate.remove(min(heart_rate))
    p = []
    for j in range(0, len(heart_rate)-x+1):
        newheart_rate.append(np.round(np.mean(heart_rate[j:j+x])))
    p = heart_rate[len(heart_rate)-x+1:len(heart_rate)]
    for j in range(0, x-1):
        newheart_rate.append(np.round(np.mean(p[j:x])))
    # 血氧---------------------------------------------------------------------------
    w2 = ((1/float(G_fplot[Fmaximum]))*1.4)*(length/s)
    left4 = 0
    Rstoretrough = []
    IRstoretrough = []
    space2 = []
    averg = 0
    key = 0
    while int(left4+w2) < length:
        # 紅光波谷
        if (key == 0):
            wave4 = Rac[:int(w2)]
            highest4 = min(wave4)
            ind4 = np.where(wave4 == highest4)
            left4 = ind4[0][0]
        elif (w2 > 2 and key == 1):
            wave4 = Rac[int(left4+(w2/2)):int(left4+w2)]
            highest4 = min(wave4)
            ind4 = np.where(wave4 == highest4)
            left4 = left4+ind4[0][0]+(w2/2)
        Rstoretrough.append(int(left4))
        if (len(space2) >= n):
            averg = space2[n-1]-space2[0]
            w2 = (averg/(n-1))*1.25
            for p in range(0, n-1):
                origin = space2[p]
                space2[p] = space2[p+1]
                space2[p+1] = origin
            space2[n-1] = int(left4)
        else:
            space2.append(int(left4))
        averg = 0
        key = 1
    IRstoretrough = Rstoretrough
   # DC
    DCcut = 0.1
    DCcut = DCcut/(fs/2)
    order = 5
    DCbuffer1, DCbuffer2 = signal.butter(order, DCcut, 'lowpass', analog=False)
    Rdc = signal.filtfilt(DCbuffer1, DCbuffer2, Rdata, axis=0)
    IRdc = signal.filtfilt(DCbuffer1, DCbuffer2, IRdata, axis=0)

    # Take the 8 amplitude frequency average of IRac
    stack = 0
    IRac_amp = []
    averagetimes = 8
    r1 = min(len(IRstoretrough), len(IRstorepeak))
    for i in range(0, r1):
        stack += IRac[IRstorepeak[i]]-IRac[IRstoretrough[i]]
        if (((i+1) % averagetimes) == 0):
            IRac_amp.append(stack/averagetimes)
            stack = 0
        elif (i == (r1-1)):
            IRac_amp.append(stack/(r1 % averagetimes))

    stack = 0
    Rac_amp = []
    r2 = min(len(Rstoretrough), len(Rstorepeak))
    for i in range(0, r2):
        stack += Rac[Rstorepeak[i]]-Rac[Rstoretrough[i]]
        if (((i+1) % averagetimes) == 0):
            Rac_amp.append(stack/averagetimes)
            stack = 0
        elif (i == (r2-1)):
            Rac_amp.append(stack/(r2 % averagetimes))
    Rdctotal = sum(Rdc)/len(Rdc)
    IRdctotal = sum(IRdc)/len(IRdc)
    Rate = np.zeros(min(len(Rac_amp), len(IRac_amp)))

    for i in range(0, min(len(Rac_amp), len(IRac_amp))):
        Rate[i] = (Rac_amp[i]/Rdctotal)/(IRac_amp[i]/IRdctotal)

    SpO2 = []

    SpO2 = np.round(-12.32356415*(Rate**2) + 17.37495538*Rate + 91.72070473)

    # 呼吸率------------------------------------------------------------------------
    q_u = np.zeros(storepeak[len(storepeak)-1])
    u_x = [0, ]
    u_y = [G[0], ]

    for i in range(0, len(storepeak)):
        u_x.append(storepeak[i])
        u_y.append(G[storepeak[i]])
    u_p = interp1d(u_x, u_y, kind='cubic', bounds_error=False, fill_value=0.0)
    for k in range(0, storepeak[len(storepeak)-1]):
        q_u[k] = u_p(k)
    q_u=q_u[storepeak[0]:]

    rlowcut = 0.1
    rhighcut = 0.6
    rlow = rlowcut/(fs/2)
    rhigh = rhighcut/(fs/2)
    r_order = 3
    rb, ra = signal.butter(r_order, [rlow, rhigh], 'bandpass', analog=False)
    r = signal.filtfilt(rb, ra, q_u, axis=0)
    r=r*3

    rlen = len(r)
    rfstep = fs/rlen
    f = np.linspace(0, (rlen-1)*rfstep, rlen)
    rdataFFT = np.fft.fft(r)
    dataFFT = abs(rdataFFT)/rlen
    r_fplot = f[0:int(rlen/2+1)]
    r_yplot = 2*dataFFT[0:int(rlen/2+1)]
    rlocal = max(r_yplot)
    rpeaks = signal.find_peaks(r_yplot, height=rlocal)
    r_Fmaximum = rpeaks[0]

    k = 0
    a = np.diff(r)
    for i in range(0, len(a)-1):
        if (a[i] > a[i+1] and a[i]*a[i+1] < 0):
            k += 1
    k=float(k)
    

    breath_times = 60/(1/r_fplot[r_Fmaximum])
    # 心率的時間戳記
    heartrate_time = np.divide(storepeak[1:], 100)
    # SDNN
    for i in range(0, PPI.count(max(PPI))):
        PPI.remove(max(PPI))
    for i in range(0, PPI.count(min(PPI))):
        PPI.remove(min(PPI))
    PPI = np.array(PPI)
    sdnn = [np.std(PPI, ddof=0, dtype=float)*1000]
    return newheart_rate, r, SpO2, heartrate_time, sdnn, breath_times