package com.last.app.presentation.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable

object MapMarkerIconFactory {

    private const val MARKER_SIZE = 40
    private const val PRIMARY_COLOR = 0xFF001A3F.toInt()
    private const val BORDER_COLOR = 0xFFFFFFFF.toInt()
    private const val SHADOW_COLOR = 0x33001A3F

    fun create(context: Context): BitmapDrawable {
        val bitmap = Bitmap.createBitmap(MARKER_SIZE, MARKER_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = MARKER_SIZE / 2f
        val radius = 14f

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = SHADOW_COLOR
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center + 1.5f, radius + 2f, shadowPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BORDER_COLOR
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, radius + 3f, borderPaint)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PRIMARY_COLOR
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, radius, fillPaint)

        return BitmapDrawable(context.resources, bitmap)
    }
}
