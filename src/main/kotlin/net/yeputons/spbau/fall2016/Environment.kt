package net.yeputons.spbau.fall2016

import java.io.File
import java.util.*

/**
 * Holds information about current shell state: current directory and environment variables.
 *
 * Initially constructed empty. One may want to call <code>fromCurrentEnvironment()</code> method
 * to inherit environment variables.
 */
class Environment {
    var currentDirectory : File = File("").absoluteFile
    var variables : MutableMap<String, String> = HashMap()
        private set

    companion object {
        fun fromCurrentEnvironment() : Environment {
            val env : Environment = Environment()
            for ((name, value) in System.getenv().entries) {
                env[name] = value
            }
            return env
        }
    }

    operator fun get(name : String) : String? {
        return variables[name]
    }

    operator fun set(name : String, value : String) {
        variables[name] = value
    }
}
