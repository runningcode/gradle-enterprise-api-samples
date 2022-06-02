package com.gradle.enterprise.api

import kotlinx.coroutines.runBlocking
import picocli.CommandLine
import kotlin.system.exitProcess


fun main(args: Array<String>)  = runBlocking<Unit> {
    exitProcess(CommandLine(CommandLineArgumentProcessor()).execute(*args))
}