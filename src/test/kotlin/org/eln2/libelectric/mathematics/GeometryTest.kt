@file:Suppress("LocalVariableName", "NonAsciiCharacters")

package org.eln2.libelectric.mathematics

import org.ageseries.libage.mathematics.*
import org.ageseries.libage.mathematics.geometry.*
import org.eln2.libelectric.TestUtils.areEqual
import org.eln2.libelectric.TestUtils.rangeScan
import org.eln2.libelectric.TestUtils.rangeScanKd
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

internal class GeometryTest {
    @Test
    fun vector2dTest() {
        assertTrue(Vector2d.zero == Vector2d(0.0, 0.0) && Vector2d.zero == Vector2d(0.0))
        assertTrue(Vector2d.one == Vector2d(1.0, 1.0) && Vector2d.one == Vector2d(1.0))
        assertTrue(Vector2d.unitX == Vector2d(1.0, 0.0))
        assertTrue(Vector2d.unitY == Vector2d(0.0, 1.0))
        assertTrue(Vector2d.unitX + Vector2d.unitY == Vector2d.one)
        assertTrue(Vector2d.zero != Vector2d.one)
        assertTrue(Vector2d.zero.norm == 0.0 && Vector2d.zero.normSqr == 0.0)
        assertTrue(Vector2d.one.norm == sqrt(2.0) && Vector2d.one.normSqr == 2.0)
        assertTrue(Vector2d.unitX.norm == 1.0 && Vector2d.unitX.normSqr == 1.0)
        assertTrue(Vector2d.unitY.norm == 1.0 && Vector2d.unitY.normSqr == 1.0)
        assertTrue((Vector2d.one * 0.5 + Vector2d.one / 2.0) == Vector2d.one)
        assertTrue(Vector2d.one == Vector2d.one * 2.0 - Vector2d.one)
        assertTrue(Vector2d(1000.0, 1000.0).normalized().norm.approxEq(1.0))
        assertTrue(Vector2d(1000.0, 1000.0).normalized() * sqrt(1000.0 * 1000.0 * 2) == Vector2d(1000.0, 1000.0))
        assertTrue(Vector2d.lerp(Vector2d.zero, Vector2d(1.0, 2.0), 0.0) == Vector2d.zero)
        assertTrue(Vector2d.lerp(Vector2d.zero, Vector2d(1.0, 2.0), 0.5) == Vector2d(1.0, 2.0) / 2.0)
        assertTrue(Vector2d.lerp(Vector2d.zero, Vector2d(1.0, 2.0), 1.0) == Vector2d(1.0, 2.0))
        assertEquals(Vector2d.unitX o Vector2d.unitY, 0.0)
        assertTrue((Vector2d.one.normalized() o Vector2d.one.normalized()).approxEq(1.0))
        assertTrue(Vector2d.one * 0.9 < Vector2d.one)
        assertTrue(Vector2d.one * 1.1 > Vector2d.one)
    }

    @Test
    fun rotation2dTest() {
        fun areEqual(vararg values: Rotation2d) {
            require(values.size > 1)

            for (i in 1 until values.size) {
                assertTrue(values[i - 1].approxEq(values[i]))
            }
        }

        val rpi = Rotation2d.exp(PI)

        assertEquals(rpi, rpi)
        assertEquals(rpi.scaled(1.0), rpi)
        assertEquals(rpi.scaled(-1.0), rpi.inverse)
        assertEquals(rpi.scaled(0.5), Rotation2d.exp(PI / 2.0))

        areEqual(rpi * rpi, Rotation2d.exp(PI * 2.0), Rotation2d.exp(PI * 4.0), Rotation2d.identity)
        areEqual(rpi * Rotation2d.exp(-PI), Rotation2d.identity)
        areEqual(rpi * rpi.inverse, Rotation2d.identity)

        assertTrue((rpi * Vector2d.unitX).approxEq(-Vector2d.unitX))
        assertTrue((Rotation2d.exp(PI * 2.0) * Vector2d.unitX).approxEq(Vector2d.unitX))
        assertTrue((Rotation2d.exp(PI * 8.0) * Vector2d.unitX).approxEq(Vector2d.unitX))

        areEqual(Rotation2d.interpolate(Rotation2d.identity, rpi, 0.0), Rotation2d.identity)
        areEqual(Rotation2d.interpolate(Rotation2d.identity, rpi, 1.0), rpi)
        areEqual(Rotation2d.interpolate(Rotation2d.identity, rpi, 0.5), Rotation2d.exp(PI / 2.0))
        areEqual(Rotation2d.interpolate(Rotation2d.identity, rpi, 0.25), Rotation2d.exp(PI / 4.0))

        rangeScan(start = 0.0, end = 1.0) { t ->
            areEqual(Rotation2d.interpolate(rpi, rpi, t), rpi)
        }

        rangeScan {
            assertEquals(
                Rotation2d.exp(it).direction,
                Vector2d(Rotation2d.exp(it).re, Rotation2d.exp(it).im)
            )
        }

        rangeScanKd(start = -100.0, end = 100.0, steps = 1000, layers = 2) { vec ->
            val a = Rotation2d.exp(vec[0])
            val b = Rotation2d.exp(vec[1])

            areEqual(a * (b / a), b)
            areEqual(a * Rotation2d.exp(b - a), b)
            areEqual(a + (b - a), b)
        }
    }

    @Test
    fun pose2dTest() {
        assertEquals(Pose2d.identity, Pose2d.exp(Twist2dIncr(Vector2d.zero, 0.0)))

        rangeScan(-10.0, 10.0, steps = 100) { rotation ->
            rangeScanKd(2, start = -100.0, end = 100.0, steps = 100) { translation ->
                val pose = Pose2d(
                    Vector2d(translation[0], translation[1]),
                    Rotation2d.exp(rotation)
                )
                
                assertEquals(pose, pose)
                assertTrue(pose.approxEq(Pose2d.exp(pose.ln())))
                assertTrue((pose.inverse * pose).approxEq(Pose2d.identity))
                assertTrue((pose * pose.inverse).approxEq(Pose2d.identity))
            }
        }

        rangeScan(-10.0, 10.0, steps = 10) { rotation1 ->
            rangeScanKd(2, start = -100.0, end = 100.0, steps = 10) { translation1 ->
                val a = Pose2d(
                    Vector2d(translation1[0], translation1[1]),
                    Rotation2d.exp(rotation1)
                )

                rangeScan(-10.0, 10.0, steps = 10) { rotation2 ->
                    rangeScanKd(2, start = -100.0, end = 100.0, steps = 10) { translation2 ->
                        val b = Pose2d(
                            Vector2d(translation2[0], translation2[1]),
                            Rotation2d.exp(rotation2)
                        )

                        assertTrue((a + (b - a)).approxEq(b))
                        assertTrue((a * (b / a)).approxEq(b))
                    }
                }
            }
        }
    }

    @Test
    fun pose3dTest() {
        assertEquals(Pose3d.identity, Pose3d.exp(Twist3dIncr(Vector3d.zero, Vector3d.zero)))

        rangeScanKd(3, start = -100.0, end = 100.0, steps = 10) { rotation ->
            rangeScanKd(3, start = -100.0, end = 100.0, steps = 10) { translation ->
                val pose = Pose3d(
                    Vector3d(translation[0], translation[1], translation[2]),
                    Rotation3d.exp(Vector3d(rotation[0], rotation[1], rotation[2]))
                )

                val log = pose.ln()

                let {
                    assertEquals(pose, pose)
                    assertTrue(pose.approxEq(Pose3d.exp(log)))
                    assertTrue((pose.inverse * pose).approxEq(Pose3d.identity))
                    assertTrue((pose * pose.inverse).approxEq(Pose3d.identity))
                }

                let {
                    assertTrue(pose.approxEq(Pose3d.fromMatrix(pose())))
                    assertTrue((pose * Vector3d.one).approxEq(pose() * Vector3d.one))
                    assertTrue((pose() * pose.inverse()).approxEq(Matrix4x4.identity))
                    assertTrue((!pose()).approxEq(pose.inverse()))
                }
            }
        }

        assertEquals(Rotation3d.createNormalized(0.1, 0.1, 0.1, 0.1).norm, 1.0)
        assertEquals(Rotation3d.createNormalized(0.5, 0.0, 0.0, 0.0).norm, 1.0)
    }

    @Test
    fun rotation3dTest() {
        assertEquals(Rotation3d.identity, Rotation3d.exp(Vector3d.zero))
        assertTrue(Rotation3d.identity.approxEq(Rotation3d.exp(Vector3d.one * 1e-10)))
        assertTrue(Rotation3d.identity.approxEq(Rotation3d.exp(Vector3d.one * 1e-12)))
        assertTrue(Rotation3d.identity.approxEq(Rotation3d.exp(Vector3d.one * 1e-15)))
        assertTrue(Rotation3d.identity.approxEq(Rotation3d.exp(Vector3d.one * 1e-16)))
        assertTrue(Rotation3d.identity.approxEq(Rotation3d.exp(Vector3d.one * 1e-20)))

        rangeScanKd(3, start = -10.0, end = 10.0, steps = 10) { r ->
            val rotation = Rotation3d.exp(Vector3d(r[0], r[1], r[2]))
            val log = rotation.ln()

            let {
                assertEquals(rotation, rotation)
                assertTrue(rotation.approxEq(rotation(1.0)))
                assertTrue(Rotation3d.identity.approxEq(rotation(0.0)))
                assertTrue((rotation * rotation.inverse).approxEq(Rotation3d.identity))
                assertTrue((rotation.inverse * rotation).approxEq(Rotation3d.identity))
                assertTrue(rotation.approxEq(Rotation3d.exp(log)))
                assertTrue(log.approxEq(Rotation3d.exp(log).ln()))
            }

            let {
                val v2 = Vector3d(r[0], r[1], r[2]) * 1e-5
                val angle = v2.norm
                assertTrue(angle.approxEq(Rotation3d.exp(v2).ln().norm))
            }

            let {
                val matrix = rotation()
                assertTrue(Rotation3d.fromRotationMatrix(matrix).approxEq(rotation))
                assertTrue(matrix.isSpecialOrthogonal)
            }
        }

        rangeScanKd(3, start = -10.0, end = 10.0, steps = 10) { r1 ->
            val a = Rotation3d.exp(Vector3d(r1[0], r1[1], r1[2]))

            rangeScanKd(3, start = -10.0, end = 10.0, steps = 10) { r2 ->
                val b = Rotation3d.exp(Vector3d(r2[0], r2[1], r2[2]))

                assertTrue((a + (b - a)).approxEq(b))
                assertTrue((a * (b / a)).approxEq(b))
            }
        }
    }

    @Test
    fun matrix3x3Multiplication() {
        val A = Matrix3x3(
            1.0, 2.0, 3.0,
            4.0, 5.0, 6.0,
            7.0, 8.0, 9.0
        )

        val B = Matrix3x3(
            9.0, 8.0, 7.0,
            6.0, 5.0, 4.0,
            3.0, 2.0, 1.0
        )

        val AB = Matrix3x3(
            30.0, 24.0, 18.0,
            84.0, 69.0, 54.0,
            138.0, 114.0, 90.0
        )

        assertEquals(A * B, AB)

        val v = Vector3d(3.0, 1.0, 4.0)
        val ABv = Vector3d(186.0, 537.0, 888.0)

        assertEquals(AB * v, ABv)
    }

    @Test
    fun matrix3x3RowsColumnsEntries() {
        val A = Matrix3x3(
            1.0, 2.0, 3.0,
            4.0, 5.0, 6.0,
            7.0, 8.0, 9.0
        )

        assertEquals(A.r0, Vector3d(1.0, 2.0, 3.0))
        assertEquals(A.r1, Vector3d(4.0, 5.0, 6.0))
        assertEquals(A.r2, Vector3d(7.0, 8.0, 9.0))

        assertEquals(A.c0, Vector3d(1.0, 4.0, 7.0))
        assertEquals(A.c1, Vector3d(2.0, 5.0, 8.0))
        assertEquals(A.c2, Vector3d(3.0, 6.0, 9.0))

        assertEquals(A.getRow(0), Vector3d(1.0, 2.0, 3.0))
        assertEquals(A.getRow(1), Vector3d(4.0, 5.0, 6.0))
        assertEquals(A.getRow(2), Vector3d(7.0, 8.0, 9.0))

        assertEquals(A.getColumn(0), Vector3d(1.0, 4.0, 7.0))
        assertEquals(A.getColumn(1), Vector3d(2.0, 5.0, 8.0))
        assertEquals(A.getColumn(2), Vector3d(3.0, 6.0, 9.0))

        assertEquals(A[0, 0], 1.0)
        assertEquals(A[0, 1], 2.0)
        assertEquals(A[0, 2], 3.0)
        assertEquals(A[1, 0], 4.0)
        assertEquals(A[1, 1], 5.0)
        assertEquals(A[1, 2], 6.0)
        assertEquals(A[2, 0], 7.0)
        assertEquals(A[2, 1], 8.0)
        assertEquals(A[2, 2], 9.0)
    }

    @Test
    fun matrix3x3TraceFrobenius() {
        val A = Matrix3x3(
            1.0, 2.0, 3.0,
            4.0, 5.0, 6.0,
            7.0, 8.0, 9.0
        )

        assertEquals(A.trace, 1.0 + 5.0 + 9.0)
        assertEquals(A.normFrobeniusSqr, 285.0)
        assertEquals(A.normFrobenius, sqrt(285.0))
    }

    @Test
    fun matrix3x3OrthogonalSpecialOrthogonal() {
        val A = Matrix3x3(
            1.0, 2.0, 3.0,
            4.0, 5.0, 6.0,
            7.0, 8.0, 9.0
        )

        assertFalse(A.isOrthogonal)
        assertFalse(A.isSpecialOrthogonal)

        assertTrue(Matrix3x3.identity.isOrthogonal && Matrix3x3.identity.isSpecialOrthogonal)
        assertTrue(Rotation3d.exp(Vector3d.one.normalized() * 42.0)().let { it.isOrthogonal && it.isSpecialOrthogonal })
    }

    @Test
    fun matrix3x3Transpose() {
        val A = Matrix3x3(
            1.0, 2.0, 3.0,
            4.0, 5.0, 6.0,
            7.0, 8.0, 9.0
        )

        val Aᵀ = Matrix3x3(
            1.0, 4.0, 7.0,
            2.0, 5.0, 8.0,
            3.0, 6.0, 9.0
        )

        assertEquals(A.transpose, Aᵀ)
    }

    @Test
    fun matrix3x3Determinant() {
        val A = Matrix3x3(
            1.0, 3.0, 1.0,
            5.0, 2.0, 5.0,
            1.0, 2.0, 4.0
        )

        assertEquals(A.determinant, -39.0)
    }

    @Test
    fun matrix3x3Inverse() {
        val A = Matrix3x3(
            1.0, 3.0, 1.0,
            5.0, 2.0, 5.0,
            1.0, 2.0, 4.0
        )

        val expected = Matrix3x3(
            0.05128205128205128, 0.2564102564102564, -0.3333333333333333,
            0.38461538461538464, -0.07692307692307693, 0.0,
            -0.20512820512820512, -0.02564102564102564, 0.3333333333333333
        )

        assertTrue((!A).approxEq(expected))
    }

    @Test
    fun matrix4x4Multiplication() {
        val A = Matrix4x4(
            1.0, 2.0, 3.0, 4.0,
            5.0, 6.0, 7.0, 8.0,
            9.0, 10.0, 11.0, 12.0,
            13.0, 14.0, 15.0, 16.0
        )

        val B = Matrix4x4(
            16.0, 15.0, 14.0, 13.0,
            12.0, 11.0, 10.0, 9.0,
            8.0, 7.0, 6.0, 5.0,
            4.0, 3.0, 2.0, 1.0
        )

        val AB = Matrix4x4(
            80.0, 70.0, 60.0, 50.0,
            240.0, 214.0, 188.0, 162.0,
            400.0, 358.0, 316.0, 274.0,
            560.0, 502.0, 444.0, 386.0
        )

        assertEquals(A * B, AB)

        val v = Vector4d(3.0, 1.0, 4.0, 1.0)
        val ABv = Vector4d(600.0, 1848.0, 3096.0, 4344.0)

        assertEquals(AB * v, ABv)
    }

    @Test
    fun matrix4x4RowsColumnsEntries() {
        val A = Matrix4x4(
            1.0, 2.0, 3.0, 4.0,
            5.0, 6.0, 7.0, 8.0,
            9.0, 10.0, 11.0, 12.0,
            13.0, 14.0, 15.0, 16.0
        )

        assertEquals(A.r0, Vector4d(1.0, 2.0, 3.0, 4.0))
        assertEquals(A.r1, Vector4d(5.0, 6.0, 7.0, 8.0))
        assertEquals(A.r2, Vector4d(9.0, 10.0, 11.0, 12.0))
        assertEquals(A.r3, Vector4d(13.0, 14.0, 15.0, 16.0))

        assertEquals(A.c0, Vector4d(1.0, 5.0, 9.0, 13.0))
        assertEquals(A.c1, Vector4d(2.0, 6.0, 10.0, 14.0))
        assertEquals(A.c2, Vector4d(3.0, 7.0, 11.0, 15.0))
        assertEquals(A.c3, Vector4d(4.0, 8.0, 12.0, 16.0))

        assertEquals(A.getRow(0), Vector4d(1.0, 2.0, 3.0, 4.0))
        assertEquals(A.getRow(1), Vector4d(5.0, 6.0, 7.0, 8.0))
        assertEquals(A.getRow(2), Vector4d(9.0, 10.0, 11.0, 12.0))
        assertEquals(A.getRow(3), Vector4d(13.0, 14.0, 15.0, 16.0))

        assertEquals(A.getColumn(0), Vector4d(1.0, 5.0, 9.0, 13.0))
        assertEquals(A.getColumn(1), Vector4d(2.0, 6.0, 10.0, 14.0))
        assertEquals(A.getColumn(2), Vector4d(3.0, 7.0, 11.0, 15.0))
        assertEquals(A.getColumn(3), Vector4d(4.0, 8.0, 12.0, 16.0))

        assertEquals(A[0, 0], 1.0)
        assertEquals(A[0, 1], 2.0)
        assertEquals(A[0, 2], 3.0)
        assertEquals(A[0, 3], 4.0)
        assertEquals(A[1, 0], 5.0)
        assertEquals(A[1, 1], 6.0)
        assertEquals(A[1, 2], 7.0)
        assertEquals(A[1, 3], 8.0)
        assertEquals(A[2, 0], 9.0)
        assertEquals(A[2, 1], 10.0)
        assertEquals(A[2, 2], 11.0)
        assertEquals(A[2, 3], 12.0)
        assertEquals(A[3, 0], 13.0)
        assertEquals(A[3, 1], 14.0)
        assertEquals(A[3, 2], 15.0)
        assertEquals(A[3, 3], 16.0)
    }

    @Test
    fun matrix4x4TraceFrobenius() {
        val A = Matrix4x4(
            1.0, 2.0, 3.0, 4.0,
            5.0, 6.0, 7.0, 8.0,
            9.0, 10.0, 11.0, 12.0,
            13.0, 14.0, 15.0, 16.0
        )

        assertEquals(A.trace, 1.0 + 6.0 + 11.0 + 16.0)
        assertEquals(A.normFrobeniusSqr, (2.0 * sqrt(374.0)).pow(2))
        assertEquals(A.normFrobenius, 2.0 * sqrt(374.0))
    }

    @Test
    fun matrix4x4Transpose() {
        val A = Matrix4x4(
            1.0, 2.0, 3.0, 4.0,
            5.0, 6.0, 7.0, 8.0,
            9.0, 10.0, 11.0, 12.0,
            13.0, 14.0, 15.0, 16.0
        )

        val Aᵀ = Matrix4x4(
            1.0, 5.0, 9.0, 13.0,
            2.0, 6.0, 10.0, 14.0,
            3.0, 7.0, 11.0, 15.0,
            4.0, 8.0, 12.0, 16.0
        )

        assertEquals(A.transpose, Aᵀ)
    }

    @Test
    fun matrix4x4Determinant() {
        val A = Matrix4x4(
            3.0, 1.0, 4.0, 1.0,
            2.0, 7.0, 1.0, 8.0,
            1.0, 6.0, 1.0, 8.0,
            1.0, 0.0, 1.0, 0.0
        )

        assertEquals(A.determinant, 10.0)
    }

    @Test
    fun matrix4x4Inverse() {
        val A = Matrix4x4(
            3.0, 1.0, 4.0, 1.0,
            2.0, 7.0, 1.0, 8.0,
            1.0, 6.0, 1.0, 8.0,
            1.0, 0.0, 1.0, 0.0
        )

        val expected = Matrix4x4(
            -8.0, 2.0, -1.0, 31.0,
            8.0, 8.0, -9.0, -31.0,
            8.0, -2.0, 1.0, -21.0,
            -6.0, -6.0, 8.0, 22.0
        ) * (1.0 / 10.0)

        assertTrue((!A).approxEq(expected))
    }

    @Test
    fun planeTest() {
        assertTrue(Plane3d.unitX.approxEq(Plane3d(Vector3d.unitX, Vector3d.zero)))
        assertTrue(Plane3d.unitY.approxEq(Plane3d(Vector3d.unitY, Vector3d.zero)))
        assertTrue(Plane3d.unitZ.approxEq(Plane3d(Vector3d.unitZ, Vector3d.zero)))

        assertTrue(Plane3d.unitX.contains(Vector3d.zero))
        assertTrue(Plane3d.unitY.contains(Vector3d.zero))
        assertTrue(Plane3d.unitZ.contains(Vector3d.zero))

        assertTrue(Plane3d.unitX.contains(Vector3d.unitY) && Plane3d.unitX.contains(Vector3d.unitZ))
        assertTrue(Plane3d.unitY.contains(Vector3d.unitX) && Plane3d.unitY.contains(Vector3d.unitZ))
        assertTrue(Plane3d.unitZ.contains(Vector3d.unitX) && Plane3d.unitZ.contains(Vector3d.unitY))

        areEqual(Plane3d.unitX.distanceToPoint(Vector3d.unitX), 1.0)
        areEqual(Plane3d.unitY.distanceToPoint(Vector3d.unitY), 1.0)
        areEqual(Plane3d.unitZ.distanceToPoint(Vector3d.unitZ), 1.0)

        areEqual(Plane3d.unitX.distanceToPoint(Vector3d.unitY), Plane3d.unitX.distanceToPoint(Vector3d.unitZ), 0.0)
        areEqual(Plane3d.unitY.distanceToPoint(Vector3d.unitX), Plane3d.unitY.distanceToPoint(Vector3d.unitZ), 0.0)
        areEqual(Plane3d.unitZ.distanceToPoint(Vector3d.unitX), Plane3d.unitZ.distanceToPoint(Vector3d.unitY), 0.0)

        val plane = Plane3d(Vector3d.unitY, -Vector3d.unitY)

        assertEquals(plane.evaluateIntersection(Vector3d.zero), PlaneIntersectionType.Positive)
        assertEquals(plane.evaluateIntersection(Vector3d.unitY * -2.0), PlaneIntersectionType.Negative)
        assertEquals(plane.evaluateIntersection(Vector3d.unitY * -1.0), PlaneIntersectionType.Intersects)

        assertEquals(plane.signedDistanceToPoint(Vector3d.zero), 1.0)
        assertEquals(plane.signedDistanceToPoint(-Vector3d.unitY * 2.0), -1.0)

        assertTrue(Ray3d(Vector3d(10.0, 0.0, 10.0), -Vector3d.unitY).intersectionWith(plane).approxEq(Vector3d(10.0, -1.0, 10.0)))

        assertTrue(
            Plane3d.createFromVertices(
                Vector3d(10.0, -1.0, 5.0),
                Vector3d(20.0, -1.0, -6.0),
                Vector3d(-10.0, -1.0, 2.0)
            ).approxEq(plane)
        )
    }

    @Test
    fun ddaTest() {
        // I calculated these intersections on paper by drawing
        // Maybe find a trusted DDA implementation to also make a 3D test (can't really draw it very easily)

        val start = Vector3d(2.5, 1.5, 0.0)
        val end = Vector3d(5.5, 8.5, 0.0)
        val ray = Ray3d.fromSourceAndDestination(start, end)

        val results = mutableListOf<Vector2di>()

        dda(ray, true) { x, y, z ->
            assertEquals(z, 0)

            if(x == 5 && y == 8) {
                false
            }
            else {
                results.add(Vector2di(x, y))
                true
            }
        }

        assertEquals(results[0], Vector2di(2, 1))
        assertEquals(results[1], Vector2di(2, 2))
        assertEquals(results[2], Vector2di(3, 2))
        assertEquals(results[3], Vector2di(3, 3))
        assertEquals(results[4], Vector2di(3, 4))
        assertEquals(results[5], Vector2di(4, 4))
        assertEquals(results[6], Vector2di(4, 5))
        assertEquals(results[7], Vector2di(4, 6))
        assertEquals(results[8], Vector2di(4, 7))
        assertEquals(results[9], Vector2di(5, 7))
        assertEquals(results.size, 10)
    }
}