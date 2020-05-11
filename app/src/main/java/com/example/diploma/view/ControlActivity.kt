package com.example.diploma.view

import android.app.Dialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.example.diploma.R
import com.example.diploma.adapters.IntegrityAdapter
import com.example.diploma.adapters.RelayAdapter
import com.example.diploma.entities.Entity
import com.example.diploma.entities.Integrity
import com.example.diploma.entities.LoadingStatus
import com.example.diploma.entities.MagnetRelay
import com.example.diploma.ext.convertLongToTime
import com.example.diploma.ext.smartSubscribe
import com.example.diploma.ext.safeSmartSubscribe
import com.github.ivbaranov.rxbluetooth.BluetoothConnection
import com.github.ivbaranov.rxbluetooth.RxBluetooth
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_control.*
import kotlinx.android.synthetic.main.bottom_sheet_more.view.*
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
    val relayAdapter = RelayAdapter()

    var moreDialog: Dialog? = null

    val PORT_1_ENABLED = "Line1true"
    val PORT_2_ENABLED = "Line2true"
    val PORT_1_DISABLED = "Line1false"
    val PORT_2_DISABLED = "Line2false"
    val PORT_1_BROKEN = "Line1broken"
    val PORT_2_BROKEN = "Line2broken"

    val RELAY_1_ENABLED = "Relay1true"
    val RELAY_2_ENABLED = "Relay2true"
    val RELAY_3_ENABLED = "Relay3true"
    val RELAY_1_DISABLED = "Relay1false"
    val RELAY_2_DISABLED = "Relay2false"
    val RELAY_3_DISABLED = "Relay3false"

    val RECORD_1_ENABLED = "Record1true"
    val RECORD_2_ENABLED = "Record2true"
    val RECORD_1_DISABLED = "Record1false"
    val RECORD_2_DISABLED = "Record2false"

    val GENERATOR_1_ENABLED = "Gener1true"
    val GENERATOR_2_ENABLED = "Gener2true"
    val GENERATOR_1_DISABLED = "Gener1false"
    val GENERATOR_2_DISABLED = "Gener2false"

    val integrityCommands = listOf(
        PORT_1_ENABLED, PORT_2_ENABLED, PORT_1_DISABLED,
        PORT_2_DISABLED, PORT_1_BROKEN, PORT_2_BROKEN
    )

    val relayCommands = listOf(
        RELAY_1_ENABLED, RELAY_2_ENABLED, RELAY_3_ENABLED,
        RELAY_1_DISABLED, RELAY_2_DISABLED, RELAY_3_DISABLED
    )

    val recordCommands = listOf(
        RECORD_1_ENABLED, RECORD_2_ENABLED,
        RECORD_1_DISABLED, RECORD_2_DISABLED
    )

    val generatorCommands = listOf(
        GENERATOR_1_ENABLED, GENERATOR_2_ENABLED,
        GENERATOR_1_DISABLED, GENERATOR_2_DISABLED
    )

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
        recyclerViewRelay.adapter = relayAdapter

        integrityAdapter.add(
            Integrity(
                portNumber = "1",
                started = false,
                workTime = 155561,
                startedTime = System.currentTimeMillis()
            )
        )
        integrityAdapter.add(Integrity(portNumber = "2", started = false))

        relayAdapter.add(MagnetRelay("1", false))
        relayAdapter.add(MagnetRelay("2", false))
        relayAdapter.add(MagnetRelay("3", false))

        integrityAdapter.startClicks().smartSubscribe(compositeDisposable) {
            sendCommand(it.name + it.portNumber + !it.started)
        }

        integrityAdapter.moreClicks().smartSubscribe(compositeDisposable) {
            createDialog(it)?.show()
        }

        relayAdapter.startClicks().smartSubscribe(compositeDisposable) {
            sendCommand(it.name + it.portNumber + !it.started)
        }

        layoutDescription.setOnClickListener {
            sendCommand("Some")
        }
    }

    private fun createDialog(entity: Entity): Dialog? {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.bottom_sheet_more, null)

        dialogView?.let { dialogView ->
            when (entity) {
                is Integrity -> {
                    showIntegrityView(dialogView, entity)
                }
                is MagnetRelay -> {
                    showRelayView(dialogView, entity)
                }
            }

            moreDialog = BottomSheetDialog(this).apply {
                setCancelable(false)
                setCanceledOnTouchOutside(true)
                setContentView(dialogView)
            }
        }
        return moreDialog
    }

    private fun startConnection(device: BluetoothDevice?) {
        compositeDisposable.add(rxBluetooth.connectAsClient(device, MY_UUID)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
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
        bluetoothConnection?.observeStringStream()
            .safeSmartSubscribe(compositeDisposable) {
                if (it.isNotEmpty()) {
                    handleCommand(it)
                }
            }
    }

    private fun handleCommand(command: String) {
        Toast.makeText(this, command, Toast.LENGTH_SHORT).show()
        when (command) {
            in integrityCommands -> {
                handleIntegrityCommand(command)
            }
            in relayCommands -> {
                handleRelayCommand(command)
            }
            in recordCommands -> {

            }
            in generatorCommands -> {

            }
            else -> {
                parseCommand(command)
            }
        }
    }

    private fun sendCommand(command: String) {
        bluetoothConnection?.send("$command\r")
    }

    private fun parseCommand(command: String) {

    }

    private fun handleIntegrityCommand(command: String) {
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
                vibrate()
                integrityAdapter.setBroken(1, System.currentTimeMillis())
            }
            PORT_2_BROKEN -> {
                vibrate()
                integrityAdapter.setBroken(2, System.currentTimeMillis())
            }
        }
    }

    private fun showIntegrityView(view: View, integrity: Integrity) {
        with(view) {
            textViewSheetName.text = integrity.name
            textViewSheetPort.text = integrity.portNumber

            textViewSheetStatus.text = if (integrity.started) "Включен" else "Выключен"
            if (integrity.broken) {
                textViewSheetStatus.text = "Обрыв линии"
            }

            if (integrity.startedTime != 0L) {
                textViewSheetStartedTime.text = integrity.startedTime.convertLongToTime()
            }

            if (integrity.workTime != 0L) {
                textViewSheetWorkTime.text = integrity.workTime.convertLongToTime()
            }
        }
    }

    private fun showRelayView(view: View, relay: MagnetRelay) {
        with(view) {
            textViewSheetName.text = relay.name
            textViewSheetPort.text = relay.portNumber

            textViewSheetStatus.text = if (relay.started) "Включен" else "Выключен"

            if (relay.startedTime != 0L) {
                textViewSheetStartedTime.text = relay.startedTime.convertLongToTime()
            }

            if (relay.workTime != 0L) {
                textViewSheetWorkTime.text = relay.workTime.convertLongToTime()
            }
        }
    }

    private fun handleRelayCommand(command: String) {
        when (command) {
            RELAY_1_ENABLED -> {
                relayAdapter.setStatus(1, true)
            }
            RELAY_2_ENABLED -> {
                relayAdapter.setStatus(2, true)
            }
            RELAY_3_ENABLED -> {
                relayAdapter.setStatus(3, true)
            }
            RELAY_1_DISABLED -> {
                relayAdapter.setStatus(1, false)
            }
            RELAY_2_DISABLED -> {
                relayAdapter.setStatus(2, false)
            }
            RELAY_3_DISABLED -> {
                relayAdapter.setStatus(3, false)
            }
        }
    }

    private fun vibrate() {
        val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(200)
        }
    }
}

