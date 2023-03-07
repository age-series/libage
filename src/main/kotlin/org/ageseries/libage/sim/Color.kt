package org.ageseries.libage.sim

import kotlin.math.pow

/*
 * Color models.
 *
 * These live in the root (right now) because they're physical but do not belong to any simulation domain; they are like
 * [Material] in this regard.
 *
 * It should be noted that these are, with exception where noted, _not_ suitable for immediate use in game engines. Most
 * engines will have their own Color abstraction and colorspace (often implicitly sRGB or linear RGB), and these are
 * often designed to be submitted to the graphics hardware or pipeline directly. While conversions in this module can
 * certainly help, many of the colorspaces in this file can contain unphysical coordinates, as well as colors which
 * cannot be faithfully represented by most display devices.
 */

/**
 * Models according to the International Commission on Illumination (Commission internationale de l'éclairage).
 */
object CIE {
    /**
     * 1931 XYZ.
     *
     * This is a very typical reference standard for colorimetry, and the internal type of most color conversions.
     *
     * While the absolute values usually don't have any direct meaning, they are here understood such that D65 (the
     * typical display whitepoint, and the brightest luminance producible by that means) is at Y=1.0. This is assumed in
     * the conversion to sRGB.
     *
     * This gamut is bigger than the perceptual colorspace; it is easy to specify impossible colors with it--however,
     * every perceptible and physically realizeable color is within [0, 1] on the chromaticity ([x], [y]) basis, where
     * [Y] is held as luminance.
     */
    data class XYZ31(val X: Float, val Y: Float, val Z: Float) {
        /** Total of the stimulus values; unitless, but used for normalization. */
        val total: Float = X + Y + Z

        /** Normalized x chromaticity. */
        val x: Float
            @JvmName("getx") get() = X / total
        /** Normalized y chromaticity. */
        val y: Float
            @JvmName("gety") get() = Y / total
        /** Normalized z chromaticity. */
        val z: Float
            @JvmName("getz") get() = Z / total

        /** Convert to 1931 RGB. Up to floating point error, this conversion is exact. */
        val asRGB31 get() = 3400850f.let { divisor ->
            RGB31(
                (8041697f * X - 3049000 * Y - 1591847f * Z) / divisor,
                (-1752003f * X + 4851000f * Y + 301853 * Z) / divisor,
                (17697f * X - 49000f * Y + 3432153f * Z) / divisor,
            )
        }

        /** Convert to [linear RGB](LinRGB), as an intermediate to [sRGB]. */
        val asLinRGB get() = LinRGB(
            3.2406f * X - 1.5372f * Y - 0.4986f * Z,
            -0.9689f * X + 1.8758f * Y + 0.0415f * Z,
            0.0557f * X - 0.2040f * Y + 1.057f * Z,
        )

        /** Convert directly to [sRGB]. This still uses [LinRGB] as an intermediate. */
        val assRGB get() = asLinRGB.assRGB

        /** Convert to [UVW60]. */
        val asUVW60 get() = UVW60(
            2f * X / 3f, Y, (-X + 3f * Y + Z) / 2f,
        )

        companion object {
            /**
             * Convert from chromaticity coordinates ([x], [y]) and an absolute luminance ([Y]) into an [XYZ31] object.
             */
            fun fromxyY(x: Float, y: Float, Y: Float) = (Y/y).let { scale ->
                XYZ31(scale * x, Y, scale * (1f - x - y))
            }
        }
    }

    /**
     * 1931 linear RGB.
     *
     * This is merely a different basis of the space above, chosen to more closely resemble human vision tristimulus
     * values based on experimental evidence.
     */
    data class RGB31(val R: Float, val G: Float, val B: Float) {
        /** Total of stimulus values; unitless, but used for normalization. */
        val total: Float = R + G + B

        /** Normalized red chromaticity. */
        val r: Float
            @JvmName("getr") get() = R / total
        /** Normalized green chromaticity. */
        val g: Float
            @JvmName("getg") get() = G / total
        /** Normalized blue chromaticity. */
        val b: Float
            @JvmName("getb") get() = B / total

        /** Convert to 1931 XYZ. Up to floating point error, the conversion is exact. */
        val asXYZ31 get() = XYZ31(
            0.49f * R + 0.31f * G + 0.2f * B,
            0.17697f * R + 0.81240f * G + 0.01063f * B,
            0.01f * G + 0.99f * B,
        )
    }

    /**
     * 1960 UVW.
     *
     * This space was a first attempt at "uniform chromaticity", such that a perceptual color would not change between
     * luminance, but 1976 UCS is thought to be a better estimate today. This gamut is still useful for "coordinated
     * color temperature" calculation, though, as it is used here.
     */
    data class UVW60(val U: Float, val V: Float, val W: Float) {
        /** Total of stimulus values; unitless, but used for normalization. */
        val total: Float = U + V + W

        /** Normalized u chromaticity. */
        val u: Float
            @JvmName("getu") get() = U / total
        /** Normalized v chromaticity. */
        val v: Float
            @JvmName("getv") get() = V / total
        /** Normalized w coordinate. This is supposed to be independent of chromaticity (thus linear in luminance), and so its normalized value isn't particularly meaningful. It is included for completeness. */
        val w: Float
            @JvmName("getw") get() = W / total

        /** Convert to [XYZ31]. */
        val asXYZ31 get() = XYZ31(
            1.5f * U, V, 1.5f * U - 3f * V + 2f * W,
        )

        companion object {
            /** Convert from chromaticity coordinates ([u], [v]) and an absolute luminance ([Y]) into a [UVW60] object. */
            fun fromuvY(u: Float, v: Float, Y: Float) = (1f - u - v).let {w ->
                val scale = Y/v
                UVW60(u * scale, v * scale, w * scale)
            }
        }
    }
}

/**
 * Linear RGB space.
 *
 * While in direct correspondence with the other linear spaces (notably CIE 1931), this is generally unsuitable for
 * reproduction without gamma correction. Conversion to [sRGB] is often preferable.
 */
data class LinRGB(
    val R: Float,
    val G: Float,
    val B: Float,
) {
    /** Convert to [sRGB].*/
    val assRGB get() = sRGB(
        sRGB.transfer(R),
        sRGB.transfer(G),
        sRGB.transfer(B),
    )
}

/**
 * The sRGB colorspace.
 *
 * This corresponds with a very commonplace IEC standard frequently used not only for displays, but many forms of color
 * reproduction (including image file formats). The gamma function was intended to closely match the performance of
 * 1996-era color CRT screens used for HDTV.
 *
 * It is a safe bet that your graphics pipeline expects these values, possibly normalized into unsigned bytes.
 */
data class sRGB(
    val R: Float,
    val G: Float,
    val B: Float,
) {
    companion object {
        /** The Electro-Optical Transfer Function (EOTF) for this gamut. */
        fun transfer(c: Float): Float = if(c <= 0.04045f) {
            c / 12.92f
        } else {
            ((c + 0.055f) / 1.055f).pow(2.4f)
        }
    }
}