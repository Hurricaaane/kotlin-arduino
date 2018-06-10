package eu.ha3.arduino.ka

import gnu.io.CommPortIdentifier
import gnu.io.SerialPort
import gnu.io.SerialPortEvent
import gnu.io.SerialPortEventListener
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.*

data class KAExperimentConfig(val portNames: List<String>, val baudRate: Int, val timeout: Int, val eventHandling: IKAExperimentEventDSL.() -> Unit)

class KAExperimentGenerator(private val config: KAExperimentConfig) {
    fun start() = KAExperimentStarted(config)
}

class KAExperimentStarted(private val config: KAExperimentConfig): AutoCloseable {
    private val serialPort: SerialPort
    private val input: BufferedReader
    private val output: OutputStream
    private val serialEventListener: SerialPortEventListener
    private var collectorFn: (String) -> Unit = {}
    private var errorFn: (Exception) -> Unit = {}

    fun stop(): KAExperimentGenerator {
        close()

        return KAExperimentGenerator(config)
    }

    init {
        val matchingPort = listAllPorts().find { config.portNames.contains(it.name) } ?: throw KAExperimentStarted.NoMatchingPortException()

        try {
            serialPort = matchingPort.open(this::class.java.name, config.timeout) as SerialPort
            serialPort.setSerialPortParams(config.baudRate,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE)

            input = BufferedReader(InputStreamReader(serialPort.inputStream))
            output = serialPort.outputStream

            serialEventListener = SerialPortEventListener(this::handleSerialEvent)

            serialPort.addEventListener(serialEventListener)
            serialPort.notifyOnDataAvailable(true)

        } catch (e: Exception) {
            throw KAExperimentStarted.CouldNotInitializeException()
        }

        val dsl = KAExperimentEventDSL()
        config.eventHandling(dsl);
        dsl.terminate()
    }

    override fun close() {
        serialPort.removeEventListener()
        serialPort.close()
    }

    private fun handleSerialEvent(oEvent: SerialPortEvent) {
        if (oEvent.eventType != SerialPortEvent.DATA_AVAILABLE) {
            return
        }

        try {
            if (input.ready()) {
                collectorFn(input.readLine())
            }

        } catch (e: Exception) {
            errorFn(e)
        }
    }

    private fun listAllPorts() = Collections.list(CommPortIdentifier.getPortIdentifiers()) as List<CommPortIdentifier>

    class NoMatchingPortException : RuntimeException()
    class CouldNotInitializeException : RuntimeException()

    inner class KAExperimentEventDSL : IKAExperimentEventDSL {
        private var terminated = false

        override fun onMessage(handler: (message: String) -> Unit) {
            if (terminated) throw IllegalStateException()

            collectorFn = handler;
        }

        override fun onError(handler: (exception: Exception) -> Unit) {
            if (terminated) throw IllegalStateException()

            errorFn = handler
        }

        fun terminate() {
            terminated = true
        }
    }
}

interface IKAExperimentEventDSL {
    fun onMessage(handler: (String) -> Unit)
    fun onError(handler: (Exception) -> Unit)
}
