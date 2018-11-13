package com.example.mumu.bluetoothcommunication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;
import com.inuker.bluetooth.library.utils.BluetoothLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.limpoxe.support.servicemanager.util.ParamUtil.result;

public class BluetoothServerActivity extends AppCompatActivity {

    @Bind(R.id.editText)
    EditText editText;
    @Bind(R.id.textview)
    TextView textview;
    @Bind(R.id.listview)
    ListView listview;

    private ArrayList<SearchResult> devices = new ArrayList();
    private StringBuilder incoming = new StringBuilder();
    private BluetoothSocket transferSocket;
    private MyAdapter myAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_server);
        ButterKnife.bind(this);
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
        initView();
    }

    private void initView() {
        myAdapter=new MyAdapter(getBaseContext(),R.layout.list_item,devices);
        listview.setAdapter(myAdapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i != 0) {
//                    final BluetoothDevice device = deviceList.get(i);
                    final BluetoothDevice device = devices.get(i).device;
                    if (device!=null)
                    new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            connectToServerSocket(device, UUID.fromString("38400000-8cf0-11bd-b23e-10b96e4ef00d"));
                        }
                    }.start();
                }
            }
        });
        search();
    }
    //扫描设备
    public void search() {
        SearchRequest request = new SearchRequest.Builder()
                .searchBluetoothLeDevice(3000, 3)
                .searchBluetoothClassicDevice(5000)
                .searchBluetoothLeDevice(2000)
                .build();
        ClientManager.getClient().search(request, mSearchResponse);
    }
    private final SearchResponse mSearchResponse = new SearchResponse() {
        @Override
        public void onSearchStarted() {
            BluetoothLog.w("MainActivity.onSearchStarted");
            devices.clear();
            myAdapter.notifyDataSetChanged();
        }

        @Override
        public void onDeviceFounded(SearchResult device) {
//            BluetoothLog.w("MainActivity.onDeviceFounded " + device.device.getAddress());
            if (!devices.contains(device)) {
                devices.add(device);
                myAdapter.notifyDataSetChanged();
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
    private void connectToServerSocket(BluetoothDevice device,UUID uuid){
        try {
            BluetoothSocket clint=device.createRfcommSocketToServiceRecord(uuid);
            transferSocket=clint;
            // Block until server connection accepted.
            clint.connect();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                // TODO Auto-generated method stub
                    Toast.makeText(BluetoothServerActivity.this, "连接成功", Toast.LENGTH_LONG)
                            .show();
                }
            });
        // Start listening for messages.
            listenForMessages(clint, incoming);
        // Add a reference to the socket used to send messages
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("BLUETOOTH", "Blueooth client I/O Exception", e);
        }
    }
    private boolean listening = false;
    private void listenForMessages(BluetoothSocket socket,final StringBuilder incoming) {
        listening=true;
        int bufferSize=1024;
        byte[]buffer=new byte[bufferSize];
        try {
            InputStream inputStream=socket.getInputStream();
            int bytesRead= -1;
            while (listening){
                bytesRead=inputStream.read(buffer);
                if (bytesRead !=-1){
                    String result="";
                    while ((bytesRead == bufferSize)&&(buffer[bufferSize-1]!=0)){
                        bytesRead=inputStream.read(buffer);
                    }
                    result = result + new String(buffer, 0, bytesRead - 1);
                    incoming.append(result);
                    Log.i("caohaidemo", "服务器说:" + incoming.toString());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        // TODO Auto-generated method stub
                            editText.setText(incoming.toString());
                        }
                    });
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.textview)
    public void onViewClicked() {
        if (transferSocket != null) {
            String str = editText.getText().toString();
            sendMessage(transferSocket, "客户端说:" + str + "/n");
        }
    }
    private void sendMessage(BluetoothSocket socket, String message) {
        OutputStream outStream;
        try {
            outStream = socket.getOutputStream();
// Add a stop character.
            byte[] byteArray = (message + " ").getBytes();
            byteArray[byteArray.length - 1] = 0;
            outStream.write(byteArray);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    class MyAdapter extends ArrayAdapter {
       private ArrayList<SearchResult> list;
        private LayoutInflater mInflater;

        public MyAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull ArrayList<SearchResult> objects) {
            super(context, resource, objects);
            this.list = objects;
            this.mInflater = LayoutInflater.from(context);
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
            viewHolder.item1.setText("Name:" + list.get(position).getName() + "\\Rssi:" + list.get(position).rssi + "\n\tMAC:" + list.get(position).getAddress());
            return convertView;
        }

        class ViewHolder {
            TextView item1;
        }
    }
}
