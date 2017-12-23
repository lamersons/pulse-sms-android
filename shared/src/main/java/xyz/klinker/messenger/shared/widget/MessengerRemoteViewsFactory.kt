/*
 * Copyright (C) 2017 Luke Klinker
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

package xyz.klinker.messenger.shared.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.pojo.BaseTheme
import xyz.klinker.messenger.shared.util.ContactUtils
import xyz.klinker.messenger.shared.util.CursorUtil
import xyz.klinker.messenger.shared.util.ImageUtils
import java.util.*

class MessengerRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var conversations: MutableList<Conversation>? = null

    override fun onCreate() {
        reloadConversations()
    }

    private fun reloadConversations() {
        val items = DataSource.getUnarchivedConversations(context)
        conversations = ArrayList()

        if (items.moveToFirst()) {
            do {
                val conversation = Conversation()
                conversation.fillFromCursor(items)
                conversations!!.add(conversation)
            } while (items.moveToNext())
        }

        CursorUtil.closeSilent(items)
    }

    override fun onDataSetChanged() {
        reloadConversations()
    }

    override fun onDestroy() {
        conversations!!.clear()
    }

    override fun getCount(): Int {
        return conversations!!.size
    }

    override fun getViewAt(position: Int): RemoteViews? {
        if (position >= conversations!!.size) {
            return null
        }

        val item = conversations!![position]

        if (item.title == null) {
            item.title = ""
        }

        if (item.snippet == null) {
            item.snippet = ""
        }

        if (!item.read) {
            item.title = "<b>" + item.title + "</b>"
            item.snippet = "<b>" + item.snippet + "</b>"
        }

        val rv = RemoteViews(context.packageName, R.layout.widget_item)

        var image = ImageUtils.getBitmap(context, item.imageUri)
        if (image == null) {
            image = ImageUtils.createColoredBitmap(item.colors.color)

            if (ContactUtils.shouldDisplayContactLetter(item)) {
                rv.setTextViewText(R.id.image_letter, item.title!!.substring(0, 1))
            } else {
                rv.setTextViewText(R.id.image_letter, null)
            }
        } else {
            rv.setTextViewText(R.id.image_letter, null)
        }

        image = ImageUtils.clipToCircle(image)

        rv.setTextViewText(R.id.conversation_title, Html.fromHtml(item.title))
        rv.setTextViewText(R.id.conversation_summary, Html.fromHtml(item.snippet))
        rv.setImageViewBitmap(R.id.picture, image)

        if (FeatureFlags.WIDGET_THEMEING) {
            when (Settings.baseTheme) {
                BaseTheme.ALWAYS_DARK -> {
                    rv.setTextColor(R.id.conversation_title, Color.WHITE)
                    rv.setTextColor(R.id.conversation_summary, Color.parseColor("#B2FFFFFF"))
                }
                BaseTheme.BLACK -> {
                    rv.setTextColor(R.id.conversation_title, Color.WHITE)
                    rv.setTextColor(R.id.conversation_summary, Color.parseColor("#B2FFFFFF"))
                }
                else -> {
                    rv.setTextColor(R.id.conversation_title, context.resources.getColor(R.color.primaryText))
                    rv.setTextColor(R.id.conversation_summary, context.resources.getColor(R.color.secondaryText))
                }
            }
        }

        if (item.read) {
            rv.setViewVisibility(R.id.unread_indicator, View.GONE)
        } else {
            rv.setViewVisibility(R.id.unread_indicator, View.VISIBLE)
        }

        val extras = Bundle()
        extras.putLong(MessengerAppWidgetProvider.EXTRA_ITEM_ID, item.id)
        val fillInIntent = Intent()
        fillInIntent.putExtras(extras)
        rv.setOnClickFillInIntent(R.id.widget_item, fillInIntent)

        return rv
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return if (conversations!!.size > 0 && position < conversations!!.size) {
            conversations!![position].id
        } else {
            0
        }
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    fun getConversations(): List<Conversation>? {
        return conversations
    }

}
