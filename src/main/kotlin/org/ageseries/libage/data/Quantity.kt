@file:Suppress("unused")
@file:JvmName(JVM_NAME)

package org.ageseries.libage.data

import org.ageseries.libage.mathematics.rounded
import kotlin.math.abs
import kotlin.math.log
import kotlin.math.pow
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.kotlinProperty

typealias ScaleRef<T> = KProperty<QuantityScale<T>>

private const val JVM_NAME = "QuantityKt"
private const val CLASS_NAME = "org.ageseries.libage.data.$JVM_NAME"

private val SELF by lazy {
    checkNotNull(Class.forName(CLASS_NAME)) {
        "Failed to resolve Quantity class $CLASS_NAME"
    }
}

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
 * Notably, [Unit] remains distinct from the [scale]; for instance, [Distance] can be measured in [METER]s, feet, dragon tails, american football fields, and other units.
 * It is important to recognize that [Unit] functions merely as a compiler mechanism and holds no inherent functional significance.
 * @param dimensionType The class of the *symbolic interface* [Unit].
 * */
open class QuantityScale<Unit>(internal val dimensionType: Class<*>, val scale: Scale) {
    val factor get() = scale.factor

    /**
     * Amplifies this scale [amplify] times.
     * Example: *GRAMS * 1000* will result in *KILOGRAMS*.
     * */
    operator fun times(amplify: Double) = QuantityScale<Unit>(dimensionType, Scale(scale.factor / amplify, scale.base))

    /**
     * Reduces this scale [reduce] times.
     * Example: *KILOGRAMS / 1000* will result in *GRAMS*.
     * */
    operator fun div(reduce: Double) = QuantityScale<Unit>(dimensionType, Scale(scale.factor * reduce, scale.base))

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
    operator fun<T> times(scale: SourceQuantityScale<T>) = scale.multiples[index]
}

/**
 * Composes the base scale with this multiplier, returning another source scale.
 * */
internal infix fun<T> ScaleMultiplier.sourceCompose(scale: SourceQuantityScale<T>) = SourceQuantityScale<T>(scale.dimensionType, scale.multiples[index].scale)

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
 * [QuantityScale] that is composable with [ScaleMultiplier].
 * "Source" means it is a fundamental multiple of the standard (base) scale; normal multiplication/division yields non-source [QuantityScale] instances.
 * Yielding [SourceQuantityScale] would cause infinite recursive creation of child scales.
 * */
class SourceQuantityScale<Unit>(dimensionType: Class<*>, scale: Scale) : QuantityScale<Unit>(dimensionType, scale) {
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
 * Amplifies this scale and results in a new [SourceQuantityScale] (as opposed to [times] which results in [QuantityScale]).
 * Separating from [this.times] is necessary to prevent the children scales creating children scales infinitely.
 * */
internal infix fun <U> SourceQuantityScale<U>.sourceAmplify(amplify: Double) = SourceQuantityScale<U>(dimensionType, Scale(scale.factor / amplify, scale.base))

/**
 * Reduces this scale and results in a new [SourceQuantityScale] (as opposed to [div] which results in [QuantityScale]).
 * Separating from [this.div] is necessary to prevent the children scales creating children scales infinitely.
 * */
internal infix fun <U> SourceQuantityScale<U>.sourceReduce(reduce: Double) = SourceQuantityScale<U>(dimensionType, Scale(scale.factor * reduce, scale.base))

internal fun <U> SourceQuantityScale<U>.sourceAmp() = this sourceAmplify 1000.0

internal fun <U> SourceQuantityScale<U>.sourceSub() = this sourceReduce 1000.0

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
 * Categorizes the given numerical value into a scale and represents it using a corresponding scale factor.
 * A scale factor (such as "kilo," "mega," etc.) is selected from a predefined list based on the magnitude of the given value in the specified base.
 * The chosen scale factor is inserted into the designated position marked by '#' within the unit string.
 * If '#' does not exist, it will be implied that the multiple shall be inserted at the start of the string.
 * [symbol] can also be left empty (you'll still get the number in formatted form and suffixed with the multiple).
 * @param value The value to classify.
 * @param base The base to use. Usually, this is 10 or 1000.
 * @param map A map of magnitudes *to* multiplier. Not actually a [Map] because the algorithm scans the entire map to find the best multiplier (justified because we don't care)
 * @param symbol The unit to place after the multiplier.
 * @param decimals The number of decimals to round the final value to.
 * @param inferFirst If true, no multiple will be prepended to the [symbol], if the value is around magnitude 0.
 * */
fun classify(value: Double, base: Double, map: List<Pair<Double, String>>, symbol: String, decimals: Int = 2, inferFirst: Boolean = true) : String {
    if(value < 0.0) {
        return "-${classify(-value, base, map, symbol)}"
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

    val multiple = if(symbol.isNotEmpty()) {
        if(symbol.contains("#")) {
            symbol.replace("#", prefix)
        }
        else {
            prefix + symbol
        }
    }
    else {
        prefix
    }

    return "${(value / base.pow(targetMagnitude)).rounded(decimals)} $multiple"
}

/**
 * Describes the base of classification base and multiples.
 * @param value The classification base.
 * @param multiples Impromptu map of magnitudes to the desired multiple.
 * */
enum class ClassificationBase(val value: Double, val multiples: List<Pair<Double, String>>) {
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
            -1.0 to "milli",
            -2.0 to "micro",
            -3.0 to "nano",
            -4.0 to "pico",
            -5.0 to "femto",
            -6.0 to "atto",
            -7.0 to "zepto",
            -8.0 to "yocto",
            1.0 to "kilo",
            2.0 to "mega",
            3.0 to "giga",
            4.0 to "tera",
            5.0 to "peta",
            6.0 to "exa",
            7.0 to "zetta",
            8.0 to "yotta"
        )
    ),
    Base10Standard(
        10.0,
        listOf(
            -1.0 to "d",
            -2.0 to "c",
            -3.0 to "m",
            -6.0 to "µ",
            -9.0 to "n",
            -12.0 to "p",
            -15.0 to "f",
            -18.0 to "a",
            -21.0 to "z",
            -24.0 to "y",
            1.0 to "da",
            2.0 to "h",
            3.0 to "k",
            6.0 to "M",
            9.0 to "G",
            12.0 to "T",
            15.0 to "P",
            18.0 to "E",
            21.0 to "Z",
            24.0 to "Y"
        )
    ),
    Base10Long(
        10.0,
        listOf(
            -1.0 to "deci",
            -2.0 to "centi",
            -3.0 to "milli",
            -6.0 to "micro",
            -9.0 to "nano",
            -12.0 to "pico",
            -15.0 to "femto",
            -18.0 to "atto",
            -21.0 to "zepto",
            -24.0 to "yocto",
            1.0 to "deka",
            2.0 to "hecto",
            3.0 to "kilo",
            6.0 to "mega",
            9.0 to "giga",
            12.0 to "tera",
            15.0 to "peta",
            18.0 to "exa",
            21.0 to "zetta",
            24.0 to "yotta"
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
 * Auxiliary classifier for scales. It is applied to [QuantityScale] fields.
 * This is used to override the default classification (e.g. using a config option downstream).
 * @param aliases Extra names for this auxiliary scale. It must be unique per dimension type.
 * */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Auxiliary(
    val symbol: String,
    vararg val aliases: String,
    val factor: Double = 1.0,
    val base: ClassificationBase = ClassificationBase.Base1000Standard
)

/**
 * Classifies this quantity into a *multiple* using the [classify] API, in the standard scale.
 * If [T] is annotated with [Classify], the symbol and base specified in the annotation will be used.
 * Otherwise, [ClassificationBase.Base1000Standard] will be used and no unit symbol will be added.
 * */
inline fun<reified T> Quantity<T>.classify() : String {
    val annotation = T::class.java.getDeclaredAnnotation(Classify::class.java)

    return if(annotation == null) {
        classify(!this, ClassificationBase.Base1000Standard.value, ClassificationBase.Base1000Standard.multiples, "")
    }
    else {
        classify(!this * annotation.factor, annotation.base.value, annotation.base.multiples, annotation.symbol)
    }
}

/**
 * Defines the standard scale of the [Unit] (a scale with factor 1).
 * */
inline fun <reified Unit> standardScale(factor: Double = 1.0) = SourceQuantityScale<Unit>(
    Unit::class.java,
    Scale(factor, 0.0)
)

@Classify("g", factor = 1000.0) interface Mass
val KILOGRAM = standardScale<Mass>()
val GRAM = KILOGRAM.sourceSub()
@Auxiliary("firkin") val FIRKIN = KILOGRAM sourceAmplify 40.8233

@Classify("Da") interface AtomicMass
val DALTON = standardScale<AtomicMass>()

/**
 * Gets the atomic mass expressed in [KILOGRAM].
 * The result may not be useful due to the limited precision we're working with.
 * */
fun Quantity<AtomicMass>.asStandardMass() = Quantity(!this * 1.66053906660e-27, KILOGRAM)

@Classify("s") interface Time
val SECOND = standardScale<Time>()
@Auxiliary("rel", "rels") val REL = SECOND sourceAmplify 1.2
@Auxiliary("min") val MINUTE = SECOND sourceAmplify 60.0
@Auxiliary("h") val HOUR = MINUTE sourceAmplify 60.0
@Auxiliary("d", "day") val DAY = HOUR sourceAmplify 24.0
@Auxiliary("fortnight") val FORTNIGHT = SECOND sourceAmplify 1209600.0

@Classify("Hz") interface Frequency
val HERTZ = standardScale<Frequency>()

@Classify("m") interface Distance
val METER = standardScale<Distance>()
@Auxiliary("cm") val CENTIMETER = METER sourceReduce 100.0
@Auxiliary("ft") val FOOT = METER sourceAmplify 0.3048
@Auxiliary("in") val INCH = METER sourceAmplify 0.0254
@Auxiliary("light-nanoseconds", "lns") val LIGHT_NANOSECONDS = CENTIMETER sourceAmplify 29.9792458
@Auxiliary("attoparsec", "apc") val ATTO_PARSEC = CENTIMETER sourceAmplify 3.086
@Auxiliary("metric foot", "mf") val METRIC_FOOT = (MILLI sourceCompose METER) sourceAmplify 300.0
@Auxiliary("cubit") val CUBIT = CENTIMETER sourceAmplify  120.0
@Auxiliary("furlong") val FURLONG = METER sourceAmplify 201.168
@Auxiliary("Å", "A", "a", "angstrom") val ANGSTROM = METER sourceReduce 1e10

@Classify("J") interface Energy
val JOULE = standardScale<Energy>()
@Auxiliary("BTU") val BTU = JOULE sourceAmplify 1055.05585262
@Auxiliary("erg") val ERG = JOULE sourceAmplify 1e-7
@Auxiliary("Ws") val WATT_SECOND = JOULE
@Auxiliary("Wmin") val WATT_MINUTE = WATT_SECOND sourceAmplify  60.0
@Auxiliary("Wh") val WATT_HOUR = WATT_MINUTE sourceAmplify 60.0
@Auxiliary("eV") val ELECTRON_VOLT = JOULE sourceAmplify  1.602176634e-19 // Serious precision issues? Hope not! :Fish_Smug:

@Classify("W") interface Power
val WATT = standardScale<Power>()

@Classify("Bq") interface Radioactivity
val BECQUEREL = standardScale<Radioactivity>()
@Auxiliary("Ci") val CURIE = (GIGA sourceCompose BECQUEREL) sourceAmplify 37.0

@Classify("Gy") interface RadiationAbsorbedDose
val GRAY = standardScale<RadiationAbsorbedDose>()
@Auxiliary("rad") val RAD = GRAY sourceReduce 100.0

@Classify("Sv") interface RadiationDoseEquivalent
val SIEVERT = standardScale<RadiationDoseEquivalent>()
@Auxiliary("rem") val REM = SIEVERT sourceReduce 100.0

@Classify("C/kg")
interface RadiationExposure val COULOMB_PER_KILOGRAM = standardScale<RadiationExposure>()
@Auxiliary("R") val ROENTGEN = COULOMB_PER_KILOGRAM sourceReduce 3875.96899225

@Classify("1/m") interface ReciprocalDistance
val RECIP_METER = standardScale<ReciprocalDistance>()
val RECIP_CENTIMETER = RECIP_METER sourceAmplify 100.0

interface ArealDensity
val KILOGRAM_PER_METER2 = standardScale<ArealDensity>()
val GRAM_PER_CENTIMETER2 = KILOGRAM_PER_METER2 sourceAmplify 10.0

@Classify("g/m³", factor = 1000.0) interface Density
val KILOGRAM_PER_METER3 = standardScale<Density>()
@Auxiliary("g/cm³")val G_PER_CM3 = KILOGRAM_PER_METER3 sourceAmplify 1000.0
@Auxiliary("g/L")val G_PER_L = KILOGRAM_PER_METER3

interface ReciprocalArealDensity
val METER2_PER_KILOGRAM = standardScale<ReciprocalArealDensity>()
val CENTIMETER2_PER_GRAM = METER2_PER_KILOGRAM sourceReduce 10.0

@Classify("m/s") interface Velocity
val METER_PER_SECOND = standardScale<Velocity>()

@Classify("mol") interface Substance
val MOLE = standardScale<Substance>()

interface MolarConcentration
val MOLE_PER_METER3 = standardScale<MolarConcentration>()

@Classify("m²") interface Area
val METER2 = standardScale<Area>()

@Classify("m³") interface Volume
val METER3 = standardScale<Volume>()
@Auxiliary("L") val LITER = METER3.sourceSub()

@Classify("K") interface Temperature
val KELVIN = standardScale<Temperature>()
@Auxiliary("°F", "F", "f", "fahrenheit") val FAHRENHEIT = SourceQuantityScale<Temperature>(Temperature::class.java, Scale(9.0 / 5.0, -459.67))
@Auxiliary("Rk") val RANKINE = KELVIN sourceAmplify 0.555556 // Not R (Roentgen)
@Auxiliary("°C") val CELSIUS = SourceQuantityScale<Temperature>(Temperature::class.java, Scale(1.0, -273.15))
@Auxiliary("mG") val MILLIGRADE = SourceQuantityScale<Temperature>(Temperature::class.java, Scale(10.0, -2731.5))
@Auxiliary("G") val GRADE = SourceQuantityScale<Temperature>(Temperature::class.java, Scale(0.01, -2.7315))
@Auxiliary("|G|") val ABSOLUTE_GRADE = SourceQuantityScale<Temperature>(Temperature::class.java, Scale(0.01, 0.0))

@Classify("J/kgK") interface SpecificHeatCapacity
val JOULE_PER_KILOGRAM_KELVIN = standardScale<SpecificHeatCapacity>()
@Auxiliary("J/gK") val JOULE_PER_GRAM_KELVIN = JOULE_PER_KILOGRAM_KELVIN.sourceAmp()
@Auxiliary("KJ/kgK") val KILOJOULE_PER_KILOGRAM_KELVIN = JOULE_PER_KILOGRAM_KELVIN.sourceAmp()

@Classify("J/K") interface HeatCapacity
val JOULE_PER_KELVIN = standardScale<HeatCapacity>()

@Classify("W/mK") interface ThermalConductivity
val WATT_PER_METER_KELVIN = standardScale<ThermalConductivity>()

@Classify("W/K") interface ThermalConductance
val WATT_PER_KELVIN = standardScale<ThermalConductance>()

@Classify("Ωm") interface ElectricalResistivity
val OHM_METER = standardScale<ElectricalResistivity>()

@Classify("kg/mol") interface MolecularWeight
val KILOGRAM_PER_MOLE = standardScale<MolecularWeight>()
@Auxiliary("g/mol") val GRAM_PER_MOLE = KILOGRAM_PER_MOLE.sourceSub()

@Classify("Pa") interface Pressure
val PASCAL = standardScale<Pressure>()
@Auxiliary("Atm") val ATMOSPHERE = PASCAL sourceAmplify 9.86923e-6

@Classify("W/m²") interface Intensity
val WATT_PER_METER2 = standardScale<Intensity>()

@Classify("V") interface Potential
val VOLT = standardScale<Potential>()

@Classify("A") interface Current
val AMPERE = standardScale<Current>()

@Classify("Ω") interface Resistance
val OHM = standardScale<Resistance>()

@Classify("F") interface Capacitance
val FARAD = standardScale<Capacitance>()

@Classify("S") interface ElectricalConductance
val SIEMENS = standardScale<ElectricalConductance>()

@Classify("C") interface ElectricalCharge
val COULOMB = standardScale<ElectricalCharge>()

@Classify("H") interface Inductance
val HENRY = standardScale<Inductance>()

@Classify("J/kg") interface MassEnergyDensity
val JOULE_PER_KILOGRAM = standardScale<MassEnergyDensity>()

@Classify("J/m³") interface VolumeEnergyDensity
val JOULE_PER_METER3 = standardScale<VolumeEnergyDensity>()

/**
 * Gets a map of dimension type (dimension interface) to a multimap of scale reference (property holding the [QuantityScale]) and its declared [Auxiliary]ers.
 * */
val AUXILIARY_CLASSIFIERS = run {
    val map = LinkedHashMap<Class<*>, MutableSetMapMultiMap<ScaleRef<*>, String>>()

    // get getters:
    SELF.declaredFields.forEach { field ->
        if((QuantityScale::class.java).isAssignableFrom(field.type)) {
            val property = field.kotlinProperty
                ?: return@forEach

            val annotation = property.annotations.firstOrNull { it is Auxiliary }
                ?: return@forEach

            annotation as Auxiliary

            val scale = checkNotNull(property.getter.call() as? QuantityScale<*>) {
                "Failed to fetch $property"
            }

            val auxiliaries = map.getOrPut(scale.dimensionType) { MutableSetMapMultiMap() }

            listOf(annotation.symbol).plus(annotation.aliases).forEach { identifier ->
                @Suppress("UNCHECKED_CAST")
                auxiliaries[property as ScaleRef<*>].add(identifier)
            }
        }
    }

    map.keys.associateWith {
        ImmutableMultiMapView(map[it]!!)
    }
}

/**
 * Classifies this quantity with the specified auxiliary unit.
 * If the auxiliary does not exist, the base classification will be used.
 * */
inline fun<reified T> Quantity<T>.classify(override: String) : String {
    val classifiers = AUXILIARY_CLASSIFIERS[T::class.java]
        ?: return this.classify()

    val scale = classifiers.keys.firstOrNull {
        classifiers[it].contains(override)
    }

    return if(scale == null) {
        this.classify()
    }
    else {
        val annotation = scale::annotations.get().firstOrNull { it is Auxiliary } as? Auxiliary

        return if(annotation == null) {
            this.classify() // Weird
        }
        else {
            @Suppress("UNCHECKED_CAST")
            classify((this..(scale.getter.call() as QuantityScale<T>)) * annotation.factor, annotation.base.value, annotation.base.multiples, annotation.symbol)
        }
    }
}

/**
 * Map of unit dimension to override identifier. The classification override is as specified in [Auxiliary.aliases]
 * */
class AuxiliaryClassifiers(val overrides: Map<String, String>) {
    companion object {
        inline fun<reified T> name() = checkNotNull(T::class.simpleName) {
            "Failed to get name of ${T::class}"
        }
    }

    inline fun<reified T> classify(quantity: Quantity<T>) : String {
        val override = overrides[name<T>()]

        return if(override == null) {
            quantity.classify()
        } else {
            quantity.classify(override)
        }
    }
}