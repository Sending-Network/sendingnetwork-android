/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sdn.android.sdk.sample.utils

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.amulyakhare.textdrawable.TextDrawable
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import org.sdn.android.sdk.api.MatrixUrls.isMxcUrl
import org.sdn.android.sdk.sample.glide.GlideApp
import org.sdn.android.sdk.api.session.content.ContentUrlResolver
import org.sdn.android.sdk.api.util.SDNItem
import org.sdn.android.sdk.sample.SessionHolder


class AvatarRenderer(private val frag: Fragment, private val sdnItemColorProvider: SDNItemColorProvider) {

    companion object {
        private const val THUMBNAIL_SIZE = 250
    }

    fun render(avatarUrl: String?, imageView: ImageView) {
        val resolvedUrl = resolvedUrl(avatarUrl)
        GlideApp.with(frag).load(resolvedUrl).into(imageView)
    }

    fun render(SDNItem: SDNItem, imageView: ImageView) {
        val resolvedUrl = resolvedUrl(SDNItem.avatarUrl)
        val placeholder = getPlaceholderDrawable(SDNItem)
        Picasso.get()
            .load(resolvedUrl)
            .placeholder(placeholder)
            .transform(CropCircleTransformation())
            .into(imageView)
    }

    private fun getPlaceholderDrawable(sdnItem: SDNItem): Drawable {
        val avatarColor = sdnItemColorProvider.getColor(sdnItem)
        return TextDrawable.builder()
            .beginConfig()
            .bold()
            .endConfig()
            .buildRound(sdnItem.firstLetterOfDisplayName(), avatarColor)
    }

    // PRIVATE API *********************************************************************************

    private fun resolvedUrl(avatarUrl: String?): String? {
        if (avatarUrl.isNullOrBlank()) return null

        if (!avatarUrl.isMxcUrl()) return avatarUrl

        // Take care of using contentUrlResolver to use with mxc://
        return SessionHolder.currentSession?.contentUrlResolver()
            ?.resolveThumbnail(
                avatarUrl,
                THUMBNAIL_SIZE,
                THUMBNAIL_SIZE,
                ContentUrlResolver.ThumbnailMethod.SCALE
            )
    }
}
