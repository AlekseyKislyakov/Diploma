package com.example.diploma.view

import android.Manifest
import android.bluetooth.BluetoothSocket
import android.os.Bundle
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
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class ConnectActivity : AppCompatActivity() {

    private val REQUEST_ENABLE_BT = 1

    private val deviceAdapter = DeviceListAdapter()
    private var currentDeviceAddress: String? = null

    var bluetoothConnection: BluetoothConnection? = null

    // SPP UUID сервиса
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    val rxBluetooth = RxBluetooth(this)
    val rxPermissions = RxPermissions(this)
    val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerViewDeviceList.adapter = deviceAdapter

        buttonConnect.setOnClickListener {
            startActivity(ControlActivity.newIntent(this, currentDeviceAddress))
        }

        compositeDisposable.add(deviceAdapter.relayClicks()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.computation())
            .subscribe
            {
                rxBluetooth.cancelDiscovery()
                buttonConnect.isEnabled = false
                deviceAdapter.setStatus(it, LoadingStatus.LOADING)
                createConnection(it)
            })
        startBluetoothSequence()

        toolbar.inflateMenu(R.menu.menu_connect)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.actionRefresh -> {
                    startBluetoothSequence()
                }
            }
            true
        }
    }

    override fun onPause() {
        super.onPause()
        rxBluetooth.cancelDiscovery()
        compositeDisposable.clear()
        bluetoothConnection?.closeConnection()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
        bluetoothConnection?.closeConnection()
    }

    private fun startBluetoothSequence() {
        compositeDisposable.add(rxPermissions
            .requestEach(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_COARSE_LOCATION)
            .subscribe
            { granted ->
                if (granted.granted) {
                    enableBluetooth()
                    startBluetoothDiscovery()
                } else {
                    showErrorMessage(getString(R.string.connect_bluetooth_forbidden))
                }
            })
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

    private fun createConnection(device: Device) {
        compositeDisposable.add(rxBluetooth.connectAsClient(device.device, MY_UUID)
            .subscribe { t1: BluetoothSocket?, t2: Throwable? ->
                if (t1 != null) {
                    bluetoothConnection = BluetoothConnection(t1)
                    deviceAdapter.setStatus(device, LoadingStatus.SUCCESS)
                    currentDeviceAddress = device.device.address
                    buttonConnect.isEnabled = true

                } else {
                    deviceAdapter.setStatus(device, LoadingStatus.FAIL)
                    showErrorMessage(t2?.localizedMessage ?: "")
                }
            })
    }

    private fun showErrorMessage(err: String) {
        Toast.makeText(this, err, Toast.LENGTH_LONG).show()
    }
}