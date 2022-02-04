package com.virginiaprivacy.drivers.sdr.exceptions

class TunerNotInitializedException(override val message: String) :
    Exception(StringBuilder().appendLine("The tuner has to be initialized first.").appendLine(message).toString())
