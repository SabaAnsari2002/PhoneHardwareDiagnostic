package com.phonehardware.diagnostic

import com.phonehardware.diagnostic.core.DiagnosticPolicies
import com.phonehardware.diagnostic.core.Outcome
import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticPoliciesTest {
    @Test fun missingOptionalSensorIsNotFailure() {
        assertEquals(Outcome.NOT_DETECTED,DiagnosticPolicies.sensorArrival(false,false))
    }
    @Test fun silentSensorIntervalIsInconclusive() {
        assertEquals(Outcome.INCONCLUSIVE,DiagnosticPolicies.sensorArrival(true,false))
    }
    @Test fun untouchedCellsNotCalledDefective() {
        assertEquals(Outcome.INCONCLUSIVE,DiagnosticPolicies.touchCoverage(55,60))
        assertEquals(Outcome.PASSED,DiagnosticPolicies.touchCoverage(60,60))
    }
    @Test fun integrityReadMismatchIsFailure() {
        assertEquals(Outcome.FAILED,DiagnosticPolicies.integrityMatch(100,90,false))
        assertEquals(Outcome.PASSED,DiagnosticPolicies.integrityMatch(100,100,true))
    }
}
