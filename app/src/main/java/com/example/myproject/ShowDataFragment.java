package com.example.myproject;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ShowDataFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ShowDataFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

    private Activity activity;

    /*Declare UIs*/
    LineChart chart;
    TextView tvheartrate,tvshowtime,tvspo2,tvhearttime,tvsdnn,tvres;
    ImageView img_restart;

    /*Initial paras*/
    ArrayList<String> heart_rate = new ArrayList<>();
    ArrayList<String> respiration_rate = new ArrayList<>();
    ArrayList<String> spo2 = new ArrayList<>();
    ArrayList<String> heartratetime = new ArrayList<>();
    ArrayList<String> breathtime = new ArrayList<>();
    String sdnn = "";
    String date = "";

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ShowDataFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ShowDataFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ShowDataFragment newInstance(String param1, String param2) {
        ShowDataFragment fragment = new ShowDataFragment();
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
        activity.getWindow().setBackgroundDrawableResource(R.drawable.getdatabg);
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
        return inflater.inflate(R.layout.fragment_show_data, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view,savedInstanceState);

        /*Find UIs*/
        chart = view.findViewById(R.id.lineChart);
        tvheartrate = view.findViewById(R.id.textView_heartrate);
        tvshowtime = view.findViewById(R.id.textView_showtime);
        tvspo2 = view.findViewById(R.id.textView_spo2);
        tvhearttime = view.findViewById(R.id.textView_hrtime);
        tvsdnn = view.findViewById(R.id.textView_sdnn);
        img_restart = view.findViewById(R.id.img_restart);
        tvres = view.findViewById(R.id.textView_res);


        /*取得Bundle*/
        Bundle bundle = getArguments();
        if(bundle != null){
            heart_rate = bundle.getStringArrayList("heartrate");
            respiration_rate = bundle.getStringArrayList("respirationrate");
            spo2 = bundle.getStringArrayList("spo2");
            heartratetime = bundle.getStringArrayList("heartratetime");
            sdnn = bundle.getString("sdnn");
            breathtime = bundle.getStringArrayList("breathtime");
            date = bundle.getString("date");

            /*k=bundle.getString("k");*/

            tvhearttime.setText(""+heartratetime);
            tvsdnn.setText(""+sdnn);
            /*tvres.setText(""+k);*/
            tvshowtime.setText("Date : "+date);

            img_restart.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_showDataFragment_to_startFragment));
        }


        /**載入圖表*/
        initChart();

        /**跑圖*/
        startRun();
        showheartrate();
        //showhrtime();
        showspo2();
        showbreath();
    }

    public float indexvalue_hr, indexvalue_res,indexvalue_time;
    public long hr_time;

    public void showheartrate(){
        new Thread(() -> {
            /*SystemClock.sleep(1000);*/
            for (int i = 0; i < heart_rate.size(); i++) {
                indexvalue_hr = Float.parseFloat(heart_rate.get(i));
                hr_time = 50000/heart_rate.size();
                /**有要在UI上面顯示的內容的話，就必須使用runOnUiThread!*/
                getActivity().runOnUiThread(() -> {
                    tvheartrate.setText(""+indexvalue_hr);
                });
                SystemClock.sleep(hr_time);
            }
        }).start();
    }

    public void showhrtime(){
        new Thread(() -> {
            /*SystemClock.sleep(1000);*/
            for (int i = 0; i < heartratetime.size(); i++) {
                indexvalue_time = Float.parseFloat(heartratetime.get(i));
                /**有要在UI上面顯示的內容的話，就必須使用runOnUiThread!*/
                getActivity().runOnUiThread(() -> {
                    tvshowtime.setText(date+"+"+indexvalue_time);
                });
                SystemClock.sleep(hr_time);
            }
        }).start();
    }

    public void showspo2(){
        new Thread(() -> {
            /*SystemClock.sleep(1000);*/
            for (int i = 0; i < spo2.size(); i++) {
                float index = Float.parseFloat(spo2.get(i));
                /**有要在UI上面顯示的內容的話，就必須使用runOnUiThread!*/
                getActivity().runOnUiThread(() -> {
                    tvspo2.setText(""+index);
                });
                SystemClock.sleep(5000);
            }
        }).start();
    }

    public void showbreath(){
        new Thread(() -> {
            /*SystemClock.sleep(1000);*/
            for (int i = 0; i < breathtime.size(); i++) {
                float index = Float.parseFloat(breathtime.get(i));
                /**有要在UI上面顯示的內容的話，就必須使用runOnUiThread!*/
                getActivity().runOnUiThread(() -> {
                    tvres.setText(""+index);
                });
                SystemClock.sleep(5000);
            }
        }).start();
    }

    /**開始跑圖表*/
    private void startRun(){

        new Thread(() -> {
            /*SystemClock.sleep(1000);*/
            for (int i = 0; i < respiration_rate.size(); i++) {
                indexvalue_res = Float.parseFloat(respiration_rate.get(i));
                /*有要在UI上面顯示的內容的話，就必須使用runOnUiThread!*/
                getActivity().runOnUiThread(() -> {
                    addData(indexvalue_res);
                });
                SystemClock.sleep(25);
            }
        }).start();
    }
    /**載入圖表*/
    private void initChart(){
        chart.getDescription().setEnabled(false);//設置不要圖表標籤
        chart.setTouchEnabled(true);//設置不可觸碰
        chart.setDragEnabled(true);//設置不可互動
        chart.setPinchZoom(false);
        //設置單一線數據
        LineData data = new LineData();
        data.setValueTextColor(Color.BLACK);
        chart.setData(data);
        //設置左下角標籤
        Legend l =  chart.getLegend();
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.BLACK);

        //設置Ｘ軸
        XAxis x =  chart.getXAxis();
        /*x.setTextColor(Color.BLACK);*/
        x.setDrawGridLines(false);//畫X軸線
        x.setPosition(XAxis.XAxisPosition.BOTTOM);//把標籤放底部
        //設置Y軸
        YAxis y = chart.getAxisLeft();
        y.setTextColor(Color.BLACK);
        y.setDrawGridLines(false);
        y.setAxisMaximum(200);
        y.setAxisMinimum(-200);//最低0*/
        chart.getAxisRight().setEnabled(false);//右邊Y軸不可視
    }
    /**新增資料*/
    private void addData(float inputData){
        LineData data =  chart.getData();//取得原數據
        float y_max = indexvalue_res+200, y_min = indexvalue_res-200;
        ILineDataSet set = data.getDataSetByIndex(0);//取得曲線(因為只有一條，故為0，若有多條則需指定)
        if (set == null){
            set = createSet();
            data.addDataSet(set);//第一次跑需要載入數據
        }
        data.addEntry(new Entry(set.getEntryCount(),inputData),0);//新增數據點
        data.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.setVisibleXRange(0,249);//設置可見範圍
        chart.moveViewToX(data.getEntryCount());//將可視焦點放在最新一個數據，使圖表可移動
        chart.getAxisLeft().resetAxisMaximum();
        chart.getAxisLeft().setAxisMaximum(Math.round(y_max));
        chart.getAxisLeft().resetAxisMinimum();
        chart.getAxisLeft().setAxisMinimum(Math.round(y_min));//最低0
    }
    /**設置數據線的樣式*/
    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Respiration Rate");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.GRAY);
        set.setLineWidth(2);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        return set;
    }
}