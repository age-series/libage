package org.ageseries.libage.data

import org.ageseries.libage.mathematics.rounded
import kotlin.math.abs
import kotlin.math.log
import kotlin.math.pow

/**
 * A linear scale.
 *
 * This scale suffices for most linear natural units; a natural example is temperature.
 *
 * In general, don't construct these unless you really are making a new scale; the associated
 * constant members should suffice for most use cases.
 *
 * This package usually uses a specific "base unit" in each of the simulation domains, chosen to be
 * an easy-to-compute value (e.g., unit in the kms system). Those domains have further documentation
 * on their choice of base unit, which is usually part of the documentation in or around wherever
 * these scales are defined.
 */
data class Scale(val factor: Double, val base: Double) {
    /** Given a value [u] in base unit, return its value in this unit. */
    fun map(u: Double): Double = factor * u + base

    /** Given a value [u] in this unit, return its value in base units. */
    fun unmap(u: Double): Double = (u - base) / factor
}

/**
 * [Scale], parameterized by [Unit], aligns with the physical attribute measured by the scale (such as distance, time, and so forth).
 * Notably, [Unit] remains distinct from the [scale]; for instance, [Distance] can be measured in [KELVIN], [GRADE], and other units.
 * It is important to recognize that [Unit] functions merely as a compiler mechanism and holds no inherent functional significance.
 * */
open class QuantityScale<Unit>(val scale: Scale) {
    val factor get() = scale.factor

    /**
     * Amplifies this scale [amplify] times.
     * Example: *GRAMS * 1000* will result in *KILOGRAMS*.
     * */
    operator fun times(amplify: Double) = QuantityScale<Unit>(Scale(scale.factor / amplify, scale.base))

    /**
     * Reduces this scale [reduce] times.
     * Example: *KILOGRAMS / 1000* will result in *GRAMS*.
     * */
    operator fun div(reduce: Double) = QuantityScale<Unit>(Scale(scale.factor * reduce, scale.base))

    /**
     * Amplifies this scale 1000 times.
     * Example: *+GRAMS* will result in *KILOGRAMS*.
     * */
    operator fun unaryPlus() = this * 1000.0

    /**
     * Reduces this scale 1000 times.
     * Example: *-KILOGRAMS* will result in *GRAMS*.
     * */
    operator fun unaryMinus() = this / 1000.0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QuantityScale<*>

        return scale == other.scale
    }

    override fun hashCode(): Int {
        return scale.hashCode()
    }
}

/**
 * Signifies a multiplication factor employed to adjust a [QuantityScale].
 * Presently, its extension is not supported; to minimize runtime cost, a lookup table is precomputed.
 * This design ensures the avoidance of additional allocations for new instances of [QuantityScale] (the composition cost boils down to accessing the precomputed instance).
 * */
class ScaleMultiplier internal constructor(val factor: Double, val index: Int) {
    /**
     * Composes the base [scale] with this multiplier, returning a scale which is equivalent to the result of *[scale] * [factor]*.
     * */
    operator fun<T> times(scale: StandardQuantityScale<T>) = scale.multiples[index]
}

/** Transforms the units into **pico- (×10⁻¹²)** */
val PICO = ScaleMultiplier(1e-12, 0)

/** Transforms the units into **nano- (×10⁻⁹)** */
val NANO = ScaleMultiplier(1e-9, 1)

/** Transforms the units into **micro- (×10⁻⁶)** */
val MICRO = ScaleMultiplier(1e-6, 2)

/** Transforms the units into **milli- (×10⁻³)** */
val MILLI = ScaleMultiplier(1e-3, 3)

/** Transforms the units into **kilo- (×10³)** */
val KILO = ScaleMultiplier(1e3, 4)

/** Transforms the units into **nano- (×10⁶)** */
val MEGA = ScaleMultiplier(1e6, 5)

/** Transforms the units into **nano- (×10⁹)** */
val GIGA = ScaleMultiplier(1e9, 6)

/** Transforms the units into **nano- (×10¹²)** */
val TERA = ScaleMultiplier(1e12, 7)

private val MULTIPLIERS = listOf(PICO, NANO, MICRO, MILLI, KILO, MEGA, GIGA, TERA)

/**
 * [QuantityScale] with factor 1. Also offers the composition operation with [ScaleMultiplier]s.
 * */
class StandardQuantityScale<Unit>(scale: Scale) : QuantityScale<Unit>(scale) {
    init {
        require(scale.factor == 1.0) {
            "Base quantity scale must have a factor of 1"
        }
    }

    /**
     * Pre-computed lookup table for multiples of this scale.
     * When we apply a [ScaleMultiplier], we just fetch the pre-computed scale.
     * */
    internal val multiples = Array(MULTIPLIERS.size) { index ->
        val multiplier = MULTIPLIERS.first { it.index == index }
        // This operation creates the base [QuantityScale] which doesn't just recursively keep creating other lookup tables.
        this * multiplier.factor
    }
}

/**
 * Denotes a tangible quantity distinguished by a designated [Unit] and a numeric [value].
 * The [value] is consistently expressed in fundamental units, such as Kelvin, Meter, Kilogram, and the like.
 * This design is sufficiently straightforward to be instantiated as an inline value-class, thereby rendering the runtime overhead of carrying around [Quantity] virtually negligible.
 * The [not] operator was chosen because it is the least "ugly" way to quickly fetch the [value] for subsequent operations with [Double].
 * If you don't like it, Grissess, just access [value].
 * */
@JvmInline
value class Quantity<Unit>(val value: Double) : Comparable<Quantity<Unit>> {
    /**
     * Creates a [Quantity] where its [value] is established as the equivalent of [sourceValue] in the [standardScale].
     * */
    constructor(sourceValue: Double, scale: QuantityScale<Unit>) : this(scale.scale.unmap(sourceValue))

    val isZero get() = value == 0.0

    /**
     * Gets the [value] quickly and nicely.
     * */
    operator fun not() = value
    operator fun unaryMinus() = Quantity<Unit>(-value)
    operator fun unaryPlus() = Quantity<Unit>(+value)
    operator fun plus(b: Quantity<Unit>) = Quantity<Unit>(this.value + b.value)
    operator fun minus(b: Quantity<Unit>) = Quantity<Unit>(this.value - b.value)
    operator fun times(scalar: Double) = Quantity<Unit>(this.value * scalar)
    operator fun div(scalar: Double) = Quantity<Unit>(this.value / scalar)

    /**
     * Divides the quantity by another quantity of the same unit. This, in turn, cancels out the [Unit], returning the resulting number.
     * */
    operator fun div(b: Quantity<Unit>) = this.value / b.value

    override operator fun compareTo(other: Quantity<Unit>) = value.compareTo(other.value)

    operator fun compareTo(b: Double) = value.compareTo(b)

    /**
     * Maps this quantity to another scale of the same unit.
     * */
    operator fun rangeTo(s: QuantityScale<Unit>) = s.scale.map(value)

    override fun toString() = value.toString()
}

@JvmName("quantityPlusDouble") operator fun <U> Quantity<U>.plus(b: Double) = Quantity<U>(this.value + b)
@JvmName("quantityMinusDouble") operator fun <U> Quantity<U>.minus(b: Double) = Quantity<U>(this.value - b)
@JvmName("doublePlusQuantity") operator fun <U> Double.plus(b: Quantity<U>) = Quantity<U>(this + b.value)
@JvmName("doubleMinusQuantity") operator fun <U> Double.minus(b: Quantity<U>) = Quantity<U>(this - b.value)

fun <U> min(a: Quantity<U>, b: Quantity<U>) = Quantity<U>(kotlin.math.min(!a, !b))
fun <U> max(a: Quantity<U>, b: Quantity<U>) = Quantity<U>(kotlin.math.max(!a, !b))
fun <U> abs(q: Quantity<U>) = Quantity<U>(abs(!q))

/**
 * An iterator over a sequence of values of type [Quantity].
 * */
abstract class QuantityIterator<Unit> : Iterator<Quantity<Unit>> {
    final override fun next() = nextQuantity()

    /**
     * Returns the next value in the sequence without boxing.
     * */
    abstract fun nextQuantity() : Quantity<Unit>
}

class ArrayQuantityIterator<Unit>(private val array: QuantityArray<Unit>) : QuantityIterator<Unit>() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextQuantity() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}

/**
 * Array of quantities backed by a [DoubleArray].
 * */
class QuantityArray<Unit>(val backing: DoubleArray) {
    constructor(size: Int) : this(DoubleArray(size))
    constructor(size: Int, init: (Int) -> Quantity<Unit>) : this(DoubleArray(size) { !init(it) })
    constructor(size: Int, value: Quantity<Unit>) : this(DoubleArray(size) { !value })

    operator fun get(index: Int): Quantity<Unit> = Quantity(backing[index])

    operator fun set(index: Int, value: Quantity<Unit>) { backing[index] = !value }

    val size get() = backing.size

    operator fun iterator() = ArrayQuantityIterator(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QuantityArray<*>

        return backing.contentEquals(other.backing)
    }

    override fun hashCode(): Int {
        return backing.contentHashCode()
    }
}

/**
 * Classifies the [value] into a **multiple** and expresses it based on that **multiple**.
 * A multiplier (e.g. "kilo", "mega", ...) is chosen from [map] based on the magnitude of [value] in the said [base].
 * The multiplier is placed in the [unit] string at the character *#* (the [unit] can also be left empty). This constitutes the **multiple**.
 * The [value] is then adjusted based on the chosen magnitude, rounded to [decimals] and then suffixed with the **multiple**.
 *
 * @param value The value to classify.
 * @param base The base to use. Usually, this is 10 or 1000.
 * @param map A map of magnitudes to the multiplier. Not actually a [Map] because the algorithm scans the entire map to find the unit (justified because we don't care)
 * @param unit The unit to place after the multiplier.
 * @param decimals The number of decimals to round the final value to.
 * @param inferFirst If true, no multiple will be prepended to the [unit], if the value is around magnitude 0.
 * */
fun classify(value: Double, base: Double, map: List<Pair<Double, String>>, unit: String, decimals: Int = 2, inferFirst: Boolean = true) : String {
    if(value < 0.0) {
        return "-${classify(-value, base, map, unit)}"
    }

    if(unit.isNotEmpty()) {
        require(unit.count { it == '#'} == 1) {
            "Non-empty unit $unit must have one # character"
        }
    }

    val magnitude = log(value, base)

    var (targetMagnitude, prefix) = map
        .filter { it.first <= magnitude }
        .maxByOrNull { it.first } ?:
    map.minByOrNull { abs(it.first - magnitude) }!!

    if(inferFirst) {
        if(0.0 <= magnitude && abs(magnitude - targetMagnitude) > abs(magnitude)) {
            prefix = ""
            targetMagnitude = 0.0
        }
    }

    val multiple = if(unit.isNotEmpty()) {
        unit.replace("#", prefix)
    }
    else {
        prefix
    }

    return "${(value / base.pow(targetMagnitude)).rounded(decimals)} $multiple"
}

enum class ClassificationBase(val value: Double, val prefixes: List<Pair<Double, String>>) {
    Base1000Standard(
        1000.0,
        listOf(
            -1.0 to "m", -2.0 to "µ", -3.0 to "n", -4.0 to "p", -5.0 to "f", -6.0 to "a", -7.0 to "z", -8.0 to "y",
            1.0 to "k", 2.0 to "M", 3.0 to "G", 4.0 to "T", 5.0 to "P", 6.0 to "E", 7.0 to "Z", 8.0 to "Y"
        )
    ),
    Base1000Long(
        1000.0,
        listOf(
            -1.0 to "milli", -2.0 to "micro", -3.0 to "nano", -4.0 to "pico", -5.0 to "femto", -6.0 to "atto", -7.0 to "zepto", -8.0 to "yocto",
            1.0 to "kilo", 2.0 to "mega", 3.0 to "giga", 4.0 to "tera", 5.0 to "peta", 6.0 to "exa", 7.0 to "zetta", 8.0 to "yotta"
        )
    ),
    Base10Standard(
        10.0,
        listOf(
            -1.0 to "d", -2.0 to "c", -3.0 to "m", -6.0 to "µ", -9.0 to "n", -12.0 to "p", -15.0 to "f", -18.0 to "a", -21.0 to "z", -24.0 to "y",
            1.0 to "da", 2.0 to "h", 3.0 to "k", 6.0 to "M", 9.0 to "G", 12.0 to "T", 15.0 to "P", 18.0 to "E", 21.0 to "Z", 24.0 to "Y"
        )
    ),
    Base10Long(
        10.0,
        listOf(
            -1.0 to "deci", -2.0 to "centi", -3.0 to "milli", -6.0 to "micro", -9.0 to "nano", -12.0 to "pico", -15.0 to "femto", -18.0 to "atto", -21.0 to "zepto", -24.0 to "yocto",
            1.0 to "deka", 2.0 to "hecto", 3.0 to "kilo", 6.0 to "mega", 9.0 to "giga", 12.0 to "tera", 15.0 to "peta", 18.0 to "exa", 21.0 to "zetta", 24.0 to "yotta"
        )
    )
}

/**
 * Classifies this physical dimension with the [symbol] (e.g. *"#m"* for meters). The symbol *#* is where the multiple will be placed.
 * @param factor An adjustment factor for special cases. E.G. for [KILOGRAM], this factor is set to *1000* and the symbol is "g".
 * @param base The base to use. This specifies the prefixes (e.g. "kilo", "mega", ...) to place in front of the [symbol] when [classify]ing.
 * */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Classify(
    val symbol: String,
    val factor: Double = 1.0,
    val base: ClassificationBase = ClassificationBase.Base1000Standard
)

/**
 * Classifies this quantity into a *multiple*. If [T] is annotated with [Classify], the symbol and base specified in the annotation will be used.
 * Otherwise, the base [ClassificationBase.Base1000Standard] will be used and no unit symbol will be added.
 * */
inline fun<reified T> Quantity<T>.classify() : String {
    val annotation = T::class.java.getDeclaredAnnotation(Classify::class.java)

    return if(annotation == null) {
        classify(!this, ClassificationBase.Base1000Standard.value, ClassificationBase.Base1000Standard.prefixes, "")
    }
    else {
        classify(!this * annotation.factor, annotation.base.value, annotation.base.prefixes, annotation.symbol)
    }
}

/**
 * Defines the standard scale of the [Unit] (a scale with factor 1).
 * */
fun <Unit> standardScale(factor: Double = 1.0) = StandardQuantityScale<Unit>(Scale(factor, 0.0))

@Classify("#g", factor = 1000.0) interface Mass
val KILOGRAM = standardScale<Mass>()
val GRAM = -KILOGRAM

@Classify("#Da")
interface AtomicMass
val DALTON = standardScale<AtomicMass>()

fun Quantity<AtomicMass>.asStandardMass() = Quantity(!this * 1.66053906660e-27, KILOGRAM)

@Classify("#s") interface Time
val SECOND = standardScale<Time>()
val MINUTE = SECOND * 60.0
val HOUR = MINUTE * 60.0
val DAY = HOUR * 24.0

@Classify("#Hz") interface Frequency
val HERTZ = standardScale<Frequency>()
val Hz by ::HERTZ

@Classify("#m") interface Distance
val METER = standardScale<Distance>()
val CENTIMETER = METER / 100.0
val ANGSTROM = METER * 1e-10

@Classify("#J") interface Energy
val JOULE = standardScale<Energy>()
val ERG = JOULE * 1e-7
val WATT_SECOND = QuantityScale<Energy>(Scale(JOULE.factor, 0.0))
val WATT_MINUTE = WATT_SECOND * 60.0
val WATT_HOUR = WATT_MINUTE * 60.0
// Serious precision issues? Hope not! :Fish_Smug:
val ELECTRON_VOLT = JOULE * 1.602176634e-19

@Classify("#W") interface Power
val WATT = standardScale<Power>()

@Classify("#Bq") interface Radioactivity
val BECQUEREL = standardScale<Radioactivity>()
val CURIE = (GIGA * BECQUEREL) * 37.0

@Classify("#Gy") interface RadiationAbsorbedDose
val GRAY = standardScale<RadiationAbsorbedDose>()
val RAD = GRAY / 100.0

@Classify("#Sv") interface RadiationDoseEquivalent
val SIEVERT = standardScale<RadiationDoseEquivalent>()
val REM = SIEVERT / 100.0

@Classify("#R", 3875.96899225)
interface RadiationExposure
val COULOMB_PER_KILOGRAM = standardScale<RadiationExposure>()
val ROENTGEN = COULOMB_PER_KILOGRAM / 3875.96899225

@Classify("#1/m") interface ReciprocalDistance
val RECIP_METER = standardScale<ReciprocalDistance>()
val RECIP_CENTIMETER = RECIP_METER * 100.0

interface ArealDensity
val KILOGRAM_PER_METER2 = standardScale<ArealDensity>()
val GRAM_PER_CENTIMETER2 = KILOGRAM_PER_METER2 * 10.0

@Classify("#g/m³", factor = 1000.0) interface Density
val KILOGRAM_PER_METER3 = standardScale<Density>()
val G_PER_CM3 = KILOGRAM_PER_METER3 * 1000.0
val G_PER_L = KILOGRAM_PER_METER3

interface ReciprocalArealDensity
val METER2_PER_KILOGRAM = standardScale<ReciprocalArealDensity>()
val CENTIMETER2_PER_GRAM = METER2_PER_KILOGRAM / 10.0

@Classify("#m/s") interface Velocity
val METER_PER_SECOND = standardScale<Velocity>()

@Classify("#mol") interface Substance
val MOLE = standardScale<Substance>()

interface MolarConcentration
val MOLE_PER_METER3 = standardScale<MolarConcentration>()

@Classify("#m²") interface Area
val METER2 = standardScale<Area>()

@Classify("#m³") interface Volume
val METER3 = standardScale<Volume>()
val LITER = METER3 / 1000.0

@Classify("#K") interface Temperature
val KELVIN = standardScale<Temperature>()
val RANKINE = KELVIN * 0.555556
val CELSIUS = QuantityScale<Temperature>(Scale(1.0, -273.15))
val CENTIGRADE by ::CELSIUS
val MILLIGRADE = QuantityScale<Temperature>(Scale(10.0, -2731.5))
val GRADE = QuantityScale<Temperature>(Scale(0.01, -2.7315))
val ABSOLUTE_GRADE = QuantityScale<Temperature>(Scale(0.01, 0.0))

@Classify("#J/kgK") interface SpecificHeatCapacity
val JOULE_PER_KILOGRAM_KELVIN = standardScale<SpecificHeatCapacity>()
val JOULE_PER_GRAM_KELVIN = +JOULE_PER_KILOGRAM_KELVIN
val KILOJOULE_PER_KILOGRAM_KELVIN = +JOULE_PER_KILOGRAM_KELVIN

@Classify("#J/K") interface HeatCapacity
val JOULE_PER_KELVIN = standardScale<HeatCapacity>()

@Classify("#W/mK") interface ThermalConductivity
val WATT_PER_METER_KELVIN = standardScale<ThermalConductivity>()

@Classify("#W/K") interface ThermalConductance
val WATT_PER_KELVIN = standardScale<ThermalConductance>()

@Classify("#Ωm") interface ElectricalResistivity
val OHM_METER = standardScale<ElectricalResistivity>()

@Classify("#kg/mol") interface MolecularWeight
val KILOGRAM_PER_MOLE = standardScale<MolecularWeight>()
val GRAM_PER_MOLE = -KILOGRAM_PER_MOLE

@Classify("#Pa") interface Pressure
val PASCAL = standardScale<Pressure>()
val ATMOSPHERES = PASCAL * 9.86923e-6

@Classify("#W/m²") interface Intensity
val WATT_PER_METER2 = standardScale<Intensity>()

@Classify("#V") interface Potential
val VOLT = standardScale<Potential>()

@Classify("#A") interface Current
val AMPERE = standardScale<Current>()

@Classify("#Ω") interface Resistance
val OHM = standardScale<Resistance>()

@Classify("#F") interface Capacitance
val FARAD = standardScale<Capacitance>()

@Classify("#S") interface ElectricalConductance
val SIEMENS = standardScale<ElectricalConductance>()

@Classify("#C") interface ElectricalCharge
val COULOMB = standardScale<ElectricalCharge>()

@Classify("#H") interface Inductance
val HENRY = standardScale<Inductance>()

@Classify("#J/kg") interface MassEnergyDensity
val JOULE_PER_KILOGRAM = standardScale<MassEnergyDensity>()

@Classify("#J/m³") interface VolumeEnergyDensity
val JOULE_PER_METER3 = standardScale<VolumeEnergyDensity>()