package eu.ha3.arduino.ka

import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
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
        // Setup
        val onMessageMock: (String) -> Unit = mock {
            on { invoke(any()) }.doReturn(Unit)
        }
        val onErrorMock: (Exception) -> Unit = mock()

        // Exercise
        KAExperimentGenerator(KAExperimentConfig(portNames = listOf("COM5"), baudRate = 9600, timeout = 2000) {
            onMessage(onMessageMock)
            onError(onErrorMock)

        }).start().apply {
            Thread.sleep(10_000)

        }.stop()

        // Verify
        val expected = "sensor = .*\t diode = .*".toRegex()
        argumentCaptor<String>().apply {
            verify(onMessageMock, atLeast(100)).invoke(capture())

            assertThat(allValues).allMatch({ it.matches(expected) })
        }
        verifyZeroInteractions(onErrorMock)
    }
}
