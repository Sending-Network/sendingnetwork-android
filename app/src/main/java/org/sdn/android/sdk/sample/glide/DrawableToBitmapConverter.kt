package org.sdn.android.sdk.sample.glide

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPoolAdapter
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import com.bumptech.glide.request.target.Target
import timber.log.Timber


object DrawableToBitmapConverter {
    private const val TAG = "DrawableToBitmap"
    private val NO_RECYCLE_BITMAP_POOL: BitmapPool = object : BitmapPoolAdapter() {
        override fun put(bitmap: Bitmap) {
            // Avoid calling super to avoid recycling the given Bitmap.
        }
    }

    fun convert(
        bitmapPool: BitmapPool,
        drawable: Drawable,
        width: Int,
        height: Int
    ): Resource<Bitmap>? {
        // Handle DrawableContainer or StateListDrawables that may contain one or more BitmapDrawables.
        val drawable = drawable.current
        var result: Bitmap? = null
        var isRecycleable = false
        if (drawable is BitmapDrawable) {
            result = drawable.bitmap
        } else if (drawable !is Animatable) {
            result = drawToBitmap(bitmapPool, drawable, width, height)
            // We created and drew to the Bitmap, so it's safe for us to recycle or re-use.
            isRecycleable = true
        }
        val toUse = if (isRecycleable) bitmapPool else NO_RECYCLE_BITMAP_POOL
        return BitmapResource.obtain(result, toUse)
    }

    private fun drawToBitmap(
        bitmapPool: BitmapPool, drawable: Drawable, width: Int, height: Int
    ): Bitmap? {
        if (width == Target.SIZE_ORIGINAL && drawable.intrinsicWidth <= 0) {
            if (Log.isLoggable(TAG, Log.WARN)) {
                Timber.w("Unable to draw $drawable to Bitmap with Target.SIZE_ORIGINAL because the Drawable has no intrinsic width")
            }
            return null
        }
        if (height == Target.SIZE_ORIGINAL && drawable.intrinsicHeight <= 0) {
            if (Log.isLoggable(TAG, Log.WARN)) {
                Timber.tag(TAG)
                    .w(("Unable to draw $drawable to Bitmap with Target.SIZE_ORIGINAL because the Drawable has no intrinsic height"))
            }
            return null
        }
        val targetWidth = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else width
        val targetHeight = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else height
        val lock = TransformationUtils.getBitmapDrawableLock()
        lock.lock()
        val result = bitmapPool[targetWidth, targetHeight, Bitmap.Config.ARGB_8888]
        try {
            val canvas = Canvas(result)
            drawable.setBounds(0, 0, targetWidth, targetHeight)
            drawable.draw(canvas)
            canvas.setBitmap(null)
        } finally {
            lock.unlock()
        }
        return result
    }
}