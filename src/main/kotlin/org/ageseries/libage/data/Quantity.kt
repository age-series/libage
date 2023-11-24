package org.ageseries.libage.data

data class QuantityScale<Unit>(val scale: Scale) {
    constructor(factor: Double, base: Double) : this(
        Scale(factor, base)
    )

    val base get() = scale.base

    val factor get() = scale.factor

    /**
     * Amplifies this scale [amplify] times.
     * */
    operator fun times(amplify: Double) = QuantityScale<Unit>(scale.factor / amplify, scale.base)

    /**
     * Reduces this scale [reduce] times.
     * */
    operator fun div(reduce: Double) = QuantityScale<Unit>(scale.factor * reduce, scale.base)

    /**
     * Amplifies this scale 1000 times.
     * */
    operator fun unaryPlus() = this * 1000.0

    /**
     * Reduces this scale 1000 times.
     * */
    operator fun unaryMinus() = this / 1000.0
}

/**
 * Represents a physical quantity, characterised by a [Unit] and a real number [value].
 * */
@JvmInline
value class Quantity<Unit>(val value: Double) : Comparable<Quantity<Unit>> {
    constructor(quantity: Double, s: QuantityScale<Unit>) : this(s.scale.unmap(quantity))

    val isZero get() = value == 0.0

    /**
     * Gets the numerical value of this quantity.
     * */
    operator fun not() = value

    operator fun unaryMinus() = Quantity<Unit>(-value)
    operator fun unaryPlus() = Quantity<Unit>(+value)
    operator fun plus(b: Quantity<Unit>) = Quantity<Unit>(this.value + b.value)
    operator fun minus(b: Quantity<Unit>) = Quantity<Unit>(this.value - b.value)
    operator fun times(scalar: Double) = Quantity<Unit>(this.value * scalar)
    operator fun div(scalar: Double) = Quantity<Unit>(this.value / scalar)
    /**
     * Divides the quantity by another quantity of the same unit. This, in turn, cancels out the quantity, returning the resulting number.
     * */
    operator fun div(b: Quantity<Unit>) = this.value / b.value

    override operator fun compareTo(other: Quantity<Unit>) = value.compareTo(other.value)

    operator fun compareTo(b: Double) = value.compareTo(b)

    /**
     * Maps this quantity to another scale of the same unit.
     * */
    operator fun rangeTo(s: QuantityScale<Unit>) = s.scale.map(value)

    override fun toString() = value.toString()

    fun <U2> reparam(factor: Double = 1.0) = Quantity<U2>(value * factor)
}

fun <U> min(a: Quantity<U>, b: Quantity<U>) = Quantity<U>(kotlin.math.min(!a, !b))
fun <U> max(a: Quantity<U>, b: Quantity<U>) = Quantity<U>(kotlin.math.max(!a, !b))
fun <U> abs(q: Quantity<U>) = Quantity<U>(kotlin.math.abs(!q))

/**
 * Defines the standard scale of the [Unit] (a scale with factor 1).
 * */
fun <Unit> standardScale(factor: Double = 1.0) = QuantityScale<Unit>(factor, 0.0)

interface Mass
val KILOGRAMS = standardScale<Mass>()
val GRAMS = -KILOGRAMS
val MILLIGRAMS = -KILOGRAMS
val kg by ::KILOGRAMS
val g by ::GRAMS
val mg by ::MILLIGRAMS

interface Time
val SECOND = standardScale<Time>()
val MILLISECONDS = -SECOND
val MICROSECONDS = -MILLISECONDS
val NANOSECONDS = -MICROSECONDS
val MINUTES = SECOND * 60.0
val HOURS = MINUTES * 60.0
val DAYS = HOURS * 24.0
val s by ::SECOND
val ms by ::MILLISECONDS

interface Distance
val METER = standardScale<Distance>()
val CENTIMETERS = METER / 100.0
val MILLIMETERS = -METER
val m by ::METER
val cm by ::CENTIMETERS
val mm by ::MILLIMETERS

interface Energy
val JOULE = standardScale<Energy>()
val KILOJOULES = +JOULE
val MEGAJOULES = +KILOJOULES
val GIGAJOULES = +MEGAJOULES
val WATT_SECONDS = QuantityScale<Energy>(JOULE.factor, 0.0)
val WATT_MINUTES = WATT_SECONDS * 60.0
val WATT_HOURS = WATT_MINUTES * 60.0
val KW_HOURS = WATT_HOURS * 1000.0
val J by ::JOULE
val kJ by ::KILOJOULES
val MJ by ::MEGAJOULES
val GJ by ::GIGAJOULES
val Ws by ::WATT_SECONDS
val Wmin by ::WATT_MINUTES
val Wh by ::WATT_HOURS

interface Power
val WATT = standardScale<Power>()
val MILLIWATT = -WATT
val KILOWATT = +WATT
val MEGAWATT = +KILOWATT
val GIGAWATT = +MEGAWATT
val W by ::WATT
val kW by ::KILOWATT
val MW by ::MEGAWATT
val GW by ::GIGAWATT

interface Potential
val VOLT = standardScale<Potential>()
val KILOVOLT = +VOLT
val MILLIVOLT = -VOLT
val V by ::VOLT
val KV by ::KILOVOLT

interface Current
val AMPERE = standardScale<Current>()
val MILLIAMPERE = -AMPERE
val A by ::AMPERE
val mA by ::MILLIAMPERE

interface Resistance
val OHM = standardScale<Resistance>()
val KILOOHM = +OHM
val MEGAOHM = +KILOOHM
val GIGAOHM = +MEGAOHM
val MILLIOHM = -OHM

// Serious precision issues? Hope not! :Fish_Smug:
val ELECTRON_VOLT = JOULE * 1.602176634e-19
val KILO_ELECTRON_VOLT = JOULE * 1.602176634e-16
val MEGA_ELECTRON_VOLT = JOULE * 1.602176634e-13
val GIGA_ELECTRON_VOLT = JOULE * 1.602176634e-10
val TERA_ELECTRON_VOLT = JOULE * 1.602176634e-7
val eV by ::ELECTRON_VOLT
val keV by ::KILO_ELECTRON_VOLT
val MeV by ::MEGA_ELECTRON_VOLT
val GeV by ::GIGA_ELECTRON_VOLT
val TeV by ::TERA_ELECTRON_VOLT

interface Radioactivity
val BECQUEREL = standardScale<Radioactivity>()
val KILOBECQUERELS = +BECQUEREL
val MEGABECQUERELS = +KILOBECQUERELS
val GIGABECQUERELS = +MEGABECQUERELS
val TERABECQUERELS = +GIGABECQUERELS
val CURIE = GIGABECQUERELS * 37.0
val MILLICURIES = MEGABECQUERELS * 37.0
val MICROCURIES = KILOBECQUERELS * 37.0
val NANOCURIES = BECQUEREL * 37.0
val KILOCURIES = +CURIE
val MEGACURIES = +KILOCURIES
val GIGACURIES = +MEGACURIES // Average conversation with Grissess (every disintegration is a cute dragon image)

interface RadiationAbsorbedDose
val GRAY = standardScale<RadiationAbsorbedDose>()
val RAD = GRAY / 100.0

interface RadiationDoseEquivalent
val SIEVERT = standardScale<RadiationDoseEquivalent>()
val MILLISIEVERTS = -SIEVERT
val MICROSIEVERTS = -MILLISIEVERTS
val REM = SIEVERT / 100.0
val MILLIREM = -REM
val MICROREM = -MILLIREM

interface RadiationExposure
val COULOMB_PER_KG = standardScale<RadiationExposure>()
val ROENTGEN = COULOMB_PER_KG / 3875.96899225

interface ReciprocalDistance
val RECIP_METER = standardScale<ReciprocalDistance>()
val RECIP_CENTIMETERS = RECIP_METER * 100.0

interface ArealDensity
val KG_PER_M2 = standardScale<ArealDensity>()
val G_PER_CM2 = KG_PER_M2 * 10.0

interface Density
val KG_PER_M3 = standardScale<Density>()
val G_PER_CM3 = KG_PER_M3 * 1000.0
val G_PER_L = KG_PER_M3

interface ReciprocalArealDensity
val M2_PER_KG = standardScale<ReciprocalArealDensity>()
val CM2_PER_G = M2_PER_KG / 10.0

interface Velocity
val M_PER_S = standardScale<Velocity>()
val KM_PER_S = +M_PER_S

interface Substance
val MOLE = standardScale<Substance>()

interface MolarConcentration
val MOLE_PER_M3 = standardScale<MolarConcentration>()

interface Area
val M2 = standardScale<Area>()

interface Volume
val M3 = standardScale<Volume>()
val LITERS = M3 / 1000.0
val MILLILITERS = -LITERS
val L by ::LITERS
val mL by ::MILLILITERS

interface Temperature
val KELVIN = standardScale<Temperature>()
val CELSIUS = QuantityScale<Temperature>(Scale(1.0, -273.15, "°C"))

interface SpecificHeatCapacity
val J_PER_KG_K = standardScale<SpecificHeatCapacity>()
val J_PER_G_K = +J_PER_KG_K
val KJ_PER_KG_K = +J_PER_KG_K

interface HeatCapacity
val J_PER_K = standardScale<HeatCapacity>()

interface ThermalConductivity
val W_PER_M_K = standardScale<ThermalConductivity>()
val mW_PER_M_K = -W_PER_M_K

interface MolecularWeight
val KG_PER_MOLE = standardScale<MolecularWeight>()
val G_PER_MOLE = -KG_PER_MOLE

interface Pressure
val PASCAL = standardScale<Pressure>()
val ATMOSPHERES = PASCAL * 9.86923e-6
val Pa by ::PASCAL
val Atm by ::ATMOSPHERES

interface Intensity
val WATT_PER_M2 = standardScale<Intensity>()
val KILOWATT_PER_M2 = +WATT_PER_M2

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
class Scale(
    val factor: Double,
    val base: Double,
    /**
     * The unit suffix string accepted for use with this linear scale, to be displayed after the value.
     *
     * This should be the "physical symbol" of this unit, which is inherently international--avoid units
     * which lack these, and set this to an empty string otherwise.
     */
    val displayUnit: String = "",
) {
    /** Given a value [u] in base unit, return its value in this unit. */
    fun map(u: Double): Double = factor * u + base

    /** Given a value [u] in this unit, return its value in base units. */
    fun unmap(u: Double): Double = (u - base) / factor

    /**
     * Give a human-readable string of a value in this scale.
     */
    fun display(u: Double): String = "${u}${displayUnit}"
}