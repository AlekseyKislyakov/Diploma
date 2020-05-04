package com.example.diploma.view

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.diploma.R
import com.example.diploma.adapters.DeviceListAdapter
import com.example.diploma.entities.Device
import com.example.diploma.entities.LoadingStatus
import com.github.ivbaranov.rxbluetooth.BluetoothConnection
import com.github.ivbaranov.rxbluetooth.RxBluetooth
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.io.IOException
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private val REQUEST_ENABLE_BT = 1

    private val deviceAdapter = DeviceListAdapter()
    private var btAdapter: BluetoothAdapter? = null
    private var btSocket: BluetoothSocket? = null
    private var outStream: OutputStream? = null

    var bluetoothConnection: BluetoothConnection? = null

    // SPP UUID сервиса
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // MAC-адрес Bluetooth модуля
    private val address = "98:D3:21:F4:71:3B"
    val rxBluetooth = RxBluetooth(this) // `this` is a context
    val rxPermissions = RxPermissions(this)
    val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar.inflateMenu(R.menu.menu_connect)

        /*
        buttonConnect.setOnClickListener {
            bluetoothConnection?.send("0".toByteArray())
        }

        buttonDisconnect.setOnClickListener {
            bluetoothConnection?.send("1".toByteArray())
            bluetoothConnection?.observeStringStream()
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribeOn(Schedulers.io())
                ?.subscribe {
                    textViewTitle.text = it
                }
        }*/

        recyclerViewDeviceList.adapter = deviceAdapter
        compositeDisposable.add(deviceAdapter.relayClicks()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.computation())
            .subscribe {
                buttonConnect.isEnabled = false
                deviceAdapter.setStatus(it, LoadingStatus.LOADING)
                rxBluetooth.connectAsClient(it.device, MY_UUID)
                    .subscribe { t1: BluetoothSocket?, t2: Throwable? ->
                        if (t1 != null) {
                            bluetoothConnection = BluetoothConnection(t1)
                            bluetoothConnection?.send("01010101010101".toByteArray())
                            deviceAdapter.setStatus(it, LoadingStatus.SUCCESS)
                            buttonConnect.isEnabled = true
                        } else {
                            deviceAdapter.setStatus(it, LoadingStatus.FAIL)
                        }
                        //bluetoothConnection!!.observeStringStream()

                    }
            })

        compositeDisposable.add(rxPermissions
            .request(Manifest.permission.ACCESS_COARSE_LOCATION)
            .subscribe { granted ->
                if (granted) {
                    enableBluetooth()
                    startBluetoothDiscovery()
                } else {
                    showErrorMessage()
                }
            })

        compositeDisposable.add(rxPermissions
            .request(Manifest.permission.BLUETOOTH)
            .subscribe { granted ->
                if (granted) {
                    enableBluetooth()
                    startBluetoothDiscovery()
                } else {
                    showErrorMessage()
                }
            })

    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothConnection?.closeConnection()
    }

    private fun enableBluetooth() {
        // check if bluetooth is supported on your hardware
        if (!rxBluetooth.isBluetoothAvailable) {
            // handle the lack of bluetooth support
        } else {
            // check if bluetooth is currently enabled and ready for use
            if (!rxBluetooth.isBluetoothEnabled) {
                // to enable bluetooth via startActivityForResult()
                rxBluetooth.enableBluetooth(this, REQUEST_ENABLE_BT)

            } else {
                rxBluetooth.startDiscovery()
                toolbar.setOnMenuItemClickListener {
                    rxBluetooth.startDiscovery()
                }
            }
        }
    }

    private fun startBluetoothDiscovery() {
        deviceAdapter.clearItems()
        compositeDisposable.add(rxBluetooth.observeDevices()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.computation())
            .subscribe {
                deviceAdapter.add(Device(it, LoadingStatus.NONE))
            })
    }

    private fun showErrorMessage() {

    }
}


/*override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    GlobalScope.async {
        btAdapter = BluetoothAdapter.getDefaultAdapter()
        checkBTState()
    }

    buttonConnect.setOnClickListener {
        sendData("1")
        Toast.makeText(baseContext, "Включаем LED", Toast.LENGTH_SHORT).show()
    }

    buttonDisconnect.setOnClickListener {
        sendData("0")
        Toast.makeText(baseContext, "Включаем LED", Toast.LENGTH_SHORT).show()
    }
}

override fun onResume() {
    super.onResume()
    Log.d("TAG", "...onResume - попытка соединения...")

    // Set up a pointer to the remote node using it's address.
    val device = btAdapter!!.getRemoteDevice(address)

    // Two things are needed to make a connection:
    //   A MAC address, which we got above.
    //   A Service ID or UUID.  In this case we are using the
    //     UUID for SPP.
    try {
        btSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
    } catch (e: IOException) {
        errorExit("Fatal Error", "In onResume() and socket create failed: " + e.message + ".")
    }

    // Discovery is resource intensive.  Make sure it isn't going on
    // when you attempt to connect and pass your message.
    btAdapter!!.cancelDiscovery()

    // Establish the connection.  This will block until it connects.
    Log.d("TAG", "...Соединяемся...")
    GlobalScope.async {
        try {

            btSocket!!.connect()

            Log.d(
                "TAG",
                "...Соединение установлено и готово к передачи данных..."
            )
        } catch (e: IOException) {
            try {
                btSocket!!.close()
            } catch (e2: IOException) {
                errorExit(
                    "Fatal Error",
                    "In onResume() and unable to close socket during connection failure" + e2.message + "."
                )
            }
        }
    }

    // Create a data stream so we can talk to server.

    // Create a data stream so we can talk to server.
    Log.d("TAG", "...Создание Socket...")

    try {
        outStream = btSocket!!.outputStream

    } catch (e: IOException) {
        errorExit(
            "Fatal Error",
            "In onResume() and output stream creation failed:" + e.message + "."
        )
    }
}

override fun onPause() {
    super.onPause()

    Log.d("TAG", "...In onPause()...")

    if (outStream != null) {
        try {
            outStream!!.flush()
        } catch (e: IOException) {
            errorExit(
                "Fatal Error",
                "In onPause() and failed to flush output stream: " + e.message + "."
            )
        }
    }

    try {
        btSocket!!.close()
    } catch (e2: IOException) {
        errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.message + ".")
    }
}

private fun checkBTState() {
    // Check for Bluetooth support and then check to make sure it is turned on
    // Emulator doesn't support Bluetooth and will return null
    if (btAdapter == null) {
        errorExit("Fatal Error", "Bluetooth не поддерживается")
    } else {
        if (btAdapter!!.isEnabled) {
            Log.d("TAG", "...Bluetooth включен...")
        } else {
            //Prompt user to turn on Bluetooth
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }
}

private suspend fun initializeBluetooth() {


    // job.await()
}

private fun errorExit(title: String, message: String) {
    Toast.makeText(baseContext, "$title - $message", Toast.LENGTH_LONG).show()
    finish()
}

private fun sendData(message: String) {
    val msgBuffer = message.toByteArray()
    Log.d("TAG", "...Посылаем данные: $message...")
    try {
        outStream!!.write(msgBuffer)
    } catch (e: IOException) {
        var msg =
            "In onResume() and an exception occurred during write: " + e.message
        msg =
            "$msg.Проверьте поддержку SPP UUID: $MY_UUID на Bluetooth модуле, к которому вы подключаетесь."
        errorExit("Fatal Error", msg)
    }
}
}

*/