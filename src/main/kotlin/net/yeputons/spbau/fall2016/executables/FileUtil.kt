package net.yeputons.spbau.fall2016.executables

import net.yeputons.spbau.fall2016.Environment
import java.io.File

fun Environment.getFile(name: String): File {
    val file = File(name)
    if (file.isAbsolute()) {
        return file
    } else {
        return File(currentDirectory, name)
    }
}