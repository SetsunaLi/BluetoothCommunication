package com.example.mumu.bluetoothcommunication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.inuker.bluetooth.library.search.SearchResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class BluetoothClintActivity extends AppCompatActivity {

    /*    @Bind(R.id.list)
        ListView list;*/
    ArrayAdapter<String> adapter;
    MyAdapter mAdapter;
    @Bind(R.id.editText)
    EditText editText;
    @Bind(R.id.textview)
    TextView textview;
    private ArrayList<SearchResult> mDevices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        ButterKnife.bind(this);
  /*      mAdapter=new MyAdapter(getBaseContext(),R.layout.list_item,mDevices);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            }
        });*/
        initData();
    }

    private BluetoothAdapter bAdapter;
    private BluetoothDevice device;
    private BluetoothSocket transferSocket;
    private StringBuilder incoming = new StringBuilder();
    private StringBuffer sb;

    private void initData() {
        bAdapter = BluetoothAdapter.getDefaultAdapter();
        sb = new StringBuffer();
        startServerSocket(bAdapter);
    }

    //    设置比返回UUID，生成蓝牙待连接服务端（客户端是被动连接）
    private UUID startServerSocket(BluetoothAdapter bluetooth) {
        UUID uuid = UUID.fromString("38400000-8cf0-11bd-b23e-10b96e4ef00d");
        String name = "BluetoothSocket";
        try {
            final BluetoothServerSocket btserver = bluetooth.listenUsingRfcommWithServiceRecord(name, uuid);
            Thread acceptThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final BluetoothSocket serverSocket = btserver.accept();
                        transferSocket = serverSocket;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(BluetoothClintActivity.this, "连接成功",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                        listenForMessages(serverSocket, incoming);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            acceptThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return uuid;
    }

    private boolean listening = false;

    private void listenForMessages(BluetoothSocket socket, final StringBuilder incoming) throws IOException {
        listening = true;
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        InputStream instream = socket.getInputStream();
        int bytesRead = -1;
        while (listening) {
            bytesRead = instream.read(buffer);
            if (bytesRead != -1) {
                String result = "";
                while ((bytesRead == bufferSize) && (buffer[bufferSize - 1] != 0)) {
                    result = result + new String(buffer, 0, bytesRead - 1);
                    bytesRead = instream.read(buffer);
                }
                result = result + new String(buffer, 0, bytesRead - 1);
                incoming.append(result);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
//                        tv.setText(incoming.toString());
                    }
                });
            }
// socket.close();
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
        }
    }

    @OnClick(R.id.textview)
    public void onViewClicked(View view ) {
        switch (view.getId()){
            case R.id.textview:
                if (transferSocket != null) {
                    String str = editText.getText().toString();
                    sendMessage(transferSocket, "服务器说:" + str + "/n");
                }
                break;
        }

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
            viewHolder.item1.setText("Name:" + list.get(position).getName() + "\\Rssi:" + list.get(position).rssi + "\n\tMAC:" + list.get(position).getAddress());
            return convertView;
        }

        class ViewHolder {
            TextView item1;
        }
    }
}
