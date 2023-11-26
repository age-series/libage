package org.ageseries.libage.sim.electrical.mna.component

import org.ageseries.libage.debug.dprintln
import org.ageseries.libage.sim.electrical.mna.Circuit

interface IResistor : IPower {
    var resistance: Double
    val current: Double
    val potential: Double
}

/**
 * Implements a simple, static resistor.
 *
 * The most important field is arguably [resistance]; updating this value will result in [Circuit.matrixChanged].
 */
open class Resistor : Port(), IResistor {
    override var name: String = "r"
    override val imageName = "resistor"

    /**
     * The resistance of this resistor, in Ohms.
     *
     * Setting this will cause [Circuit.factorMatrix] to be called on the next step or substep.
     */
    override var resistance: Double = 1.0
        set(value) {
            if(isInCircuit) {
                // Remove our contribution to the matrix (using a negative resistance... should work)
                field = -field
                stamp()
            }

            // Add our new contribution
            field = value
            if(isInCircuit) stamp()
        }

    /**
     * Returns the current through this resistor as a function of its potential and resistance, in Amperes.
     */
    override val current: Double
        get() = potential / resistance

    /**
     * Returns the power dissipated by this resistor, as a function of its current and potential, in Watts.
     */
    override val power: Double get() = current * potential

    override fun detail(): String {
        return "[resistor $name: ${potential}v, ${current}A, ${resistance}Ω, ${power}W]"
    }

    override fun stamp() {
        dprintln("pos=$pos neg=$neg r=$resistance")
        // We have to guard here specifically against stamp() being called out of context from the resistance setter above.
        if(pos != null && neg != null) pos!!.stampResistor(neg!!, resistance)
    }
}

/**
 * A linear series of Resistors.
 *
 * This class is optimized for a common simulation case where, say, a wire is represented by a large number of discrete
 * simulation elements (often Resistors). Matrix size--and thus solve time--goes up \Omega(n^2), O(n^3) with the number
 * of nodes, so these "line graphs" tend to be wasteful of simulation time.
 *
 * Instead, if the line graph property is guaranteed, a much faster KCL/KVL solution exists for a series of resistors,
 * which would be familiar as the typical "voltage divider". The runtime of this algorithm is linear in the number of
 * resistors.
 *
 * The Line is a component, and can be added and [Component.connect]ed as any other. The individual [parts] are _not_
 * [Component]s and, thus, cannot be talked about in [Circuit] terms. They do, however, support many fields of other
 * [Component]s in their own way.
 */
open class Line: Resistor() {
    /**
     * A "Part" of a linear series resistor.
     *
     * Each "Part" is, in a sense, a resistor of its own, with its own [resistance] being of quintessential importance.
     * However, Parts are not [Component]s, and do not participate directly in [Circuit]s. Nonetheless, they can be used
     * similarly:
     * - They implement [potential], [current], and (through [IPower]) [power];
     * - While they do not have [Port.pos] or [Port.neg], they do have [posPotential] and [negPotential];
     * - They have a mutable [resistance], just like [Resistor].
     *
     * These are intended to be used where long series of resistors are to be expected in modeling. If that's less than
     * particularly likely, a [Resistor] itself is a more flexible [Component], at a little greater simulation cost.
     */
    class Part(line: Line, resistance: Double = 1.0): IPower {
        /**
         * The [Line] to which this belongs.
         *
         * This would be the `this@Line` of an inner class, but for its need to change under [Line.merge].
         */
        var line: Line = line
            internal set

        /**
         * The index of this resistor in its [Line].
         *
         * While this is associated with a [Line], this identifier can be used to [Line.remove] it. It is no longer
         * valid after removal.
         */
        var index: Int = -1
            internal set

        /**
         * The absolute potential of the emulated resistor's "negative" terminal, in Volts.
         *
         * This is essentially equivalent to `Resistor.neg.potential`, a la [Port.neg] and [Node.potential]. Equivalent
         * to that property, the "absolute" potential is only with respect to [Circuit.ground], and can not only be
         * positive, but greater than [posPotential] (when "reverse biased").
         */
        var negPotential: Double = 0.0
            internal set

        /**
         * The absolute potential of the emulated resistor's "positive" terminal, in Volts.
         *
         * This is essentially equivalent to `Resistor.pos.potential`, a la [Port.pos] and [Node.potential]. Equivalent
         * to that property, the "absolute" potential is only with respect to [Circuit.ground], and can not only be
         * negative, but less than [negPotential] (when "reverse biased").
         */
        var posPotential: Double = 0.0
            internal set

        /**
         * The potential across this resistor in its bias direction, in Volts.
         *
         * This is the analogue of [Port.potential]; see its notes for the bias direction details.
         */
        val potential: Double get() = posPotential - negPotential

        /**
         * The current through this resistor in its bias direction, in Amperes.
         *
         * This is the analogue of [Resistor.current], computed with Ohm's law--thus only valid when the [potential] is
         * known.
         */
        var current: Double = 0.0
            internal set

        /**
         * The power dissipated by this resistor, in Watts.
         *
         * This is the analogue of [Resistor.power].
         */
        override val power: Double
            get() = potential * current

        /**
         * The resistance of this resistor, in Ohms.
         *
         * Setting this updates the total resistance of the [Line] to which it belongs. This can cause
         * [Circuit.matrixChanged].
         */
        var resistance: Double = resistance
            set(value) {
                field = value
                line.update()
            }
    }

    /**
     * The [Part]s belonging to this Line, in order from [neg] to [pos].
     *
     * This means that index 0 is on the negative bias, and the last index is on the positive bias. This order is
     * important if the absolute potentials ([Part.posPotential], [Part.negPotential]) matter to you.
     */
    val parts: MutableList<Part> = mutableListOf()

    /**
     * Update the [resistance] of this Line, and data of individual [Part]s.
     *
     * This shouldn't need to be manually called--it is invoked whenever [Part.resistance] changes, when [merge] is
     * done, or any other time the [parts] data can change. Nonetheless, it is always safe to call.
     */
    fun update() {
        parts.withIndex().forEach { (index, part) ->
            part.index = index
            part.line = this
        }
        val res = parts.map { it.resistance }.sum()
        if(res != resistance) resistance = res
    }

    override fun postStep(dt: Double) {
        super.postStep(dt)
        val cur = current
        val n = neg!!.potential
        val pot = potential
        var pcur = n
        parts.forEach { part ->
            part.current = cur
            val contr = part.resistance / resistance
            part.negPotential = pcur
            pcur += contr * pot
            part.posPotential = pcur
        }
    }

    /**
     * Add a [Part], returning it.
     *
     * As per [parts], index 0 (the default) is the [neg] terminal side. The last index ([size] - 1) is the [pos]
     * terminal side.
     */
    fun add(index: Int = 0, resistance: Double = 1.0): Part =
        Part(this, resistance).also { parts.add(index, it); update() }

    /**
     * Remove a [Part] at a given index.
     *
     * You can get this from [Part.index], if you have a reference to a [Part].
     *
     * You should drop all references to this [Part] after removal--it can no longer be added to any [Line], and its
     * values will never be updated again.
     */
    fun remove(index: Int): Part = parts.removeAt(index)

    /**
     * Return the number of [Part]s in this Line.
     */
    val size: Int get() = parts.size

    /**
     * Merge two Lines, returning the winner.
     *
     * This is intended to connect two adjacent Lines. [positive] is from the perspective of `this` line--if true,
     * [other]'s [parts]' negative terminal will be appended to the positive terminal of `this`, otherwise [other]'s
     * positive terminal will be connected to our negative terminal.
     *
     * The return value is a Line, intended to be the sole surviving instance of the merge. Presently, this is always
     * `this`, but this is a detail subject to change. Any references to `this` and [other] should be replaced with the
     * return value of this function.
     *
     * It is an invariant that all [Part]s belonging to either Line before the merge will remain valid and associated
     * with the Line that is returned.
     */
    fun merge(other: Line, positive: Boolean): Line = this.apply {
        val old = parts.toList()
        parts.clear()
        val merged = if(positive) {
            old.asSequence() + other.parts.asSequence()
        } else {
            other.parts.asSequence() + old.asSequence()
        }
        parts.addAll(merged)
        update()
    }
}
