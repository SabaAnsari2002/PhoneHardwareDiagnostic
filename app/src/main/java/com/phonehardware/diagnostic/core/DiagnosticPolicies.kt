package com.phonehardware.diagnostic.core

/** Transparent rules, designed for JVM unit testing. */
object DiagnosticPolicies {
    fun touchCoverage(touched: Int, required: Int): Outcome =
        if(required > 0 && touched == required) Outcome.PASSED else Outcome.INCONCLUSIVE

    fun integrityMatch(bytesWritten: Int, bytesRead: Int, hashesEqual: Boolean): Outcome =
        when {
            bytesWritten <= 0 || bytesRead <= 0 -> Outcome.INCONCLUSIVE
            bytesWritten != bytesRead || !hashesEqual -> Outcome.FAILED
            else -> Outcome.PASSED
        }

    fun sensorArrival(sensorPresent: Boolean, eventReceived: Boolean): Outcome =
        when { !sensorPresent -> Outcome.NOT_DETECTED; !eventReceived -> Outcome.INCONCLUSIVE; else -> Outcome.PASSED }
}
