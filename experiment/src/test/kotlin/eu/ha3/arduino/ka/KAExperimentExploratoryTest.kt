package eu.ha3.arduino.ka

import org.junit.jupiter.api.Test

/**
 * (Default template)
 * Created on 2018-06-10
 *
 * @author Ha3
 */
public class KAExperimentExploratoryTest {
    @Test
    public fun `this is exploratory code, read from arduino for 10 seconds`() {
        KAExperiment(portNames = listOf("COM5"), baudRate = 9600, timeout = 2000) {
            println(it)

        }.use {
            Thread.sleep(10_000)
        }
    }
}