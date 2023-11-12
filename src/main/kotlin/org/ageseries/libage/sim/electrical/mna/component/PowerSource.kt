package org.ageseries.libage.sim.electrical.mna.component

import org.ageseries.libage.debug.dprintln
import org.ageseries.libage.sim.electrical.mna.Circuit
import kotlin.math.abs
import kotlin.math.sqrt

interface IPower {
    val power: Double
}

interface PowerSource: IPower {
    var target: Double
    var targetAbsMax: Double?

    var powerIdeal: Double

    // We expect to be under a Component, probably even a Port
    val psCircuit: Circuit

    fun solve(): Boolean {
        val factor = if(psCircuit.nearlyZero(powerIdeal)) {
            0.0
        } else {
            power / powerIdeal
        }

        dprintln("power $power ideal $powerIdeal factor $factor")
        if(psCircuit.nearlyZero(factor - 1.0)) return false  // close enough

        // We assume a quadratic relationship between the target and the power. This is perfectly true for LTI circuits,
        // but breaks down in the presence of non-linear components in potentially-exciting ways, including ways that
        // may prevent convergence. (TODO: account for these cases)
        var desTarget = if(psCircuit.nearlyZero(factor)) {
            // Degenerate case: power is very small (absolutely) relative to powerIdeal.
            // This usually happens under open-circuit conditions (ELN calls them "highImpedance").
            // In those cases, just float to the maximum target, or zero if we don't have one.
            targetAbsMax ?: 0.0
        } else {
            // Safety: t^2 is always positive, as is abs(factor)
            sqrt(target * target / abs(factor))
        }

        targetAbsMax?.also {
            desTarget = desTarget.coerceIn(-it, it)
        }

        // No change in target--usually because we hit AbsMax
        if(psCircuit.nearlyZero(target - desTarget)) return false

        dprintln("target was $target, now $desTarget")
        target = desTarget
        return true
    }
}

class PowerVoltageSource: VoltageSource(), PowerSource {
    override var target: Double
        get() = potential
        set(value) { potential = value }
    override var targetAbsMax: Double? = null
    var potentialMax: Double?
        get() = targetAbsMax
        set(value) {
            targetAbsMax = value
            potential = potential  // force a rightSide recomputation
        }
    override var powerIdeal: Double = 0.0
    override val psCircuit: Circuit
        get() = circuit!!

    override fun simStep() {
        solve()
        super.simStep()
    }
}
class PowerCurrentSource: CurrentSource(), PowerSource {
    override var target: Double
        get() = current
        set(value) { current = value }
    override var targetAbsMax: Double? = null
    var currentMax: Double?
        get() = targetAbsMax
        set(value) {
            targetAbsMax = value
            current = current  // force a rightSide recomputation
        }
    override var powerIdeal: Double = 0.0
    override val psCircuit: Circuit
        get() = circuit!!

    override fun simStep() {
        solve()
        super.simStep()
    }
}
