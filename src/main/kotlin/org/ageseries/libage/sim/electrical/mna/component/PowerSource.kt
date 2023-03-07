package org.ageseries.libage.sim.electrical.mna.component

import org.ageseries.libage.sim.electrical.mna.Circuit
import kotlin.math.abs

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

        if(psCircuit.nearlyZero(factor - 1.0)) return false  // close enough

        // We assume a linear relationship between the target and the power. This is perfectly true for LTI circuits,
        // but breaks down in the presence of non-linear components in potentially-exciting ways, including ways that
        // may prevent convergence. (TODO: account for these cases)
        var desTarget = if(psCircuit.nearlyZero(factor)) {
            // Degenerate case: power is very small (absolutely) relative to powerIdeal.
            // This usually happens under open-circuit conditions (ELN calls them "highImpedance").
            // In those cases, just float to the maximum target, or zero if we don't have one.
            targetAbsMax ?: 0.0
        } else {
            target / factor
        }

        targetAbsMax?.also {
            desTarget = desTarget.coerceIn(-it, it)
        }

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
        set(value) { targetAbsMax = value }
    override var powerIdeal: Double = 0.0
    override val psCircuit: Circuit
        get() = circuit!!
}
class PowerCurrentSource: CurrentSource(), PowerSource {
    override var target: Double
        get() = current
        set(value) { current = value }
    override var targetAbsMax: Double? = null
    var currentMax: Double?
        get() = targetAbsMax
        set(value) { targetAbsMax = value }
    override var powerIdeal: Double = 0.0
    override val psCircuit: Circuit
        get() = circuit!!
}
