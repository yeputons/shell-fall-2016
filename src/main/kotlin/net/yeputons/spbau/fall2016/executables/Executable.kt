package net.yeputons.spbau.fall2016.executables

import java.io.*

/**
 * Common interface for all things which could be executed by the shell.
 * Each of <code>stdin</code>/<code>stdout</code> can be run in one of two modes:
 * 1. Default mode. In that mode, <code>Executable.start()</code> creates a
 *    corresponding stream which can be read/written/closed by anybody at any
 *    time without raising errors. Owner of <code>Executable</code> is reponsible
 *    for closing that corresponding stream. If one tries to access <code>stdin</code>
 *    or <code>stdout</code> before calling <code>start()</code>, it may raise an
 *    exception.
 * 2. Inherited mode. In that mode, <code>stdin</code> is assumed to be <code>System.in</code>
 *    (if stdin is redirected), <code>stdout</code> is assumed to be <code>System.out</code>.
 *    One is not allowed to access <code>stdin</code>/<code>stdout</code> properties in that mode,
 *    though implementations are not required to throw an exception in that case.
 *
 * Standard error stream is inherited from current process.
 */
interface Executable {
    /**
     * Starts <code>Executable</code> in background. Optionally sets stdin/stdout
     * in the "inherited" mode (see <code>Executable</code>).
     */
    fun start(inheritStdin: Boolean = false, inheritStdout: Boolean = false): Unit

    /**
     * Returns <code>OutputStream</code> corresponding to <code>Executable</code>'s
     * standard input. Should not be accessed if it's inherited.
     */
    val stdin: OutputStream

    /**
     * Returns <code>InputStream</code> corresponding to <code>Executable</code>'s
     * standard output. Should not be accessed if it's inherited.
     */
    val stdout: InputStream

    /**
     * Returns <code>null</code> if <code>Executable</code> has not been started yet or
     * has not terminated yet. Otherwise, returns its exit code.
     */
    val exitCode: Int?

    /**
     * Blocks until <code>Executable</code> is finished and <code>exitCode</code> is available.
     * Should not be called before <code>start</code>
     */
    fun waitForTermination(): Unit
}
