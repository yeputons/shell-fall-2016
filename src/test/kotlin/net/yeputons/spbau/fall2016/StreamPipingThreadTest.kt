package net.yeputons.spbau.fall2016

import com.nhaarman.mockito_kotlin.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.*
import java.util.concurrent.CountDownLatch

class StreamPipingThreadTest {
    @Test fun testCopiesData() {
        val from = ByteArrayInputStream("hello".toByteArray())
        val to = ByteArrayOutputStream()
        val thread = StreamPipingThread(from, to)
        thread.start()
        thread.join()
        assertEquals("hello", to.toString())
    }

    @Test fun testCopiesDataContinuously() {
        val from = mock<InputStream>()
        val to = ByteArrayOutputStream()
        val thread = StreamPipingThread(from, to)

        val latch1 = CountDownLatch(1)
        val latch2 = CountDownLatch(1)
        val latch3 = CountDownLatch(1)
        whenever(from.read())
                .thenAnswer { latch1.await(); 'a'.toInt() }
                .thenAnswer { latch2.await(); 'b'.toInt() }
                .thenAnswer { latch3.await(); -1 }

        thread.start()
        assertEquals("", to.toString())

        latch1.countDown()
        while (to.toString() != "a") Thread.`yield`()

        latch2.countDown()
        while (to.toString() != "ab") Thread.`yield`()

        latch3.countDown()
        thread.join()

        assertEquals("ab", to.toString())
        verify(from, times(3)).read()
        verify(from, times(1)).close()
    }

    @Test fun testCopiesUntilInputIsBroken() {
        val from = mock<InputStream>()
        val to = ByteArrayOutputStream()
        val thread = StreamPipingThread(from, to)

        whenever(from.read())
                .thenReturn('a'.toInt())
                .thenReturn('b'.toInt())
                .thenThrow(IOException::class.java)
                .thenReturn('c'.toInt())
                .thenReturn(-1)

        thread.start()
        thread.join()

        assertEquals("ab", to.toString())
        verify(from, times(3)).read()
        verify(from, times(1)).close()
    }

    @Test fun testCopiesUntilOutputIsBroken() {
        val from = ByteArrayInputStream("abc".toByteArray())
        val to = mock<OutputStream>()
        val thread = StreamPipingThread(from, to)

        whenever(to.write('b'.toInt())).thenThrow(IOException::class.java)

        thread.start()
        thread.join()

        val order = inOrder(to)
        order.verify(to).write('a'.toInt())
        order.verify(to).write('b'.toInt())
        order.verify(to, times(0)).write('c'.toInt())
        order.verify(to, times(1)).close()
    }

    @RunWith(Parameterized::class)
    class StreamsAreClosedTest(
            val answerLastRead: () -> Int,
            val answerFromClose: () -> Unit,
            val expectedReads: Int,
            val answerLastWrite: () -> Unit,
            val answerToClose: () -> Unit,
            val closeTo: Boolean
    ) {
        companion object {
            @JvmStatic
            @Parameterized.Parameters
            fun data(): List<Array<Any>> {
                return listOf(
                        arrayOf({ -1 }, {}, 3, { -1 }, {}, true),
                        arrayOf({ -1 }, {}, 3, { -1 }, {}, false),
                        arrayOf({ throw IOException() }, {}, 3, { -1 }, {}, true),
                        arrayOf({ -1 }, { throw IOException() }, 3, { -1 }, {}, true),
                        arrayOf({ -1 }, { }, 2, { throw IOException() }, {}, true),
                        arrayOf({ -1 }, { }, 3, { -1 }, { throw IOException() }, true)
                )
            }
        }

        @Test fun testClosesStreams() {
            val from = mock<InputStream>()
            val to = mock<OutputStream>()
            val thread = StreamPipingThread(from, to, closeTo)

            whenever(from.read())
                    .thenReturn('a'.toInt())
                    .thenReturn('b'.toInt())
                    .thenAnswer({ answerLastRead() })
            whenever(from.close())
                    .thenAnswer({ answerFromClose() })
            whenever(to.write('b'.toInt())).thenAnswer({ answerLastWrite() })
            whenever(to.close())
                    .thenAnswer({ answerToClose() })

            thread.start()
            thread.join()

            val orderFrom = inOrder(from)
            orderFrom.verify(from, times(expectedReads)).read()
            orderFrom.verify(from, times(1)).close()

            val orderTo = inOrder(to)
            orderTo.verify(to, times(2)).write(any<Int>())
            if (closeTo) {
                orderTo.verify(to, times(1)).close()
            } else {
                orderTo.verify(to, times(0)).close()
            }
        }
    }
}