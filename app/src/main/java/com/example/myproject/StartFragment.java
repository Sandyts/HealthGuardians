package com.example.myproject;

import android.app.Activity;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.navigation.Navigation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StartFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StartFragment extends Fragment {

    private Activity activity;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private USBStatus status = new USBStatus();
    public static final String TAG = MainActivity.class.getSimpleName() + "My";
    private static final String USB_PERMISSION = "USB_Demo";
    List<UsbSerialDriver> drivers;
    UsbManager manager;

    private CountDownTimer countdowntimer;
    TextView tvusb,tvRes,tvTimer;
    ScrollView scview;
    ConstraintLayout StartLY, DataLY;
    ImageView imgstart,img_usbdis,imgload,imgheart,imgheart1,imgheart2;
    public int TimerCounter = 2;


    int[] alllight = new int[6000];
    ArrayList<String> heart_rate = new ArrayList<>();
    ArrayList<String> respiration_rate = new ArrayList<>();
    ArrayList<String> spo2 = new ArrayList<>();
    ArrayList<String> heartratetime = new ArrayList<>();
    ArrayList<String> breathtime = new ArrayList<>();
    String sdnn = "";
    String date = "";

    int datanum = 0;
    String[] SaveData = new String[6000];

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public StartFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment StartFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static StartFragment newInstance(String param1, String param2) {
        StartFragment fragment = new StartFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        /**測試呼叫Python function*/
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(activity));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_start, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        /*註冊廣播*/
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(USB_PERMISSION);
        getActivity().registerReceiver(status, filter);

        /*Find UIs*/
        tvusb = view.findViewById(R.id.textView_Respond);
        tvRes = view.findViewById(R.id.textView_Respond);
        tvTimer = view.findViewById(R.id.tv_Timer);
        scview = view.findViewById(R.id.scroll_view);
        StartLY = view.findViewById(R.id.StartLayout);
        DataLY = view.findViewById(R.id.GetDataLayout);
        imgstart = view.findViewById(R.id.imgstart);
        imgload = view.findViewById(R.id.img_loading);
        img_usbdis = view.findViewById(R.id.img_usbdisconnect);
        imgheart = view.findViewById(R.id.img_heart);
        imgheart1 = view.findViewById(R.id.img_heart1);
        imgheart2 = view.findViewById(R.id.img_heart2);
        imgheart.setVisibility(view.INVISIBLE);
        imgheart1.setVisibility(view.INVISIBLE);
        imgheart2.setVisibility(view.INVISIBLE);

        datanum = 0;

        /*設定初始畫面*/
        StartLY.setVisibility(View.VISIBLE);
        DataLY.setVisibility(View.INVISIBLE);

        /**偵測是否正在有裝置插入*/
        detectUSB();

        imgstart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss",
                        Locale.getDefault()).format(System.currentTimeMillis());
                /*對裝置送出指令*/
                TimerCountDown();
                StartLY.setVisibility(View.INVISIBLE);
                DataLY.setVisibility(View.VISIBLE);
                new Handler().postDelayed(() -> {
                    // do what you want after 1000 miliseconds
                    sendValue(drivers);
                }, 500);
            }

            public void TimerCountDown() {
                countdowntimer = new CountDownTimer(21000,500) {
                    @Override
                    public void onTick(long l) {
                        tvTimer.setText(""+l/1000);
                        TimerCounter++;
                        switch (TimerCounter/4){
                            case 2:
                                imgload.setImageResource(R.drawable.count1);
                                break;
                            case 3:
                                imgload.setImageResource(R.drawable.count2);
                                break;
                            case 4:
                                imgload.setImageResource(R.drawable.count3);
                                break;
                            case 5:
                                imgload.setImageResource(R.drawable.count4);
                                break;
                            case 6:
                                imgload.setImageResource(R.drawable.count5);
                                break;
                            case 7:
                                imgload.setImageResource(R.drawable.count6);
                                break;
                            case 8:
                                imgload.setImageResource(R.drawable.count7);
                                break;
                            case 9:
                                imgload.setImageResource(R.drawable.count8);
                                break;
                            case 10:
                                imgload.setImageResource(R.drawable.count9);
                                break;
                            case 11:
                                imgload.setImageResource(R.drawable.count10);
                                break;
                        }
                        /**計時動畫*/
                        switch((TimerCounter-4)%14){
                        case 0:
                            imgheart.setVisibility(view.VISIBLE);
                            imgheart.setImageResource(R.drawable.emptyheart);
                            break;
                        case 1:
                            imgheart1.setVisibility(view.VISIBLE);
                            imgheart1.setImageResource(R.drawable.emptyheart);
                            break;
                        case 2:
                            imgheart2.setVisibility(view.VISIBLE);
                            imgheart2.setImageResource(R.drawable.emptyheart);
                            break;
                        case 3:
                            imgheart.setImageResource(R.drawable.fullheart);
                            break;
                        case 4:
                            imgheart1.setImageResource(R.drawable.fullheart);
                            break;
                        case 5:
                            imgheart2.setImageResource(R.drawable.fullheart);
                            break;
                        case 6:
                            imgheart.setImageResource(R.drawable.fullheart);
                            imgheart1.setImageResource(R.drawable.emptyheart);
                            imgheart2.setImageResource(R.drawable.emptyheart);
                            break;
                        case 7:
                            imgheart.setImageResource(R.drawable.emptyheart);
                            imgheart1.setImageResource(R.drawable.fullheart);
                            imgheart2.setImageResource(R.drawable.emptyheart);
                            break;
                        case 8:
                            imgheart.setImageResource(R.drawable.emptyheart);
                            imgheart1.setImageResource(R.drawable.emptyheart);
                            imgheart2.setImageResource(R.drawable.fullheart);
                            break;
                        case 9:
                            imgheart.setImageResource(R.drawable.fullheart);
                            imgheart1.setImageResource(R.drawable.fullheart);
                            imgheart2.setImageResource(R.drawable.fullheart);
                            break;
                        case 10:
                            imgheart.getLayoutParams().width = 195;
                            imgheart1.getLayoutParams().width = 195;
                            imgheart2.getLayoutParams().width = 195;
                            break;
                        case 11:
                            imgheart.getLayoutParams().width = 192;
                            imgheart1.getLayoutParams().width = 192;
                            imgheart2.getLayoutParams().width = 192;
                            break;
                        case 12:
                            imgheart.getLayoutParams().width = 197;
                            imgheart1.getLayoutParams().width = 197;
                            imgheart2.getLayoutParams().width = 197;
                            break;
                        case 13:
                            imgheart.getLayoutParams().width = 190;
                            imgheart1.getLayoutParams().width = 190;
                            imgheart2.getLayoutParams().width = 190;
                            break;
                        case 14:
                            imgheart.getLayoutParams().width = 193;
                            imgheart1.getLayoutParams().width = 193;
                            imgheart2.getLayoutParams().width = 193;
                            break;
                        }
                    }

                    @Override
                    public void onFinish() {
                        sendValuetopy();
                        makeCSV();
                        scview.setVisibility(View.INVISIBLE);
                        tvTimer.setVisibility(View.INVISIBLE);

                        /**計時到跳轉畫面*/
                        /*建Bundle放資料給2nd Activity*/
                        Bundle bundle = new Bundle();
                        bundle.putStringArrayList("heartrate",heart_rate);
                        bundle.putStringArrayList("respirationrate",respiration_rate);
                        bundle.putStringArrayList("spo2",spo2);
                        bundle.putStringArrayList("heartratetime",heartratetime);
                        bundle.putString("sdnn",sdnn);
                        /*bundle.putString("k",k);*/
                        bundle.putStringArrayList("breathtime",breathtime);

                        bundle.putString("date",date);
                        /*將bundle交給intent*/
                        Navigation.findNavController(view).navigate(R.id.action_startFragment_to_showDataFragment, bundle);
                    }
                }.start();
            }
        });

        /**取得讀寫權限*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            getActivity().requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        /*反註冊廣播*/
        getActivity().unregisterReceiver(status);
    }

    private class USBStatus extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                /**當Check完授權狀態後進入此處*/
                case USB_PERMISSION:
                    if (drivers.size() == 0) return;
                    boolean hasPermission = manager.hasPermission(drivers.get(0).getDevice());
                    /*tvusb.setText("授權狀態: " + hasPermission);*/
                    img_usbdis.setImageResource(R.drawable.usbconnected);
                    if (!hasPermission) {
                        getPermission(drivers);
                        return;
                    }
                    Toast.makeText(context, "已獲取權限", Toast.LENGTH_SHORT).show();
                    img_usbdis.setImageResource(R.drawable.usbconnected);
                    break;
                /**偵測USB裝置插入*/
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    Toast.makeText(context, "USB裝置插入", Toast.LENGTH_SHORT).show();
                    img_usbdis.setImageResource(R.drawable.usbconnected);
                    detectUSB();
                    break;
                /**偵測USB裝置拔出*/
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    Toast.makeText(context, "USB裝置拔出", Toast.LENGTH_SHORT).show();
                    img_usbdis.setImageResource(R.drawable.usbdisconnect);
                    /*tvusb.setText("授權狀態: false");*/
                    break;
            }
        }
    }

    /**偵測裝置*/
    private void detectUSB() {
        manager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);
        if (manager == null) return;
        if (manager.getDeviceList().size() == 0)return;
        tvusb.setText("授權狀態: false");
        /*取得目前插在USB-OTG上的裝置*/
        drivers = getDeviceInfo();
        /*確認使用者是否有同意使用OTG(權限)*/
        getPermission(drivers);
    }
    /**取得目前插在USB-OTG上的裝置列表，並取得"第一個"裝置的資訊*/
    private List<UsbSerialDriver> getDeviceInfo() {
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Log.d(TAG, "裝置資訊列表:\n " + deviceList);
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        ProbeTable customTable = new ProbeTable();
        List<UsbSerialDriver> drivers = null;
        String info = "";
        while (deviceIterator.hasNext()) {
            /*取得裝置資訊*/
            UsbDevice device = deviceIterator.next();
            /*info = "Vendor ID: " + device.getVendorId()
                    + "\nProduct Id: " + device.getDeviceId()
                    + "\nManufacturerName: " + device.getManufacturerName()
                    + "\nProduceName: " + device.getProductName();*/
            /*設置驅動*/
            customTable.addProduct(
                    device.getVendorId(),
                    device.getProductId(),
                    CdcAcmSerialDriver.class
                    /*我的設備Diver是CDC，另有
                     * CP21XX, CH34X, FTDI, Prolific 等等可以使用*/
            );
            /*將驅動綁定給此裝置*/
            UsbSerialProber prober = new UsbSerialProber(customTable);
            drivers = prober.findAllDrivers(manager);
        }
        /*更新UI*/
        tvusb.setText(info);
        return drivers;
    }
    /**確認OTG使用權限，此處為顯示詢問框*/
    private void getPermission(List<UsbSerialDriver> drivers) {
        if (PendingIntent.getBroadcast(activity, 0, new Intent(USB_PERMISSION), 0) != null) {
            manager.requestPermission(drivers.get(0).getDevice(), PendingIntent.getBroadcast(
                    activity, 0, new Intent(USB_PERMISSION), 0)); }
    }
    /**送出資訊*/
    private void sendValue(List<UsbSerialDriver> drivers) {
        if (drivers == null) return;
        /*初始化整個發送流程*/
        UsbDeviceConnection connect = manager.openDevice(drivers.get(0).getDevice());
        /*取得此USB裝置的PORT*/
        UsbSerialPort port = drivers.get(0).getPorts().get(0);
        try {
            /*開啟port*/
            port.open(connect);
            /*取得要發送的字串*/
            /*EditText edInput = findViewById(R.id.editText_Input);
            String s = edInput.getText().toString();*/
            String s = "Start";
            if (s.length() == 0) return;
            /*設定胞率、資料長度、停止位元、檢查位元*/
            port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            /*寫出資訊*/
            port.write(s.getBytes(), 200);
            /*設置回傳執行緒*/
            SerialInputOutputManager.Listener serialInputOutputManager = getRespond;
            SerialInputOutputManager sL = new SerialInputOutputManager(port, serialInputOutputManager);
            ExecutorService service = Executors.newSingleThreadExecutor();
            service.submit(sL);
        } catch (IOException e) {
            try {
                /*如果Port是開啟狀態，則關閉；再使用遞迴法重複呼叫並嘗試*/
                port.close();
                sendValue(drivers);
            } catch (IOException ex) {
                ex.printStackTrace();
                Log.e(TAG, "送出失敗，原因: " + ex);
            }
        }
    }


    /**接收回傳*/
    private SerialInputOutputManager.Listener getRespond = new SerialInputOutputManager.Listener() {
        @Override
        public void onNewData(byte[] data) {
            String res = new String(data);
            SaveData[datanum] = new String(data);
            datanum++;
            String print_String = "";
            int counter = 0;

            String S_spilt[] = res.split(",");
            for (String s: S_spilt) {
                switch(counter){
                    case 0:
                        print_String = print_String + "綠光:" + s +"   " ;
                        alllight[datanum-1]= Integer.parseInt(s);
                        break;
                    case 1:
                        print_String = print_String + "紅外光:" + s+"   " ;
                        alllight[datanum+1999]= Integer.parseInt(s);
                        break;
                    case 2:
                        print_String = print_String + "紅光:" + s+"   " ;
                        alllight[datanum+3999]= Integer.parseInt(s);
                        break;
                }
                counter++;
            }

            String finalPrint_String = print_String;
            getActivity().runOnUiThread(() -> {
                tvRes.append(finalPrint_String+"\n");
                scview.fullScroll(ScrollView.FOCUS_DOWN);
            });
        }

        @Override
        public void onRunError(Exception e) {
        }
    };

    public void sendValuetopy(){
        Python py = Python.getInstance();
        PyObject pyobj = py.getModule("forphone");
        float[][] hrnpeak = pyobj.callAttr("heartratebloodoxygen",alllight).toJava(float[][].class);

        /*System.arraycopy(hrnpeak[0],0,arrheart_rate,0,hrnpeak[0].length);
        System.arraycopy(hrnpeak[1],0,arrstore_peak,0,hrnpeak[1].length);*/


        /**spilt 2d array by for loop*/
        for(int hr =0; hr<hrnpeak[0].length;hr++)
        {
            heart_rate.add(""+hrnpeak[0][hr]);
        }
        for(int pk =0; pk<hrnpeak[1].length;pk++)
        {
            respiration_rate.add(""+hrnpeak[1][pk]);
        }
        for(int sp =0; sp<hrnpeak[2].length;sp++)
        {
            spo2.add(""+hrnpeak[2][sp]);
        }
        for(int ht =0; ht<hrnpeak[3].length;ht++)
        {
            heartratetime.add(""+hrnpeak[3][ht]);
        }
        sdnn = ""+hrnpeak[4][0];
        /*k=""+hrnpeak[5][0];*/
        for(int bt =0; bt<hrnpeak[5].length;bt++)
        {
            breathtime.add(""+Math.round(hrnpeak[5][bt]));
        }
        /*PyObject pyenv = pyobj.callAttr("EnvelopeDetector",alllight);*/
        /*ptitle.setText("心率"+heart_rate+" | 呼吸率"+respiration_rate);*/
    }



    /**產生CSV檔*/
    public void makeCSV() {
        new Thread(() -> {
            /**決定檔案名稱*/
            String date = new SimpleDateFormat("yyyy-MM-dd-HH:mm",
                    Locale.getDefault()).format(System.currentTimeMillis());
            String fileName = date + ".csv";
            final String[] filepath = {""};

            /**撰寫內容*/
    //以下用詞：直行橫列
            StringBuffer csvText = new StringBuffer();

            //設置其餘內容，共15行
            for (int i = 0; i < 2000; i++) {
                csvText.append(SaveData[i]+"\n");
                //此處迴圈為設置每一列的內容
            }




            getActivity().runOnUiThread(() -> {
                try {
                    //->遇上exposed beyond app through ClipData.Item.getUri() 錯誤時在onCreate加上這行
                    StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                    StrictMode.setVmPolicy(builder.build());
                    builder.detectFileUriExposure();
                    //->遇上exposed beyond app through ClipData.Item.getUri() 錯誤時在onCreate加上這行
                    FileOutputStream out = getActivity().openFileOutput(fileName, Context.MODE_PRIVATE);
                    out.write((csvText.toString().getBytes()));
                    out.close();
                    File fileLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), fileName);
                    FileOutputStream fos = new FileOutputStream(fileLocation);
                    fos.write(csvText.toString().getBytes());
                    Uri path = Uri.fromFile(fileLocation);
                    Intent fileIntent = new Intent(Intent.ACTION_SEND);
                    fileIntent.setType("text/csv");
                    fileIntent.putExtra(Intent.EXTRA_SUBJECT, fileName);
                    fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    fileIntent.putExtra(Intent.EXTRA_STREAM, path);
                    startActivity(Intent.createChooser(fileIntent, "輸出檔案"));
                } catch (IOException e) {
                    e.printStackTrace();
                    /*Log.w(TAG, "makeCSV: "+e.toString());*/
                }
            });
        }).start();
    }
}