package com.ronsuo.ble;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.BitmapDrawable;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    TextView tvInfo;
    AlertDialog mScanDialog;
    ListView mDeviceList;
    DeviceListAdapter mDeviceListAdapter;
    SmartGloveService mSmartGloveService;
    TextView showData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvInfo = (TextView) findViewById(R.id.info);
        showData = (TextView) findViewById(R.id.showData);

        findViewById(R.id.scan).setOnClickListener(this);
        Intent gattServiceIntent = new Intent(this, SmartGloveService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);


        //NTEXT

//        byte hh = 0x21;
//        byte ll = (byte) 0xff;
//        int getByteH = 0,getByteL = 0;
//
//        getByteH = hh;
//        getByteH <<= 8;
//        getByteH = getByteH & 65280;
//
//        getByteL = ll;
//        getByteL = getByteL & 255;
//
//        getByteH += getByteL;
//        Log.d("NTEXT","从16进制转换10进制结果----->" + getByteH);
//        showData.setText(getByteH);




        //16进制直接输出结果与对应10进制
//        byte y = 0x00;
//        int getByte = 0;
//        final StringBuilder stringBuilder = new StringBuilder(256);
//        for (int i=0 ; i < 256 ; i++ , y++){
//            getByte = y;
//            getByte = getByte & 255;
//            stringBuilder.append(String.format("%02X ", y));
//            Log.i("CHANGE",i + "    " + y + "    " + stringBuilder.toString());
//            stringBuilder.delete(0,stringBuilder.length());
//        }



    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mSmartGloveService = ((SmartGloveService.LocalBinder) service).getService();
            if (mScanDialog != null) {
                mSmartGloveService.startLeScan(deviceListener);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSmartGloveService = null;
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.scan:
                popupDeviceList();
                break;
            case R.id.close:
                closeDeviceList();
                break;
        }
    }

    void closeDeviceList() {
        mDeviceListAdapter = null;
        mDeviceList = null;
        mScanDialog.dismiss();
        mScanDialog = null;
        if (mSmartGloveService != null) {
            mSmartGloveService.stopLeScan();
        }
    }

    void popupDeviceList() {
        View view = View.inflate(this, R.layout.popup_scan_list, null);
        view.findViewById(R.id.close).setOnClickListener(this);

        mDeviceList = (ListView) view.findViewById(R.id.device_list);
        mDeviceListAdapter = new DeviceListAdapter(this);
        mDeviceList.setAdapter(mDeviceListAdapter);
        mDeviceList.setOnItemClickListener(deviceClickListener);

        AlertDialog.Builder builder = new AlertDialog.Builder(this).setView(view).setCancelable(false);
        mScanDialog = builder.create();
        mScanDialog.show();

        int height = getWindow().getDecorView().getHeight() * 5 / 7;
        int width = getWindow().getDecorView().getWidth() * 7 / 8;
        mScanDialog.getWindow().setLayout(width, height);

        Log.i("Oxygen", "-----> " + mSmartGloveService);
        if (mSmartGloveService != null) {
            mSmartGloveService.startLeScan(deviceListener);
        }
    }

    SmartGloveService.DeviceListener deviceListener = new SmartGloveService.DeviceListener() {
        @Override
        public void onNewDevice(final String address) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mDeviceListAdapter != null)
                        mDeviceListAdapter.addDevice(address);
                }
            });

        }

        @Override
        public void onScanStateChanged(boolean scaning) {

        }
    };

    SmartGloveService.DataListener dataListener = new SmartGloveService.DataListener() {
        @Override
        public void onData(byte[] data) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for(byte byteChar : data) {
                stringBuilder.append(String.format("%02X ", byteChar));
            }
//            Log.i("HtoL", "----> data = " + stringBuilder.toString());
            Log.i("Oxygen", "----> data = " + stringBuilder.toString());


            double[] AData = new double[3];double Acceleration;
            int[] GData = new int[3];double G;
            int[] CData = new int[3];double C;
            int[] DData = new int[3];double D;


            int Turn1 = 0,Turn2 = 0;
            if (data[2] == 1){
                    //处理第一组数据数据
                for (Turn1 = 0;Turn1 < 2;Turn1++){
                    for (Turn2 = 0;Turn2 < 3;Turn2++){
                        if (Turn1 == 0){//磁力计
                            GData[Turn2] = HtoX(data[2 + Turn2*2 + 2], data[2 + Turn2*2  + 1]);
//                            Log.i("Acceleration","磁力计" + "  " + String.valueOf(GData[Turn2]));
                        }
                        else {//加速度
                            AData[Turn2] = HtoX(data[8 + Turn2*2 + 2], data[8 + Turn2*2  + 1]);
//                            Log.i("Acceleration","加速度" + "  " + String.valueOf(AData[Turn2]));
                        }
                    }
                }

                for (int temp = 0;temp < 3; temp++){
                    AData[temp] /= 16384;
                }
                Log.i("xyz","X轴 = " + AData[0] + "  " + "Y轴 = " + AData[1] +"  " +"Z轴 = " + AData[2]);
                Acceleration = Math.sqrt(AData[0]*AData[0] + AData[1]*AData[1] + AData[2]*AData[2]);
                
                Log.d("xyz", String.valueOf(Acceleration));

                G = Math.sqrt(GData[0]*GData[0] + GData[1]*GData[1] + GData[2]*GData[2]);
            }
            else {//处理第二组数据
                for (Turn1 = 0;Turn1 < 2;Turn1++){
                    for (Turn2 = 0;Turn2 < 3;Turn2++){
                        if (Turn1 == 0){
                            CData[Turn2] = HtoX(data[Turn2*2 + 2], data[2 + Turn2*2  + 1]);}
                        else {
                            DData[Turn2] = HtoX(data[6 + Turn2*2 + 2], data[8 + Turn2*2  + 1]);}
                    }
                }
                C = Math.sqrt(GData[0]*GData[0] + GData[1]*GData[1] + GData[2]*GData[2]);
                D = Math.sqrt(GData[0]*GData[0] + GData[1]*GData[1] + GData[2]*GData[2]);
            }


        }


        public int HtoX(byte hh,byte ll ){//把接收到的bytep[] data数据转换成10进制数
            int getByteH = 0,getByteL = 0;

            getByteH = hh;
            getByteH <<= 8;

            getByteL = ll;
            getByteL = getByteL & 255;
            getByteH += getByteL;



            //验证代码正确性
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(String.format("%02X ", hh));
            stringBuilder.append(String.format("%02X ", ll));
            Log.i("HtoX","接受的16进制数位  " + stringBuilder.toString() + "  " + getByteH);


            return getByteH;
        }

        @Override
        public void onConnectStateChanged(boolean connected) {

        }
    };

    AdapterView.OnItemClickListener deviceClickListener =  new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String address = (String)mDeviceListAdapter.getItem(position);
            closeDeviceList();
            mSmartGloveService.connectToDevice(address, dataListener);
        }
    };
}
