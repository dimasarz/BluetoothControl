package com.example.myapplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewDebug;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.os.Handler;
import java.io.InputStream;

public class MainActivity extends Activity {
    TextView myLabel, status;
    EditText myTextbox;
    Button openButton, sendButton, closeButton, ledOn, ledOff;
    ImageView lampon, lampoff;
    BluetoothAdapter mBluetoothAdapter;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
    private static final String TAG = "bluetooth1";
    private OutputStream outStream = null;
    private InputStream inStream = null;
    private static String address = "20:15:03:31:58:10";
    // nomor MAC-address perangkat bluetooth milik pribadi (tanpa tombol reset)
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    Handler BtIn;
    final int handlerState = 0;
    private StringBuilder recDataString = new StringBuilder();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        openButton = (Button) findViewById(R.id.open);
        sendButton = (Button) findViewById(R.id.send);
        closeButton = (Button) findViewById(R.id.close);
        lampon = (ImageView) findViewById(R.id.lampOn);
        lampoff = (ImageView) findViewById(R.id.lampOff);
        ledOn = (Button) findViewById(R.id.ledon);
        ledOff = (Button) findViewById(R.id.ledoff);
        myLabel = (TextView) findViewById(R.id.label);
        final EditText myTextbox = (EditText) findViewById(R.id.entry);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        status = (TextView) findViewById(R.id.status);
        findBT();

        //Open Button
        openButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    openBT();
                    openButton.setVisibility(View.GONE);
                    sendButton.setVisibility(View.VISIBLE);
                    closeButton.setVisibility(View.VISIBLE);
                    ledOn.setVisibility(View.VISIBLE);
                    ledOff.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                }
            }
        });

        //Send Button
        sendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                String input = myTextbox.getText().toString();
                sendData(input);
                Toast.makeText(getBaseContext(), "Kirim Data " + input, Toast.LENGTH_SHORT).show();
                myTextbox.clearFocus();
            }
        });

        //Close button
        closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    closeBT();
                } catch (IOException ex) {
                }
            }

        });

        ledOn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                sendData("1");
                Toast.makeText(getBaseContext(), "LED ON", Toast.LENGTH_SHORT).show();
                myTextbox.clearFocus();
            }
        });

        ledOff.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                sendData("0");
                Toast.makeText(getBaseContext(), "LED OFF", Toast.LENGTH_SHORT).show();
                myTextbox.clearFocus();
            }
        });
    }

    private void findBT() {
        if(btAdapter==null) {
            myLabel.setText("Perangkat Bluetooth TIDAK Tersedia");
        } else {
            myLabel.setText("Perangkat Bluetooth Tersedia");
            openButton.setVisibility(View.VISIBLE);
            sendButton.setVisibility(View.GONE);
            closeButton.setVisibility(View.GONE);
            ledOn.setVisibility(View.GONE);
            ledOff.setVisibility(View.GONE);
            lampoff.setVisibility(View.VISIBLE);
            lampon.setVisibility(View.GONE);
        }
    }

    void openBT() {
        if(btAdapter==null) {
            errorExit("Fatal Error", "Perangkat Bluetooth Tidak tersedia");
            myLabel.setText("Perangkat Bluetooth TIDAK Tersedia");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth Dinyalakan...");
                myLabel.setText("Mencoba Koneksi ke Perangkat Bluetooth");
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
                myLabel.setText("Silakan Masukkan Angka 0-6 saja");
            }
        }
    }

    private void sendData(String message) {
        byte[] msgBuffer = message.getBytes();
        recDataString = new StringBuilder();

        try {
            Log.d(TAG, "...Mengirim data : " + message + "...");

            if (message.equals("1")) {
                lampon.setVisibility(View.VISIBLE);
                lampoff.setVisibility(View.GONE);
            } else if (message.equals("0")){
                lampon.setVisibility(View.GONE);
                lampoff.setVisibility(View.VISIBLE);
            } else {
                status.setText("Status: Inputkan Hanya 0 atau 1");
                return;
            }

            outStream.write(msgBuffer);
            inStream = btSocket.getInputStream();
            while (true) {
                try {
                    String msg =   new String (msgBuffer, 0, inStream.read(msgBuffer));
                    String last = msg.substring(msg.length() - 1);
                    recDataString.append(msg);
                    Log.d(TAG, "handleMessage: "+recDataString);
                    if(last.equals("!")) break;
                } catch (IOException e) {
                    Log.d(TAG, "sendData: stop");
                    break;
                }
            }
            status.setText(recDataString);
        } catch (IOException e) {
            String msg = "Pada onResume() dan sebuah pengecualian terjadi saat menuliskan: " + e.getMessage();
            if (address.equals("00:00:00:00:00:00"))
                msg = msg + ".\n\nUpdate alamat server pada 00:00:00:00:00:00 pada baris ke-35 kode java";
            msg = msg +	".\n\nCek SPP UUID: " + MY_UUID.toString() + " ada di server.\n\n";
            errorExit("Fatal Error", msg);
        }
    }

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    void closeBT() throws IOException {
        if(btAdapter.isEnabled()) {
            btAdapter.disable();
            openButton.setVisibility(View.VISIBLE);
            sendButton.setVisibility(View.GONE);
            closeButton.setVisibility(View.GONE);
            ledOn.setVisibility(View.GONE);
            ledOff.setVisibility(View.GONE);
            myLabel.setText("Silakan Klik Tombol BUKA BT Untuk Aktifasi Koneksi Lagi");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "...onResume - mencoba terhubung...");
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e1) {
            errorExit("Fatal Error", "Terdapat kesalahan onResume() dan socket pada: " + e1.getMessage() + ".");
        }
        btAdapter.cancelDiscovery();
        Log.d(TAG, "...Terhubung...");
        try {
            btSocket.connect();
            Log.d(TAG, "...Terhubung dengan perangkat Bluetooth...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "Terdapat kesalahan onResume() dan penutupan socket" + e2.getMessage() + ".");
            }
        }
        Log.d(TAG, "...Buat koneksi Socket...");
        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            errorExit("Fatal Error", "Terdapat kesalahan onResume() dan keluaran stream pada :" + e.getMessage() + ".");
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws
            IOException {
        if(Build.VERSION.SDK_INT >= 10){
            try {
                final Method	m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] {
                        UUID.class });
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Error koneksi RFComm",e);
            }
        }
        return	device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "...aktifkan onPause()...");
        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                errorExit("Fatal Error", "Terdapat kesalahan onPause() dan flush keluaran stream pada : " + e.getMessage() + ".");
            }
        }
        try {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "Terdapat kesalahan onPause() dan penutupan socket." + e2.getMessage() + ".");
        }
    }

}
