package net.yeputons.spbau.fall2016

import java.io.File
import java.util.*

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
