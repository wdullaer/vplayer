/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import com.bumptech.glide.load.Transformation

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.util.Util
import java.security.MessageDigest


/**
 * Glide transformation class for blur bitmap
 * Based on: https://gist.github.com/stepango/040bc2a47595c9e7121e
 */
class StackBlurTransform(private val blurRadius: Int = DEFAULT_BLUR_RADIUS,
                         private var scaleFactor: Int = DEFAULT_SCALE_FACTOR) : Transformation<Bitmap> {
    val id: String
        get() = "$TRANSFORMATION_ID(blurRadius = $blurRadius, scaleFactor = $scaleFactor)"

    @Suppress("unused")
    constructor() : this(DEFAULT_BLUR_RADIUS)

    constructor(blurRadius: Int): this(blurRadius, DEFAULT_SCALE_FACTOR)

    init {
        setScaleFactor(DEFAULT_SCALE_FACTOR)
    }

    override fun transform(context: Context, resource: Resource<Bitmap>, outWidth: Int,
                  outHeight: Int): Resource<Bitmap> {
        if (!Util.isValidDimensions(outWidth, outHeight)) {
            throw IllegalArgumentException(
                    "Cannot apply transformation on width: " + outWidth + " or height: " + outHeight
                            + " less than or equal to zero and not Target.SIZE_ORIGINAL")
        }
        val bitmapPool = Glide.get(context).bitmapPool
        val toTransform = resource.get()
        // val targetWidth = if (outWidth == Target.SIZE_ORIGINAL) toTransform.width else outWidth
        // val targetHeight = if (outHeight == Target.SIZE_ORIGINAL) toTransform.height else outHeight
        val transformed = transform(context.applicationContext, bitmapPool, toTransform)

        return if (toTransform == transformed) {
            Log.d("StackBlurTransform", "Bitmap wasn't changed")
            resource
        } else {
            Log.d("StackBlurTransform", "Converting bitmap to a resource")
            BitmapResource.obtain(transformed, bitmapPool) as Resource<Bitmap>
        }
    }

    /**
     * Scale factor for decrease bitmap size before blur processing.
     * [.DEFAULT_SCALE_FACTOR]
     */
    private fun setScaleFactor(scaleFactor: Int) {
        if (scaleFactor < 1) {
            throw IllegalArgumentException("Scale factor must be >= 1")
        }
        this.scaleFactor = scaleFactor
    }

    private fun transform(context: Context, pool: BitmapPool, toTransform: Bitmap): Bitmap? {
        val deviceBlurRadius = (blurRadius * context.resources.displayMetrics.density).toInt()

        var bitmap = pool.get(
                toTransform.width / scaleFactor,
                toTransform.height / scaleFactor,
                Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        canvas.scale(1 / scaleFactor.toFloat(), 1 / scaleFactor.toFloat())
        val paint = Paint()
        paint.flags = Paint.FILTER_BITMAP_FLAG
        canvas.drawBitmap(toTransform, 0f, 0f, paint)

        //Add blur to bitmap
        bitmap = getBlurredBitmap(bitmap, deviceBlurRadius / scaleFactor)

        return bitmap
    }

    companion object {

        const val TRANSFORMATION_ID = "com.wdullaer.vplayer.StackBlurTransform"
        const val DEFAULT_SCALE_FACTOR = 5
        private const val DEFAULT_BLUR_RADIUS = 21

        fun getBlurredBitmap(sentBitmap: Bitmap, radius: Int): Bitmap {
            Log.d("vrtnu.StackBlurTrans", "Blurring bitmap")

            // Stack Blur v1.0 from
            // http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html
            //
            // Java Author: Mario Klingemann <mario at quasimondo.com>
            // http://incubator.quasimondo.com
            // created Feburary 29, 2004
            // Android port : Yahel Bouaziz <yahel at kayenko.com>
            // http://www.kayenko.com
            // ported april 5th, 2012

            // This is a compromise between Gaussian Blur and Box blur
            // It creates much better looking blurs than Box Blur, but is
            // 7x faster than my Gaussian Blur implementation.
            //
            // I called it Stack Blur because this describes best how this
            // filter works internally: it creates a kind of moving stack
            // of colors whilst scanning through the image. Thereby it
            // just has to add one new block of color to the right side
            // of the stack and remove the leftmost color. The remaining
            // colors on the topmost layer of the stack are either added on
            // or reduced by one, depending on if they are on the right or
            // on the left side of the stack.
            //
            // If you are using this algorithm in your code please add
            // the following line:
            //
            // Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>

            if (radius < 1) {
                throw IllegalArgumentException("Blur radius should be >= 1")
            }

            val w = sentBitmap.width
            val h = sentBitmap.height

            val pix = IntArray(w * h)
            sentBitmap.getPixels(pix, 0, w, 0, 0, w, h)

            val wm = w - 1
            val hm = h - 1
            val wh = w * h
            val div = radius + radius + 1

            val r = IntArray(wh)
            val g = IntArray(wh)
            val b = IntArray(wh)
            var rsum: Int
            var gsum: Int
            var bsum: Int
            var x: Int
            var y: Int
            var i: Int
            var p: Int
            var yp: Int
            var yi: Int
            var yw: Int
            val vmin = IntArray(Math.max(w, h))

            var divsum = div + 1 shr 1
            divsum *= divsum
            val dv = IntArray(256 * divsum)
            i = 0
            while (i < 256 * divsum) {
                dv[i] = i / divsum
                i++
            }

            yi = 0
            yw = yi

            val stack = Array(div) { IntArray(3) }
            var stackpointer: Int
            var stackstart: Int
            var sir: IntArray
            var rbs: Int
            val r1 = radius + 1
            var routsum: Int
            var goutsum: Int
            var boutsum: Int
            var rinsum: Int
            var ginsum: Int
            var binsum: Int

            y = 0
            while (y < h) {
                bsum = 0
                gsum = bsum
                rsum = gsum
                boutsum = rsum
                goutsum = boutsum
                routsum = goutsum
                binsum = routsum
                ginsum = binsum
                rinsum = ginsum
                i = -radius
                while (i <= radius) {
                    p = pix[yi + Math.min(wm, Math.max(i, 0))]
                    sir = stack[i + radius]
                    sir[0] = p and 0xff0000 shr 16
                    sir[1] = p and 0x00ff00 shr 8
                    sir[2] = p and 0x0000ff
                    rbs = r1 - Math.abs(i)
                    rsum += sir[0] * rbs
                    gsum += sir[1] * rbs
                    bsum += sir[2] * rbs
                    if (i > 0) {
                        rinsum += sir[0]
                        ginsum += sir[1]
                        binsum += sir[2]
                    } else {
                        routsum += sir[0]
                        goutsum += sir[1]
                        boutsum += sir[2]
                    }
                    i++
                }
                stackpointer = radius

                x = 0
                while (x < w) {

                    r[yi] = dv[rsum]
                    g[yi] = dv[gsum]
                    b[yi] = dv[bsum]

                    rsum -= routsum
                    gsum -= goutsum
                    bsum -= boutsum

                    stackstart = stackpointer - radius + div
                    sir = stack[stackstart % div]

                    routsum -= sir[0]
                    goutsum -= sir[1]
                    boutsum -= sir[2]

                    if (y == 0) {
                        vmin[x] = Math.min(x + radius + 1, wm)
                    }
                    p = pix[yw + vmin[x]]

                    sir[0] = p and 0xff0000 shr 16
                    sir[1] = p and 0x00ff00 shr 8
                    sir[2] = p and 0x0000ff

                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]

                    rsum += rinsum
                    gsum += ginsum
                    bsum += binsum

                    stackpointer = (stackpointer + 1) % div
                    sir = stack[stackpointer % div]

                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]

                    rinsum -= sir[0]
                    ginsum -= sir[1]
                    binsum -= sir[2]

                    yi++
                    x++
                }
                yw += w
                y++
            }
            x = 0
            while (x < w) {
                bsum = 0
                gsum = bsum
                rsum = gsum
                boutsum = rsum
                goutsum = boutsum
                routsum = goutsum
                binsum = routsum
                ginsum = binsum
                rinsum = ginsum
                yp = -radius * w
                i = -radius
                while (i <= radius) {
                    yi = Math.max(0, yp) + x

                    sir = stack[i + radius]

                    sir[0] = r[yi]
                    sir[1] = g[yi]
                    sir[2] = b[yi]

                    rbs = r1 - Math.abs(i)

                    rsum += r[yi] * rbs
                    gsum += g[yi] * rbs
                    bsum += b[yi] * rbs

                    if (i > 0) {
                        rinsum += sir[0]
                        ginsum += sir[1]
                        binsum += sir[2]
                    } else {
                        routsum += sir[0]
                        goutsum += sir[1]
                        boutsum += sir[2]
                    }

                    if (i < hm) {
                        yp += w
                    }
                    i++
                }
                yi = x
                stackpointer = radius
                y = 0
                while (y < h) {
                    // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                    pix[yi] = -0x1000000 and pix[yi] or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                    rsum -= routsum
                    gsum -= goutsum
                    bsum -= boutsum

                    stackstart = stackpointer - radius + div
                    sir = stack[stackstart % div]

                    routsum -= sir[0]
                    goutsum -= sir[1]
                    boutsum -= sir[2]

                    if (x == 0) {
                        vmin[y] = Math.min(y + r1, hm) * w
                    }
                    p = x + vmin[y]

                    sir[0] = r[p]
                    sir[1] = g[p]
                    sir[2] = b[p]

                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]

                    rsum += rinsum
                    gsum += ginsum
                    bsum += binsum

                    stackpointer = (stackpointer + 1) % div
                    sir = stack[stackpointer]

                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]

                    rinsum -= sir[0]
                    ginsum -= sir[1]
                    binsum -= sir[2]

                    yi += w
                    y++
                }
                x++
            }

            sentBitmap.setPixels(pix, 0, w, 0, 0, w, h)

            return sentBitmap
        }
    }

    override fun toString() : String {
        return this.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is StackBlurTransform) return other.id == this.id
        return false
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(id.toByteArray())
    }
}