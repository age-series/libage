@file:Suppress("LocalVariableName", "NOTHING_TO_INLINE", "MemberVisibilityCanBePrivate")

package org.ageseries.libage.mathematics

import java.util.ArrayList
import kotlin.math.*

const val GEOMETRY_COMPARE_EPS = 1e-6
const val GEOMETRY_NORMALIZED_EPS = 1e-7

data class Matrix3x3(val c0: Vector3d, val c1: Vector3d, val c2: Vector3d) {
    constructor(
        m00: Double, m01: Double, m02: Double,
        m10: Double, m11: Double, m12: Double,
        m20: Double, m21: Double, m22: Double,
    ) : this(
        Vector3d(m00, m10, m20),
        Vector3d(m01, m11, m21),
        Vector3d(m02, m12, m22)
    )

    constructor(mat: DoubleArray) : this(mat[0], mat[1], mat[2], mat[3], mat[4], mat[5], mat[6], mat[7], mat[8])

    val r0 get() = Vector3d(c0.x, c1.x, c2.x)
    val r1 get() = Vector3d(c0.y, c1.y, c2.y)
    val r2 get() = Vector3d(c0.z, c1.z, c2.z)
    val determinant get() = c0.x * (c1.y * c2.z - c2.y * c1.z) - c1.x * (c0.y * c2.z - c2.y * c0.z) + c2.x * (c0.y * c1.z - c1.y * c0.z)
    val transpose get() = Matrix3x3(r0, r1, r2)
    val trace get() = c0.x + c1.y + c2.z
    val normFrobeniusSqr get() = c0.normSqr + c1.normSqr + c2.normSqr
    val normFrobenius get() = sqrt(normFrobeniusSqr)
    val isOrthogonal get() = (this * this.transpose).approxEq(identity) && this.determinant.absoluteValue.approxEq(1.0)
    val isSpecialOrthogonal get() = (this * this.transpose).approxEq(identity) && this.determinant.approxEq(1.0)

    override fun toString(): String {
        fun rowStr(row: Vector3d) = "${row.x} ${row.y} ${row.z}"
        return "${rowStr(r0)}\n${rowStr(r1)}\n${rowStr(r2)}"
    }

    operator fun not() = Matrix3x3(
        (c1.y * c2.z - c2.y * c1.z), -(c1.x * c2.z - c2.x * c1.z), (c1.x * c2.y - c2.x * c1.y),
        -(c0.y * c2.z - c2.y * c0.z), (c0.x * c2.z - c2.x * c0.z), -(c0.x * c2.y - c2.x * c0.y),
        (c0.y * c1.z - c1.y * c0.z), -(c0.x * c1.z - c1.x * c0.z), (c0.x * c1.y - c1.x * c0.y)
    ) * (1.0 / determinant)

    operator fun times(scalar: Double) = Matrix3x3(c0 * scalar, c1 * scalar, c2 * scalar)
    operator fun times(v: Vector3d) = c0 * v.x + c1 * v.y + c2 * v.z
    operator fun times(m: Matrix3x3) = Matrix3x3(this * m.c0, this * m.c1, this * m.c2)
    operator fun plus(m: Matrix3x3) = Matrix3x3(c0 + m.c0, c1 + m.c1, c2 + m.c2)
    operator fun minus(m: Matrix3x3) = Matrix3x3(c0 - m.c0, c1 - m.c1, c2 - m.c2)

    fun getColumn(c: Int) = when (c) {
        0 -> c0
        1 -> c1
        2 -> c2
        else -> error("Column $c out of bounds")
    }

    fun getRow(r: Int) = when (r) {
        0 -> r0
        1 -> r1
        2 -> r2
        else -> error("Row $r out of bounds")
    }

    operator fun get(r: Int, c: Int) = when (r) {
        0 -> getColumn(c).x
        1 -> getColumn(c).y
        2 -> getColumn(c).z
        else -> error("Row $r out of bounds")
    }

    fun approxEq(other: Matrix3x3, eps: Double = GEOMETRY_COMPARE_EPS) =
        this.c0.approxEq(other.c0, eps) && this.c1.approxEq(other.c1, eps) && this.c2.approxEq(other.c2, eps)

    fun toArray() : DoubleArray {
        val array = DoubleArray(9)

        array[0] = c0.x
        array[1] = c1.x
        array[2] = c2.x
        array[3] = c0.y
        array[4] = c1.y
        array[5] = c2.y
        array[6] = c0.z
        array[7] = c1.z
        array[8] = c2.z

        return array
    }

    companion object {
        val identity = Matrix3x3(Vector3d.unitX, Vector3d.unitY, Vector3d.unitZ)

        inline fun get(array: DoubleArray, c: Int, r: Int) = r * 3 + c
    }
}

data class Matrix3x3Dual(val c0: Vector3dDual, val c1: Vector3dDual, val c2: Vector3dDual) {
    constructor(
        m00: Dual, m01: Dual, m02: Dual,
        m10: Dual, m11: Dual, m12: Dual,
        m20: Dual, m21: Dual, m22: Dual,
    ) : this(
        Vector3dDual(m00, m10, m20),
        Vector3dDual(m01, m11, m21),
        Vector3dDual(m02, m12, m22)
    )

    constructor(mat: List<Dual>) : this(
        mat[0], mat[1], mat[2],
        mat[3], mat[4], mat[5],
        mat[6], mat[7], mat[8]
    )

    init {
        require(c0.size == c1.size && c1.size == c2.size)
    }

    val r0 get() = Vector3dDual(c0.x, c1.x, c2.x)
    val r1 get() = Vector3dDual(c0.y, c1.y, c2.y)
    val r2 get() = Vector3dDual(c0.z, c1.z, c2.z)

    val size get() = c0.size
    val isReal get() = size == 1
    val value get() = Matrix3x3(c0.value, c1.value, c2.value)
    fun head(n: Int) = Matrix3x3Dual(c0.head(n), c1.head(n), c2.head(n))
    fun tail(n: Int) = Matrix3x3Dual(c0.tail(n), c1.tail(n), c2.tail(n))
    val transpose get() = Matrix3x3Dual(r0, r1, r2)
    val trace get() = c0.x + c1.y + c2.z
    val normFrobeniusSqr get() = c0.normSqr + c1.normSqr + c2.normSqr
    val normFrobenius get() = sqrt(normFrobeniusSqr)
    val determinant get() = c0.x * (c1.y * c2.z - c2.y * c1.z) - c1.x * (c0.y * c2.z - c2.y * c0.z) + c2.x * (c0.y * c1.z - c1.y * c0.z)

    operator fun not() = Matrix3x3Dual(
        (c1.y * c2.z - c2.y * c1.z), -(c1.x * c2.z - c2.x * c1.z), (c1.x * c2.y - c2.x * c1.y),
        -(c0.y * c2.z - c2.y * c0.z), (c0.x * c2.z - c2.x * c0.z), -(c0.x * c2.y - c2.x * c0.y),
        (c0.y * c1.z - c1.y * c0.z), -(c0.x * c1.z - c1.x * c0.z), (c0.x * c1.y - c1.x * c0.y)
    ) * (1.0 / determinant)

    operator fun times(scalar: Dual) = Matrix3x3Dual(c0 * scalar, c1 * scalar, c2 * scalar)
    operator fun times(constant: Double) = Matrix3x3Dual(c0 * constant, c1 * constant, c2 * constant)
    operator fun div(scalar: Dual) = Matrix3x3Dual(c0 / scalar, c1 / scalar, c2 / scalar)
    operator fun div(constant: Double) = Matrix3x3Dual(c0 / constant, c1 / constant, c2 / constant)
    operator fun times(v: Vector3dDual) = c0 * v.x + c1 * v.y + c2 * v.z
    operator fun times(v: Vector3d) = c0 * v.x + c1 * v.y + c2 * v.z
    operator fun times(m: Matrix3x3Dual) = Matrix3x3Dual(this * m.c0, this * m.c1, this * m.c2)
    operator fun times(m: Matrix3x3) = Matrix3x3Dual(this * m.c0, this * m.c1, this * m.c2)
    operator fun get(n: Int) = Matrix3x3(c0[n], c1[n], c2[n])

    companion object {
        fun const(v: Matrix3x3, n: Int = 1) = Matrix3x3Dual(
            Vector3dDual.const(v.c0, n),
            Vector3dDual.const(v.c1, n),
            Vector3dDual.const(v.c2, n)
        )
    }
}

data class Matrix4x4(val c0: Vector4d, val c1: Vector4d, val c2: Vector4d, val c3: Vector4d) {
    constructor(
        m00: Double, m01: Double, m02: Double, m03: Double,
        m10: Double, m11: Double, m12: Double, m13: Double,
        m20: Double, m21: Double, m22: Double, m23: Double,
        m30: Double, m31: Double, m32: Double, m33: Double,
    ) : this(
        Vector4d(m00, m10, m20, m30),
        Vector4d(m01, m11, m21, m31),
        Vector4d(m02, m12, m22, m32),
        Vector4d(m03, m13, m23, m33)
    )

    val r0 get() = Vector4d(c0.x, c1.x, c2.x, c3.x)
    val r1 get() = Vector4d(c0.y, c1.y, c2.y, c3.y)
    val r2 get() = Vector4d(c0.z, c1.z, c2.z, c3.z)
    val r3 get() = Vector4d(c0.w, c1.w, c2.w, c3.w)
    val transpose get() = Matrix4x4(r0, r1, r2, r3)
    val trace get() = c0.x + c1.y + c2.z + c3.w
    val normFrobeniusSqr get() = c0.normSqr + c1.normSqr + c2.normSqr + c3.normSqr
    val normFrobenius get() = sqrt(normFrobeniusSqr)

    val determinant get() =
        Matrix3x3(c1.y, c2.y, c3.y, c1.z, c2.z, c3.z, c1.w, c2.w, c3.w).determinant * c0.x -
        Matrix3x3(c1.x, c2.x, c3.x, c1.z, c2.z, c3.z, c1.w, c2.w, c3.w).determinant * c0.y +
        Matrix3x3(c1.x, c2.x, c3.x, c1.y, c2.y, c3.y, c1.w, c2.w, c3.w).determinant * c0.z -
        Matrix3x3(c1.x, c2.x, c3.x, c1.y, c2.y, c3.y, c1.z, c2.z, c3.z).determinant * c0.w

    fun eliminate(eliminateR: Int, eliminateC: Int): Matrix3x3 {
        val values = DoubleArray(3 * 3)

        var irActual = 0
        var icActual = 0

        for (ir in 0 until 4) {
            if (ir != eliminateR) {
                for (ic in 0 until 4) {
                    if (ic != eliminateC) {
                        values[(irActual * 3) + icActual++] = this[ic, ir]
                    }
                }

                irActual++
                icActual = 0
            }
        }

        return Matrix3x3(values)
    }

    fun minor(c: Int, r: Int) = eliminate(c, r).determinant
    fun cofactor(c: Int, r: Int) = minor(c, r) * (-1).pow(c + r)

    val cofactorMatrix get() =
        Matrix4x4(
            cofactor(0, 0), cofactor(1, 0), cofactor(2, 0), cofactor(3, 0),
            cofactor(0, 1), cofactor(1, 1), cofactor(2, 1), cofactor(3, 1),
            cofactor(0, 2), cofactor(1, 2), cofactor(2, 2), cofactor(3, 2),
            cofactor(0, 3), cofactor(1, 3), cofactor(2, 3), cofactor(3, 3)
        )

    val adjugateMatrix get() = cofactorMatrix.transpose

    fun approxEq(other: Matrix4x4, eps: Double = GEOMETRY_COMPARE_EPS) =
        c0.approxEq(other.c0, eps) &&
        c1.approxEq(other.c1, eps) &&
        c2.approxEq(other.c2, eps) &&
        c3.approxEq(other.c3, eps)

    override fun toString(): String {
        fun rowStr(row: Vector4d) = "${row.x} ${row.y} ${row.z} ${row.w}"
        return "${rowStr(r0)}\n${rowStr(r1)}\n${rowStr(r2)}\n${rowStr(r3)}"
    }

    operator fun not() = adjugateMatrix * (1.0 / determinant)
    operator fun times(scalar: Double) = Matrix4x4(c0 * scalar, c1 * scalar, c2 * scalar, c3 * scalar)
    operator fun times(v: Vector4d) = c0 * v.x + c1 * v.y + c2 * v.z + c3 * v.w
    operator fun times(v: Vector3d) = (this * Vector4d(v.x, v.y, v.z, 1.0)).let { Vector3d(it.x, it.y, it.z) }
    operator fun times(m: Matrix4x4) = Matrix4x4(this * m.c0, this * m.c1, this * m.c2, this * m.c3)

    fun getColumn(c: Int) = when (c) {
        0 -> c0
        1 -> c1
        2 -> c2
        3 -> c3
        else -> error("Column $c out of bounds")
    }

    fun getRow(r: Int) = when (r) {
        0 -> r0
        1 -> r1
        2 -> r2
        3 -> r3
        else -> error("Row $r out of bounds")
    }

    operator fun get(r: Int, c: Int) = when (r) {
        0 -> getColumn(c).x
        1 -> getColumn(c).y
        2 -> getColumn(c).z
        3 -> getColumn(c).w
        else -> error("Row $r out of bounds")
    }

    companion object {
        val identity = Matrix4x4(Vector4d.unitX, Vector4d.unitY, Vector4d.unitZ, Vector4d.unitW)
    }
}

data class Matrix4x4Dual(val c0: Vector4dDual, val c1: Vector4dDual, val c2: Vector4dDual, val c3: Vector4dDual) {
    constructor(
        m00: Dual, m01: Dual, m02: Dual, m03: Dual,
        m10: Dual, m11: Dual, m12: Dual, m13: Dual,
        m20: Dual, m21: Dual, m22: Dual, m23: Dual,
        m30: Dual, m31: Dual, m32: Dual, m33: Dual,
    ) : this(
        Vector4dDual(m00, m10, m20, m30),
        Vector4dDual(m01, m11, m21, m31),
        Vector4dDual(m02, m12, m22, m32),
        Vector4dDual(m03, m13, m23, m33)
    )

    constructor(mat: List<Dual>) : this(
        mat[0], mat[1], mat[2], mat[3],
        mat[4], mat[5], mat[6], mat[7],
        mat[8], mat[9], mat[10], mat[11],
        mat[12], mat[13], mat[14], mat[15]
    )

    init {
        require(c0.size == c1.size && c1.size == c2.size && c2.size == c3.size)
    }

    val size get() = c0.size
    val isReal get() = size == 1
    val value get() = Matrix4x4(c0.value, c1.value, c2.value, c3.value)
    fun head(n: Int) = Matrix4x4Dual(c0.head(n), c1.head(n), c2.head(n), c3.head(n))
    fun tail(n: Int) = Matrix4x4Dual(c0.tail(n), c1.tail(n), c2.tail(n), c3.tail(n))

    val r0 get() = Vector4dDual(c0.x, c1.x, c2.x, c3.x)
    val r1 get() = Vector4dDual(c0.y, c1.y, c2.y, c3.y)
    val r2 get() = Vector4dDual(c0.z, c1.z, c2.z, c3.z)
    val r3 get() = Vector4dDual(c0.w, c1.w, c2.w, c3.w)

    val transpose get() = Matrix4x4Dual(r0, r1, r2, r3)
    val trace get() = c0.x + c1.y + c2.z + c3.w
    val normFrobeniusSqr get() = c0.normSqr + c1.normSqr + c2.normSqr + c3.normSqr
    val normFrobenius get() = sqrt(normFrobeniusSqr)

    // Inlined for performance. We're already down a deep rabbit hole of performance issues
    // because of our design (and ⅅ, like this matrix), so at least let me have this one.
    val determinant get() =
        Matrix3x3Dual(c1.y, c2.y, c3.y, c1.z, c2.z, c3.z, c1.w, c2.w, c3.w).determinant * c0.x -
        Matrix3x3Dual(c1.x, c2.x, c3.x, c1.z, c2.z, c3.z, c1.w, c2.w, c3.w).determinant * c0.y +
        Matrix3x3Dual(c1.x, c2.x, c3.x, c1.y, c2.y, c3.y, c1.w, c2.w, c3.w).determinant * c0.z -
        Matrix3x3Dual(c1.x, c2.x, c3.x, c1.y, c2.y, c3.y, c1.z, c2.z, c3.z).determinant * c0.w

    fun eliminate(eliminateC: Int, eliminateR: Int): Matrix3x3Dual {
        val values = ArrayList<Dual>(9)

        repeat(9) { values.add(Dual.empty) }

        var irActual = 0
        var icActual = 0

        for (ir in 0 until 4) {
            if (ir != eliminateR) {
                for (ic in 0 until 4) {
                    if (ic != eliminateC) {
                        values[(irActual * 3) + icActual++] = this[ic, ir]
                    }
                }

                irActual++
                icActual = 0
            }
        }

        return Matrix3x3Dual(values)
    }

    fun minor(c: Int, r: Int) = eliminate(c, r).determinant
    fun cofactor(c: Int, r: Int) = minor(c, r) * (-1).pow(c + r).toDouble()
    val cofactorMatrix
        get() = Matrix4x4Dual(
            cofactor(0, 0), cofactor(1, 0), cofactor(2, 0), cofactor(3, 0),
            cofactor(0, 1), cofactor(1, 1), cofactor(2, 1), cofactor(3, 1),
            cofactor(0, 2), cofactor(1, 2), cofactor(2, 2), cofactor(3, 2),
            cofactor(0, 3), cofactor(1, 3), cofactor(2, 3), cofactor(3, 3)
        )
    val adjugateMatrix get() = cofactorMatrix.transpose

    operator fun not() = adjugateMatrix * (1.0 / determinant)
    operator fun times(scalar: Dual) = Matrix4x4Dual(c0 * scalar, c1 * scalar, c2 * scalar, c3 * scalar)
    operator fun times(constant: Double) = Matrix4x4Dual(c0 * constant, c1 * constant, c2 * constant, c3 * constant)
    operator fun times(v: Vector4dDual) = c0 * v.x + c1 * v.y + c2 * v.z + c3 * v.w
    operator fun times(v: Vector4d) = c0 * v.x + c1 * v.y + c2 * v.z + c3 * v.w
    operator fun times(m: Matrix4x4Dual) = Matrix4x4Dual(this * m.c0, this * m.c1, this * m.c2, this * m.c3)
    operator fun times(m: Matrix4x4) = Matrix4x4Dual(this * m.c0, this * m.c1, this * m.c2, this * m.c3)
    operator fun get(n: Int) = Matrix4x4(c0[n], c1[n], c2[n], c3[n])

    fun getColumn(c: Int) = when (c) {
        0 -> c0
        1 -> c1
        2 -> c2
        3 -> c3
        else -> error("Column $c out of bounds")
    }

    fun getRow(r: Int) = when (r) {
        0 -> r0
        1 -> r1
        2 -> r2
        3 -> r3
        else -> error("Row $r out of bounds")
    }

    operator fun get(c: Int, r: Int) = when (r) {
        0 -> getColumn(c).x
        1 -> getColumn(c).y
        2 -> getColumn(c).z
        3 -> getColumn(c).w
        else -> error("Row $r out of bounds")
    }

    companion object {
        fun const(v: Matrix4x4, n: Int = 1) = Matrix4x4Dual(
            Vector4dDual.const(v.c0, n),
            Vector4dDual.const(v.c1, n),
            Vector4dDual.const(v.c2, n),
            Vector4dDual.const(v.c3, n)
        )
    }
}

data class Vector2di(val x: Int, val y: Int) {
    constructor(value: Int) : this(value, value)

    operator fun unaryPlus() = this
    operator fun unaryMinus() = Vector2di(-x, -y)
    operator fun times(b: Int) = Vector2di(x * b, y * b)
    operator fun div(b: Int) = Vector2di(x / b, y / b)
    operator fun minus(b: Vector2di) = Vector2di(x - b.x, y - b.y)
    operator fun plus(b: Vector2di) = Vector2di(x + b.x, y + b.y)

    companion object {
        val zero = Vector2di(0, 0)
        val one = Vector2di(1, 1)
        val unitX = Vector2di(1, 0)
        val unitY = Vector2di(0, 1)

        fun manhattan(a: Vector2di, b: Vector2di) : Int {
            val dx = abs(a.x - b.x)
            val dy = abs(a.y - b.y)

            return dx + dy
        }
    }
}

data class Vector2d(val x: Double, val y: Double) {
    constructor(value: Double) : this(value, value)

    val isNaN get() = x.isNaN() || y.isNaN()
    val isInfinity get() = x.isInfinite() || y.isInfinite()

    infix fun dot(b: Vector2d) = x * b.x + y * b.y
    val normSqr get() = this dot this
    val norm get() = sqrt(normSqr)
    infix fun distanceTo(b: Vector2d) = (this - b).norm
    infix fun distanceToSqr(b: Vector2d) = (this - b).normSqr
    fun nz() = Vector2d(x.nz(), y.nz())
    fun normalized() = this / norm
    fun normalizedNz() = this.nz() / norm.nz()
    val perpendicularLeft get() = Vector2d(-y, x)
    val perpendicularRight get() = Vector2d(y, -x)

    fun approxEq(other: Vector2d, eps: Double = GEOMETRY_COMPARE_EPS) = x.approxEq(other.x, eps) && y.approxEq(other.y, eps)

    override fun toString() = "x=$x, y=$y"

    operator fun unaryPlus() = this
    operator fun unaryMinus() = Vector2d(-x, -y)
    operator fun plus(other: Vector2d) = Vector2d(x + other.x, y + other.y)
    operator fun minus(other: Vector2d) = Vector2d(x - other.x, y - other.y)
    operator fun times(other: Vector2d) = Vector2d(x * other.x, y * other.y)
    operator fun div(other: Vector2d) = Vector2d(x / other.x, y / other.y)
    operator fun times(scalar: Double) = Vector2d(x * scalar, y * scalar)
    operator fun div(scalar: Double) = Vector2d(x / scalar, y / scalar)

    operator fun compareTo(other: Vector2d) = this.normSqr.compareTo(other.normSqr)

    companion object {
        val zero = Vector2d(0.0, 0.0)
        val one = Vector2d(1.0, 1.0)
        val unitX = Vector2d(1.0, 0.0)
        val unitY = Vector2d(0.0, 1.0)

        fun lerp(a: Vector2d, b: Vector2d, t: Double) = Vector2d(
            lerp(a.x, b.x, t),
            lerp(a.y, b.y, t)
        )

        fun min(a: Vector2d, b: Vector2d) = Vector2d(min(a.x, b.x), min(a.y, b.y))

        fun max(a: Vector2d, b: Vector2d) = Vector2d(max(a.x, b.x), max(a.y, b.y))
    }
}

infix fun Vector2d.o(other: Vector2d) = this dot other
operator fun Vector2d.rangeTo(b: Vector2d) = this distanceTo b

data class Vector2dDual(val x: Dual, val y: Dual) {
    constructor(value: Dual) : this(value, value)

    constructor(values: List<Vector2d>) : this(
        Dual(values.map { it.x }),
        Dual(values.map { it.y })
    )

    init {
        require(x.size == y.size) { "Dual X and Y must be of the same size" }
        require(x.size > 0) { "X and Y must not be empty" }
    }

    val size get() = x.size
    val isReal get() = size == 1
    infix fun dot(b: Vector2dDual) = x * b.x + y * b.y
    val normSqr get() = this dot this
    val norm get() = sqrt(normSqr)
    fun normalized() = this / norm
    val value get() = Vector2d(x.value, y.value)
    fun head(n: Int = 1) = Vector2dDual(x.head(n), y.head(n))
    fun tail(n: Int = 1) = Vector2dDual(x.tail(n), y.tail(n))

    override fun toString() = "x=$x, y=$y"

    operator fun unaryPlus() = this
    operator fun unaryMinus() = Vector2dDual(-x, -y)
    operator fun plus(other: Vector2dDual) = Vector2dDual(x + other.x, y + other.y)
    operator fun minus(other: Vector2dDual) = Vector2dDual(x - other.x, y - other.y)
    operator fun times(other: Vector2dDual) = Vector2dDual(x * other.x, y * other.y)
    operator fun div(other: Vector2dDual) = Vector2dDual(x / other.x, y / other.y)
    operator fun times(scalar: Dual) = Vector2dDual(x * scalar, y * scalar)
    operator fun div(scalar: Dual) = Vector2dDual(x / scalar, y / scalar)
    operator fun times(constant: Double) = Vector2dDual(x * constant, y * constant)
    operator fun div(constant: Double) = Vector2dDual(x / constant, y / constant)
    operator fun get(n: Int) = Vector2d(x[n], y[n])

    companion object {
        fun const(x: Double, y: Double, n: Int = 1) = Vector2dDual(Dual.const(x, n), Dual.const(y, n))
        fun const(value: Vector2d, n: Int = 1) = const(value.x, value.y, n)
        fun of(vararg values: Vector2d) = Vector2dDual(values.asList())
    }
}

infix fun Vector2dDual.o(other: Vector2dDual) = this dot other

data class Rotation2d(val re: Double, val im: Double) {
    val isNaN get() = re.isNaN() || im.isNaN()
    val isInfinity get() = re.isInfinite() || im.isInfinite()

    fun ln() = atan2(im, re)
    fun scaled(k: Double) = exp(ln() * k)
    val inverse get() = Rotation2d(re, -im)
    val direction get() = Vector2d(re, im)

    fun approxEq(other: Rotation2d, eps: Double = GEOMETRY_COMPARE_EPS) = re.approxEq(other.re, eps) && im.approxEq(other.im, eps)

    override fun toString() = "θ×=${Math.toDegrees(ln())} deg"

    operator fun not() = this.inverse
    operator fun times(other: Rotation2d) = Rotation2d(this.re * other.re - this.im * other.im, this.re * other.im + this.im * other.re)
    operator fun times(v: Vector2d) = Vector2d(this.re * v.x - this.im * v.y, this.im * v.x + this.re * v.y)
    operator fun times(transform: Pose2d) = Pose2d(this * transform.translation, this * transform.rotation)
    operator fun div(b: Rotation2d) = b.inverse * this
    operator fun plus(incr: Double) = this * exp(incr)
    operator fun minus(b: Rotation2d) = (this / b).ln()

    companion object {
        val identity = exp(0.0)

        fun exp(angleIncr: Double) = Rotation2d(cos(angleIncr), sin(angleIncr))

        fun dir(direction: Vector2d): Rotation2d {
            val dir = direction.normalized()

            return Rotation2d(dir.x, dir.y)
        }

        fun interpolate(r0: Rotation2d, r1: Rotation2d, t: Double) = exp(t * (r1 / r0).ln()) * r0
    }
}

data class Rotation2dDual(val re: Dual, val im: Dual) {
    fun ln() = atan2(im, re)
    fun scaled(k: Double) = exp(ln() * k)
    val value get() = Rotation2d(re.value, im.value)
    val angularVelocity get() = re * im.tail() - im * re.tail()
    val inverse get() = Rotation2dDual(re, -im)
    val direction get() = Vector2dDual(re, im)

    operator fun not() = this.inverse
    operator fun times(b: Rotation2dDual) = Rotation2dDual(this.re * b.re - this.im * b.im, this.re * b.im + this.im * b.re)
    operator fun times(b: Rotation2d) = Rotation2dDual(this.re * b.re - this.im * b.im, this.re * b.im + this.im * b.re)
    operator fun times(r2: Vector2dDual) = Vector2dDual(this.re * r2.x - this.im * r2.y, this.im * r2.x + this.re * r2.y)

    operator fun times(r2: Vector2d) = Vector2dDual(this.re * r2.x - this.im * r2.y, this.im * r2.x + this.re * r2.y)

    companion object {
        fun exp(angleIncr: Dual) = Rotation2dDual(cos(angleIncr), sin(angleIncr))
        fun const(value: Rotation2d, n: Int = 1) = Rotation2dDual(Dual.const(value.re, n), Dual.const(value.im, n))
        fun const(angleIncr: Double, n: Int = 1) = exp(Dual.const(angleIncr, n))
    }
}

data class Twist2dIncr(val trIncr: Vector2d, val rotIncr: Double) {
    constructor(xIncr: Double, yIncr: Double, rotIncr: Double) : this(Vector2d(xIncr, yIncr), rotIncr)

    override fun toString() = "X=${trIncr.x} Y=${trIncr.y} R=$rotIncr"
}

data class Twist2dIncrDual(val trIncr: Vector2dDual, val rotIncr: Dual) {
    constructor(xIncr: Dual, yIncr: Dual, rotIncr: Dual) : this(Vector2dDual(xIncr, yIncr), rotIncr)

    val value get() = Twist2dIncr(trIncr.value, rotIncr.value)
    val velocity get() = Twist2dDual(trIncr.tail(), rotIncr.tail())

    companion object {
        fun const(trIncr: Vector2d, rotIncr: Double, n: Int = 1) =
            Twist2dIncrDual(Vector2dDual.const(trIncr, n), Dual.const(rotIncr, n))
    }
}

data class Twist2d(val trVelocity: Vector2d, val rotVelocity: Double) {
    constructor(dx: Double, dy: Double, dTheta: Double) : this(Vector2d(dx, dy), dTheta)

    operator fun plus(other: Twist2d) = Twist2d(trVelocity + other.trVelocity, rotVelocity + other.rotVelocity)
    operator fun minus(other: Twist2d) = Twist2d(trVelocity - other.trVelocity, rotVelocity - other.rotVelocity)
    operator fun times(scalar: Double) = Twist2d(trVelocity * scalar, rotVelocity * scalar)
    operator fun div(scalar: Double) = Twist2d(trVelocity / scalar, rotVelocity / scalar)
}

data class Twist2dDual(val trVelocity: Vector2dDual, val rotVelocity: Dual) {
    constructor(dx: Dual, dy: Dual, dTheta: Dual) : this(Vector2dDual(dx, dy), dTheta)

    val value get() = Twist2d(trVelocity.value, rotVelocity.value)
    fun head(n: Int = 1) = Twist2dDual(trVelocity.head(n), rotVelocity.head(n))
    fun tail(n: Int = 1) = Twist2dDual(trVelocity.tail(n), rotVelocity.tail(n))

    operator fun plus(other: Twist2dDual) = Twist2dDual(trVelocity + other.trVelocity, rotVelocity + other.rotVelocity)
    operator fun minus(other: Twist2dDual) = Twist2dDual(trVelocity - other.trVelocity, rotVelocity - other.rotVelocity)

    companion object {
        fun const(value: Twist2d, n: Int = 1) =
            Twist2dDual(Vector2dDual.const(value.trVelocity, n), Dual.const(value.rotVelocity, n))
    }
}

data class Pose2d(val translation: Vector2d, val rotation: Rotation2d) {
    constructor(x: Double, y: Double, angle: Double) : this(Vector2d(x, y), Rotation2d.exp(angle))
    constructor(x: Double, y: Double) : this(x, y, 0.0)

    val inverse get() = Pose2d(rotation.inverse * -translation, rotation.inverse)
    operator fun not() = this.inverse

    fun ln(): Twist2dIncr {
        val angle = rotation.ln()
        val u = 0.5 * angle
        val c = rotation.re - 1.0

        val ht = if (abs(c) < 1e-9) {
            1.0 - 1.0 / 12.0 * (angle * angle)
        } else {
            -u * rotation.im / c
        }

        return Twist2dIncr(
            Vector2d(
                ht * translation.x + u * translation.y,
                -u * translation.x + ht * translation.y
            ),
            angle
        )
    }

    fun approxEq(other: Pose2d, eps: Double = GEOMETRY_COMPARE_EPS) = translation.approxEq(other.translation, eps) && rotation.approxEq(other.rotation, eps)

    override fun toString() = "$translation $rotation"

    operator fun times(b: Pose2d) = Pose2d(this.translation + this.rotation * b.translation, this.rotation * b.rotation)
    operator fun times(v: Vector2d) = this.translation + this.rotation * v
    operator fun div(b: Pose2d) = b.inverse * this
    operator fun plus(incr: Twist2dIncr) = this * exp(incr)
    operator fun minus(b: Pose2d) = (this / b).ln()

    companion object {
        val identity = Pose2d(Vector2d.zero, Rotation2d.identity)

        fun exp(tw: Twist2dIncr): Pose2d {
            val z = Rotation2d.exp(tw.rotIncr)
            val s: Double
            val c: Double

            if (abs(tw.rotIncr) < 1e-9) {
                s = 1.0 - 1.0 / 6.0 * tw.rotIncr * tw.rotIncr
                c = 0.5 * tw.rotIncr
            } else {
                s = z.im / tw.rotIncr
                c = (1.0 - z.re) / tw.rotIncr
            }

            return Pose2d(
                Vector2d(
                    s * tw.trIncr.x - c * tw.trIncr.y,
                    c * tw.trIncr.x + s * tw.trIncr.y
                ),
                z
            )
        }

    }
}

data class Pose2dDual(val translation: Vector2dDual, val rotation: Rotation2dDual) {
    val inverse get() = Pose2dDual(rotation.inverse * -translation, rotation.inverse)
    operator fun not() = this.inverse

    val value get() = Pose2d(translation.value, rotation.value)
    val velocity get() = Twist2dDual(translation.tail(), rotation.angularVelocity)

    override fun toString() = "$translation $rotation"

    operator fun times(b: Pose2dDual) = Pose2dDual(this.translation + this.rotation * b.translation, this.rotation * b.rotation)
    operator fun times(b: Pose2d) = Pose2dDual(this.translation + this.rotation * b.translation, this.rotation * b.rotation)
    operator fun times(b: Twist2dDual) = Twist2dDual(rotation * b.trVelocity, b.rotVelocity)
    operator fun div(b: Pose2dDual) = b.inverse * this
    operator fun plus(incr: Twist2dIncr) = this * Pose2d.exp(incr)

    companion object {
        fun const(v: Pose2d, n: Int = 1) = Pose2dDual(Vector2dDual.const(v.translation, n), Rotation2dDual.const(v.rotation, n))
    }
}

data class Vector3di(val x: Int, val y: Int, val z: Int) {
    constructor(value: Int) : this(value, value, value)

    operator fun unaryPlus() = this
    operator fun unaryMinus() = Vector3di(-x, -y, -z)
    operator fun times(b: Int) = Vector3di(x * b, y * b, z * b)
    operator fun div(b: Int) = Vector3di(x / b, y / b, z / b)
    operator fun minus(b: Vector3di) = Vector3di(x - b.x, y - b.y, z - b.z)
    operator fun plus(b: Vector3di) = Vector3di(x + b.x, y + b.y, z + b.z)

    companion object {
        val zero = Vector3di(0, 0, 0)
        val one = Vector3di(1, 1, 1)
        val unitX = Vector3di(1, 0, 0)
        val unitY = Vector3di(0, 1, 0)
        val unitZ = Vector3di(0, 0, 1)

        fun manhattan(a: Vector3di, b: Vector3di) : Int {
            val dx = abs(a.x - b.x)
            val dy = abs(a.y - b.y)
            val dz = abs(a.z - b.z)

            return dx + dy + dz
        }
    }
}

data class Vector3d(val x: Double, val y: Double, val z: Double) {
    constructor(x: Int, y: Int, z: Int) : this(x.toDouble(), y.toDouble(), z.toDouble())
    constructor(value: Double) : this(value, value, value)

    val isNaN get() = x.isNaN() || y.isNaN() || z.isNaN()
    val isInfinity get() = x.isInfinite() || y.isInfinite() || z.isInfinite()

    infix fun dot(b: Vector3d) = x * b.x + y * b.y + z * b.z
    val normSqr get() = this dot this
    val norm get() = sqrt(normSqr)
    val isUnit get() = normSqr.approxEq(1.0, GEOMETRY_NORMALIZED_EPS)
    infix fun distanceTo(b: Vector3d) = (this - b).norm
    infix fun distanceToSqr(b: Vector3d) = (this - b).normSqr
    infix fun cosAngle(b: Vector3d) = ((this dot b) / (this.norm * b.norm)).coerceIn(-1.0, 1.0)
    infix fun angle(b: Vector3d) = acos(this cosAngle b)

    fun nz() = Vector3d(x.nz(), y.nz(), z.nz())
    fun normalized() = this / norm
    fun normalizedNz() = this.nz() / norm.nz()

    infix fun cross(b: Vector3d) = Vector3d(
        this.y * b.z - this.z * b.y,
        this.z * b.x - this.x * b.z,
        this.x * b.y - this.y * b.x
    )

    fun approxEq(other: Vector3d, eps: Double = GEOMETRY_COMPARE_EPS) = x.approxEq(other.x, eps) && y.approxEq(other.y, eps) && z.approxEq(other.z, eps)

    override fun toString() = "x=$x, y=$y, z=$z"

    operator fun not() = if(this.isUnit) {
        this
    } else if(this != zero) {
        this.normalized()
    } else {
        this
    }

    operator fun unaryPlus() = this
    operator fun unaryMinus() = Vector3d(-x, -y, -z)
    operator fun plus(b: Vector3d) = Vector3d(x + b.x, y + b.y, z + b.z)
    operator fun minus(b: Vector3d) = Vector3d(x - b.x, y - b.y, z - b.z)
    operator fun times(b: Vector3d) = Vector3d(x * b.x, y * b.y, z * b.z)
    operator fun div(b: Vector3d) = Vector3d(x / b.x, y / b.y, z / b.z)
    operator fun times(scalar: Double) = Vector3d(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Double) = Vector3d(x / scalar, y / scalar, z / scalar)
    operator fun compareTo(other: Vector3d) = this.normSqr.compareTo(other.normSqr)

    fun projectOnPlane(n: Vector3d) = this - n * ((this dot n) / n.normSqr)
    fun projectOnVector(v: Vector3d) = if (this == zero || v == zero) zero else v * (this dot v) / v.normSqr

    fun frac() = Vector3d(frac(this.x), frac(this.y), frac(this.z))
    fun floor() = Vector3d(floor(this.x), floor(this.y), floor(this.z))
    fun ceil() = Vector3d(ceil(this.x), ceil(this.y), ceil(this.z))
    fun round() = Vector3d(round(this.x), round(this.y), round(this.z))
    fun floorInt() = Vector3di(floor(this.x).toInt(), floor(this.y).toInt(), floor(this.z).toInt())
    fun ceilInt() = Vector3di(ceil(this.x).toInt(), ceil(this.y).toInt(), ceil(this.z).toInt())
    fun roundInt() = Vector3di(round(this.x).toInt(), round(this.y).toInt(), round(this.z).toInt())
    fun intCast() = Vector3di(x.toInt(), y.toInt(), z.toInt())

    fun perpendicular() : Vector3d {
        val (x, y, z) = if(!this.isUnit && this != zero) {
            this.normalized()
        }
        else {
            this
        }

        val result = if (abs(y + z) > GEOMETRY_NORMALIZED_EPS || abs(x) > GEOMETRY_NORMALIZED_EPS) {
            Vector3d(-y - z, x, x)
        }
        else {
            Vector3d(z, z, -x - y)
        }

        return result.normalized()
    }

    // pixar
    fun onb() : Pair<Vector3d, Vector3d> {
        val sign = z.sign
        val a = -1.0 / (sign + z)
        val b = x * y * a

        return Pair(
            Vector3d(
                1.0 + sign * x * x * a,
                sign * b,
                -sign * x
            ),
            Vector3d(
                b,
                sign + y * y * a,
                -y
            )
        )
    }

    companion object {
        val zero = Vector3d(0.0, 0.0, 0.0)
        val one = Vector3d(1.0, 1.0, 1.0)
        val unitX = Vector3d(1.0, 0.0, 0.0)
        val unitY = Vector3d(0.0, 1.0, 0.0)
        val unitZ = Vector3d(0.0, 0.0, 1.0)

        fun lerp(from: Vector3d, to: Vector3d, t: Double) = Vector3d(
            lerp(from.x, to.x, t),
            lerp(from.y, to.y, t),
            lerp(from.z, to.z, t)
        )

        fun min(a: Vector3d, b: Vector3d) = Vector3d(
            min(a.x, b.x),
            min(a.y, b.y),
            min(a.z, b.z)
        )

        fun max(a: Vector3d, b: Vector3d) = Vector3d(
            max(a.x, b.x),
            max(a.y, b.y),
            max(a.z, b.z)
        )
    }
}

infix fun Vector3d.o(other: Vector3d) = this dot other
infix fun Vector3d.x(other: Vector3d) = this cross other
operator fun Vector3d.rangeTo(b: Vector3d) = this distanceTo b

fun min(a: Vector3d, b: Vector3d) = Vector3d(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z))
fun max(a: Vector3d, b: Vector3d) = Vector3d(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z))
infix fun Vector3d.directionTo(b: Vector3d) = (b - this).normalized()

data class Vector3dDual(val x: Dual, val y: Dual, val z: Dual) {
    constructor(value: Dual) : this(value, value, value)

    constructor(values: List<Vector3d>) : this(
        Dual(values.map { it.x }),
        Dual(values.map { it.y }),
        Dual(values.map { it.z })
    )

    init {
        require(x.size == y.size && y.size == z.size) {
            "Dual X, Y and Z must be of the same size"
        }
    }

    val size get() = x.size
    val isReal get() = size == 1
    infix fun dot(b: Vector3d) = x * b.x + y * b.y + z * b.z
    infix fun dot(b: Vector3dDual) = x * b.x + y * b.y + z * b.z
    val normSqr get() = this dot this
    val norm get() = sqrt(normSqr)
    fun normalized() = this / norm

    infix fun cross(b: Vector3d) = Vector3dDual(
        this.y * b.z - this.z * b.y,
        this.z * b.x - this.x * b.z,
        this.x * b.y - this.y * b.x
    )

    infix fun cross(b: Vector3dDual) = Vector3dDual(
        this.y * b.z - this.z * b.y,
        this.z * b.x - this.x * b.z,
        this.x * b.y - this.y * b.x
    )

    val value get() = Vector3d(x.value, y.value, z.value)
    fun head(n: Int = 1) = Vector3dDual(x.head(n), y.head(n), z.head(n))
    fun tail(n: Int = 1) = Vector3dDual(x.tail(n), y.tail(n), z.tail(n))

    fun projectOnPlane(n: Vector3dDual) = this - n * ((this dot n) / n.normSqr)
    fun projectOnPlane(n: Vector3d) = projectOnPlane(const(n, size))
    fun projectOnVector(v: Vector3dDual) = v * (this dot v) / v.normSqr
    fun projectOnVector(v: Vector3d) = projectOnVector(const(v, size))

    operator fun unaryPlus() = this
    operator fun unaryMinus() = Vector3dDual(-x, -y, -z)
    operator fun plus(other: Vector3dDual) = Vector3dDual(x + other.x, y + other.y, z + other.z)
    operator fun plus(other: Vector3d) = Vector3dDual(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3dDual) = Vector3dDual(x - other.x, y - other.y, z - other.z)
    operator fun minus(other: Vector3d) = Vector3dDual(x - other.x, y - other.y, z - other.z)
    operator fun times(other: Vector3dDual) = Vector3dDual(x * other.x, y * other.y, z * other.z)
    operator fun times(other: Vector3d) = Vector3dDual(x * other.x, y * other.y, z * other.z)
    operator fun div(other: Vector3dDual) = Vector3dDual(x / other.x, y / other.y, z / other.z)
    operator fun div(other: Vector3d) = Vector3dDual(x / other.x, y / other.y, z / other.z)
    operator fun times(scalar: Dual) = Vector3dDual(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Dual) = Vector3dDual(x / scalar, y / scalar, z / scalar)
    operator fun times(constant: Double) = Vector3dDual(x * constant, y * constant, z * constant)
    operator fun div(constant: Double) = Vector3dDual(x / constant, y / constant, z / constant)
    operator fun get(n: Int) = Vector3d(x[n], y[n], z[n])

    companion object {
        fun const(x: Double, y: Double, z: Double, n: Int = 1) = Vector3dDual(Dual.const(x, n), Dual.const(y, n), Dual.const(z, n))
        fun const(value: Vector3d, n: Int = 1) = const(value.x, value.y, value.z, n)
        fun of(vararg values: Vector3d) = Vector3dDual(values.asList())

        fun lerp(from: Vector3dDual, to: Vector3dDual, t: Dual) = Vector3dDual(
            lerp(from.x, to.x, t),
            lerp(from.y, to.y, t),
            lerp(from.z, to.z, t)
        )

        fun lerp(from: Vector3d, to: Vector3d, t: Dual) = lerp(
            const(from, t.size),
            const(to, t.size),
            t
        )
    }
}

fun Vector3dDual.o(other: Vector3d) = this dot other
fun Vector3dDual.o(other: Vector3dDual) = this dot other
fun Vector3dDual.x(other: Vector3d) = this cross other
fun Vector3dDual.x(other: Vector3dDual) = this cross other

data class Vector4d(val x: Double, val y: Double, val z: Double, val w: Double) {
    constructor(value: Double) : this(value, value, value, value)

    val isNaN get() = x.isNaN() || y.isNaN() || z.isNaN() || w.isNaN()
    val isInfinity get() = x.isInfinite() || y.isInfinite() || z.isInfinite() || w.isInfinite()

    infix fun dot(b: Vector4d) = x * b.x + y * b.y + z * b.z + w * b.w
    val normSqr get() = this dot this
    val norm get() = sqrt(normSqr)
    infix fun distanceTo(b: Vector4d) = (this - b).norm
    infix fun distanceToSqr(b: Vector4d) = (this - b).normSqr
    infix fun cosAngle(b: Vector4d) = (this o b) / (this.norm * b.norm)
    infix fun angle(b: Vector4d) = acos((this cosAngle b).coerceIn(-1.0, 1.0))

    fun nz() = Vector4d(x.nz(), y.nz(), z.nz(), w.nz())
    fun normalized() = this / norm
    fun normalizedNz() = this.nz() / norm.nz()

    fun approxEq(other: Vector4d, eps: Double = GEOMETRY_COMPARE_EPS) = x.approxEq(other.x, eps) && y.approxEq(other.y, eps) && z.approxEq(other.z, eps) && w.approxEq(other.w, eps)

    override fun toString() = "x=$x, y=$y, z=$z, w=$w"

    operator fun rangeTo(b: Vector4d) = this distanceTo b
    operator fun unaryPlus() = this
    operator fun unaryMinus() = Vector4d(-x, -y, -z, -w)
    operator fun plus(b: Vector4d) = Vector4d(x + b.x, y + b.y, z + b.z, w + b.w)
    operator fun minus(b: Vector4d) = Vector4d(x - b.x, y - b.y, z - b.z, w - b.w)
    operator fun times(b: Vector4d) = Vector4d(x * b.x, y * b.y, z * b.z, w * b.w)
    operator fun div(b: Vector4d) = Vector4d(x / b.x, y / b.y, z / b.z, w / b.w)
    operator fun rem(b: Vector4d) = this angle b
    operator fun times(scalar: Double) = Vector4d(x * scalar, y * scalar, z * scalar, w * scalar)
    operator fun div(scalar: Double) = Vector4d(x / scalar, y / scalar, z / scalar, w / scalar)

    operator fun compareTo(other: Vector4d) = this.normSqr.compareTo(other.normSqr)

    companion object {
        val zero = Vector4d(0.0, 0.0, 0.0, 0.0)
        val one = Vector4d(1.0, 1.0, 1.0, 1.0)
        val unitX = Vector4d(1.0, 0.0, 0.0, 0.0)
        val unitY = Vector4d(0.0, 1.0, 0.0, 0.0)
        val unitZ = Vector4d(0.0, 0.0, 1.0, 0.0)
        val unitW = Vector4d(0.0, 0.0, 0.0, 1.0)

        fun lerp(a: Vector4d, b: Vector4d, t: Double) = Vector4d(
            lerp(a.x, b.x, t),
            lerp(a.y, b.y, t),
            lerp(a.z, b.z, t),
            lerp(a.w, b.w, t)
        )
    }
}

infix fun Vector4d.o(other: Vector4d) = this dot other

data class Vector4dDual(val x: Dual, val y: Dual, val z: Dual, val w: Dual) {
    constructor(value: Dual) : this(value, value, value, value)
    constructor(values: List<Vector4d>) : this(
        Dual(values.map { it.x }),
        Dual(values.map { it.y }),
        Dual(values.map { it.z }),
        Dual(values.map { it.w })
    )

    init {
        require(x.size == y.size && y.size == z.size && z.size == w.size) { "Dual X, Y, Z and W must be of the same size" }
    }

    val size get() = x.size
    val isReal get() = size == 1
    infix fun o(b: Vector4dDual) = x * b.x + y * b.y + z * b.z + w * b.w
    val normSqr get() = this o this
    val norm get() = sqrt(normSqr)
    val value get() = Vector4d(x.value, y.value, z.value, w.value)
    fun head(n: Int = 1) = Vector4dDual(x.head(n), y.head(n), z.head(n), w.head(n))
    fun tail(n: Int = 1) = Vector4dDual(x.tail(n), y.tail(n), z.tail(n), w.tail(n))

    fun normalized() = this / norm

    operator fun unaryPlus() = this
    operator fun unaryMinus() = Vector4dDual(-x, -y, -z, -w)
    operator fun plus(b: Vector4dDual) = Vector4dDual(x + b.x, y + b.y, z + b.z, w + b.w)
    operator fun plus(b: Vector4d) = Vector4dDual(x + b.x, y + b.y, z + b.z, w + b.w)
    operator fun minus(b: Vector4dDual) = Vector4dDual(x - b.x, y - b.y, z - b.z, w - b.w)
    operator fun minus(b: Vector4d) = Vector4dDual(x - b.x, y - b.y, z - b.z, w - b.w)
    operator fun times(b: Vector4dDual) = Vector4dDual(x * b.x, y * b.y, z * b.z, w * b.w)
    operator fun times(b: Vector4d) = Vector4dDual(x * b.x, y * b.y, z * b.z, w * b.w)
    operator fun div(b: Vector4dDual) = Vector4dDual(x / b.x, y / b.y, z / b.z, w / b.w)
    operator fun div(b: Vector4d) = Vector4dDual(x / b.x, y / b.y, z / b.z, w / b.w)
    operator fun times(scalar: Dual) = Vector4dDual(x * scalar, y * scalar, z * scalar, w * scalar)
    operator fun times(constant: Double) = Vector4dDual(x * constant, y * constant, z * constant, w * constant)
    operator fun div(scalar: Dual) = Vector4dDual(x / scalar, y / scalar, z / scalar, w / scalar)
    operator fun div(constant: Double) = Vector4dDual(x / constant, y / constant, z / constant, w / constant)
    operator fun get(n: Int) = Vector4d(x[n], y[n], z[n], w[n])

    companion object {
        fun const(x: Double, y: Double, z: Double, w: Double, n: Int = 1) =
            Vector4dDual(Dual.const(x, n), Dual.const(y, n), Dual.const(z, n), Dual.const(w, n))

        fun const(value: Vector4d, n: Int = 1) = const(value.x, value.y, value.z, value.w, n)
        fun of(vararg values: Vector4d) = Vector4dDual(values.asList())
    }
}

fun avg(a: Vector2d, b: Vector2d) = (a + b) / 2.0
fun avg(a: Vector2d, b: Vector2d, c: Vector2d) = (a + b + c) / 3.0
fun avg(a: Vector2d, b: Vector2d, c: Vector2d, d: Vector2d) = (a + b + c + d) / 4.0
fun avg(vectors: List<Vector2d>) = vectors.reduce { a, b -> a + b } / vectors.size.toDouble()
fun avg(a: Vector3d, b: Vector3d) = (a + b) / 2.0
fun avg(a: Vector3d, b: Vector3d, c: Vector3d) = (a + b + c) / 3.0
fun avg(a: Vector3d, b: Vector3d, c: Vector3d, d: Vector3d) = (a + b + c + d) / 4.0
fun avg(vectors: List<Vector3d>) = vectors.reduce { a, b -> a + b } / vectors.size.toDouble()
fun avg(a: Vector4d, b: Vector4d) = (a + b) / 2.0
fun avg(a: Vector4d, b: Vector4d, c: Vector4d) = (a + b + c) / 3.0
fun avg(a: Vector4d, b: Vector4d, c: Vector4d, d: Vector4d) = (a + b + c + d) / 4.0
fun avg(vectors: List<Vector4d>) = vectors.reduce { a, b -> a + b } / vectors.size.toDouble()

data class AxisAngle3d(val axis: Vector3d, val angle: Double)

data class Rotation3d(val x: Double, val y: Double, val z: Double, val w: Double) {
    val isNaN get() = x.isNaN() || y.isNaN() || z.isNaN() || w.isNaN()
    val isInfinity get() = x.isInfinite() || y.isInfinite() || z.isInfinite() || w.isInfinite()

    val normSqr get() = this dot this
    val norm get() = sqrt(normSqr)
    fun normalized() = this / norm
    val xyz get() = Vector3d(x, y, z)
    val inverse get() = Rotation3d(-x, -y, -z, w) / normSqr

    infix fun dot(other: Rotation3d) = this.x * other.x + this.y * other.y + this.z * other.z + this.w * other.w

    fun ln(): Vector3d {
        val n = xyz.norm

        return xyz * if (n < 1e-9) {
            2.0 / w - 2.0 / 3.0 * n * n / (w * w * w)
        } else {
            2.0 * atan2(n * snz(w), w * snz(w)) / n
        }
    }

    fun toAxisAngle() : AxisAngle3d {
        val euler = ln() // Mathematics was invented by Euler, keep that in mind
        val angle = euler.norm

        return if(angle == 0.0) {
            // Possible?
            AxisAngle3d(Vector3d.zero, 0.0)
        } else {
            AxisAngle3d(euler / angle, angle)
        }
    }

    operator fun times(scalar: Double) = Rotation3d(x * scalar, y * scalar, z * scalar, w * scalar)
    operator fun div(scalar: Double) = Rotation3d(x / scalar, y / scalar, z / scalar, w / scalar)

    operator fun unaryPlus() = this
    operator fun unaryMinus() = Rotation3d(-x, -y, -z, -w)

    operator fun not() = this.inverse

    operator fun plus(w: Vector3d) = this * exp(w)
    operator fun minus(b: Rotation3d) = (this / b).ln()
    operator fun plus(angle: Double) : Rotation3d {
        if(this == identity) {
            return identity
        }

        val w = ln()
        val n = w.norm

        return exp((w / n) * (n + angle))
    }

    operator fun times(other: Rotation3d) = Rotation3d(
        x * other.w + other.x * w + (y * other.z - z * other.y), // Could also use FMADD
        y * other.w + other.y * w + (z * other.x - x * other.z),
        z * other.w + other.z * w + (x * other.y - y * other.x),
        w * other.w - (x * other.x + y * other.y + z * other.z)
    )
    operator fun times(v: Vector3d): Vector3d {
        val `2wx` = 2.0 * (w * x)
        val `2wy` = 2.0 * (w * y)
        val `2wz` = 2.0 * (w * z)
        val `2xx` = 2.0 * (x * x)
        val `2xy` = 2.0 * (x * y)
        val `2xz` = 2.0 * (x * z)
        val `2yy` = 2.0 * (y * y)
        val `2yz` = 2.0 * (y * z)
        val `2zz` = 2.0 * (z * z)

        return Vector3d(
            (v.x * (1.0 - `2yy` - `2zz`) + v.y * (`2xy` - `2wz`) + v.z * (`2xz` + `2wy`)),
            (v.x * (`2xy` + `2wz`) + v.y * (1.0 - `2xx` - `2zz`) + v.z * (`2yz` - `2wx`)),
            (v.x * (`2xz` - `2wy`) + v.y * (`2yz` + `2wx`) + v.z * (1.0 - `2xx` - `2yy`))
        )
    }
    operator fun times(transform: Pose3d) = Pose3d(this * transform.translation, this * transform.rotation)

    operator fun div(b: Rotation3d) = b.inverse * this

    operator fun invoke() : Matrix3x3 {
        val `2xx` = 2.0 * (x * x)
        val `2yy` = 2.0 * (y * y)
        val `2zz` = 2.0 * (z * z)
        val `2xy` = 2.0 * (x * y)
        val `2xz` = 2.0 * (x * z)
        val `2xw` = 2.0 * (x * w)
        val `2yz` = 2.0 * (y * z)
        val `2yw` = 2.0 * (y * w)
        val `2zw` = 2.0 * (z * w)

        return Matrix3x3(
            1.0 - `2yy` - `2zz`, `2xy` - `2zw`, `2xz` + `2yw`,
            `2xy` + `2zw`, 1.0 - `2xx` - `2zz`, `2yz` - `2xw`,
            `2xz` - `2yw`, `2yz` + `2xw`, 1.0 - `2xx` - `2yy`
        )
    }

    operator fun invoke(k: Double) = exp(ln() * k)

    fun approxEqComponentWise(other: Rotation3d, eps: Double = GEOMETRY_COMPARE_EPS) = x.approxEq(other.x, eps) && y.approxEq(other.y, eps) && z.approxEq(other.z, eps) && w.approxEq(other.w, eps)
    fun approxEq(other: Rotation3d, eps: Double = GEOMETRY_COMPARE_EPS) = abs(this dot other).approxEq(1.0, eps)

    companion object {
        val identity = Rotation3d(0.0, 0.0, 0.0, 1.0)

        fun exp(w: Vector3d): Rotation3d {
            val t = w.norm

            if (t == 0.0) {
                return identity
            }

            val axis = w / t
            val s = sin(t / 2.0)

            return Rotation3d(axis.x * s, axis.y * s, axis.z * s, cos(t / 2.0))
        }

        fun fromAxisAngle(axis: Vector3d, angle: Double) = exp(axis.normalized() * angle)

        fun fromAxisAngle(axisAngle3d: AxisAngle3d) = Companion.fromAxisAngle(axisAngle3d.axis, axisAngle3d.angle)

        fun fromRotationMatrix(matrix: Matrix3x3): Rotation3d {
            val c0 = matrix.c0
            val c1 = matrix.c1
            val c2 = matrix.c2

            val m00 = c0.x
            val m01 = c0.y
            val m02 = c0.z
            val m10 = c1.x
            val m11 = c1.y
            val m12 = c1.z
            val m20 = c2.x
            val m21 = c2.y
            val m22 = c2.z

            val t: Double
            val q: Rotation3d

            if (m22 < 0) {
                if (m00 > m11) {
                    t = 1.0 + m00 - m11 - m22
                    q = Rotation3d(
                        t,
                        m01 + m10,
                        m20 + m02,
                        m12 - m21
                    )
                } else {
                    t = 1.0 - m00 + m11 - m22
                    q = Rotation3d(
                        m01 + m10,
                        t,
                        m12 + m21,
                        m20 - m02
                    )
                }
            } else {
                if (m00 < -m11) {
                    t = 1.0 - m00 - m11 + m22
                    q = Rotation3d(
                        m20 + m02,
                        m12 + m21,
                        t,
                        m01 - m10
                    )
                } else {
                    t = 1.0 + m00 + m11 + m22
                    q = Rotation3d(
                        m12 - m21,
                        m20 - m02,
                        m01 - m10,
                        t
                    )
                }
            }

            return q * 0.5 / sqrt(t)
        }

        fun alg(w: Vector3d) = Matrix3x3(
            0.0, -w.z, w.y,
            w.z, 0.0, -w.x,
            -w.y, w.x, 0.0
        )

        fun interpolate(r0: Rotation3d, r1: Rotation3d, t: Double) = exp((r1 / r0).ln() * t) * r0
    }
}

data class Rotation3dDual(val x: Dual, val y: Dual, val z: Dual, val w: Dual) {
    init {
        require(x.size == y.size && y.size == z.size && z.size == w.size)
    }

    val size get() = x.size

    val xyz get() = Vector3dDual(x, y, z)
    val normSqr get() = x * x + y * y + z * z + w * w
    val norm get() = sqrt(normSqr)
    fun normalized() = this / norm
    val inverse get() = Rotation3dDual(-x, -y, -z, w) / normSqr
    val value get() = Rotation3d(x.value, y.value, z.value, w.value)

    fun ln(): Vector3dDual {
        val n = xyz.norm

        return xyz * if (n.value < 1e-9) {
            2.0 / w - 2.0 / 3.0 * n * n / (w * w * w)
        } else {
            2.0 * atan2(n * snz(w.value), w * snz(w.value)) / n
        }
    }

    val angularVelocity: Vector3dDual
        get() {
            val n = xyz.norm
            val im = n * snz(w.value)
            val re = w * snz(w.value)
            return xyz * (2.0 * (re * im.tail() - im * re.tail()) / n)
        }

    fun head(n: Int = 1) = Rotation3dDual(x.head(n), y.head(n), z.head(n), w.head(n))
    fun tail(n: Int = 1) = Rotation3dDual(x.tail(n), y.tail(n), z.tail(n), w.tail(n))

    operator fun div(scalar: Dual) = Rotation3dDual(x / scalar, y / scalar, z / scalar, w / scalar)

    operator fun unaryPlus() = this
    operator fun unaryMinus() = Rotation3dDual(-x, -y, -z, -w)
    operator fun not() = this.inverse
    operator fun times(b: Rotation3dDual) =
        Rotation3dDual(
            x * b.w + b.x * w + (y * b.z - z * b.y),
            y * b.w + b.y * w + (z * b.x - x * b.z),
            z * b.w + b.z * w + (x * b.y - y * b.x),
            w * b.w - (x * b.x + y * b.y + z * b.z)
        )

    operator fun times(value: Vector3dDual): Vector3dDual {
        val a = 2.0 * (w * x)
        val b = 2.0 * (w * y)
        val c = 2.0 * (w * z)
        val d = 2.0 * (x * x)
        val e = 2.0 * (x * y)
        val f = 2.0 * (x * z)
        val g = 2.0 * (y * y)
        val h = 2.0 * (y * z)
        val i = 2.0 * (z * z)

        return Vector3dDual(
            (value.x * (1.0 - g - i) + value.y * (e - c) + value.z * (f + b)),
            (value.x * (e + c) + value.y * (1.0 - d - i) + value.z * (h - a)),
            (value.x * (f - b) + value.y * (h + a) + value.z * (1.0 - d - g))
        )
    }

    operator fun div(b: Rotation3dDual) = b.inverse * this

    operator fun invoke() : Matrix3x3Dual {
        val `2xx` = 2.0 * (x * x)
        val `2yy` = 2.0 * (y * y)
        val `2zz` = 2.0 * (z * z)
        val `2xy` = 2.0 * (x * y)
        val `2xz` = 2.0 * (x * z)
        val `2xw` = 2.0 * (x * w)
        val `2yz` = 2.0 * (y * z)
        val `2yw` = 2.0 * (y * w)
        val `2zw` = 2.0 * (z * w)

        return Matrix3x3Dual(
            1.0 - `2yy` - `2zz`, `2xy` - `2zw`, `2xz` + `2yw`,
            `2xy` + `2zw`, 1.0 - `2xx` - `2zz`, `2yz` - `2xw`,
            `2xz` - `2yw`, `2yz` + `2xw`, 1.0 - `2xx` - `2yy`
        )
    }

    companion object {
        fun fromAxisAngle(axis: Vector3dDual, angle: Dual) = exp(axis.normalized() * angle)

        fun exp(w: Vector3dDual): Rotation3dDual {
            val angle = w.norm
            val axis = w / angle
            val s = sin(angle / 2.0)

            return Rotation3dDual(axis.x * s, axis.y * s, axis.z * s, cos(angle / 2.0))
        }
    }
}

data class Twist3d(val trVelocity: Vector3d, val rotVelocity: Vector3d)

data class Twist3dDual(val trVelocity: Vector3dDual, val rotVelocity: Vector3dDual) {
    val value get() = Twist3d(trVelocity.value, rotVelocity.value)
    fun head(n: Int = 1) = Twist3dDual(trVelocity.head(n), rotVelocity.head(n))
    fun tail(n: Int = 1) = Twist3dDual(trVelocity.tail(n), rotVelocity.tail(n))
}

data class Twist3dIncr(val trIncr: Vector3d, val rotIncr: Vector3d)

data class Twist3dIncrDual(val trIncr: Vector3dDual, val rotIncr: Vector3dDual) {
    val value get() = Twist3dIncr(trIncr.value, rotIncr.value)
    val velocity get() = Twist3dDual(trIncr.tail(), rotIncr.tail())
}

data class Pose3d(val translation: Vector3d, val rotation: Rotation3d) {
    val inverse get() = Pose3d(rotation.inverse * -translation, rotation.inverse)

    fun ln(): Twist3dIncr {
        val w = rotation.ln()
        val wx = Rotation3d.alg(w)
        val t = w.norm
        val t2 = t * t

        val c = if (abs(t) < 1e-7) {
            1 / 12.0 + t2 / 720.0 + t2 * t2 / 30240.0
        } else {
            // Evaluates to -Inf at 1e-8
            (1.0 - sin(t) / t / (2.0 * ((1.0 - cos(t)) / t2))) / t2
        }

        return Twist3dIncr(
            (Matrix3x3.identity - (wx * 0.5) + ((wx * wx) * c)) * translation,
            w
        )
    }

    operator fun not() = this.inverse

    operator fun plus(incr: Twist3dIncr) = this * exp(incr)
    operator fun plus(displacement: Vector3d) = Pose3d(translation + displacement, rotation)
    operator fun plus(rotation: Rotation3d) = Pose3d(translation, rotation * this.rotation)
    operator fun minus(displacement: Vector3d) = Pose3d(translation - displacement, rotation)
    operator fun minus(rotation: Rotation3d) = Pose3d(translation, rotation.inverse / this.rotation)
    operator fun minus(other: Pose3d) = (this / other).ln()
    operator fun times(other: Pose3d) = Pose3d(this.translation + this.rotation * other.translation, this.rotation * other.rotation)
    operator fun times(v: Vector3d) = this.translation + this.rotation * v
    operator fun times(transform: Rotation3d) = this.rotation * transform
    operator fun times(obb: OrientedBoundingBox3d) = OrientedBoundingBox3d(this * obb.transform, obb.halfSize)
    operator fun div(b: Pose3d) = b.inverse * this

    fun approxEq(other: Pose3d, eps: Double = GEOMETRY_COMPARE_EPS) =
        translation.approxEq(other.translation, eps) &&
        rotation.approxEq(other.rotation, eps)

    operator fun invoke() : Matrix4x4 {
        val (rc0, rc1, rc2) = rotation()

        return Matrix4x4(
            rc0.x, rc1.x, rc2.x, translation.x,
            rc0.y, rc1.y, rc2.y, translation.y,
            rc0.z, rc1.z, rc2.z, translation.z,
            0.0, 0.0, 0.0, 1.0
        )
    }

    companion object {
        val identity = Pose3d(Vector3d.zero, Rotation3d.identity)

        fun fromMatrix(matrix: Matrix4x4) : Pose3d {
            val (c0, c1, c2, c3) = matrix

            return Pose3d(
                Vector3d(c3.x, c3.y, c3.z),
                Rotation3d.fromRotationMatrix(
                    Matrix3x3(
                        c0.x, c1.x, c2.x,
                        c0.y, c1.y, c2.y,
                        c0.z, c1.z, c2.z
                    )
                )
            )
        }

        fun exp(incr: Twist3dIncr): Pose3d {
            val t = incr.rotIncr.norm
            val t2 = t * t
            val t4 = t2 * t2

            val b: Double
            val c: Double

            if (abs(t) < 1e-7) {
                b = 1.0 / 2.0 - t2 / 24.0 + t4 / 720.0
                c = 1.0 / 6.0 - t2 / 120.0 + t4 / 5040.0
            } else {
                b = (1.0 - cos(t)) / t2
                c = (1.0 - sin(t) / t) / t2
            }

            val wx = Rotation3d.alg(incr.rotIncr)

            return Pose3d(
                (Matrix3x3.identity + wx * b + (wx * wx) * c) * incr.trIncr,
                Rotation3d.exp(incr.rotIncr)
            )
        }
    }
}

data class Pose3dDual(val translation: Vector3dDual, val rotation: Rotation3dDual) {
    init {
        require(translation.size == rotation.size)
    }

    val size get() = translation.size

    val inverse get() = Pose3dDual(rotation.inverse * -translation, rotation.inverse)
    val value get() = Pose3d(translation.value, rotation.value)
    val velocity get() = Twist3dDual(translation.tail(), rotation.angularVelocity)

    operator fun not() = this.inverse
    operator fun times(b: Pose3dDual) =
        Pose3dDual(this.translation + this.rotation * b.translation, this.rotation * b.rotation)

    operator fun times(v: Vector3dDual) = this.translation + this.rotation * v
    operator fun div(b: Pose3dDual) = b.inverse * this
    operator fun invoke() = rotation().let { (rc0, rc1, rc2) ->
        val `0` = Dual.const(0.0, size)
        val `1` = Dual.const(1.0, size)

        Matrix4x4Dual(
            rc0.x, rc1.x, rc2.x, translation.x,
            rc0.y, rc1.y, rc2.y, translation.y,
            rc0.z, rc1.z, rc2.z, translation.z,
            `0`, `0`, `0`, `1`
        )
    }
}

data class CoordinateSystem(val transform: Matrix3x3) {
    init {
        require(transform.isOrthogonal)
    }

    operator fun rangeTo(b: CoordinateSystem) = b.transform * !this.transform

    companion object {
        val rfu = CoordinateSystem(Matrix3x3.identity)

        val minecraft = CoordinateSystem(
            Matrix3x3(
                Vector3d.unitX,
                Vector3d.unitZ,
                Vector3d.unitY
            )
        )
    }
}

/**
 * Describes the mode of containment of an object inside another object.
 * */
enum class ContainmentMode {
    /**
     * The two objects do not intersect at all.
     * */
    Disjoint,
    /**
     * The two objects intersect, but the right-hand-side object is not contained completely inside the left-hand-side object.
     * */
    Intersected,
    /**
     * The right-hand-side object is contained fully inside the left-hand-side object.
     * */
    ContainedFully
}

/**
 * Generalized (higher dimensionality) cube-like bounding box with some common operations.
 * */
interface BoundingBox<Self : BoundingBox<Self>> {
    /**
     * Checks if the bounding box is valid. This does not imply that its volume is larger than 0.
     * */
    val isValid: Boolean

    val isZero get() = capacity == 0.0

    /**
     * Gets the capacity of the bounding box.
     * For example, a 2D bounding box would supply its surface area. A 3D bounding box would supply its volume.
     * */
    val capacity: Double

    /**
     * Gets the total surface area of the bounding box.
     * For example, a 2D bounding box would supply its surface area. A 3D bounding box would supply the sum of the surface areas of its faces.
     * */
    val surface: Double

    /**
     * Gets the union of this bounding box with another bounding box of the same type.
     * */
    infix fun unionWith(other: Self): Self

    /**
     * Gets the intersection of this bounding box with another box of the same type.
     * The result will be an empty (zero-capacity) bounding box if there is no intersection.
     * */
    infix fun intersectionWith(other: Self): Self

    /**
     * Checks if this bounding box intersects with another bounding box of the same type.
     * */
    infix fun intersectsWith(other: Self): Boolean

    /**
     * Inflates the bounding box along each axis by [percent].
     * */
    fun inflatedBy(percent: Double): Self

    /**
     * Evaluates the containment of the other bounding box in this one.
     * @return The containment mode.
     * [ContainmentMode.Disjoint] means the boxes do not intersect and implicitly testing [intersectsWith] the other box will result in **false**.
     * [ContainmentMode.Intersected] means the boxes intersect but the other box is not contained inside this box.
     * [ContainmentMode.ContainedFully] means the boxes intersect and the other box is contained fully inside this box.
     * */
    fun evaluateContainment(other: Self): ContainmentMode
}

infix fun<T : BoundingBox<T>> BoundingBox<T>.u(other: T) = this unionWith other
infix fun<T : BoundingBox<T>> BoundingBox<T>.n(other: T) = this intersectionWith other

data class BoundingBox2d(val min: Vector2d, val max: Vector2d) : BoundingBox<BoundingBox2d> {
    constructor(minX: Double, minY: Double, width: Double, height: Double) : this(
        Vector2d(minX, minY),
        Vector2d(minX + width, minY + height)
    )

    override val isValid get() = min.x <= max.x && min.y <= max.y
    val center get() = (min + max) / 2.0
    val width get() = max.x - min.x
    val height get() = max.y - min.y
    val size get() = max - min
    override val capacity get() = width * height
    override val surface get() = width * height

    override infix fun intersectionWith(other: BoundingBox2d): BoundingBox2d {
        val result = BoundingBox2d(
            Vector2d.max(this.min, other.min),
            Vector2d.min(this.max, other.max)
        )

        return if (result.isValid) {
            result
        } else {
            zero
        }
    }

    override infix fun intersectsWith(other: BoundingBox2d) =
        other.min.x < this.max.x && this.min.x < other.max.x &&
        other.min.y < this.max.y && this.min.y < other.max.y

    override infix fun unionWith(other: BoundingBox2d) = BoundingBox2d(
        Vector2d.min(this.min, other.min),
        Vector2d.max(this.max, other.max)
    )

    fun inflated(amountX: Double, amountY: Double) = BoundingBox2d(
        Vector2d(this.min.x - amountX, this.min.y - amountY),
        Vector2d(this.max.x + amountX, this.max.y + amountY)
    )

    fun inflated(amount: Vector2d) = inflated(amount.x, amount.y)
    fun inflated(amount: Double) = inflated(amount, amount)
    override fun inflatedBy(percent: Double) = inflated(width * percent, height * percent)

    override fun evaluateContainment(other: BoundingBox2d): ContainmentMode {
        if (!this.intersectsWith(other)) {
            return ContainmentMode.Disjoint
        }

        val test = this.min.x > other.min.x || other.max.x > this.max.x ||
            this.min.y > other.min.y || other.max.y > this.max.y

        return if (test) ContainmentMode.Intersected
        else ContainmentMode.ContainedFully
    }

    fun contains(point: Vector2d) =
        this.min.x < point.x && this.min.y < point.y &&
        this.max.x > point.x && this.max.y > point.y

    companion object {
        val zero = BoundingBox2d(Vector2d.zero, Vector2d.zero)

        fun fromCenterSize(center: Vector2d, size: Vector2d) : BoundingBox2d {
            val halfX = size.x * 0.5
            val halfY = size.y * 0.5

            return BoundingBox2d(
                Vector2d(
                    center.x - halfX,
                    center.y - halfY
                ),
                Vector2d(
                    center.x + halfX,
                    center.y + halfY
                )
            )
        }

        fun fromCenterSize(center: Vector2d, size: Double) : BoundingBox2d {
            val half = size * 0.5

            return BoundingBox2d(
                Vector2d(
                    center.x - half,
                    center.y - half
                ),
                Vector2d(
                    center.x + half,
                    center.y + half
                )
            )
        }
    }
}

/**
 * Represents a 3D Axis-Aligned Bounding Box (AABB).
 * */
data class BoundingBox3d(val min: Vector3d, val max: Vector3d) : BoundingBox<BoundingBox3d> {
    constructor(minX: Double, minY: Double, minZ: Double, width: Double, height: Double, depth: Double) : this(
        Vector3d(minX, minY, minZ),
        Vector3d(minX + width, minY + height, minZ + depth)
    )

    constructor(sphere: BoundingSphere3d) : this(
        sphere.origin - Vector3d(sphere.radius),
        sphere.origin + Vector3d(sphere.radius)
    )

    val isNaN get() = min.isNaN || max.isNaN
    val isInfinity get() = min.isInfinity || max.isInfinity

    /**
     * True if a correct AABB is described (minimum coordinates are smaller than or equal to maximum coordinates) and none of the coordinates are infinity or NaN.
     * */
    override val isValid get() = !isInfinity && min.x <= max.x && min.y <= max.y && min.z <= max.z

    val center get() = (min + max) * 0.5
    val width get() = max.x - min.x
    val height get() = max.y - min.y
    val depth get() = max.z - min.z
    val size get() = max - min // also called extent
    val halfSize get() = Vector3d((max.x - min.x) * 0.5, (max.y - min.y) * 0.5, (max.z - min.z) * 0.5) // also called half extent

    /**
     * Gets the classical volume of the bounding box.
     * */
    override val capacity get() = width * height * depth

    /**
     * Gets the surface area of the bounding box.
     * */
    override val surface get() = 2.0 * (width * height + depth * height + width * depth)

    /**
     * Gets the intersection between this box and the [other] box. The result will be [zero] if there is no intersection.
     * */
    override infix fun intersectionWith(other: BoundingBox3d): BoundingBox3d {
        val result = BoundingBox3d(
            Vector3d.max(this.min, other.min),
            Vector3d.min(this.max, other.max)
        )

        return if (result.isValid) {
            result
        } else {
            zero
        }
    }

    /**
     * Checks if this box intersects with the [other] box.
     * */
    override infix fun intersectsWith(other: BoundingBox3d) =
        other.min.x < this.max.x && this.min.x < other.max.x &&
        other.min.y < this.max.y && this.min.y < other.max.y &&
        other.min.z < this.max.z && this.min.z < other.max.z

    /**
     * Computes the union of this box with the [other box].
     * @return A box that contains this box and the other box.
     * */
    override infix fun unionWith(other: BoundingBox3d) = BoundingBox3d(
        Vector3d.min(min, other.min),
        Vector3d.max(max, other.max)
    )

    fun inflated(amountX: Double, amountY: Double, amountZ: Double) = BoundingBox3d(
        Vector3d(this.min.x - amountX, this.min.y - amountY, this.min.z - amountZ),
        Vector3d(this.max.x + amountX, this.max.y + amountY, this.max.z + amountZ)
    )

    fun inflated(amount: Vector3d) = inflated(amount.x, amount.y, amount.z)
    fun inflated(amount: Double) = inflated(amount, amount, amount)
    override fun inflatedBy(percent: Double) = inflated(width * percent, height * percent, depth * percent)

    /**
     * Evaluates the containment mode of [other] in this box.
     * */
    override fun evaluateContainment(other: BoundingBox3d): ContainmentMode {
        if (!this.intersectsWith(other)) {
            return ContainmentMode.Disjoint
        }

        val test = this.min.x > other.min.x || other.max.x > this.max.x ||
            this.min.y > other.min.y || other.max.y > this.max.y ||
            this.min.z > other.min.z || other.max.z > this.max.z

        return if (test) ContainmentMode.Intersected
        else ContainmentMode.ContainedFully
    }

    /**
     * Checks if this box contains the [point].
     * */
    fun contains(point: Vector3d) =
        min.x < point.x && min.y < point.y && min.z < point.z &&
        max.x > point.x && max.y > point.y && max.z > point.z

    companion object {
        val zero = BoundingBox3d(Vector3d.zero, Vector3d.zero)

        /**
         * Gets a bounding box centered at [center], with its width, height and depth as specified by [size].
         * */
        fun fromCenterSize(center: Vector3d, size: Vector3d) : BoundingBox3d {
            val halfX = size.x * 0.5
            val halfY = size.y * 0.5
            val halfZ = size.z * 0.5

            return BoundingBox3d(
                Vector3d(
                    center.x - halfX,
                    center.y - halfY,
                    center.z - halfZ
                ),
                Vector3d(
                    center.x + halfX,
                    center.y + halfY,
                    center.z + halfZ
                )
            )
        }

        /**
         * Gets a bounding box centered at [center], with its width, height and depth equal to [size] (a cube).
         * */
        fun fromCenterSize(center: Vector3d, size: Double) : BoundingBox3d {
            val half = size * 0.5

            return BoundingBox3d(
                Vector3d(
                    center.x - half,
                    center.y - half,
                    center.z - half
                ),
                Vector3d(
                    center.x + half,
                    center.y + half,
                    center.z + half
                )
            )
        }
    }
}

/**
 * Represents a 3D Oriented Bounding Box (OBB).
 * This is like an AABB, but has rotation (thus is not an axis-aligned box).
 * */
data class OrientedBoundingBox3d(val transform: Pose3d, val halfSize: Vector3d) {
    /**
     * Constructs an OBB from the position of its [center], its [orientation] and [halfExtent].
     * */
    constructor(center: Vector3d, orientation: Rotation3d, halfExtent: Vector3d) : this(Pose3d(center, orientation), halfExtent)

    /**
     * Constructs an OBB from the [transform] and an axis-aligned [boundingBox].
     * *The center of the resulting OBB is a composition of the transform and the [boundingBox]'s center.*
     * */
    constructor(transform: Pose3d, boundingBox: BoundingBox3d) : this(transform * boundingBox.center, transform.rotation, boundingBox.halfSize)

    /**
     * Constructs an OBB equivalent to [boundingBox] (the [transform] consist of [BoundingBox3d.center] and [Rotation3d.identity]).
     * */
    constructor(boundingBox: BoundingBox3d) : this(Pose3d(boundingBox.center, Rotation3d.identity), boundingBox.halfSize)

    val width get() = halfSize.x * 2.0
    val height get() = halfSize.y * 2.0
    val depth get() = halfSize.z * 2.0
    val size get() = halfSize * 2.0

    /**
     * Gets the classical volume of the bounding box.
     * */
    val capacity get() = width * height * depth

    /**
     * Gets the surface area of the bounding box.
     * */
    val surface get() = 2.0 * (width * height + depth * height + width * depth)

    /**
     * Transitions the [point] into the local space of the bounding box.
     * */
    fun transformLocal(point: Vector3d) = transform.rotation.inverse * (point - transform.translation)

    /**
     * Evaluates the mode of containment of the [other] oriented bounding box inside this box.
     * */
    fun evaluateContainment(other: OrientedBoundingBox3d) = evaluateContainmentRelative(this.halfSize, other.halfSize, other.transform / this.transform)

    /**
     * Evaluates the mode of containment of the axis-aligned [box] inside this box.
     * */
    fun evaluateContainment(box: BoundingBox3d) = evaluateContainment(OrientedBoundingBox3d(box))

    /**
     * Evaluates the mode of containment of the [sphere] inside this box.
     * */
    fun evaluateContainment(sphere: BoundingSphere3d) : ContainmentMode {
        val localSphere = transformLocal(sphere.origin)

        var dx = abs(localSphere.x) - halfSize.x
        var dy = abs(localSphere.y) - halfSize.y
        var dz = abs(localSphere.z) - halfSize.z

        // Check for full containment
        val radius = sphere.radius

        if (dx <= -radius && dy <= -radius && dz <= -radius) {
            return ContainmentMode.ContainedFully
        }

        // Compute distance in each dimension
        dx = max(dx, 0.0)
        dy = max(dy, 0.0)
        dz = max(dz, 0.0)

        if (dx * dx + dy * dy + dz * dz >= radius * radius) {
            return ContainmentMode.Disjoint
        }

        return ContainmentMode.Intersected
    }

    /**
     * Checks if [other] intersects this box.
     * */
    infix fun intersects(other: OrientedBoundingBox3d) = evaluateContainment(other) != ContainmentMode.Disjoint

    /**
     * Checks if [box] intersects this box.
     * */
    infix fun intersects(box: BoundingBox3d) = evaluateContainment(box) != ContainmentMode.Disjoint

    /**
     * Checks if [sphere] intersects this box.
     * */
    infix fun intersects(sphere: BoundingSphere3d) = evaluateContainment(sphere) != ContainmentMode.Disjoint

    /**
     * Checks if [other] is contained fully in this box.
     * */
    infix fun contains(other: OrientedBoundingBox3d) = evaluateContainment(other) == ContainmentMode.ContainedFully

    /**
     * Checks if [box] is contained fully in this box.
     * */
    infix fun contains(box: BoundingBox3d) = evaluateContainment(box) == ContainmentMode.ContainedFully

    /**
     * Checks if [sphere] is contained fully in this box.
     * */
    infix fun contains(sphere: BoundingSphere3d) = evaluateContainment(sphere) == ContainmentMode.ContainedFully

    /**
     * Checks if [point] is contained within this box.
     * */
    infix fun contains(point: Vector3d) : Boolean {
        val localPoint = transformLocal(point)

        val dx = abs(localPoint.x)
        val dy = abs(localPoint.y)
        val dz = abs(localPoint.z)

        return dx <= halfSize.x && dy <= halfSize.y && dz <= halfSize.z
    }

    /**
     * Iterates the corners of this box.
     * */
    inline fun corners(consumer: (Vector3d) -> Unit) {
        val rotation = transform.rotation
        val hx = (rotation * Vector3d.unitX) * halfSize.x
        val hy = (rotation * Vector3d.unitY) * halfSize.y
        val hz = (rotation * Vector3d.unitZ) * halfSize.z

        val center = transform.translation
        consumer(center - hx + hy + hz)
        consumer(center + hx + hy + hz)
        consumer(center + hx - hy + hz)
        consumer(center - hx - hy + hz)
        consumer(center - hx + hy - hz)
        consumer(center + hx + hy - hz)
        consumer(center + hx - hy - hz)
        consumer(center - hx - hy - hz)
    }

    fun approxEq(other: OrientedBoundingBox3d, eps: Double = GEOMETRY_COMPARE_EPS) = halfSize.approxEq(other.halfSize, eps) && transform.approxEq(other.transform, eps)

    override fun toString() = "T[${transform.translation}] R[${transform.rotation.ln()}] S[$size]"

    companion object {
        /**
         * Evaluates containment of the box B with half extent [halfB] and transform [transformB] inside the box A with half extent [halfA], axis-aligned and at the origin.
         * */
        fun evaluateContainmentRelative(halfA: Vector3d, halfB: Vector3d, transformB: Pose3d): ContainmentMode {
            val BT = transformB.translation
            val BTa = Vector3d(abs(BT.x), abs(BT.y), abs(BT.z))

            // Transform the extents of B:
            val bx = transformB.rotation * Vector3d.unitX
            val by = transformB.rotation * Vector3d.unitY
            val bz = transformB.rotation * Vector3d.unitZ
            val hxB = bx * halfB.x // x extent of box B
            val hyB = by * halfB.y // y extent of box B
            val hzB = bz * halfB.z // z extent of box B

            // Check for containment first:
            val projXB = abs(hxB.x) + abs(hyB.x) + abs(hzB.x)
            val projYB = abs(hxB.y) + abs(hyB.y) + abs(hzB.y)
            val projZB = abs(hxB.z) + abs(hyB.z) + abs(hzB.z)

            if (BTa.x + projXB <= halfA.x && BTa.y + projYB <= halfA.y && BTa.z + projZB <= halfA.z) {
                return ContainmentMode.ContainedFully
            }

            if (BTa.x > halfA.x + abs(hxB.x) + abs(hyB.x) + abs(hzB.x)) {
                return ContainmentMode.Disjoint
            }

            if (BTa.y > halfA.y + abs(hxB.y) + abs(hyB.y) + abs(hzB.y)) {
                return ContainmentMode.Disjoint
            }

            if (BTa.z > halfA.z + abs(hxB.z) + abs(hyB.z) + abs(hzB.z)) {
                return ContainmentMode.Disjoint
            }

            // Check for separation along the axes box B, hxB/hyB/hzB
            if (abs(BT o bx) > abs(halfA.x * bx.x) + abs(halfA.y * bx.y) + abs(halfA.z * bx.z) + halfB.x) {
                return ContainmentMode.Disjoint
            }

            if (abs(BT o by) > abs(halfA.x * by.x) + abs(halfA.y * by.y) + abs(halfA.z * by.z) + halfB.y){
                return ContainmentMode.Disjoint
            }

            if (abs(BT o bz) > abs(halfA.x * bz.x) + abs(halfA.y * bz.y) + abs(halfA.z * bz.z) + halfB.z) {
                return ContainmentMode.Disjoint
            }

            // a.x ^ b.x = (1,0,0) ^ bX
            var axis = Vector3d(0.0, -bx.z, bx.y)
            if (abs(BT o axis) > abs(halfA.y * axis.y) + abs(halfA.z * axis.z) + abs(axis o hyB) + abs(axis o hzB)) {
                return ContainmentMode.Disjoint
            }

            // a.x ^ b.y = (1,0,0) ^ bY
            axis = Vector3d(0.0, -by.z, by.y)
            if (abs(BT o axis) > abs(halfA.y * axis.y) + abs(halfA.z * axis.z) + abs(axis o hzB) + abs(axis o hxB)) {
                return ContainmentMode.Disjoint
            }

            // a.x ^ b.z = (1,0,0) ^ bZ
            axis = Vector3d(0.0, -bz.z, bz.y)
            if (abs(BT o axis) > abs(halfA.y * axis.y) + abs(halfA.z * axis.z) + abs(axis o hxB) + abs(axis o hyB)) {
                return ContainmentMode.Disjoint
            }

            // a.y ^ b.x = (0,1,0) ^ bX
            axis = Vector3d(bx.z, 0.0, -bx.x)
            if (abs(BT o axis) > abs(halfA.z * axis.z) + abs(halfA.x * axis.x) + abs(axis o hyB) + abs(axis o hzB)) {
                return ContainmentMode.Disjoint
            }

            // a.y ^ b.y = (0,1,0) ^ bY
            axis = Vector3d(by.z, 0.0, -by.x)
            if (abs((BT o axis)) > abs(halfA.z * axis.z) + abs(halfA.x * axis.x) + abs(axis o hzB) + abs(axis o hxB)) {
                return ContainmentMode.Disjoint
            }

            // a.y ^ b.z = (0,1,0) ^ bZ
            axis = Vector3d(bz.z, 0.0, -bz.x)
            if (abs(BT o axis) > abs(halfA.z * axis.z) + abs(halfA.x * axis.x) + abs(axis o hxB) + abs(axis o hyB)) {
                return ContainmentMode.Disjoint
            }

            // a.z ^ b.x = (0,0,1) ^ bX
            axis = Vector3d(-bx.y, bx.x, 0.0)
            if (abs(BT o axis) > abs(halfA.x * axis.x) + abs(halfA.y * axis.y) + abs(axis o hyB) + abs(axis o hzB)) {
                return ContainmentMode.Disjoint
            }

            // a.z ^ b.y = (0,0,1) ^ bY
            axis = Vector3d(-by.y, by.x, 0.0)
            if (abs(BT o axis) > abs(halfA.x * axis.x) + abs(halfA.y * axis.y) + abs(axis o hzB) + abs(axis o hxB)) {
                return ContainmentMode.Disjoint
            }

            // a.z ^ b.z = (0,0,1) ^ bZ
            axis = Vector3d(-bz.y, bz.x, 0.0)
            if (abs(BT o axis) > abs(halfA.x * axis.x) + abs(halfA.y * axis.y) + abs(axis o hxB) + abs(axis o hyB)) {
                return ContainmentMode.Disjoint
            }

            return ContainmentMode.Intersected
        }
    }
}

/**
 * Describes an intersection between a ray and a volume.
 * [entry] and [exit] are the arguments to the ray equation that will yield the two points of intersection.
 * */
data class RayIntersection(val entry: Double, val exit: Double)

/**
 * Represents a line segment bounded by the [origin] and with the specified [direction].
 * */
data class Ray3d(val origin: Vector3d, val direction: Vector3d) {
    /**
     * True, if the [origin] is not infinite or NaN, and the [direction] is a ~unit vector. Otherwise, false.
     * */
    val isValid get() = !origin.isNaN && !origin.isInfinity && direction.isUnit

    /**
     * Evaluates the parametric equation of the ray to get the point in space.
     * */
    fun evaluate(t: Double) = origin + direction * t

    /**
     * Evaluates the intersection with the [box].
     * @return A [RayIntersection], if an intersection exists. Otherwise, null.
     * */
    infix fun intersectionWith(box: BoundingBox3d): RayIntersection? {
        var tMin = Double.NEGATIVE_INFINITY
        var tMax = Double.POSITIVE_INFINITY

        if (this.direction.x != 0.0) {
            val tx1 = (box.min.x - this.origin.x) / this.direction.x
            val tx2 = (box.max.x - this.origin.x) / this.direction.x
            tMin = max(tMin, min(tx1, tx2))
            tMax = min(tMax, max(tx1, tx2))
        }

        if (this.direction.y != 0.0) {
            val ty1 = (box.min.y - this.origin.y) / this.direction.y
            val ty2 = (box.max.y - this.origin.y) / this.direction.y
            tMin = max(tMin, min(ty1, ty2))
            tMax = min(tMax, max(ty1, ty2))
        }

        if (this.direction.z != 0.0) {
            val tz1 = (box.min.z - this.origin.z) / this.direction.z
            val tz2 = (box.max.z - this.origin.z) / this.direction.z
            tMin = max(tMin, min(tz1, tz2))
            tMax = min(tMax, max(tz1, tz2))
        }

        return if (tMax >= tMin) {
            RayIntersection(tMin, tMax)
        }
        else {
            null
        }
    }

    /**
     * Gets the intersection point of the ray with the [plane].
     * @return The point of intersection, which lies in the plane and along the normal line of the ray.
     * You will get NaNs and infinities if no intersection occurs; consider [Vector3d.isNaN] and [Vector3d.isInfinity] before using the results.
     * */
    infix fun intersectionWith(plane: Plane3d) = this.origin + this.direction * -((plane.normal o this.origin) + plane.d) / (plane.normal o this.direction)

    companion object {
        /**
         * Calculates a ray from a starting point [source] and a point [destination], that the ray shall pass through.
         * You will get NaNs and infinities if the source and destination are NaNs, infinities, or they are ~close to each other.
         * */
        fun fromSourceAndDestination(source: Vector3d, destination: Vector3d) = Ray3d(source, source directionTo destination)
    }
}

/**
 * Represents a 3D bounding sphere.
 * */
data class BoundingSphere3d(val origin: Vector3d, val radius: Double) {
    val radiusSqr get() = radius * radius

    /**
     * Constructs a [BoundingSphere3d] from the [box].
     * */
    constructor(box: BoundingBox3d) : this(box.center, box.center..box.max)

    /**
     * Computes the union of this sphere and the [other] sphere.
     * @return A sphere that contains both this sphere and the [other] sphere.
     * */
    infix fun unionWith(other: BoundingSphere3d): BoundingSphere3d {
        val dxAB = other.origin - this.origin
        val distance = dxAB.norm

        if (radius + other.radius >= distance) {
            if (radius - other.radius >= distance) {
                return this
            } else if (other.radius - radius >= distance) {
                return other
            }
        }

        val direction = dxAB / distance
        val a = min(-radius, distance - other.radius)
        val r = (max(radius, distance + other.radius) - a) * 0.5

        return BoundingSphere3d(this.origin + direction * (r + a), r)
    }

    /**
     * Checks if the [point] is inside the sphere.
     * */
    fun contains(point: Vector3d) = (origin distanceToSqr point) < radiusSqr

    override fun toString() = "$origin r=$radius"
}

infix fun BoundingSphere3d.u(other: BoundingSphere3d) = this unionWith other

/**
 * Describes the mode of intersection between a plane and another object.
 * */
enum class PlaneIntersectionType {
    /**
     * The plane and object do not intersect, and the object is in the positive half-space.
     * */
    Positive,
    /**
     * The plane and object do not intersect, and the object is in the negative half-space.
     * */
    Negative,
    /**
     * The plane and object intersect.
     * */
    Intersects
}

/**
 * Represents a 3D Plane.
 * @param normal The normal of the plane.
 * @param d The distance from the origin along the normal to the plane.
 * *The distance is signed and is not what you would expect:*
 *
 * Plane Equation:
 * (n **o** v) + d = 0 -> d = -(n **o** v)
 * */
data class Plane3d(val normal: Vector3d, val d: Double) {
    /**
     * True, if the [normal] is a ~unit vector, and the distance [d] is not infinite or NaN. Otherwise, false.
     * */
    val isValid get() = normal.isUnit && !d.isInfinite() && !d.isNaN()

    /**
     * Constructs a [Plane3d] from the normal vector and distance from origin.
     * @param x The X component of the normal.
     * @param y The Y component of the normal.
     * @param z The Z component of the normal.
     * @param w The distance from the origin.
     * */
    constructor(x: Double, y: Double, z: Double, w: Double) : this(Vector3d(x, y, z), w)

    /**
     * Constructs a [Plane3d] from the normal vector and a point in the plane.
     * @param normal The normal of the plane.
     * @param point A point that lies within the plane.
     * */
    constructor(normal: Vector3d, point: Vector3d) : this(normal, -(point o normal))

    /**
     * Calculates the distance between the plane and the [point].
     * If the point is in the plane, the result is 0.
     * The distance is signed; if the point is above the plane, it will be positive. If below the plane, it will be negative.
     * */
    fun signedDistanceToPoint(point: Vector3d) = (normal o point) + d

    /**
     * Calculates the distance between the plane and the [point].
     * If the point is in the plane, the result is 0.
     * */
    fun distanceToPoint(point: Vector3d) = abs(signedDistanceToPoint(point))

    /**
     * Evaluates the mode of intersection with the [point].
     * Here, [PlaneIntersectionType.Intersects] means that the plane ~contains the point.
     * @param eps The tolerance. If the distance to the point is below this tolerance, it is considered that the plane contains the point.
     * */
    fun evaluateIntersection(point: Vector3d, eps: Double = GEOMETRY_COMPARE_EPS) : PlaneIntersectionType {
        val distance = signedDistanceToPoint(point)

        if(distance > eps) {
            return PlaneIntersectionType.Positive
        }

        if(distance < -eps) {
            return PlaneIntersectionType.Negative
        }

        return PlaneIntersectionType.Intersects
    }

    /**
     * Checks if this plane contains the [point].
     * */
    infix fun contains(point: Vector3d) = evaluateIntersection(point) == PlaneIntersectionType.Intersects

    /**
     * Evaluates the mode of intersection with the [box].
     * */
    fun evaluateIntersection(box: BoundingBox3d) : PlaneIntersectionType {
        val nx = normal.x
        val ny = normal.y
        val nz = normal.z

        val x1 = if (nx >= 0.0) box.min.x else box.max.x
        val y1 = if (ny >= 0.0) box.min.y else box.max.y
        val z1 = if (nz >= 0.0) box.min.z else box.max.z
        val x2 = if (nx >= 0.0) box.max.x else box.min.x
        val y2 = if (ny >= 0.0) box.max.y else box.min.y
        val z2 = if (nz >= 0.0) box.max.z else box.min.z

        if (nx * x1 + ny * y1 + nz * z1 + d > 0.0) {
            return PlaneIntersectionType.Positive
        }

        if (nx * x2 + ny * y2 + nz * z2 + d < 0.0) {
            return PlaneIntersectionType.Negative
        }

        return PlaneIntersectionType.Intersects
    }

    /**
     * Evaluates the mode of intersection with the [sphere].
     * */
    fun evaluateIntersection(sphere: BoundingSphere3d) = evaluateIntersection(sphere.origin, sphere.radius)

    /**
     * Checks if the plane intersects with the [box].
     * @return True, if the plane cuts the box. Otherwise, false.
     * */
    infix fun intersectsWith(box: BoundingBox3d) = evaluateIntersection(box) == PlaneIntersectionType.Intersects

    /**
     * Checks if the plane intersects with the [sphere].
     * @return True, if the plane cuts the sphere. Otherwise, false.
     * */
    infix fun intersectsWith(sphere: BoundingSphere3d) = evaluateIntersection(sphere) == PlaneIntersectionType.Intersects

    fun approxEq(other: Plane3d, eps: Double = GEOMETRY_COMPARE_EPS) = normal.approxEq(other.normal, eps) && d.approxEq(other.d, eps)

    override fun toString() = "$normal, d=$d"

    companion object {
        val unitX = Plane3d(Vector3d.unitX, 0.0)
        val unitY = Plane3d(Vector3d.unitY, 0.0)
        val unitZ = Plane3d(Vector3d.unitZ, 0.0)

        /**
         * Creates a plane that contains the specified points. The returned plane is valid if the points form a non-degenerate triangle.
         * */
        fun createFromVertices(a: Vector3d, b: Vector3d, c: Vector3d) : Plane3d {
            val ax = b.x - a.x
            val ay = b.y - a.y
            val az = b.z - a.z

            val bx = c.x - a.x
            val by = c.y - a.y
            val bz = c.z - a.z

            val nx = ay * bz - az * by
            val ny = az * bx - ax * bz
            val nz = ax * by - ay * bx

            val n = 1.0 / sqrt(nx * nx + ny * ny + nz * nz)

            val normal = Vector3d(nx * n, ny * n, nz * n)
            val d = -(normal.x * a.x + normal.y * a.y + normal.z * a.z)

            return Plane3d(normal, d)
        }
    }
}

inline fun dda(ray: Ray3d, withSource: Boolean = true, user: (Int, Int, Int) -> Boolean) {
    var x = floor(ray.origin.x).toInt()
    var y = floor(ray.origin.y).toInt()
    var z = floor(ray.origin.z).toInt()

    if (withSource) {
        if (!user(x, y, z)) {
            return
        }
    }

    val sx = snzi(ray.direction.x)
    val sy = snzi(ray.direction.y)
    val sz = snzi(ray.direction.z)

    val nextBX = x + sx + if (sx.sign == -1) 1
    else 0
    val nextBY = y + sy + if (sy.sign == -1) 1
    else 0
    val nextBZ = z + sz + if (sz.sign == -1) 1
    else 0

    var tmx = if (ray.direction.x != 0.0) (nextBX - ray.origin.x) / ray.direction.x else Double.MAX_VALUE
    var tmy = if (ray.direction.y != 0.0) (nextBY - ray.origin.y) / ray.direction.y else Double.MAX_VALUE
    var tmz = if (ray.direction.z != 0.0) (nextBZ - ray.origin.z) / ray.direction.z else Double.MAX_VALUE
    val tdx = if (ray.direction.x != 0.0) 1.0 / ray.direction.x * sx else Double.MAX_VALUE
    val tdy = if (ray.direction.y != 0.0) 1.0 / ray.direction.y * sy else Double.MAX_VALUE
    val tdz = if (ray.direction.z != 0.0) 1.0 / ray.direction.z * sz else Double.MAX_VALUE

    while (true) {
        if (tmx < tmy) {
            if (tmx < tmz) {
                x += sx
                tmx += tdx
            } else {
                z += sz
                tmz += tdz
            }
        } else {
            if (tmy < tmz) {
                y += sy
                tmy += tdy
            } else {
                z += sz
                tmz += tdz
            }
        }

        if (!user(x, y, z)) {
            return
        }
    }
}