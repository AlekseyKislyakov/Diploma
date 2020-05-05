package com.example.diploma.view

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.diploma.R
import com.example.diploma.adapters.IntegrityAdapter
import com.example.diploma.entities.Device
import com.example.diploma.entities.Integrity
import com.example.diploma.entities.LoadingStatus
import com.github.ivbaranov.rxbluetooth.BluetoothConnection
import com.github.ivbaranov.rxbluetooth.RxBluetooth
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_control.*
import java.util.*

class ControlActivity : AppCompatActivity() {

    companion object {
        const val ADDRESS_EXTRA = "ADDRESS_EXTRA"
        fun newIntent(context: Context, deviceAddress: String?): Intent {
            return if (deviceAddress != null) {
                val intent = Intent(context, ControlActivity::class.java)
                intent.putExtra(ADDRESS_EXTRA, deviceAddress)
                intent
            } else Intent(context, ControlActivity::class.java)
        }
    }

    var currentAddress = ""
    var currentDevice: BluetoothDevice? = null
    val compositeDisposable = CompositeDisposable()

    var bluetoothConnection: BluetoothConnection? = null

    val integrityAdapter = IntegrityAdapter()

    val PORT_1_ENABLED = "Line1true"
    val PORT_2_ENABLED = "Line2true"
    val PORT_1_DISABLED = "Line1false"
    val PORT_2_DISABLED = "Line2false"
    val PORT_1_BROKEN = "Line1broken"
    val PORT_2_BROKEN = "Line2broken"

    // SPP UUID сервиса
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    val rxBluetooth = RxBluetooth(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)

        setStatus(LoadingStatus.NONE)
        if (intent.hasExtra(ADDRESS_EXTRA)) {
            currentAddress = intent.getStringExtra(ADDRESS_EXTRA) ?: ""
            currentDevice = rxBluetooth.bondedDevices?.find { it.address == currentAddress }
            startConnection(currentDevice)
        }

        recyclerViewIntegrity.adapter = integrityAdapter
        integrityAdapter.add(Integrity("Line", "1", false))
        integrityAdapter.add(Integrity("Line", "2", false))

        integrityAdapter.startClicks().smartSubscribe {
            sendCommand(it.name + it.portNumber + !it.started)
        }

        integrityAdapter.moreClicks().smartSubscribe {
            Toast.makeText(this, it.name, Toast.LENGTH_SHORT).show()
        }

        layoutDescription.setOnClickListener {
            sendCommand("Some")
        }
    }

    private fun startConnection(device: BluetoothDevice?) {
        compositeDisposable.add(rxBluetooth.connectAsClient(device, MY_UUID)
            .subscribe { t1: BluetoothSocket?, t2: Throwable? ->
                if (t1 != null) {
                    bluetoothConnection = BluetoothConnection(t1)
                    textViewName.text = currentDevice?.name
                    textViewAddress.text = currentDevice?.address
                    setStatus(LoadingStatus.SUCCESS)
                    setIncomingListener()
                } else {
                    setStatus(LoadingStatus.FAIL)
                }
            })
    }

    private fun setStatus(state: LoadingStatus) {
        when (state) {
            LoadingStatus.NONE -> {
                imageViewStatus.visibility = View.VISIBLE
                progressBarStatus.visibility = View.GONE
                imageViewStatus.setImageResource(R.drawable.bg_status_drawable)
            }
            LoadingStatus.LOADING -> {
                imageViewStatus.visibility = View.GONE
                progressBarStatus.visibility = View.VISIBLE
            }
            LoadingStatus.SUCCESS -> {
                imageViewStatus.visibility = View.VISIBLE
                progressBarStatus.visibility = View.GONE
                imageViewStatus.setImageResource(R.drawable.bg_drawable_success)
            }
            LoadingStatus.FAIL -> {
                imageViewStatus.visibility = View.VISIBLE
                progressBarStatus.visibility = View.GONE
                imageViewStatus.setImageResource(R.drawable.bg_drawable_fail)
            }
        }
    }

    private fun setIncomingListener() {
        val incoming = bluetoothConnection?.observeStringStream()
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribeOn(Schedulers.io())
            ?.subscribe {
                if (it.isNotEmpty()) {
                    handleCommand(it)
                }
            }
        if (incoming != null) {
            compositeDisposable.add(incoming)
        }

    }

    private fun handleCommand(command: String) {
        Toast.makeText(this, command, Toast.LENGTH_SHORT).show()
        when (command) {
            PORT_1_ENABLED -> {
                integrityAdapter.setStatus(1, true)
            }
            PORT_1_DISABLED -> {
                integrityAdapter.setStatus(1, false)
            }
            PORT_2_ENABLED -> {
                integrityAdapter.setStatus(2, true)
            }
            PORT_2_DISABLED -> {
                integrityAdapter.setStatus(2, false)
            }
            PORT_1_BROKEN -> {
                integrityAdapter.setBroken(1, System.currentTimeMillis())
            }
            PORT_2_BROKEN -> {
                integrityAdapter.setBroken(2, System.currentTimeMillis())
            }
        }
    }

    private fun sendCommand(command: String) {
        bluetoothConnection?.send("$command\r")
    }

    fun <T> Observable<T>.smartSubscribe(consumer: (T) -> Unit) {
        compositeDisposable.add(observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.computation())
            .subscribe { consumer.invoke(it) })
    }
}
