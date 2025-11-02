package org.ageseries.libage.sim.electrical

import org.ageseries.libage.data.Quantity
import org.ageseries.libage.data.SECOND
import org.ageseries.libage.mathematics.approxEq
import org.ageseries.libage.sim.electrical.ElectricalComponent.Companion.ID_GENERATOR
import org.ageseries.libage.sim.electrical.ElectricalNode.Companion.GROUND_ID
import org.ageseries.libage.sim.electrical.ElectricalNode.Companion.VIRTUAL_ID
import org.ageseries.libage.utils.measureDuration
import org.ejml.data.DMatrixRMaj
import org.ejml.data.DMatrixSparseCSC
import org.ejml.dense.row.CommonOps_DDRM
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM
import org.ejml.interfaces.linsol.LinearSolverDense
import org.ejml.sparse.FillReducing
import org.ejml.sparse.csc.factory.LinearSolverFactory_DSCC
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * Representation for an electrical pin. This allows the downstream code to deal with localized pins of components, instead of nodes.
 * The pins that are connected together form a `Pin Star`, which will be converted into an electrical node.
 * */
class ElectricalPin(val component: ElectricalComponent, val symbol: String) {
    companion object {
        private val ID_GENERATOR = AtomicInteger()
    }

    /**
     * Gets the friendly [toString] ID (see [ID_GENERATOR]).
     * */
    val displayId = ID_GENERATOR.getAndIncrement()

    private var nodeInternal: ElectricalNode? = null
    val node get() = if(component.isInSimulation) nodeInternal!! else error("Cannot get node of pin from $component. Not added to simulation!")

    /**
     * Sets the generated electrical node. It can be a real node in the simulation, the ground node, or a virtual node from the optimizer.
     * */
    internal fun setNode(node: ElectricalNode) {
        check(nodeInternal == null) {
            "Tried to set new node for pin $this, but previous simulation wasn't destroyed!"
        }

        nodeInternal = node
    }

    /**
     * Gets the potential of the node.
     * */
    val potential: Double get() {
        val node = nodeInternal
            ?: return 0.0

        return node.potential
    }

    fun simulationDestroyed() {
        nodeInternal = null
    }

    override fun toString() = "Pin$displayId$symbol$component"
}

/**
 * Data holder and stamping rule for electrical components.
 * The pin configuration must be specified (see [Port], which is the most common and probably only useful configuration).
 * */
abstract class ElectricalComponent {
    companion object {
        /**
         * Friendly number generated for each component, used by [toString].
         * Instead of showing `Resistor` + some hash code, we show `Resistor0`, `Resistor1`, `PotentialSource0`, and so on.
         * */
        private val ID_GENERATOR = ConcurrentHashMap<Class<*>, AtomicInteger>()
    }

    /**
     * Gets the friendly [toString] ID (see [ID_GENERATOR]).
     * */
    val displayId = ID_GENERATOR
        .computeIfAbsent(this.javaClass) { AtomicInteger() }
        .getAndIncrement()

    private var simulationInternal: ElectricalSimulation? = null
    val simulation get() = simulationInternal ?: error("Cannot get simulation of component $this. Not added to simulation!")
    val isInSimulation get() = simulationInternal != null

    /**
     * Used when component values are updated.
     * If true, it means the component was stamped with its previous values in [stamp].
     * If that's the case, then the previous contribution must be updated.
     * If false, then nothing needs to be done for the simulation.
     * */
    val isStamped get() = isInSimulation && simulation.constructed

    /**
     * Stamps the structure of this component, and the initial knowns vector entry.
     * This is called only once.
     * */
    internal open fun stamp() { }

    /**
     * Called before the system is solved, in order to update the companion model.
     * */
    internal open fun prepareStep() { }

    /**
     * Called after the system is solved and the results have been copied into the wrapper classes.
     * */
    internal open fun finishStep() { }

    internal open fun setSimulation(simulation: ElectricalSimulation) {
        simulationInternal = simulation
    }

    internal open fun simulationDestroyed() {
        simulationInternal = null
    }

    override fun toString() = "${this.javaClass.simpleName}$displayId"

    /**
     * List of all pins in the component.
     * */
    abstract val allPins: List<ElectricalPin>
}

/**
 * The repository for [ElectricalReadoutRepositoryLayer].
 * */
interface ReadoutRepository {
    /**
     * Reads and stores the values from the [ElectricalComponent].
     * */
    fun loadValues()
}

/**
 * [ElectricalComponent] with a double-buffer functionality.
 * We may read some values from the game thread, e.g. for display.
 * But it is very common to catch values during nonlinear iteration and other things.
 * Repositories offer logic to form a complete picture and swap it atomically after each complete step.
 * */
interface ReadoutElectricalComponent<Self, Repository>
        where Self : ReadoutElectricalComponent<Self, Repository>, Self : ElectricalComponent,
              Repository : ReadoutRepository
{
    /**
     * Gets the double-buffering layer for the readouts.
     * */
    val repositoryLayer: ElectricalReadoutRepositoryLayer<Repository>

    /**
     * Gets the data readouts for this component.
     * */
    val readouts: Repository get() = repositoryLayer.presentedCopy
}

/**
 * Double-buffer for readout repositories. [ReadoutElectricalComponent.swap] will swap the references betwen them.
 * */
class ElectricalReadoutRepositoryLayer<Repository : ReadoutRepository>(factory: () -> Repository) {
    var presentedCopy = factory()
        private set

    private var unpresentedCopy = factory()

    /**
     * Used to load the initial values of the component, before the simulation starts running.
     * */
    internal fun loadPresentation() {
        presentedCopy.loadValues()
    }

    /**
     * Loads the new values into [unpresentedCopy], and swaps the two copies.
     * */
    internal fun loadAndSwap() {
        unpresentedCopy.loadValues()
        val temp = presentedCopy
        presentedCopy = unpresentedCopy
        unpresentedCopy = temp
    }
}

/**
 * A node is a set of connected pins. It can contain 1, 2, or more [ElectricalPin]s.
 * When [id] is [GROUND_ID], then this node is a ground. When it is [VIRTUAL_ID], then it is a virtual node for optimized components.
 * */
class ElectricalNode(val id: Int, val pins: Array<ElectricalPin>) {
    companion object {
        const val GROUND_ID = -1
        const val VIRTUAL_ID = -2
    }

    val isGround get() = id == GROUND_ID
    val isVirtual get() = id == VIRTUAL_ID

    /**
     * Set by the solver or by the optimized system.
     * */
    var potential = 0.0
        internal set
}

/**
 * A port is a component with two terminals, conventionally named [positive] and [negative].
 * The name is from circuit theory: a pair of two terminals obeying the port condition: the currents flowing into the two nodes must be equal and opposite.
 * */
abstract class Port : ElectricalComponent() {
    val positive = ElectricalPin(this, Pole.Positive.symbol)
    val negative = ElectricalPin(this, Pole.Negative.symbol)

    override val allPins = listOf(positive, negative)

    /**
     * Gets the potential across the device.
     * */
    open val potential: Double get() = positive.potential - negative.potential

    override fun simulationDestroyed() {
        super.simulationDestroyed()

        positive.simulationDestroyed()
        negative.simulationDestroyed()
    }

    /**
     * Gets the pin corresponding to the [pole].
     * */
    fun getPin(pole: Pole) = when(pole) {
        Pole.Positive -> positive
        Pole.Negative -> negative
    }

    enum class Pole(val symbol: String) {
        Positive("+"),
        Negative("-");

        val opposite get() = when(this) {
            Positive -> Negative
            Negative -> Positive
        }
    }
}

/**
 * Used to model components with a companion model consisting of a conductance in parallel with a current source.
 * */
abstract class NortonSystem : Port() {
    /**
     * Gets the equivalent resistance of the Norton system.
     * @param value The characteristic value.
     * */
    protected open fun getNortonResistance(value: Double) : Double = value

    /**
     * Stamps the equivalent conductance of the Norton system.
     * */
    override fun stamp() {
        simulation.system.stampResistance(
            positive.node,
            negative.node,
            getNortonResistance(componentValue)
        )
    }

    /**
     * The component's characteristic. Causes a change in the conductance.
     * A property that is named accordingly must be created, that just defers to this property.
     *
     * P.S. The current validation only checks for weird values or 0.
     * This isn't enough; since the equivalent resistance is proportional to this value, then some limits should be imposed to make sure the solver doesn't fail (just like [Resistor] has limits).
     * I don't have time to figure out these values right now, so validation will be left as-is for now.
     * */
    protected var componentValue: Double = 1.0
        set(value) {
            // Accept positive, except NaN and infinity:
            if(field != value) {
                if(value < 0.0) {
                    error("Invalid negative component value $value!")
                }

                if(value.isNaN() || value.isInfinite()) {
                    error("Invalid component value $value!")
                }

                if(isStamped) {
                    simulation.system.changeResistance(
                        positive.node,
                        negative.node,
                        getNortonResistance(field),
                        getNortonResistance(value)
                    )
                }

                field = value
            }
        }

    /**
     * Changes the current injected by the Norton system.
     * */
    var nortonCurrent: Double = 0.0
        protected set(value) {
            if(value.isNaN() || value.isInfinite()) {
                error("Invalid Norton current $value")
            }

            if(field != value) {
                if(isStamped) {
                    simulation.system.changeCurrentKnown(
                        positive.node,
                        negative.node,
                        field,
                        value
                    )
                }

                field = value
            }
        }

    /**
     * The actual current over the device.
     * */
    val current: Double get() = if(isInSimulation) {
        potential / getNortonResistance(componentValue) - nortonCurrent
    }
    else {
        0.0
    }

    /**
     * Gets the current Norton conductance.
     * */
    val nortonConductance get() = 1.0 / getNortonResistance(componentValue)

    /**
     * Resets the Norton current.
     * Not doing so creates an invalid state in the new simulation.
     * */
    override fun simulationDestroyed() {
        super.simulationDestroyed()

        nortonCurrent = 0.0
    }
}

/**
 * Electrical circuit simulation with an immutable component set and an immutable connection set.
 *
 * Inductors and capacitors are approximated by a Norton systems.
 * Other special components (the power sources, for example) are also implemented as Norton systems.
 *
 * The [PowerSource]s are nonlinear components that do not fit into the usual MNA components.
 * They are coupled electrically, so they must be solved simultaneously. They are also well-constrained by the potential constraint.
 * This creates a nonlinear system of equations with a unique solution that must be solved. This is done after [ElectricalComponent.prepareStep], and after the MNA matrix is factored.
 * The system is solved using a *Quasi-Newton method (Broyden's Method)*, with a line search. The system size is `NxN`, where `N` is the number of sources. So it can get expensive with many [PowerSource]s.
 * For a step, an initial Jacobian is computed analytically (in most cases), and then updated based on further residual evaluations. The system is solved with a dense LU decomposition.
 * The result is the unknown Norton currents to apply to the sources so their potential constraint is satisfied and the target power is reached, if possible.
 * The [PowerSource]s are intended as precise energy transfer devices (the upper bound on the energy released in a step is controlled precisely).
 * This is useful for implementing advanced devices such as the DC-DC converter.
 *
 * @param components Real components (that will be included in the system). Components that were optimized away don't show up here.
 * @param pinForest The pin forest. Used to construct the nodes.
 * */
@Suppress("NOTHING_TO_INLINE")
class ElectricalSimulation(val dt: Double, val components: Array<ElectricalComponent>, pinForest: Map<ElectricalCircuitCompiler.PinDisjointSet, Set<ElectricalPin>>) {
    companion object {
        // Values chosen by feeling (they usually don't disrupt the solvers):
        const val MIN_RESISTANCE = 1e-6
        const val MAX_RESISTANCE = 1e9

        /**
         * If true, the simulation may become much slower, but extra safeguards are enabled to detect some implementation errors.
         * */
        var USE_VALIDATION = false
    }

    var destroyed = false
        private set

    /**
     * If true, components need to start applying deltas when their values change (they have been stamped).
     * */
    var constructed = false
        private set

    /**
     * Utility class for creating and manipulating the MNA matrices.
     * Has methods for stamping conductance, potential sources, and known currents and potentials.
     * */
    class System(val simulation: ElectricalSimulation) {
        /**
         * The size of the system matrix ([matrixSize] × [matrixSize]).
         * It is the *number of nodes* + *number of potential sources*.
         * */
        val matrixSize = simulation.nodes.size + simulation.potentialSources.size
        /**
         * The system matrix. Once all the components are stamped, its structure is considered locked.
         * */
        val matrix = DMatrixSparseCSC(matrixSize, matrixSize)
        /**
         * The knowns vector. Implemented as a dense matrix.
         * */
        val known = DMatrixRMaj(matrixSize, 1)
        /**
         * The unknowns vector. Implemented as a dense matrix.
         * */
        val unknown = DMatrixRMaj(matrixSize, 1)

        /**
         * In debug mode, it is used to ensure the structure of the matrix isn't mutated during simulation.
         * */
        private class Validator(val stamper: System) {
            val matrixEntries = HashSet<Pair<Int, Int>>()

            inline fun validateMatrix(row: Int, col: Int) {
                val isNew = matrixEntries.add(Pair(row, col))

                if(stamper.constructionMode) {
                    return
                }

                if(isNew) {
                    error("Mutated matrix structure after init!")
                }
            }

            inline fun requireConstruction() {
                if(!stamper.constructionMode) {
                    error("Cannot perform this action outside of system construction!")
                }
            }
        }

        /**
         * If true, the structure is allowed to mutate.
         * */
        var constructionMode = true
            private set

        private var validator = if(USE_VALIDATION) {
            Validator(this)
        }
        else {
            null
        }

        fun endConstruction() {
            constructionMode = false
        }

        /**
         * Increases the value in the specified cell at [row], [col] in the system [matrix] by [value].
         * */
        private inline fun incrementInMatrix(row: Int, col: Int, value: Double) {
            validator?.validateMatrix(row, col)
            val previous = matrix.get(row, col)
            matrix.set(row, col, previous + value)
        }

        /**
         * Increases the value in the specified [row] in the [known] vector by [value].
         * */
        private inline fun incrementInVector(row: Int, value: Double) {
            val previous = known.get(row)
            known.set(row, 0, previous + value)
        }

        /**
         * Stamps a conductance between the two nodes. Legal to call if and only if, during construction, a call for the same nodes was done so the entries were created.
         * */
        fun stampResistance(nodeP: ElectricalNode, nodeN: ElectricalNode, resistance: Double) {
            val conductance = 1.0 / resistance

            val groundNode = simulation.groundNode

            // Build diagonal:
            if(nodeP != groundNode) {
                incrementInMatrix(nodeP.id, nodeP.id, conductance)
            }

            if(nodeN != groundNode) {
                incrementInMatrix(nodeN.id, nodeN.id, conductance)
            }

            // Build off-diagonal:
            if(nodeP != groundNode && nodeN != groundNode) {
                incrementInMatrix(nodeP.id, nodeN.id, -conductance)
                incrementInMatrix(nodeN.id, nodeP.id, -conductance)
            }

            simulation.setMatrixChanged()
        }

        /**
         * Changes a conductance between the two nodes.
         * Legal to call if and only if, during construction, a [stampResistance] call for the same nodes was done so the entries were created.
         * Evidently, the [previous] should have been contributed previously, or this call will result in undefined behavior.
         * */
        fun changeResistance(nodeP: ElectricalNode, nodeN: ElectricalNode, previous: Double, new: Double) {
            /**
             * We do not apply the delta update here.
             * The difference between [previous] and [new] can be very small, so when we compute the conductance of the delta, we can have issues.
             * */
            stampResistance(nodeP, nodeN, -previous)
            stampResistance(nodeP, nodeN, new)
        }

        /**
         * Stamps the topological information for a potential source. **Only legal to call during construction.**
         * */
        fun stampVoltageStructure(i: Int, nodeP: ElectricalNode, nodeN: ElectricalNode) {
            validator?.requireConstruction()

            val column = simulation.nodes.size + i
            val groundNode = simulation.groundNode

            if(nodeP != groundNode) {
                incrementInMatrix(nodeP.id, column, 1.0)
                incrementInMatrix(column, nodeP.id, 1.0)
            }

            if(nodeN != groundNode) {
                incrementInMatrix(nodeN.id, column, -1.0)
                incrementInMatrix(column, nodeN.id, -1.0)
            }

            simulation.setMatrixChanged()
        }

        /**
         * Increments the cell [i] in the potentials vector. Legal to call anywhere.
         * */
        fun stampVoltageKnown(i: Int, potential: Double) {
            incrementInVector(simulation.nodes.size + i, potential)
            simulation.setKnownsChanged()
        }

        /**
         * Changes a potential in the cell [i] from [previous] to [new]. Legal to call anywhere.
         * */
        fun changeVoltageKnown(i: Int, previous: Double, new: Double) {
            stampVoltageKnown(i, new - previous)
        }

        /**
         * Adds a current flowing from [nodeN] to [nodeP]. Legal to call anywhere.
         * */
        fun stampCurrentKnown(nodeP: ElectricalNode, nodeN: ElectricalNode, current: Double) {
            val groundNode = simulation.groundNode

            if(nodeP != groundNode) {
                incrementInVector(nodeP.id, current)
            }

            if(nodeN != groundNode) {
                incrementInVector(nodeN.id, -current)
            }

            simulation.setKnownsChanged()
        }

        /**
         * Changes a current flowing from [nodeN] to [nodeP] from [previous] to [new]. Legal to call anywhere.
         * */
        fun changeCurrentKnown(nodeP: ElectricalNode, nodeN: ElectricalNode, previous: Double, new: Double) {
            stampCurrentKnown(nodeP, nodeN, new - previous)
        }
    }

    /**
     * Determines how much the solution vector changes when an input parameter is perturbed.
     *
     * Solves for the partial derivative of an internal state variable (node potential V and source current Iv, which together form the solutions for our MNA) with respect to an external control variable.
     * Can be used to analyze the circuit using Thévenin's theorem (I know Grissess loves that), among other things.
     * */
    class SensitivityAnalysis(val simulation: ElectricalSimulation) {
        private val rhs = DMatrixRMaj(simulation.system.matrixSize, 1)
        val results = DMatrixRMaj(simulation.system.matrixSize, 1)

        fun solve(nodeP: ElectricalNode?, nodeN: ElectricalNode?) {
            rhs.zero()

            if (nodeP != simulation.groundNode) {
                rhs.set(nodeP!!.id, 0, 1.0)
            }

            if (nodeN != simulation.groundNode) {
                rhs.set(nodeN!!.id, 0, -1.0)
            }

            // Solve G × (dx/dINk) = (db/dINk) for (db/dINk)
            simulation.solver.solve(rhs, results)
        }
    }

    /**
     * Solver used for the power sources **only**.
     * The solver looks to bring the residual `F = targetPower - power + penalty` to 0 for all sources.
     * Only sources were implemented like this because `targetPower` is difficult to compute for consumers (unknown circuit capacity).
     * For sources, the `targetPower` lies somewhere in the range `[0, desiredPower]`, and it is found by applying the potential constraint of the source.
     * For the power consumers, we cannot readily know how much power to ask for.
     * */
    @Suppress("PrivatePropertyName")
    class NonlinearSourceSolver(val simulation: ElectricalSimulation) {
        private val size = simulation.powerSources.size
        /**
         * The (approximate) Jacobian:
         * */
        private val J = DMatrixRMaj(size, size)
        /**
         * The power residual:
         * */
        private val F = DMatrixRMaj(size, 1)
        /**
         * RHS for the Newton solve (-[F]):
         * */
        private val MinusF = DMatrixRMaj(size, 1)
        /**
         * Residuals after solve and apply new unknowns:
         * */
        private val FNew = DMatrixRMaj(size, 1)
        /**
         * Step produced by Newton:
         * */
        private val delta = DMatrixRMaj(size, 1)
        private val solver: LinearSolverDense<DMatrixRMaj> = LinearSolverFactory_DDRM.lu(size)

        // Temporary storage:

        // FDM:
        private val f0 = DMatrixRMaj(size, 1)
        private val f1 = DMatrixRMaj(size, 1)
        private val x1 = DoubleArray(size)

        // Step:
        private val x = DoubleArray(size)
        private val xNew = DoubleArray(size)
        private val yMinusJs = DMatrixRMaj(size, 1)
        private val Js = DMatrixRMaj(size, 1)

        // Line search:
        var lineSearchC = 1e-4
        var lineSearchReduction = 0.5
        var lineSearchMaxSteps = 10 // Each one is a new residual calculation. Better give up and update the Jacobian.

        var lastSystemEvaluations = 0
            private set

        var lastMaxResidual = 0.0
            private set

        var lastMaxDelta = 0.0
            private set

        /**
         * Gets the residual for [device].
         * The residual takes into account the desired power, but instead becomes a penalty when the potential constraint is violated.
         * */
        fun getResidualForDevice(device: PowerSource) : Double {
            val desiredPower = device.targetPower.coerceIn(0.0, device.maxPower)
            val potentialConstraint = device.maxPotential
            val potentialAbs = abs(device.potential)

            if(potentialAbs > potentialConstraint) {
                // Outside the potential constraint. Add a proportional penalty term.
                return -(potentialAbs - potentialConstraint) - device.power // Do we remove this subtraction?
            }

            val blendStart = potentialConstraint * device.blendRegion

            if (potentialAbs <= blendStart) {
                // Constant power region:
                return desiredPower - device.power
            }

            // Blending region:
            val k = 1.0 - (potentialAbs - blendStart) / (potentialConstraint - blendStart)

            return k * desiredPower - device.power
        }

        /**
         * Evaluates residual `f(x) = (targetPower - measuredPower OR penalty)`, by setting the Norton currents and solving the system.
         * @param x The Norton currents to apply.
         * @param out The result vector.
         * */
        fun evaluateResidual(x: DoubleArray, out: DMatrixRMaj) : Double {
            ++lastSystemEvaluations

            /**
             * Updates the RHS:
             * */
            for (i in 0 until size) {
                simulation.powerSources[i].setCurrentFromSolver(x[i])
            }

            // Solve circuit for new power:
            if (simulation.knownsChanged) {
                simulation.solver.solve(simulation.system.known, simulation.system.unknown)
                simulation.copyResults()
                simulation.knownsChanged = false
            }

            var maxResidual = 0.0

            simulation.powerSources.forEachIndexed { i, device ->
                val powerResidual = getResidualForDevice(device)

                out.set(i, powerResidual)

                val magnitude = abs(powerResidual)
                if (magnitude > maxResidual) {
                    maxResidual = magnitude
                }
            }

            return maxResidual
        }

        /**
         * Evaluates the Jacobian using forward differences (N + 1 solves).
         * It uses a very simple adaptive step size.
         * */
        private fun computeJacobianFallback() {
            J.zero()
            x.copyInto(x1)
            evaluateResidual(x, f0)

            val factor = 1e-2
            val currentScale = 1.0 // A
            val maxStep = 10.0 // A

            for (j in 0 until size) {
                val dI = min(maxStep, factor * max(abs(0.01 * x1[j]), currentScale))

                x1[j] += dI
                evaluateResidual(x1, f1)
                x1[j] -= dI

                for (i in 0 until size) {
                    val differential = (f1.get(i) - f0.get(i)) / dI
                    J.set(i, j, differential)
                }
            }
        }

        /**
         * Computes the Jacobian analyically.
         * It can be degenerate, so we use [computeJacobianFallback] if it happens.
         * */
        @Suppress("LocalVariableName", "SpellCheckingInspection")
        private fun computeJacobian() {
            J.zero()

            val size = simulation.powerSources.size
            val sensitivity = simulation.sensitivity.results

            var norm = 0.0

            for (k in 0 until size) { // k is the column index (d/dINk)
                val deviceK = simulation.powerSources[k]

                ++lastSystemEvaluations
                simulation.sensitivity.solve(
                    deviceK.positive.node,
                    deviceK.negative.node
                )

                /**
                 * The partial potential derivative (dVi/dINk) is derived from the sensitivity.
                 * `i` is the row index (dFi/d...)
                 */
                for (i in 0 until size) {
                    val device_i = simulation.powerSources[i]
                    val potential_i = device_i.potential

                    val positive_i = device_i.positive.node
                    val negative_i = device_i.negative.node

                    val s1 = if(positive_i.isGround) 0.0 else sensitivity[positive_i.id]
                    val s2 = if(negative_i.isGround) 0.0 else sensitivity[negative_i.id]

                    // dVi/dINk: How potential of source i changes
                    // with respect to Norton current of source k
                    val dVi_dINk = s1 - s2

                    val INi = device_i.nortonCurrent
                    val RNi = device_i.characteristicResistance // Constant

                    // Term 1: Calculate d(P_actual,i) / d(INk)
                    var dPa_dINk = (INi - 2.0 * potential_i / RNi) * dVi_dINk
                    if (i == k) {
                        dPa_dINk += potential_i
                    }

                    // TERM 2: Calculate d(Ptargeti) / d(INk)
                    var dPt_dVi = 0.0 // This is d(P_target_i) / d(V_i)
                    val Vi = potential_i
                    val V = abs(Vi)
                    val C = device_i.maxPotential
                    val Vb = C * device_i.blendRegion

                    if (V > C) {
                        // Penalty region: Pt = -(V - C)
                        dPt_dVi = -sign(Vi)
                    } else if (V > Vb) {
                        // Blend region: Pt = Pd * (1 - k)
                        val Pd = device_i.targetPower.coerceIn(0.0, device_i.maxPower)
                        val dV_dVb = C - Vb

                        // Avoid division by zero if blendRegion = 1.0
                        if (dV_dVb > 1e-9) {
                            dPt_dVi = -Pd / dV_dVb * sign(Vi)
                        }
                    }

                    val dPt_dINk = dPt_dVi * dVi_dINk
                    val Jik = dPt_dINk - dPa_dINk

                    J.set(i, k, Jik)

                    norm += Jik * Jik
                }
            }

            if(norm.approxEq(0.0, 1e-3)) {
                // Seems dangerous.
                computeJacobianFallback()
            }
        }

        /**
         * Checks if any values inside the [delta] are infinite or NaN.
         * This doesn't mean it is strictly valid; numerical errors could have driven it to bad (very large) values.
         * */
        private fun isDeltaValid() = !delta.data.any { it.isInfinite() || it.isNaN() }

        /**
         * Solve J × δ = -F for δ.
         * ~Handles a failed factorization by trying to regularize the Jacobian by adding a multiple of the identity matrix to the Jacobian and solving again.
         * If that fails, it gives up.
         * @return True if the method "succeeded". False if nothing could be done.
         * */
        private fun solveForDelta() : Boolean {
            try {
                solver.setA(J)
                solver.solve(MinusF, delta)

                if(isDeltaValid()) {
                    return true // Factorization successful
                }
            } catch(e: Exception) {
                // Factorization failed
            }

            // Tries to regularize the Jacobian:
            var maxDiagonal = 1.0
            for (i in 0 until size) {
                val value = abs(J.get(i, i))
                if (value > maxDiagonal) {
                    maxDiagonal = value
                }
            }

            val diagonalScaleFactor = 1e-6
            var lambda = diagonalScaleFactor * maxDiagonal

            // Add lambda * I to the matrix:
            for (attempt in 0 until 10) {
                for (i in 0 until size) {
                    J.set(i, i, J.get(i, i) + lambda)
                }

                try {
                    solver.setA(J) // attempt to factor
                    solver.solve(MinusF, delta)

                    if(isDeltaValid()) {
                        return true // Successful
                    }
                } catch (e: Exception) {
                    // Restore diagonal before next trial:
                    for (i in 0 until size) {
                        J.set(i, i, J.get(i, i) - lambda)
                    }

                    // Increase factor:
                    lambda *= 10.0
                }
            }

            // GG
            return false
        }

        /**
         * Solves the nonlinear system for the Norton currents that satisfy the devices.
         *
         * Quasi-Newton method (Broyden's Method).
         * We get an initial Jacobian analytically, and then update that Jacobian based on further residual evaluations, unless it is deemed that a new one is needed.
         * Uses a line search ([lineSearchC], [lineSearchReduction], [lineSearchMaxSteps])
         * The linear system is solved with as a dense system. When this fails, the Jacobian is regularized and the factorization is re-attempted.
         * If that also fails, a new Jacobian is computed. If the solve fails now too, then GG.
         * @return The number of iterations.
         * */
        fun solve() : Int {
            lastSystemEvaluations = 0

            for (i in 0 until size) {
                x[i] = simulation.powerSources[i].nortonCurrent
            }

            lastMaxResidual = evaluateResidual(x, F)

            if (lastMaxResidual <= simulation.powerSourceResidualTolerance) {
                lastMaxDelta = 0.0
                return 0 // Already in a good state.
            }

            var iterations = 0

            /**
             * Set in the loop to mark that the Jacobian needs to be recomputed (done at the start of the loop).
             * */
            var recomputeJacobian = true // Computes the initial Jacobian.

            /**
             * The number of times the jacobian has been re-used.
             * Used for periodic recompute (restart).
             * */
            var jacobianReuseCount = 0

            while (iterations < simulation.powerSourceMaxIterations) {
                iterations++

                if(recomputeJacobian) {
                    computeJacobian()
                    recomputeJacobian = false
                    jacobianReuseCount = 0
                }
                else {
                    jacobianReuseCount++
                    if(jacobianReuseCount % simulation.powerSourceJacobianRecomputeInterval == 0) {
                        // Convergence might be too slow:
                        computeJacobian()
                        jacobianReuseCount = 0
                    }
                }

                /**
                 * The norm of the residual at this timestep.
                 * Used for the line search later on.
                 * */
                var initialResidualNorm = 0.0

                // Negate F and calculate the residual's norm:
                for (i in 0 until size){
                    val value = F.get(i)
                    initialResidualNorm += value * value
                    MinusF.set(i, -value)
                }

                if(!solveForDelta()) {
                    // The linear solve failed, even with regularization.
                    // The Broyden-updated Jacobian is trash.

                    // Discard and recompute:
                    computeJacobian()
                    jacobianReuseCount = 0

                    // Try the solve *one* more time with the fresh Jacobian:
                    if (!solveForDelta()) {
                        // If the new Jacobian also fails to solve, the problem is truly ill-conditioned. GG.
                        break
                    }
                }

                // Line search:
                var alpha = 1.0 // Step size
                var maxNewResidual = 0.0

                // Residual before applying the new delta:
                val previousResidual = lastMaxResidual

                for (step in 0 until lineSearchMaxSteps) {
                    // New currents, by applying the dampened δ:
                    for (i in 0 until size) {
                        xNew[i] = x[i] + alpha * delta.get(i)
                    }

                    // P.S. this changes the residual used by Broyden, I also change the delta after this loop.
                    maxNewResidual = evaluateResidual(xNew, FNew)

                    // Calculate new norm:
                    var newNorm = 0.0
                    for (i in 0 until size) {
                        val value = FNew.get(i)
                        newNorm += value * value
                    }

                    // Armijo–Goldstein condition:
                    if (newNorm <= (1.0 - lineSearchC * alpha) * initialResidualNorm) {
                        break
                    }

                    // Reduce the step size:
                    alpha *= lineSearchReduction
                }

                lastMaxResidual = maxNewResidual

                // Apply alpha to the delta for Broyden:
                for (i in 0 until size) {
                    delta[i] *= alpha
                }

                // If the line search didn't converge, then our Jacobian might be bad.
                // The Broyden update might help.
                var deltaMax = 0.0
                var deltaNorm = 0.0 // Used for Broyden below.

                for (i in 0 until size) {
                    val dx = delta.get(i) // Doesn't apply alpha:
                    deltaNorm += dx * dx

                    val magnitude = abs(dx)
                    if (magnitude > deltaMax){
                        deltaMax = magnitude
                    }
                }

                lastMaxDelta = deltaMax

                val maxPotentialViolation = simulation.powerSources
                    .maxOf { abs(it.potential) - it.maxPotential }
                    .coerceAtLeast(0.0)

                /**
                 * Convergence check.
                 * The residual check is solid, but the delta check is a bit suspicious.
                 * It might be worth it to build a counter that measures how many consecutive steps resulted in the delta being low,
                 * and only then apply the criterion.
                 * */
                if ((maxNewResidual <= simulation.powerSourceResidualTolerance || deltaMax <= simulation.powerSourceDeltaTolerance) && maxPotentialViolation < simulation.powerSourcePotentialTolerance) {
                    // Accept xNew and copy into devices:
                    for (i in 0 until size) {
                        simulation.powerSources[i].setCurrentFromSolver(xNew[i])
                    }

                    return iterations
                }

                if(lastMaxResidual >= previousResidual) {
                    // The residual increased or convergence is stalling.
                    // The Broyden loop is likely diverging.
                    // Forces a recompute:
                    recomputeJacobian = true
                }

                if(!recomputeJacobian) {
                    // Broyden's method to update the Jacobian:
                    if (deltaNorm > 1e-9) {
                        CommonOps_DDRM.mult(J, delta, Js)

                        for (i in 0 until size) {
                            val y = FNew.get(i) - F.get(i)
                            yMinusJs.set(i, y - Js.get(i))
                        }

                        val recip = 1.0 / deltaNorm

                        for (i in 0 until size) {
                            val yi = yMinusJs.get(i)

                            for (j in 0 until size) {
                                J.add(i, j, (yi * delta.get(j)) * recip)
                            }
                        }
                    }
                    else {
                        // The norm is too small. The update will be unstable.
                        recomputeJacobian = true
                    }
                }

                for (i in 0 until size) {
                    x[i] = xNew[i]
                }

                for (i in 0 until size) {
                    F.set(i, 0, FNew.get(i, 0))
                }
            }

            // Did not converge. GG.
            for (i in 0 until size) {
                simulation.powerSources[i].setCurrentFromSolver(x[i])
            }

            return simulation.powerSourceMaxIterations
        }
    }

    /**
     * The ground node (also called the *reference node*). Has an ID of [ElectricalNode.GROUND_ID] and contains all the pins which are grounded.
     * **Always** contains some pins. This is ensured by the circuit compiler.
     * The node isn't included in [nodes].
     * */
    val groundNode: ElectricalNode

    /**
     * All created nodes. Doesn't include the [groundNode].
     * */
    val nodes: Array<ElectricalNode>

    /**
     * The MNA system. See [System] for more information.
     * */
    val system: System
    private val solver = LinearSolverFactory_DSCC.lu(FillReducing.NONE)

    /**
     * The components which double-buffer their readouts for display.
     * */
    val repositoryComponents = components.mapNotNull { it as? ReadoutElectricalComponent<*, *> }.toTypedArray()

    /**
     * The potential sources, included in [components].
     * These sources contribute additional equations that solve for an unknown current.
     * */
    val potentialSources: Array<PotentialSource>

    /**
     * The power sources, included in [components].
     * These sources introduce nonlinear equations and are handled specially by the [NonlinearSourceSolver].
     * */
    val powerSources: Array<PowerSource>

    /**
     * MNA Sensitivity solver.
     * */
    val sensitivity: SensitivityAnalysis

    /**
     * The nonlinear system and solver for power sources, used internally.
     * `Null` if no power sources exist.
     * */
    val sourceSystem: NonlinearSourceSolver?

    init {
        val potentialSources = ArrayList<PotentialSource>()
        val powerSources = ArrayList<PowerSource>()

        components.forEach {
            it.setSimulation(this)

            when (it) {
                is PotentialSource -> {
                    potentialSources.add(it)
                }

                is PowerSource -> {
                    powerSources.add(it)
                }
            }
        }

        this.potentialSources = potentialSources.toTypedArray()
        this.powerSources = powerSources.toTypedArray()

        potentialSources.forEachIndexed { idx, source ->
            source.setPotentialIndex(idx)
        }

        powerSources.forEachIndexed { idx, device ->
            device.setPowerSourceIndex(idx)
        }

        val groundedPins = ArrayList<ElectricalPin>()
        val nonGroundedForest = ArrayList<Set<ElectricalPin>>()

        pinForest.forEach { (disjointSet, pins) ->
            if(disjointSet.representative.grounded) {
                groundedPins.addAll(pins)
            }
            else {
                nonGroundedForest.add(pins)
            }
        }

        check(groundedPins.isNotEmpty()) {
            "Circuit is floating. A node should be grounded implicitly!"
        }

        groundNode = ElectricalNode(ElectricalNode.GROUND_ID, groundedPins.toTypedArray())

        groundedPins.forEach { pin ->
            pin.setNode(groundNode)
        }

        nodes = Array<ElectricalNode>(nonGroundedForest.size) { id ->
            ElectricalNode(id, nonGroundedForest[id].toTypedArray())
        }

        nodes.forEach { node ->
            node.pins.forEach { pin ->
                pin.setNode(node)
            }
        }

        system = System(this)
        sensitivity = SensitivityAnalysis(this)
        sourceSystem = if(powerSources.isNotEmpty()) NonlinearSourceSolver(this) else null

        repositoryComponents.forEach {
            it.repositoryLayer.loadPresentation()
        }
    }

    var matrixChanged = false
        private set

    var knownsChanged = false
        private set

    private fun validateUsage() {
        if(destroyed) {
            error("Cannot use electrical simulation after destroyed")
        }
    }

    internal fun setMatrixChanged() {
        matrixChanged = true
    }

    internal fun setKnownsChanged() {
        knownsChanged = true
    }

    /**
     * Copies MNA results into the various classes.
     * Can throw [InvalidResultsException].
     * */
    private fun copyResults() {
        /**
         * Copies potential into the nodes:
         * */
        for (i in 0 until nodes.size) {
            val result = system.unknown[i]
            val node = nodes[i]

            if(result.isNaN() || result.isInfinite()) {
                throw InvalidResultsException(this, node)
            }

            node.potential = result
        }

        /**
         * Copies current into the potential sources:
         * */
        for (i in 0 until potentialSources.size) {
            val result = -system.unknown[nodes.size + i]
            val source = potentialSources[i]

            if(result.isNaN() || result.isInfinite()) {
                throw InvalidResultsException(this, source)
            }

            source.current = result
        }
    }

    /**
     * Max iterations the nonlinear solver for the power sources can take.
     * Usually takes a few tens of iterations in bad cases (switching states), but normally it takes 1-5 iterations in a system whose values are evolving in a continuous manner.
     * */
    var powerSourceMaxIterations = 512

    /**
     * How many times the solver is allowed to apply the Broyden update to the same Jacobian.
     * This recompute is done because it's possible the Broyden update is calculating a low-quality Jacobian that is slowing down convergence.
     * */
    var powerSourceJacobianRecomputeInterval = 16

    /**
     * The max allowed power residual for the power devices.
     * */
    var powerSourceResidualTolerance = 1e-4

    /**
     * Exit if the largest current delta is less than this tolerance for the power devices.
     * */
    var powerSourceDeltaTolerance = 1e-6

    /**
     * Tolerance for the potential constraint.
     * */
    var powerSourcePotentialTolerance = 1e-4

    /**
     * The number of iterations the nonlinear solver needed in the last step.
     * If equal to [powerSourceMaxIterations], the system did not converge implicitly.
     * If lower, it doesn't mean it converged (could have hit impossible conditions). Check the [lastPowerSourceMaxResidual].
     * If it didn't converge, the sources are likely in a broken state.
     * */
    var lastPowerSourceIterationCount = 0
        private set

    /**
     * The number of MNA solves the nonlinear system solver needed in the last step.
     * */
    var lastPowerSourceSystemEvaluations = 0
        private set

    /**
     * The (absolute) max residual from the nonlinear solver in the last step (not the norm of the residual vector).
     * */
    val lastPowerSourceMaxResidual get() = sourceSystem?.lastMaxResidual ?: 0.0

    /**
     * The (absolute) max change in Norton current applied by the nonlinear solver in the last step (not the norm of the delta vector).
     * */
    val lastPowerSourceMaxDelta get() = sourceSystem?.lastMaxDelta ?: 0.0

    /**
     * The approximate time spent by the power source solver last step.
     * Most of these values should be accurate (GC usually shouldn't be happening during the solver loop, but it could).
     * */
    var lastPowerSourceTime = Quantity(0.0, SECOND)
        private set

    /**
     * Steps the system.
     * Can throw:
     * [SingularSystemException] when the linear system factorization failed.
     * [InvalidResultsException] when the result currents or potentials are NaN or Infinity.
     * */
    fun step() {
        validateUsage()

        if(system.constructionMode) {
            /**
             * Constructs the shape of the matrix and sets the initial knowns vector.
             * After this is done, no matrix entries can be added or removed.
             * At best, some matrix entries can be changed (e.g. changing a conductance), which needs a re-factor.
             * This is why I aim to implement most features as norton or thevenin systems, with a constant conductance.
             * */

            /**
             * Stamps each component. [ElectricalComponent.stamp] is called here, and only this step.
             * After that, the only legal operations to perform are conductance changes (topology-related entries cannot change).
             * */
            components.forEach {
                it.stamp()
            }

            system.endConstruction()

            // Likely not necessary:
            matrixChanged = true
            knownsChanged = true
            constructed = true
        }

        components.forEach {
            it.prepareStep()
        }

        /**
         * Updates the matrix factorization, if necessary.
         * */
        if(matrixChanged) {
            solver.setA(system.matrix)
        }

        /**
         * Solves the new system, if necessary.
         * */
        if(matrixChanged || knownsChanged) {
            try {
                solver.solve(system.known, system.unknown)
            }
            catch (e: Exception) {
                throw SingularSystemException(this, e)
            }

            copyResults()
            matrixChanged = false
            knownsChanged = false
        }

        /**
         * Solves the Power Sources:
         * */
        if (powerSources.isNotEmpty()) {
            lastPowerSourceTime = measureDuration {
                val system = sourceSystem!!
                lastPowerSourceIterationCount = system.solve()
                lastPowerSourceSystemEvaluations = system.lastSystemEvaluations
            }
        }

        components.forEach {
            it.finishStep()
        }

        repositoryComponents.forEach {
            it.repositoryLayer.loadAndSwap()
        }
    }

    /**
     * Thévenin equivalent for the circuit surrounding a [Port].
     * */
    @Suppress("PropertyName")
    data class Thevenin(val VTh: Double, val RTh: Double)

    /**
     * Computes the Thévenin equivalent for the circuit surrounding the [nortonSystem].
     * */
    @Suppress("LocalVariableName")
    fun computeTheveninForNortonSystem(nortonSystem: NortonSystem) : Thevenin {
        validateUsage()

        val nodeP = nortonSystem.positive.node
        val nodeN = nortonSystem.negative.node

        sensitivity.solve(nodeP, nodeN)
        val results = sensitivity.results

        val s1 = if (nodeP.isGround) 0.0 else results[nodeP.id]
        val s2 = if (nodeN.isGround) 0.0 else results[nodeN.id]

        val Rth = s1 - s2
        val V = nortonSystem.potential
        val IN = nortonSystem.nortonCurrent

        val Vth = V + Rth * IN

        val RthSafe = if (Rth <= 0.0 || Rth.isNaN() || Rth.isInfinite()) Double.POSITIVE_INFINITY else Rth

        return Thevenin(Vth, RthSafe)
    }

    fun destroy() {
        validateUsage()

        components.forEach {
            it.simulationDestroyed()
        }

        destroyed = true
    }

    /**
     * Thrown when the **main MNA solve** fails (power sources should handle their failures by themselves).
     * */
    class SingularSystemException(val simulation: ElectricalSimulation, ejmlException: Exception) : Exception(ejmlException)

    /**
     * Thrown when the results are invalid (NaN or infinity).
     * @param obj The node or device where the error was discovered (if it's any help).
     * */
    class InvalidResultsException(val simulation: ElectricalSimulation, val obj: Any) : Exception()
}