@file:Suppress("NOTHING_TO_INLINE")

package org.ageseries.libage.mathematics

import org.jetbrains.annotations.ApiStatus.Internal
import kotlin.math.*

const val APPROX_COMPARE_EPS = 1e-7
const val APPROX_COMPARE_EPS_FLOAT = 1e-5f
const val DUAL_COMPARE_EPS = 1e-7
const val INTEGRAL_SCAN_EPS = 1e-10
const val SYMFORCE_EPS = 2.2e-15
const val SYMFORCE_EPS_FLOAT = 1.2e-6f

inline fun lerp(from: Double, to: Double, factor: Double) = (1.0 - factor) * from + factor * to
inline fun lerp(from: Float, to: Float, factor: Float) = (1f - factor) * from + factor * to
inline fun lerp(from: Double, to: Double, factor: Dual) = (1.0 - factor) * from + factor * to
inline fun lerp(from: Dual, to: Dual, factor: Dual) = (1.0 - factor) * from + factor * to

/**
 * Computes the [base] raised to the specified power [exponent] efficiently.
 * Optimized cases:
 *  - -1 (constant time)
 * */
fun powi(base: Int, exponent: Int): Int {
    if (base == -1) {
        return 1 * snzi(-exponent % 2)
    }

    var b = base
    var exp = exponent
    var result = 1

    while (true) {
        if (exp and 1 != 0) {
            result *= b
        }
        exp = exp shr 1
        if (exp == 0) {
            break
        }
        b *= b
    }

    return result
}

/**
 * Computes the fractional part of [x].
 * */
fun frac(x: Double): Double = x - floor(x)

/**
 * Computes the fractional part of [x].
 * */
fun frac(x: Float): Float = x - floor(x)

/**
 * Maps [v] from a source range to a destination range.
 * @param srcMin The minimum value in [v]'s range.
 * @param srcMax The maximum value in [v]'s range.
 * @param dstMin The resulting range minimum.
 * @param dstMax The resulting range maximum.
 * @return [v] mapped from the source range to the destination range.
 * */
fun map(v: Double, srcMin: Double, srcMax: Double, dstMin: Double, dstMax: Double): Double {
    return dstMin + (v - srcMin) * (dstMax - dstMin) / (srcMax - srcMin)
}

/**
 * Maps [v] from a source range to a destination range.
 * @param srcMin The minimum value in [v]'s range.
 * @param srcMax The maximum value in [v]'s range.
 * @param dstMin The resulting range minimum.
 * @param dstMax The resulting range maximum.
 * @return [v] mapped from the source range to the destination range.
 * */
fun map(v: Float, srcMin: Float, srcMax: Float, dstMin: Float, dstMax: Float): Float {
    return dstMin + (v - srcMin) * (dstMax - dstMin) / (srcMax - srcMin)
}

/**
 * Maps [v] from a source range to a destination range.
 * @param srcMin The minimum value in [v]'s range.
 * @param srcMax The maximum value in [v]'s range.
 * @param dstMin The resulting range minimum.
 * @param dstMax The resulting range maximum.
 * @return [v] mapped from the source range to the destination range.
 * */
fun map(v: Dual, srcMin: Dual, srcMax: Dual, dstMin: Dual, dstMax: Dual): Dual {
    return dstMin + (v - srcMin) * (dstMax - dstMin) / (srcMax - srcMin)
}

/**
 * Maps [v] from a source range to a destination range.
 * @param srcMin The minimum value in [v]'s range.
 * @param srcMax The maximum value in [v]'s range.
 * @param dstMin The resulting range minimum.
 * @param dstMax The resulting range maximum.
 * @return [v] mapped from the source range to the destination range.
 * */
fun map(v: Int, srcMin: Int, srcMax: Int, dstMin: Int, dstMax: Int): Int {
    return dstMin + (v - srcMin) * (dstMax - dstMin) / (srcMax - srcMin)
}

fun avg(a: Double, b: Double): Double = (a + b) / 2.0
fun avg(a: Double, b: Double, c: Double): Double = (a + b + c) / 3.0
fun avg(a: Double, b: Double, c: Double, d: Double): Double = (a + b + c + d) / 4.0
fun avg(values: List<Double>) = values.sum() / values.size.toDouble()

fun Double.mappedTo(srcMin: Double, srcMax: Double, dstMin: Double, dstMax: Double) = map(this, srcMin, srcMax, dstMin, dstMax)
fun Dual.mappedTo(srcMin: Dual, srcMax: Dual, dstMin: Dual, dstMax: Dual) = map(this, srcMin, srcMax, dstMin, dstMax)

fun approxEqual(a: Double, b: Double, epsilon: Double = APPROX_COMPARE_EPS): Boolean = abs(a - b) < epsilon
fun approxEqual(a: Float, b: Float, epsilon: Float = APPROX_COMPARE_EPS_FLOAT): Boolean = abs(a - b) < epsilon
fun Double.approxEq(other: Double, epsilon: Double = APPROX_COMPARE_EPS): Boolean = approxEqual(this, other, epsilon)
infix fun Double.approxEq(other: Double): Boolean = approxEqual(this, other)
fun Float.approxEq(other: Float, epsilon: Float = APPROX_COMPARE_EPS_FLOAT) = approxEqual(this, other, epsilon)
infix fun Float.approxEq(other: Float) = approxEqual(this, other)

/**
 * Rounds the number to the specified number of [decimals].
 * */
fun Double.rounded(decimals: Int = 3): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}

fun snz(a: Double) = if (a >= 0.0) 1.0 else -1.0
fun nsnz(a: Double) = if (a <= 0.0) -1.0 else 1.0
fun snzi(a: Double) = if (a >= 0.0) 1 else -1
fun nsnzi(a: Double) = if (a <= 0.0) -1 else 1
fun snzi(a: Int) = if (a >= 0) 1 else -1
fun nsnzi(a: Int) = if (a <= 0) -1 else 1
fun snzE(a: Double) = if (a >= 0.0) SYMFORCE_EPS else -SYMFORCE_EPS
fun snzE(a: Float) = if (a >= 0f) SYMFORCE_EPS_FLOAT else -SYMFORCE_EPS_FLOAT
fun nsnzE(a: Float) = if (a <= 0f) -SYMFORCE_EPS_FLOAT else SYMFORCE_EPS_FLOAT
fun Double.nz() = this + snzE(this)
fun Float.nz() = this + snzE(this)

private const val ADAPTLOB_ALPHA = 0.816496580927726
private const val ADAPTLOB_BETA = 0.447213595499958

private fun adaptlobStp(f: (Double) -> Double, a: Double, b: Double, fa: Double, fb: Double, `is`: Double): Double {
    val h = (b - a) / 2.0
    val m = (a + b) / 2.0
    val mll = m - ADAPTLOB_ALPHA * h
    val ml = m - ADAPTLOB_BETA * h
    val mr = m + ADAPTLOB_BETA * h
    val mrr = m + ADAPTLOB_ALPHA * h
    val fmll = f(mll)
    val fml = f(ml)
    val fm = f(m)
    val fmr = f(mr)
    val fmrr = f(mrr)

    val i2 = h / 6.0 * (fa + fb + 5.0 * (fml + fmr))
    val i1 = h / 1470.0 * (77.0 * (fa + fb) + 432.0 * (fmll + fmrr) + 625.0 * (fml + fmr) + 672.0 * fm)

    return if (`is` + (i1 - i2) == `is` || mll <= a || b <= mrr) {
        i1
    } else {
        adaptlobStp(f, a, mll, fa, fmll, `is`) +
        adaptlobStp(f, mll, ml, fmll, fml, `is`) +
        adaptlobStp(f, ml, m, fml, fm, `is`) +
        adaptlobStp(f, m, mr, fm, fmr, `is`) +
        adaptlobStp(f, mr, mrr, fmr, fmrr, `is`) +
        adaptlobStp(f, mrr, b, fmrr, fb, `is`)
    }
}

fun integralScan(a: Double, b: Double, tolerance: Double = INTEGRAL_SCAN_EPS, f: (Double) -> Double): Double {
    var tol = tolerance

    val eps = 1e-15

    val m = (a + b) / 2.0
    val h = (b - a) / 2.0

    val x1 = 0.942882415695480
    val x2 = 0.641853342345781
    val x3 = 0.236383199662150

    val y1 = f(a)
    val y2 = f(m - x1 * h)
    val y3 = f(m - ADAPTLOB_ALPHA * h)
    val y4 = f(m - x2 * h)
    val y5 = f(m - ADAPTLOB_BETA * h)
    val y6 = f(m - x3 * h)
    val y7 = f(m)
    val y8 = f(m + x3 * h)
    val y9 = f(m + ADAPTLOB_BETA * h)
    val y10 = f(m + x2 * h)
    val y11 = f(m + ADAPTLOB_ALPHA * h)
    val y12 = f(m + x1 * h)
    val y13 = f(b)

    val i2 = h / 6.0 * (y1 + y13 + 5.0 * (y5 + y9))
    val i1 = h / 1470.0 * (77.0 * (y1 + y13) + 432.0 * (y3 + y11) + 625.0 * (y5 + y9) + 672.0 * y7)

    var `is` = h * (
        0.0158271919734802 * (y1 + y13) +
        0.0942738402188500 * (y2 + y12) +
        0.155071987336585 * (y3 + y11) +
        0.188821573960182 * (y4 + y10) +
        0.199773405226859 * (y5 + y9) +
        0.224926465333340 * (y6 + y8) +
        0.242611071901408 * y7
    )

    val s = snz(`is`)
    val erri1 = abs(i1 - `is`)
    val erri2 = abs(i2 - `is`)
    var r = 1.0

    if (erri2 != 0.0) {
        r = erri1 / erri2
    }

    if (r > 0.0 && r < 1.0) {
        tol /= r
    }

    `is` = s * abs(`is`) * tol / eps

    if (`is` == 0.0) {
        `is` = b - a
    }

    return adaptlobStp(f, a, b, y1, y13, `is`)
}

fun sec(x: Double) = 1.0 / cos(x)
fun csc(x: Double) = 1.0 / sin(x)
fun cot(x: Double) = 1.0 / tan(x)
fun coth(x: Double) = cosh(x) / sinh(x)
fun sech(x: Double) = 1.0 / cosh(x)
fun csch(x: Double) = 1.0 / sinh(x)

/**
 * I'm [Vector3d.cross] with Grissess. I ended up making those tests
 * */

class Dual private constructor(private val values: DoubleArray) : AbstractList<Double>() {
    constructor(values: List<Double>) : this(values.toDoubleArray())

    fun bind() = values.clone()

    /**
     * Constructs a [Dual] from the value [value] and the [tail].
     * */
    constructor(value: Double, tail: Dual) : this(
        DoubleArray(tail.values.size + 1).also {
            it[0] = value

            val size = tail.values.size
            var i = 0

            while (i < size) {
                it[i + 1] = tail.values[i]
                i++
            }
        }
    )

    /**
     * Constructs a [Dual] from the values [head] and the [tail].
     * */
    constructor(head: Dual, tail: Double) : this(
        DoubleArray(head.values.size + 1).also {
            val size = head.values.size
            var i = 0

            while (i < size) {
                it[i] = head.values[i]
                i++
            }

            it[i] = tail
        }
    )

    override operator fun get(index: Int) = values[index]

    override val size get() = values.size

    val isReal get() = values.size == 1

    /**
     * Gets the first value in this [Dual].
     * */
    val value get() = values[0]

    /**
     * Gets the values at the start of the [Dual], ignoring the last [n] values.
     * Equivalent to [dropLast].
     * */
    fun head(n: Int = 1) = Dual(DoubleArray(size - n) { values[it] })

    /**
     * Gets the values at the end of the [Dual], ignoring the first [n] values.
     * Equivalent to [drop].
     * */
    fun tail(n: Int = 1) = Dual(DoubleArray(size - n) { values[it + n] })

    operator fun unaryPlus() = this

    operator fun unaryMinus() = Dual(
        DoubleArray(size).also {
            for (i in it.indices) {
                it[i] = -this[i]
            }
        }
    )

    operator fun plus(other: Dual): Dual =
        if (this.isReal || other.isReal) const(this[0] + other[0])
        else Dual(this.value + other.value, this.tail() + other.tail())

    operator fun minus(other: Dual): Dual =
        if (this.isReal || other.isReal) const(this[0] - other[0])
        else Dual(this.value - other.value, this.tail() - other.tail())

    operator fun times(other: Dual): Dual =
        if (this.isReal || other.isReal) const(this[0] * other[0])
        else Dual(this.value * other.value, this.tail() * other.head() + this.head() * other.tail())

    operator fun div(other: Dual): Dual =
        if (this.isReal || other.isReal) const(this[0] / other[0])
        else Dual(this.value / other.value, (this.tail() * other - this * other.tail()) / (other * other))

    inline fun function(x: ((Double) -> Double), dx: ((Dual) -> Dual)): Dual =
        if (this.isReal) const(x(this.value))
        else Dual(x(this.value), dx(this.head()) * this.tail())

    inline fun mapValue(transform: (Double) -> Double) : Dual {
        val values = bind()

        if(values.isNotEmpty()) {
            values[0] = transform(values[0])
        }

        return castFromArray(values)
    }

    inline fun mapValueAndTail(transformValue: (Double) -> Double, transformTail: (Double) -> Double) : Dual {
        val values = bind()
        val size = values.size

        if(size > 0) {
            values[0] = transformValue(values[0])

            var i = 1
            while (i < size) {
                values[i] = transformTail(values[i])
                i++
            }
        }

        return castFromArray(values)
    }

    inline fun map(transform: (Double) -> Double) : Dual {
        val values = bind()
        val size = values.size

        var i = 0
        while(i < size) {
            values[i] = transform(values[i])
            i++
        }

        return castFromArray(values)
    }

    operator fun plus(const: Double) = mapValue { it + const }
    operator fun minus(const: Double) = mapValue { it - const }
    operator fun times(constant: Double) = map { v -> v * constant }
    operator fun div(constant: Double) = map { v -> v / constant }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (javaClass != other?.javaClass) {
            return false
        }

        other as Dual

        return values.contentEquals(other.values)
    }

    override fun hashCode() = values.contentHashCode()

    override fun toString(): String {
        if (values.isEmpty()) {
            return "empty"
        }

        return values.mapIndexed { i, v -> "x$i=$v" }.joinToString(", ")
    }

    companion object {
        val empty = Dual(doubleArrayOf())

        fun const(x: Double, n: Int = 1) = Dual(DoubleArray(n).also { it[0] = x })

        fun variable(v: Double, n: Int = 1) = Dual(
            DoubleArray(n).also {
                it[0] = v
                if (n > 1) {
                    it[1] = 1.0
                }
            }
        )

        fun of(vararg values: Double) = Dual(values.asList())

        /**
         * Adjusts [a] and [b] using the [head] operator so that their [size] is equal to the smallest size between [a] and [b].
         * */
        fun intersect(a: Dual, b: Dual) : Pair<Dual, Dual> {
            val sizeA = a.size
            val sizeB = b.size

            return if(sizeA == sizeB) {
                Pair(a, b)
            } else if(sizeA < sizeB) {
                Pair(a, b.head(sizeB - sizeA))
            } else {
                Pair(a.head(sizeA - sizeB), b)
            }
        }

        /**
         * Constructs a [Dual] referencing the [array] without a defensive copy.
         * Only appropriate to use when [array] is guaranteed to not be mutated after the resulting [Dual] is in use.
         * */
        @Internal
        fun castFromArray(array: DoubleArray) = Dual(array)
    }
}

operator fun Double.plus(dual: Dual) = dual.mapValue { this + it }
operator fun Double.minus(dual: Dual) = dual.mapValueAndTail({ this - it }, { -it })
operator fun Double.times(dual: Dual) = dual.map { this * it }
operator fun Double.div(dual: Dual) = this * (pow(dual, -1)) // Found that using the power then product is much quicker to compute! But it introduces some extra numerical error...

/**
 * Future implementors (Grissess), do:
 * - Generate derivatives for sin, cos, pow, etc. instead of using dual for it
 * - Replace tan, tanh etc. to calls to the function itself and generate derivatives
 * */

fun sin(x: Dual): Dual = x.function({ sin(it) }) { cos(it) }
fun cos(x: Dual): Dual = x.function({ cos(it) }) { -sin(it) }
fun tan(x: Dual): Dual = sin(x) / cos(x)
fun cot(x: Dual): Dual = cos(x) / sin(x)
fun sec(x: Dual): Dual = 1.0 / cos(x)
fun csc(x: Dual): Dual = 1.0 / sin(x)
fun sinh(x: Dual): Dual = x.function({ sinh(it) }) { cosh(it) }
fun cosh(x: Dual): Dual = x.function({ cosh(it) }) { sinh(it) }
fun tanh(x: Dual): Dual = sinh(x) / cosh(x)
fun coth(x: Dual): Dual = cosh(x) / sinh(x)
fun sech(x: Dual): Dual = 1.0 / cosh(x)
fun csch(x: Dual): Dual = 1.0 / sinh(x)
fun asin(x: Dual): Dual = x.function({ asin(it) }) { 1.0 / sqrt(1.0 - x * x) }
fun acos(x: Dual): Dual = x.function({ acos(it) }) { -1.0 / sqrt(1.0 - x * x) }
fun atan(x: Dual): Dual = x.function({ atan(it) }) { 1.0 / (x * x + 1.0)}
fun asinh(x: Dual): Dual = x.function( { asinh(it) }) { 1.0 / sqrt(x * x + 1.0) }
fun acosh(x: Dual): Dual = x.function( { acosh(it) }) { 1.0 / (sqrt(x - 1.0) * sqrt(x + 1.0)) }
fun atanh(x: Dual): Dual = x.function( { atanh(it) }) { 1.0 / (1.0 - x * x) }
fun pow(x: Dual, n: Double): Dual = x.function({ it.pow(n) }) { n * pow(it, n - 1) }
fun pow(x: Dual, n: Int): Dual = x.function({ it.pow(n) }) { n.toDouble() * pow(it, n - 1) }
fun sqrt(x: Dual): Dual = x.function({ sqrt(it) }) { 1.0 / (2.0 * sqrt(it)) }
fun ln(x: Dual): Dual = x.function({ ln(it) }) { 1.0 / it }
fun exp(x: Dual): Dual = x.function({ exp(it) }) { exp(it) }
// Exercise for the reader (Grissess):
// exponential and logarithmic function with custom base

/**
 * Dual version of [kotlin.math.atan2]
 * The **value** is evaluated with [kotlin.math.atan2], and the **tail** is evaluated like *arctan(y / x)* normally.
 * This is justified because *arctan2(y, x)* is *arctan(y / x) + C*.
 * */
fun atan2(y: Dual, x: Dual) : Dual {
    require(y.size == x.size) {
        "Dual atan2 requires y(size=${y.size}) and x(size=${x.size}) be of same size"
    }

    val size = y.size

    return if(size == 1) {
        Dual.const(atan2(y.value, x.value))
    }
    else if(size > 1) {
        val tangent = (y / x)
        val head = tangent.head()
        Dual(atan2(y.value, x.value), 1.0 / (head * head + 1.0) * tangent.tail())
    }
    else {
        Dual.empty
    }
}

fun Dual.approxEq(other: Dual, eps: Double = DUAL_COMPARE_EPS) : Boolean {
    val size = this.size

    if(size != other.size) {
        return false
    }

    var i = 0
    while (i < size) {
        if(!this[i].approxEq(other[i], eps)) {
            return false
        }

        i++
    }

    return true
}

data class DualArray(val values: List<DoubleArray>) : AbstractList<Dual>() {
    override val size: Int = if (values.isNotEmpty()) {
        val result = values[0].size
        require(values.all { it.size == result })
        result
    } else {
        0
    }

    override operator fun get(index: Int) = Dual(values.map { it[index] })
    operator fun set(index: Int, dual: Dual) = values.forEachIndexed { iDual, arr -> arr[index] = dual[iDual] }

    fun toList(): ArrayList<Dual> {
        val result =  ArrayList<Dual>(size)

        for (i in 0 until size) {
            result.add(this[i])
        }

        return result
    }

    companion object {
        fun ofZeros(count: Int, dualSize: Int) = DualArray(
            ArrayList<DoubleArray>(dualSize).apply {
                repeat(dualSize) {
                    this.add(DoubleArray(count))
                }
            }
        )
    }
}
