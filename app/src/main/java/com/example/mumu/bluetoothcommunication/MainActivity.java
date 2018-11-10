package com.example.mumu.bluetoothcommunication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.inuker.bluetooth.library.BluetoothClient;
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener;
import com.inuker.bluetooth.library.connect.options.BleConnectOptions;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse;
import com.inuker.bluetooth.library.connect.response.BleReadResponse;
import com.inuker.bluetooth.library.connect.response.BleReadRssiResponse;
import com.inuker.bluetooth.library.connect.response.BleUnnotifyResponse;
import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;
import com.inuker.bluetooth.library.utils.BluetoothLog;
import com.inuker.bluetooth.library.utils.BluetoothUtils;

import java.util.ArrayList;
import java.util.UUID;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.inuker.bluetooth.library.Constants.REQUEST_SUCCESS;
import static com.inuker.bluetooth.library.Constants.STATUS_CONNECTED;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.list)
    ListView list;
    private ArrayList<SearchResult> mDevices=new ArrayList<>();
    private MyAdapter mAdapter;
    private String MAC="";
    private SearchResult mResult;
    private BluetoothDevice mDevice;
    private boolean mConnected;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        getApplication();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        mAdapter=new MyAdapter(getBaseContext(),R.layout.list_item,mDevices);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
              /*  if (!mConnected) {
                    return;
                }*/
                MAC=mDevices.get(i).getAddress();
                mResult=mDevices.get(i);
                mDevice= BluetoothUtils.getRemoteDevice(MAC);
                ClientManager.getClient().registerConnectStatusListener(MAC,mConnectStatusListener);
                connectDeviceIfNeeded();

            }
        });
        search();
    }
    private final BleConnectStatusListener mConnectStatusListener = new BleConnectStatusListener() {
        @Override
        public void onConnectStatusChanged(String mac, int status) {
            BluetoothLog.v(String.format("DeviceDetailActivity onConnectStatusChanged %d in %s",
                    status, Thread.currentThread().getName()));
            mConnected = (status == STATUS_CONNECTED);
            connectDeviceIfNeeded();
        }
    };

    private final SearchResponse mSearchResponse = new SearchResponse() {
        @Override
        public void onSearchStarted() {
            BluetoothLog.w("MainActivity.onSearchStarted");
            mDevices.clear();
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onDeviceFounded(SearchResult device) {
//            BluetoothLog.w("MainActivity.onDeviceFounded " + device.device.getAddress());
            if (!mDevices.contains(device)) {
                mDevices.add(device);
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onSearchStopped() {
            BluetoothLog.w("MainActivity.onSearchStopped");
        }

        @Override
        public void onSearchCanceled() {
            BluetoothLog.w("MainActivity.onSearchCanceled");
        }
    };
//扫描设备
    public void search() {
        SearchRequest request = new SearchRequest.Builder()
                .searchBluetoothLeDevice(3000, 3)
                .searchBluetoothClassicDevice(5000)
                .searchBluetoothLeDevice(2000)
                .build();
        ClientManager.getClient().search(request, mSearchResponse);
    }
    //    停止蓝牙扫描
    public void stopSearch() {
        if (ClientManager.getClient() != null)
            ClientManager.getClient().stopSearch();
    }
    //     BLE设备连接 (开启蓝牙扫描 连接，使用已知的 MAC 地址)
    public void connectDevice(String MAC) {
        BleConnectOptions options=new BleConnectOptions.Builder()
                .setConnectRetry(3)//重连3次
                .setConnectTimeout(5000)//5秒后为连接超时
                .setServiceDiscoverRetry(3)//连接Servicec重试3次
                .setServiceDiscoverTimeout(10000)//5s后连接服务超时
                .build();
            ClientManager.getClient().connect(MAC, options,new BleConnectResponse() {
                @Override
                public void onResponse(int code, BleGattProfile data) {
                    BluetoothLog.v(String.format("profile:\n%s", data));
//                    链接监听
//                    mListView.setVisibility(View.VISIBLE);

                    if (code == REQUEST_SUCCESS) {
//                        mAdapter.setGattProfile(data);
//                        连接成功
                        Toast.makeText(getBaseContext(),"Success",Toast.LENGTH_SHORT);
                        return;
                    }
                    Toast.makeText(getBaseContext(),"false",Toast.LENGTH_SHORT);
                }
            });
    }

    //    BLE设备断开连接
    public void disConnectSearch(String MAC) {
        if (ClientManager.getClient() != null) {
            ClientManager.getClient().disconnect(MAC);
        }
    }

    //    读取BLE设备
    public void readSearch(String MAC, UUID service, UUID character) {
        if (ClientManager.getClient() != null) {
            ClientManager.getClient().read(MAC, service, character, new BleReadResponse() {
                @Override
                public void onResponse(int code, byte[] data) {
                    if (code == REQUEST_SUCCESS) {

                    }
                }
            });
        }
    }

    // 写BLE设备
    public void wrieSearch(String MAC, UUID service, UUID character, byte[] bytes) {
        if (ClientManager.getClient() != null) {
            ClientManager.getClient().write(MAC, service, character, bytes, new BleWriteResponse() {
                @Override
                public void onResponse(int code) {
                    if (code == REQUEST_SUCCESS) {

                    }
                }
            });
        }
    }

    //    打开设备通知
    public void onNotifySearch(String MAC, UUID service, UUID character) {
        ClientManager.getClient().notify(MAC, service, character, new BleNotifyResponse() {
            @Override
            public void onNotify(UUID service, UUID character, byte[] value) {
            }

            @Override
            public void onResponse(int code) {
                if (code == REQUEST_SUCCESS) {
                }

            }
        });
    }

    //    关闭设备通知
    public void unNotifySearch(String MAC, UUID service, UUID character) {
        ClientManager.getClient().unnotify(MAC, service, character, new BleUnnotifyResponse() {
            @Override
            public void onResponse(int code) {
                if (code == REQUEST_SUCCESS) {

                }
            }
        });
    }

    //    读取rssi
    public void readRssiSearch(String MAC) {
        ClientManager.getClient().readRssi(MAC, new BleReadRssiResponse() {
            @Override
            public void onResponse(int code, Integer data) {
                if (code == REQUEST_SUCCESS) {

                }
            }
        });
    }

    private void connectDeviceIfNeeded() {
        if (!mConnected) {
            connectDevice(MAC);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ClientManager.getClient().disconnect(MAC);
        ClientManager.getClient().unregisterConnectStatusListener(MAC,mConnectStatusListener);
    }

    class MyAdapter extends ArrayAdapter<SearchResult> {
        private ArrayList<SearchResult> list;
        private LayoutInflater mInflater;

        public MyAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull ArrayList<SearchResult> objects) {
            super(context, resource, objects);
            this.list = objects;
            this.mInflater = LayoutInflater.from(context);
        }
        @Override
        public synchronized void add(@Nullable SearchResult object) {
            super.add(object);
        }

        @NonNull
        @Override
        public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            ViewHolder viewHolder = null;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.item1 = (TextView) convertView.findViewById(R.id.text1);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.item1.setText("Name:"+list.get(position).getName()+"\\Rssi:"+list.get(position).rssi+"\n\tMAC:"+list.get(position).getAddress());
            return convertView;
        }
        class ViewHolder {
            TextView item1;
        }
    }
}
