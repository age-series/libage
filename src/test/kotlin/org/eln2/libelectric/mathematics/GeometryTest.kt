package org.eln2.libelectric.mathematics

import org.ageseries.libage.mathematics.*
import org.eln2.libelectric.TestUtils.rangeScan
import org.eln2.libelectric.TestUtils.rangeScanKd
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
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
                assertTrue(pose.approxEq(Pose2d.exp(pose.log())))
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

                val log = pose.log()

                let {
                    assertEquals(pose, pose)
                    assertTrue(pose.approxEq(Pose3d.exp(log)))
                    assertTrue((pose.inverse * pose).approxEq(Pose3d.identity))
                    assertTrue((pose * pose.inverse).approxEq(Pose3d.identity))
                }

                assertTrue(pose.approxEq(Pose3d.fromMatrix(pose())))
            }
        }
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
            val log = rotation.log()

            let {
                assertEquals(rotation, rotation)
                assertTrue(rotation.approxEq(rotation(1.0)))
                assertTrue(Rotation3d.identity.approxEq(rotation(0.0)))
                assertTrue((rotation * rotation.inverse).approxEq(Rotation3d.identity))
                assertTrue((rotation.inverse * rotation).approxEq(Rotation3d.identity))
                assertTrue(rotation.approxEq(Rotation3d.exp(log)))
                assertTrue(log.approxEq(Rotation3d.exp(log).log()))
            }

            let {
                val v2 = Vector3d(r[0], r[1], r[2]) * 1e-5
                val angle = v2.norm
                assertTrue(angle.approxEq(Rotation3d.exp(v2).log().norm))
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
}