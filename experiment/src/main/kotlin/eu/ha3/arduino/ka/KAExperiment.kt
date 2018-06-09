package eu.ha3.arduino.ka

import gnu.io.CommPortIdentifier
import gnu.io.SerialPort
import gnu.io.SerialPortEvent
import gnu.io.SerialPortEventListener
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.*

class KAExperiment(portNames: List<String>, baudRate: Int, timeout: Int, private val collectorFn: (String) -> Unit): AutoCloseable {
    private val serialPort: SerialPort
    private val input: BufferedReader
    private val output: OutputStream
    private val serialEventListener: SerialPortEventListener

    init {
        val matchingPort = listAllPorts().find { portNames.contains(it.name) } ?: throw KAExperiment.NoMatchingPortException()

        try {
            serialPort = matchingPort.open(this::class.java.name, timeout) as SerialPort
            serialPort.setSerialPortParams(baudRate,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE)

            input = BufferedReader(InputStreamReader(serialPort.inputStream))
            output = serialPort.outputStream

            serialEventListener = SerialPortEventListener(this::handleSerialEvent)

            serialPort.addEventListener(serialEventListener)
            serialPort.notifyOnDataAvailable(true)

        } catch (e: Exception) {
            throw KAExperiment.CouldNotInitializeException()
        }
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
            e.printStackTrace()
            //errorFn()
        }
    }

    private fun listAllPorts() = Collections.list(CommPortIdentifier.getPortIdentifiers()) as List<CommPortIdentifier>

    class NoMatchingPortException : RuntimeException()
    class CouldNotInitializeException : RuntimeException()

    override fun close() {
        serialPort.removeEventListener()
        serialPort.close()
    }
}