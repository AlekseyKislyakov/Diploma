package com.example.diploma.view

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.app.Dialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.diploma.R
import com.example.diploma.adapters.GeneratorAdapter
import com.example.diploma.adapters.IntegrityAdapter
import com.example.diploma.adapters.RecordAdapter
import com.example.diploma.adapters.RelayAdapter
import com.example.diploma.entities.Entity
import com.example.diploma.entities.Generator
import com.example.diploma.entities.Integrity
import com.example.diploma.entities.LoadingStatus
import com.example.diploma.entities.MagnetRelay
import com.example.diploma.entities.Record
import com.example.diploma.entities.simplify
import com.example.diploma.entities.toBoolean
import com.example.diploma.ext.cleverAmpl
import com.example.diploma.ext.cleverFreq
import com.example.diploma.ext.convertLongToTime
import com.example.diploma.ext.convertLongToTimeUTC
import com.example.diploma.ext.safeSmartSubscribe
import com.example.diploma.ext.simplify
import com.example.diploma.ext.smartSubscribe
import com.example.diploma.ext.toPrettyValue
import com.example.diploma.ext.toShapeId

import com.github.ivbaranov.rxbluetooth.BluetoothConnection
import com.github.ivbaranov.rxbluetooth.RxBluetooth
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.nbsp.materialfilepicker.ui.FilePickerActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_control.*
import kotlinx.android.synthetic.main.bottom_sheet_more.view.*
import java.util.*

class ControlActivity : AppCompatActivity() {

    companion object {
        const val ADDRESS_EXTRA = "ADDRESS_EXTRA"

        val FILE_PICKER_REQUEST_CODE = 1522

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
    val generatorAdapter = GeneratorAdapter()
    val recordAdapter = RecordAdapter()

    var moreDialog: Dialog? = null

    val PORT_1_ENABLED = "INT_0_1"
    val PORT_2_ENABLED = "INT_1_1"
    val PORT_1_DISABLED = "INT_0_0"
    val PORT_2_DISABLED = "INT_1_0"
    val PORT_1_BROKEN = "Line1broken"
    val PORT_2_BROKEN = "Line2broken"

    val RELAY_1_ENABLED = "REL_0_1"
    val RELAY_2_ENABLED = "REL_1_1"
    val RELAY_3_ENABLED = "REL_2_1"
    val RELAY_1_DISABLED = "REL_0_0"
    val RELAY_2_DISABLED = "REL_1_0"
    val RELAY_3_DISABLED = "REL_2_0"

    val RECORD_1_ENABLED = "REC_0_1"
    val RECORD_2_ENABLED = "REC_1_1"
    val RECORD_1_DISABLED = "REC_0_0"
    val RECORD_2_DISABLED = "REC_1_0"
    val RECORD_3_ENABLED = "REC_2_1"
    val RECORD_4_ENABLED = "REC_3_1"
    val RECORD_3_DISABLED = "REC_2_0"
    val RECORD_4_DISABLED = "REC_3_0"

    val GENERATOR_1_ENABLED = "GEN_0_1"
    val GENERATOR_2_ENABLED = "GEN_1_1"
    val GENERATOR_1_DISABLED = "GEN_0_0"
    val GENERATOR_2_DISABLED = "GEN_1_0"

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
        RECORD_1_DISABLED, RECORD_2_DISABLED,
        RECORD_3_ENABLED, RECORD_4_ENABLED,
        RECORD_3_DISABLED, RECORD_4_DISABLED
    )

    val generatorCommands = listOf(
        GENERATOR_1_ENABLED, GENERATOR_2_ENABLED,
        GENERATOR_1_DISABLED, GENERATOR_2_DISABLED
    )

    // SPP UUID сервиса
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    val rxBluetooth = RxBluetooth(this)
    val rxPermissions = RxPermissions(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)
        setStatus(LoadingStatus.NONE)

        if (intent.hasExtra(ADDRESS_EXTRA)) {
            currentAddress = intent.getStringExtra(ADDRESS_EXTRA) ?: ""
            currentDevice = rxBluetooth.bondedDevices?.find { it.address == currentAddress }
            startConnection(currentDevice)
        }

        compositeDisposable.add(rxPermissions
            .requestEach(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)
            .subscribe { granted ->
                if (granted.granted) {

                } else {
                    showErrorMessage(getString(R.string.connect_bluetooth_forbidden))
                }
            })

        recyclerViewIntegrity.adapter = integrityAdapter
        recyclerViewRelay.adapter = relayAdapter
        recyclerViewGenerator.adapter = generatorAdapter
        recyclerViewRecord.adapter = recordAdapter

        integrityAdapter.add(Integrity(portNumber = "0", started = LoadingStatus.NONE, explicitName = "Ц1"))
        integrityAdapter.add(Integrity(portNumber = "1", started = LoadingStatus.NONE, explicitName = "Ц2"))

        relayAdapter.add(MagnetRelay("0", started = LoadingStatus.NONE, explicitName = "РЕЛЕ1"))
        relayAdapter.add(MagnetRelay("1", started = LoadingStatus.NONE, explicitName = "РЕЛЕ2"))
        relayAdapter.add(MagnetRelay("2", started = LoadingStatus.NONE, explicitName = "РЕЛЕ3"))

        generatorAdapter.add(Generator("0", LoadingStatus.NONE, explicitName = "Г1"))
        generatorAdapter.add(Generator("1", LoadingStatus.NONE, explicitName = "Г2"))

        recordAdapter.add(Record("0", LoadingStatus.NONE, maxAmpl = 9.84f, explicitName = "ЗА1"))
        recordAdapter.add(Record("1", LoadingStatus.NONE, maxAmpl = 55.8f, explicitName = "ЗА2"))
        recordAdapter.add(Record("2", LoadingStatus.NONE, maxAmpl = 1023.0f, explicitName = "ЗЦ1"))
        recordAdapter.add(Record("3", LoadingStatus.NONE, maxAmpl = 1023.0f, explicitName = "ЗЦ2"))

        integrityAdapter.startClicks().smartSubscribe(compositeDisposable) {
            sendCommand(it.name + "_" + it.portNumber + "_" + it.started.simplify())
            it.started = LoadingStatus.LOADING
        }

        integrityAdapter.moreClicks().smartSubscribe(compositeDisposable) {
            createDialog(it)?.show()
        }

        relayAdapter.startClicks().smartSubscribe(compositeDisposable) {
            sendCommand(it.name + "_" + it.portNumber + "_" + it.started.simplify())
            it.started = LoadingStatus.LOADING
        }

        relayAdapter.moreClicks().smartSubscribe(compositeDisposable) {
            createDialog(it)?.show()
        }

        generatorAdapter.startClicks().smartSubscribe(compositeDisposable) {
            sendCommand(
                "${it.name}_${it.portNumber}_${it.started.simplify()}_" +
                        "${it.shape.toShapeId()}_${it.ampl.cleverAmpl()}_${it.freq.cleverFreq()}"
            )
            it.started = LoadingStatus.LOADING
        }

        generatorAdapter.moreClicks().smartSubscribe(compositeDisposable) {
            createDialog(it)?.show()
        }

        recordAdapter.startClicks().smartSubscribe(compositeDisposable) {
            sendCommand("${it.name}_${it.portNumber}_${it.started.simplify()}_${it.period}")
            it.started = LoadingStatus.LOADING
        }

        recordAdapter.moreClicks().smartSubscribe(compositeDisposable) {
            createDialog(it)?.show()
        }

        layoutDescription.setOnClickListener {
            sendCommand("Some")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            val filePath = data?.getStringExtra(FilePickerActivity.RESULT_FILE_PATH)
            // Do anything with file
        }
    }

    private fun createDialog(entity: Entity): Dialog? {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.bottom_sheet_more, null)

        dialogView?.let { view ->
            when (entity) {
                is Integrity -> {
                    showIntegrityView(view, entity)
                }
                is MagnetRelay -> {
                    showRelayView(view, entity)
                }
                is Generator -> {
                    showGeneratorView(view, entity)
                }
                is Record -> {
                    showRecordView(view, entity)
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
        when (command) {
            in integrityCommands -> {
                handleIntegrityCommand(command)
            }
            in relayCommands -> {
                handleRelayCommand(command)
            }
            in recordCommands -> {
                handleRecordCommand(command)
            }
            in generatorCommands -> {
                handleGeneratorCommand(command)
            }
            else -> {
                if (command.matches("^[R][0123][_][0-9]{1,4}".toRegex())) {
                    parseRecordFeedback(command)
                }
            }
        }
    }

    private fun sendCommand(command: String) {
        bluetoothConnection?.send("$command\r")
    }

    private fun parseRecordFeedback(command: String) {
        recordAdapter.addRecord(command[1].toInt() - 48, command.substring(3).toInt())

    }

    private fun showIntegrityView(view: View, integrity: Integrity) {
        with(view) {
            textViewSheetName.text = integrity.name
            textViewSheetPort.text = integrity.portNumber

            textViewSheetStatus.text = if (integrity.started.toBoolean()) "Включен" else "Выключен"
            if (integrity.started == LoadingStatus.FAIL) {
                textViewSheetStatus.text = "Обрыв линии"
            }

            if (integrity.startedTime != 0L) {
                textViewSheetStartedTime.text = integrity.startedTime.convertLongToTime()
            }

            if (integrity.workTime != 0L) {
                textViewSheetWorkTime.text = integrity.workTime.convertLongToTimeUTC()
            }
        }
    }

    private fun showRelayView(view: View, relay: MagnetRelay) {
        with(view) {
            textViewSheetName.text = relay.name
            textViewSheetPort.text = relay.portNumber

            textViewSheetStatus.text = if (relay.started.toBoolean()) "Включен" else "Выключен"

            if (relay.startedTime != 0L) {
                textViewSheetStartedTime.text = relay.startedTime.convertLongToTime()
            }

            if (relay.workTime != 0L) {
                textViewSheetWorkTime.text = relay.workTime.convertLongToTimeUTC()
            }
        }
    }

    private fun showGeneratorView(view: View, generator: Generator) {
        with(view) {
            textViewSheetName.text = generator.name
            textViewSheetPort.text = generator.portNumber

            textViewSheetStatus.text = if (generator.started.toBoolean()) "Включен" else "Выключен"

            if (generator.startedTime != 0L) {
                textViewSheetStartedTime.text = generator.startedTime.convertLongToTime()
            }

            if (generator.workTime != 0L) {
                textViewSheetWorkTime.text = generator.workTime.convertLongToTimeUTC()
            }

            editTextAmplitude.setText(generator.ampl.toPrettyValue())
            editTextFrequency.setText(generator.freq.toPrettyValue())

            val spinnerAdapter =
                ArrayAdapter.createFromResource(context, R.array.shape_list, android.R.layout.simple_spinner_item)
            spinnerShape.adapter = spinnerAdapter
            spinnerShape.setSelection(spinnerAdapter.getPosition(generator.shape))

            textViewAdjustParameters.visibility = View.VISIBLE
            layoutAmplitude.visibility = View.VISIBLE
            layoutFrequency.visibility = View.VISIBLE
            layoutSetSignal.visibility = View.VISIBLE

            buttonApply.setOnClickListener {
                var ampl = editTextAmplitude.text.toString().toFloat()
                var freq = editTextFrequency.text.toString().toFloat()
                val shape = spinnerShape.selectedItem.toString()

                if (ampl > generator.maxAmpl) {
                    ampl = generator.maxAmpl
                    editTextAmplitude.setText(ampl.toString())
                }

                if (freq > generator.maxFreq) {
                    freq = generator.maxFreq
                    editTextFrequency.setText(freq.toString())
                }

                generatorAdapter.updateItem(generator, ampl, freq, shape)
                Toast.makeText(context, "Сохранено!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRecordView(view: View, record: Record) {
        with(view) {
            textViewSheetName.text = record.name
            textViewSheetPort.text = record.portNumber

            textViewSheetStatus.text = if (record.started.toBoolean()) "Включен" else "Выключен"

            if (record.startedTime != 0L) {
                textViewSheetStartedTime.text = record.startedTime.convertLongToTime()
            }

            if (record.workTime != 0L) {
                textViewSheetWorkTime.text = record.workTime.convertLongToTimeUTC()
            }

            editTextPeriod.setText(record.period.toString())

            textViewAdjustParameters.visibility = View.VISIBLE
            layoutPeriod.visibility = View.VISIBLE
            graph.visibility = View.VISIBLE

            if (record.map.isNotEmpty()) {
                var series = LineGraphSeries<DataPoint>()
                record.map.forEach {
                    series.appendData(DataPoint(it.key.toDouble() / 1000.0, it.value.toDouble()), true, record.map.size)
                }
                graph.addSeries(series)
                graph.viewport.isScalable = true // enables horizontal zooming and scrolling
                graph.viewport.setScalableY(true)
                graph.viewport.setMaxX(record.map.keys.last() / 1000.0)
            }
            buttonApply.setOnClickListener {
                var period = editTextPeriod.text.toString().toInt()

                if (period < record.minPeriod) {
                    period = record.minPeriod
                    editTextFrequency.setText(period.toString())
                }

                recordAdapter.updateItem(record, period)
                Toast.makeText(context, "Сохранено!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleIntegrityCommand(command: String) {
        when (command) {
            PORT_1_ENABLED -> {
                integrityAdapter.setStatus(0, LoadingStatus.SUCCESS)
            }
            PORT_1_DISABLED -> {
                integrityAdapter.setStatus(0, LoadingStatus.NONE)
            }
            PORT_2_ENABLED -> {
                integrityAdapter.setStatus(1, LoadingStatus.SUCCESS)
            }
            PORT_2_DISABLED -> {
                integrityAdapter.setStatus(1, LoadingStatus.NONE)
            }
            PORT_1_BROKEN -> {
                vibrate()
                integrityAdapter.setStatus(0, LoadingStatus.FAIL)
            }
            PORT_2_BROKEN -> {
                vibrate()
                integrityAdapter.setStatus(1, LoadingStatus.FAIL)
            }
        }
    }

    private fun handleRelayCommand(command: String) {
        when (command) {
            RELAY_1_ENABLED -> {
                relayAdapter.setStatus(0, LoadingStatus.SUCCESS)
            }
            RELAY_2_ENABLED -> {
                relayAdapter.setStatus(1, LoadingStatus.SUCCESS)
            }
            RELAY_3_ENABLED -> {
                relayAdapter.setStatus(2, LoadingStatus.SUCCESS)
            }
            RELAY_1_DISABLED -> {
                relayAdapter.setStatus(0, LoadingStatus.NONE)
            }
            RELAY_2_DISABLED -> {
                relayAdapter.setStatus(1, LoadingStatus.NONE)
            }
            RELAY_3_DISABLED -> {
                relayAdapter.setStatus(2, LoadingStatus.NONE)
            }
        }
    }

    private fun handleGeneratorCommand(command: String) {
        when (command) {
            GENERATOR_1_ENABLED -> {
                generatorAdapter.setStatus(0, LoadingStatus.SUCCESS)
            }
            GENERATOR_2_ENABLED -> {
                generatorAdapter.setStatus(1, LoadingStatus.SUCCESS)
            }
            GENERATOR_1_DISABLED -> {
                generatorAdapter.setStatus(0, LoadingStatus.NONE)
            }
            GENERATOR_2_DISABLED -> {
                generatorAdapter.setStatus(1, LoadingStatus.NONE)
            }
        }
    }

    private fun handleRecordCommand(command: String) {
        when (command) {
            RECORD_1_ENABLED -> {
                recordAdapter.setStatus(0, LoadingStatus.SUCCESS)
            }
            RECORD_1_DISABLED -> {
                recordAdapter.setStatus(0, LoadingStatus.NONE)
            }
            RECORD_2_ENABLED -> {
                recordAdapter.setStatus(1, LoadingStatus.SUCCESS)
            }
            RECORD_2_DISABLED -> {
                recordAdapter.setStatus(1, LoadingStatus.NONE)
            }
            RECORD_3_ENABLED -> {
                recordAdapter.setStatus(2, LoadingStatus.SUCCESS)
            }
            RECORD_3_DISABLED -> {
                recordAdapter.setStatus(2, LoadingStatus.NONE)
            }
            RECORD_4_ENABLED -> {
                recordAdapter.setStatus(3, LoadingStatus.SUCCESS)
            }
            RECORD_4_DISABLED -> {
                recordAdapter.setStatus(3, LoadingStatus.NONE)
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

    private fun showErrorMessage(err: String) {
        Toast.makeText(this, err, Toast.LENGTH_LONG).show()
    }
}


