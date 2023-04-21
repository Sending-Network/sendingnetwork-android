package org.sdn.android.sdk.sample.glide

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.PictureDrawable
import com.bumptech.glide.load.ResourceDecoder
import kotlin.Throws
import com.caverock.androidsvg.SVG
import com.bumptech.glide.Glide
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.engine.Resource
import com.caverock.androidsvg.SVGParseException
import java.io.IOException
import java.io.InputStream

/**
 * Decodes an SVG internal representation from an [InputStream].
 */
class SvgBitmapDecoder(private val context: Context) : ResourceDecoder<InputStream, Bitmap> {

    override fun handles(source: InputStream, options: Options): Boolean {
        // TODO: Can we tell?
        return true
    }

    @Throws(IOException::class)
    override fun decode(
        source: InputStream, width: Int, height: Int, options: Options): Resource<Bitmap>? {
        return try {
            val svg = SVG.getFromInputStream(source)
            val picture = svg.renderToPicture()
            val drawable = PictureDrawable(picture)
            val bitmapPool = Glide.get(context).bitmapPool
            DrawableToBitmapConverter.convert(bitmapPool, drawable, drawable.intrinsicWidth, drawable.intrinsicWidth)
        } catch (ex: SVGParseException) {
            throw IOException("Cannot load SVG from stream", ex)
        }
    }
}