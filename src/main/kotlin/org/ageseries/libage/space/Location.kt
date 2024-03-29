package org.ageseries.libage.space

import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * A vector in a two-dimensional space of integers.
 */
data class Vec2i(val x: Int, val y: Int) {
    /**
     * Gets the opposite vector.
     */
    operator fun unaryMinus() = Vec2i(-x, -y)

    /**
     * Gets the sum of this vector and another vector.
     */
    operator fun plus(other: Vec2i) = Vec2i(x + other.x, y + other.y)

    /**
     * Gets the sum of this vector and the opposite of another vector.
     */
    operator fun minus(other: Vec2i) = this + (-other)

    /**
     * Gets the scalar multiple of this vector.
     */
    operator fun times(scalar: Int) = Vec2i(x * scalar, y * scalar)

    val isZero: Boolean get() = this == ZERO

    companion object {
        val ZERO = Vec2i(0, 0)
        val XU = Vec2i(1, 0)
        val YU = Vec2i(0, 1)
    }
}

/**
 * A vector in a three-dimensional space of integers.
 */
data class Vec3i(val x: Int, val y: Int, val z: Int) {
    /**
     * Gets the opposite vector.
     */
    operator fun unaryMinus() = Vec3i(-x, -y, -z)

    /**
     * Gets the sum of this vector and another vector.
     */
    operator fun plus(other: Vec3i) = Vec3i(x + other.x, y + other.y, z + other.z)

    /**
     * Gets the sum of this vector and the opposite of another vector.
     */
    operator fun minus(other: Vec3i) = this + (-other)

    /**
     * Gets the scalar multiple of this vector.
     */
    operator fun times(other: Int) = Vec3i(x * other, y * other, z * other)

    val isZero: Boolean get() = (x == 0) && (y == 0) && (z == 0)

    /**
     * Calculates the L1 distance between two vectors.
     * See https://en.wikipedia.org/wiki/Taxicab_geometry
     */
    fun l1norm(v: Vec3i): Int = abs(v.x - x) + abs(v.y - y) + abs(v.z - z)

    val vec3f: Vec3f get() = Vec3f(x.toDouble(), y.toDouble(), z.toDouble())

    companion object {
        val ZERO = Vec3i(0, 0, 0)
        val XU = Vec3i(1, 0, 0)
        val YU = Vec3i(0, 1, 0)
        val ZU = Vec3i(0, 0, 1)
    }
}

/**
 * A vector in a three-dimensional real space.
 */
data class Vec3f(val x: Double, val y: Double, val z: Double) {
    operator fun unaryMinus() = Vec3f(-x, -y, -z)
    operator fun plus(other: Vec3f) = Vec3f(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3f) = this + (-other)
    fun recip() = Vec3f(1.0/x, 1.0/y, 1.0/z)
    operator fun times(other: Vec3f) = Vec3f(x * other.x, y * other.y, z * other.z)
    operator fun div(other: Vec3f) = this * other.recip()

    operator fun plus(other: Double) = this + diag(other)
    operator fun minus(other: Double) = this - diag(other)
    operator fun times(other: Double) = this * diag(other)
    operator fun div(other: Double) = this / diag(other)

    val isZero: Boolean
        get() = x == 0.0 && y == 0.0 && z == 0.0

    fun dot(other: Vec3f) = x * other.x + y * other.y + z * other.z
    val magSquared: Double get() = dot(this)
    val mag: Double get() = sqrt(magSquared)

    val normalized: Vec3f
        get() = if(isZero) { ZERO } else { this / mag }

    fun cross(other: Vec3f) = Vec3f(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    // TODO: figure out this rounding mode and document it accordingly
    // (Until then, don't rely on this!)
    val vec3i: Vec3i get() = Vec3i(x.toInt(), y.toInt(), z.toInt())

    companion object {
        fun diag(v: Double) = Vec3f(v, v, v)

        val ZERO = Vec3f(0.0, 0.0, 0.0)
        val XU = Vec3f(1.0, 0.0, 0.0)
        val YU = Vec3f(0.0, 1.0, 0.0)
        val ZU = Vec3f(0.0, 0.0, 1.0)
    }
}

/**
 * A direction oriented in two-dimensional space.
 */
enum class PlanarDir(val int: Int) {
    Up(0), Right(1), Down(2), Left(3);

    companion object {
        fun fromInt(i: Int) = when (i) {
            0 -> Up
            1 -> Right
            2 -> Down
            3 -> Left
            else -> error("Not a PlanarDir: $i")
        }
    }

    val rotated: PlanarDir get() = fromInt((int + 1) % 4)
    val inverted: PlanarDir get() = fromInt((int + 2) % 4)
    val rotated_left: PlanarDir get() = fromInt((int + 3) % 4)
}

/**
 * A set of unit vectors that represents each direction in three-dimensional space.
 */
enum class Axis(val int: Int) {
    X(0), Y(1), Z(2);

    companion object {
        fun fromInt(i: Int) = when (i) {
            0 -> X
            1 -> Y
            2 -> Z
            else -> null
        }

        /**
         * Returns the axis in which a given vector is closest to. Perfect diagonals may return an axis that is nonsense.
         */
        fun fromVecMajor(v: Vec3i): Axis {
            var (x, y, z) = v
            x = abs(x); y = abs(y); z = abs(z)
            val max = arrayOf(x, y, z).maxOrNull()
            return when (max) {
                x -> X
                y -> Y
                else -> Z
            }
        }

        /* Microoptimization: avoid the overhead from constructing these repeatedly */
        val X_VEC = Vec3i.XU
        val Y_VEC = Vec3i.YU
        val Z_VEC = Vec3i.ZU
    }

    val vec3i: Vec3i
        get() = when (this) {
            X -> X_VEC
            Y -> Y_VEC
            Z -> Z_VEC
        }

    /**
     * Returns the cross product of this axis and another.
     */
    fun cross(other: Axis): Axis? = when (this) {
        X -> when (other) {
            X -> null
            Y -> Z
            Z -> Y
        }
        Y -> when (other) {
            X -> Z
            Y -> null
            Z -> X
        }
        Z -> when (other) {
            X -> Y
            Y -> X
            Z -> null
        }
    }
}

/**
 * The six orthogonal vectors corresponding to the faces of a cube as seen from the center.
 */
enum class PlanarFace(val int: Int) {
    PosX(0), PosY(1), PosZ(2), NegX(3), NegY(4), NegZ(5);

    companion object {
        /**
         * Gets the face corresponding to a number.
         */
        fun fromInt(i: Int) = when (i) {
            0 -> PosX
            1 -> PosY
            2 -> PosZ
            3 -> NegX
            4 -> NegY
            5 -> NegZ
            else -> error("Invalid PlanarFace: $i")
        }

        /**
         * Gets the plane that the axis is pointing to from the center.
         */
        fun fromAxis(a: Axis, n: Boolean): PlanarFace = fromInt(a.int + if (n) 3 else 0)

        /**
         * Gets the plane that the vector is pointing to in respect to the center of the cube.
         */
        fun fromVec(v: Vec3i): PlanarFace {
            val axis = Axis.fromVecMajor(v)
            return fromAxis(
                axis, when (axis) {
                    Axis.X -> v.x < 0
                    Axis.Y -> v.y < 0
                    Axis.Z -> v.z < 0
                }
            )
        }

        /**
         * Gets the plane that has the closest normal vector to the given vector.
         */
        fun fromNormal(v: Vec3i): PlanarFace = fromVec(v).inverse

        /* See the microopt in Axis above */
        val PosX_VEC = Axis.X_VEC
        val PosY_VEC = Axis.Y_VEC
        val PosZ_VEC = Axis.Z_VEC
        val NegX_VEC = -PosX_VEC
        val NegY_VEC = -PosY_VEC
        val NegZ_VEC = -PosZ_VEC

        val ADJACENCIES = arrayOf(
            arrayOf(PosY, PosZ, NegY, NegZ),
            arrayOf(PosX, PosZ, NegX, NegZ),
            arrayOf(PosX, PosY, NegX, NegY)
        )
    }

    val neg: Boolean get() = int > 2
    val axis: Axis
        get() = when (this) {
            PosX, NegX -> Axis.X
            PosY, NegY -> Axis.Y
            PosZ, NegZ -> Axis.Z
        }
    val inverse: PlanarFace
        get() = when (this) {
            PosX -> NegX
            NegX -> PosX
            PosY -> NegY
            NegY -> PosY
            PosZ -> NegZ
            NegZ -> PosZ
        }

    /**
    "Vector": the vec that points out of the block center toward this face.
     */
    val vec3i: Vec3i
        get() = if (neg) when (axis) {
            Axis.X -> NegX_VEC
            Axis.Y -> NegY_VEC
            Axis.Z -> NegZ_VEC
        } else axis.vec3i

    /**
     * "Normal": the vec that points, normal to this face, toward the block center.
     */
    val normal: Vec3i
        get() = if (neg) axis.vec3i else when (axis) {
            Axis.X -> NegX_VEC
            Axis.Y -> NegY_VEC
            Axis.Z -> NegZ_VEC
        }

    val adjacencies: Array<PlanarFace> get() = ADJACENCIES[int % 3]
}

/**
 * A generic location in a generic space. Also contains a method to check if something can connect with this and list
 * of locatable objects that can connect with this.
 */
interface Locator {
    val vec3i: Vec3i

    /**
     * A list of neighbors in which connections are possible with.
     */
    fun neighbors(): List<Locator>

    /**
     * Returns true if it is possible for this node and another node to connect.
     * When overriding this, make sure a specific test is implemented for the specific
     * locator that extended this interface. Ensure that the coverage of the test is
     * proportional to the size of the implementation.
     */
    fun canConnect(other: Locator): Boolean = true
}

/**
 *  Locator support for "simple nodes" that take up an entire block in three-dimensional space.
 */
open class BlockPos(override val vec3i: Vec3i) : Locator {
    companion object {
        val CONNECTIVITY_DELTAS = PlanarFace.values().map { it.vec3i }
    }

    override fun toString() = "BlockPos($vec3i)"

    override fun neighbors(): List<Locator> = CONNECTIVITY_DELTAS.map { translated(it) }

    /**
     * Offsets a vector based on the position of this node.
     */
    fun translated(v: Vec3i) = BlockPos(vec3i + v)
}

/**
 *  Locator support for surface-mounted nodes the exist on a three-dimensional block. Up to six can exist per block,
 *  corresponding to each face.
 */
open class SurfacePos(override val vec3i: Vec3i, val face: PlanarFace) : Locator {
    companion object {
        // On the same plane:
        val PLANAR_DELTAS = arrayListOf(
            Vec3i(1, 0, 0), Vec3i(-1, 0, 0), Vec3i(0, 0, 1), Vec3i(0, 0, -1)
        )

        // On adjacent planes:
        val ADJACENT_DELTAS = arrayListOf(
            // SurfacePos locators can connect to adjacent planes on the same block:
            Vec3i(0, 0, 0),
            // One unit down (anti-normal) in cardinal directions ("wrapping around")
            Vec3i(1, -1, 0), Vec3i(-1, -1, 0), Vec3i(0, -1, 1), Vec3i(0, -1, -1)
        )
    }

    override fun toString() = "SurfacePos($vec3i, $face)"

    // Preserve chirality: invert _two_ components, or none.
    // There's very little thought in the permutation otherwise, however; if those need to be changed, they can be.
    /**
     * Orients a vector based on which plane of a cube this node is on. This preserves chirality.
     */
    fun toGlobal(v: Vec3i): Vec3i = when (face) {
        PlanarFace.NegX -> Vec3i(v.y, v.x, v.z)
        PlanarFace.PosX -> Vec3i(-v.y, -v.x, v.z)
        PlanarFace.PosY -> Vec3i(-v.x, -v.y, v.z)
        PlanarFace.NegY -> v
        PlanarFace.NegZ -> Vec3i(v.x, v.z, v.y)
        PlanarFace.PosZ -> Vec3i(-v.x, v.z, -v.y)
    }

    /**
     * Offsets a vector based on the position and orientation of this node.
     */
    fun translated(v: Vec3i) = SurfacePos(vec3i + toGlobal(v), face)

    override fun neighbors(): List<Locator> = (PLANAR_DELTAS.map { translated(it) } + // Other adjacent blocks on the same plane
        face.adjacencies.map { SurfacePos(vec3i, it) } + // Connections within the same block
        face.adjacencies.map { SurfacePos(vec3i + face.vec3i + it.normal, it) } // "Wrapping" (L1=2) connections
        )

    override fun canConnect(other: Locator): Boolean = when (other) {
        is SurfacePos -> when (other.vec3i.l1norm(vec3i)) {
            0 -> other.face != face && other.face != face.inverse
            1 -> face == other.face
            2 -> {
                val delta = other.vec3i - vec3i
                val otherNorm = delta + face.normal
                println(
                    "SP.cC: L1=2: delta $delta other_norm $otherNorm face.normal ${face.normal} this.vec $vec3i other.vec ${other.vec3i} other.face ${other.face} PF.fN(on) ${
                        PlanarFace.fromNormal(
                            otherNorm
                        )
                    }"
                )
                other.face == PlanarFace.fromNormal(otherNorm)
            }
            else -> error("Illegal norm")
        }
        is BlockPos -> true
        else -> true
    }
}

object GPUtil {
    fun gp(v: Vec3i): String = "${v.x},${v.z},${v.y}"
    fun gp(v: Vec3f): String = "${v.x},${v.z},${v.y}"

    data class FaceInfo(val offset: Vec3f, val ax1: Axis, val ax2: Axis)
    fun faceInfo(face: PlanarFace): FaceInfo {
        val axes = face.adjacencies.map { it.axis }
        // Conjecture: there are two axes
        val ax1 = axes[0]
        val ax2 = axes[1]
        var off = ax1.cross(ax2)!!.vec3i.vec3f
        if(face.neg) off *= -1.0
        return FaceInfo(off, ax1, ax2)
    }

    val signs = arrayOf(
        Pair(-1.0, -1.0),
        Pair(1.0, -1.0),
        Pair(1.0, 1.0),
        Pair(-1.0, 1.0),
    )
    fun plotFace(face: PlanarFace, origin: Vec3f = Vec3f.ZERO, style: String = "") {
        val (off, ax1, ax2) = faceInfo(face)
        val pts = mutableListOf<Vec3f>()
        for ((sgn1, sgn2) in signs) {
            val pt = origin + off + ax1.vec3i.vec3f * sgn1 + ax2.vec3i.vec3f * sgn2
            pts.add(pt)
        }
        for ((idx, pt) in pts.withIndex()) {
            val next = pts[(idx + 1) % pts.size]
            println("set arrow from ${PlotSurfacePosRotations.gp(pt)} to ${PlotSurfacePosRotations.gp(next)} nohead $style")
        }
    }

    fun plotCube(origin: Vec3f = Vec3f.ZERO, style: String = "") {
        for(face in PlanarFace.values()) {
            plotFace(face, origin, style)
        }
    }

    val axisColors = arrayOf("#ff0000", "#00ff00", "#0000ff")
    fun plotRose(origin: Vec3f = Vec3f.ZERO, scale: Double = 0.2, style: String = "") {
        for(axis in Axis.values()) {
            println("set arrow from ${gp(origin)} to ${gp(origin + axis.vec3i.vec3f * scale)} lc \"${axisColors[axis.int]}\" $style")
        }
    }

    fun locatorSpace(pos: Vec3f) = pos * 2.0
    fun locatorSpace(pos: Vec3i) = locatorSpace(pos.vec3f)

    fun plotLocator(loc: Locator, style: String = "") {
        when(loc) {
            is BlockPos -> plotLocator(loc, style)
            is SurfacePos -> plotLocator(loc, style)
        }
    }
    fun plotLocator(loc: BlockPos, style: String = "") {
        plotCube(locatorSpace(loc.vec3i), style)
    }
    fun plotLocator(loc: SurfacePos, style: String = "") {
        plotFace(loc.face, locatorSpace(loc.vec3i), style)
    }
}

class PlotSurfacePosRotations {
    companion object {
        val gp: (Vec3f) -> String = GPUtil::gp
        val colors = GPUtil.axisColors

        @JvmStatic
        fun main(args: Array<String>) {
            val units = with(Vec3i) { arrayOf(XU, YU, ZU) }
            val faces = PlanarFace.values()
            for(axis in arrayOf("x", "y", "z")) {
                println("set ${axis}range [-1:1]")
            }

            for(face in faces) {
                println("# Face: $face")
                val off = GPUtil.faceInfo(face).offset
                GPUtil.plotFace(face, style = "lc \"#007700\"")
                println("set label \"$face\" at ${gp(off)} tc \"${colors[face.axis.int]}\"")

                val sp = SurfacePos(Vec3i.ZERO, face)
                for((idx, unit) in units.withIndex()) {
                    val color = colors[idx]
                    val mapped = sp.toGlobal(unit).vec3f / 5.0
                    println("set arrow from ${gp(off)} to ${gp(off + mapped)} lc \"$color\"")
                }
                println("")
            }
        }
    }
}

class PlotBlockPosNeighbors {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val bp = BlockPos(Vec3i.ZERO)
            for(neighbor in bp.neighbors()) {
                GPUtil.plotLocator(neighbor, "lc \"#007700\"")
                GPUtil.plotRose(GPUtil.locatorSpace(neighbor.vec3i))
            }
            GPUtil.plotLocator(bp, "lc \"#ff0000\"")
            GPUtil.plotRose()
        }
    }
}