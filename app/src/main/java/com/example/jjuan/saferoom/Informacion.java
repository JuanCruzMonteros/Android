package com.example.jjuan.saferoom;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static com.example.jjuan.saferoom.R.*;

public class Informacion extends AppCompatActivity {

    //-------------------------------------------
    Handler bluetoothIn;
    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder DataStringIN = new StringBuilder();
    private ConnectedThread MyConexionBT;
    // Identificador unico de servicio - SPP UUID
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // String para la direccion MAC
    private static String address = null;
    private TextView txtTemperatura;
    private TextView txtHumedad;
    private TextView txtST;
    private TextView txtMonoxido;
    private Button btnObtenerDatos;
    private Button btnVentana;
    private Button btnEncVent;
    private Button  btnStart;
    private Button btnAsignarValores;

    private TextView txtSTMin;
    private TextView txtSTMax;
    private TextView txtMonoxidoMin;
    private TextView txtMonoxidoMax;
    //-------------------------------------------


    //------------------------- TIMER ------------------
    long startTime = 0;

    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            /*
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            txtMonoxido.setText(String.format("%d:%02d", minutes, seconds));*/

            MyConexionBT.write("1");

            timerHandler.postDelayed(this, 5000);
        }
    };

    //--------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(layout.activity_informacion);
        btnObtenerDatos= (Button)findViewById(id.btnObtenerDatos);
        btnVentana= (Button)findViewById(id.btnVentana);
        btnEncVent= (Button)findViewById(id.btnEncVent);


        txtTemperatura = (TextView)findViewById(id.txtTemperatura);
        txtHumedad = (TextView)findViewById(id.txtHumedad);
        txtST = (TextView)findViewById(id.txtST);
        txtMonoxido = (TextView)findViewById(id.txtMonoxido);

        txtSTMin = (TextView)findViewById(id.txtSTMin);
        txtSTMax = (TextView)findViewById(id.txtSTMax);
        txtMonoxidoMin = (TextView)findViewById(id.txtMonoxidoMin);
        txtMonoxidoMax = (TextView)findViewById(id.txtMonoxidoMax);

        btnStart = (Button)findViewById(id.btnStart);
        btnAsignarValores = (Button)findViewById(id.btnAsignarValores);

        btAdapter = BluetoothAdapter.getDefaultAdapter(); // get Bluetooth adapter
        VerificarEstadoBT();

        bluetoothIn = new Handler() {

            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    DataStringIN.append(readMessage);

                    int endOfLineIndex = DataStringIN.indexOf("#");
                    //si encuentra el numeral quiere decir que recibio datos
                    if (endOfLineIndex > 0) {
                        //Ahora hay que ver que acomodar los datos:
                        String dataInPrint = DataStringIN.substring(0, endOfLineIndex);
                        String[] partes = dataInPrint.split(",");
                        txtTemperatura.setText(partes[0]);
                        txtHumedad.setText(partes[1]);
                        txtST.setText(partes[2]);
                        txtMonoxido.setText(partes[3]);
                        DataStringIN.delete(0, DataStringIN.length());
                    }
                }
            }
        };


        btnObtenerDatos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyConexionBT.write("1");
            }
        });

        btnVentana.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyConexionBT.write("b");
            }
        });

        btnEncVent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyConexionBT.write("c");
            }
        });

        //----- TIMER
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button btnStart = (Button) v;
                if (btnStart.getText().equals("stop")) {
                    timerHandler.removeCallbacks(timerRunnable);
                    btnStart.setText("start");
                } else {
                    startTime = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnable, 0);
                    btnStart.setText("stop");
                }
            }
        });

        btnAsignarValores.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String datos = "&" + txtSTMin.getText() + "," +
                        txtSTMax.getText() + "," +
                        txtMonoxidoMin.getText() + "," +
                        txtMonoxidoMax.getText() + "#";
                //btnStart.setText(datos);
                MyConexionBT.write(datos);
            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
        Button btnStart = (Button)findViewById(R.id.btnStart);
        btnStart.setText("start");
    }


    @Override
    public void onResume()
    {
        super.onResume();
        //Consigue la direccion MAC desde DeviceListActivity via intent
        Intent intent = getIntent();
        //Consigue la direccion MAC desde DeviceListActivity via EXTRA
        address = intent.getStringExtra(DispositivosBT.EXTRA_DEVICE_ADDRESS);//<-<- PARTE A MODIFICAR >->->
        //Setea la direccion MAC
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try
        {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
        }
        // Establece la conexión con el socket Bluetooth.
        try
        {
            btSocket.connect();
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {}
        }
        MyConexionBT = new ConnectedThread(btSocket);
        MyConexionBT.start();
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException
    {
        //crea un conexion de salida segura para el dispositivo
        //usando el servicio UUID
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    //Comprueba que el dispositivo Bluetooth Bluetooth está disponible y solicita que se active si está desactivado
    private void VerificarEstadoBT() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "El dispositivo no soporta bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }
    //Crea la clase que permite crear el evento de conexion
    private class ConnectedThread extends Thread
    {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run()
        {
            byte[] buffer = new byte[256];
            int bytes;

            // Se mantiene en modo escucha para determinar el ingreso de datos
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    // Envia los datos obtenidos hacia el evento via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
        //Envio de trama
        public void write(String input)
        {
            try {
                mmOutStream.write(input.getBytes());
            }
            catch (IOException e)
            {
                //si no es posible enviar datos se cierra la conexión
                Toast.makeText(getBaseContext(), "La Conexión fallo", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}
