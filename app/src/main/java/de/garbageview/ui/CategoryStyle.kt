package de.garbageview.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.garbageview.R
import de.garbageview.core.WasteCategory

@get:DrawableRes
val WasteCategory.iconRes: Int
    get() = when (this) {
        WasteCategory.WASTE_BASKET -> R.drawable.ic_cat_waste_basket
        WasteCategory.DOG_WASTE -> R.drawable.ic_cat_dog_waste
        WasteCategory.GLASS -> R.drawable.ic_cat_glass
        WasteCategory.PAPER -> R.drawable.ic_cat_paper
        WasteCategory.CLOTHES -> R.drawable.ic_cat_clothes
        WasteCategory.RECYCLING_CENTRE -> R.drawable.ic_cat_recycling_centre
        WasteCategory.OTHER_RECYCLING -> R.drawable.ic_cat_other_recycling
        WasteCategory.WASTE_DISPOSAL -> R.drawable.ic_cat_waste_disposal
    }

val WasteCategory.color: Color
    get() = Color(android.graphics.Color.parseColor(colorHex))

/** Rundes Kategoriesymbol: weißes Icon auf Kategoriefarbe – identisch zum Kartenmarker. */
@Composable
fun CategoryBadge(category: WasteCategory, size: Dp = 24.dp, enabled: Boolean = true) {
    Box(
        Modifier
            .size(size)
            .background(if (enabled) category.color else Color.Gray.copy(alpha = 0.5f), CircleShape)
            .padding(size * 0.18f),
    ) {
        Icon(painterResource(category.iconRes), contentDescription = null, tint = Color.White)
    }
}

/** Bitmap für den Kartenmarker einer Kategorie. */
fun markerBitmap(context: Context, category: WasteCategory, sizeDp: Int = 30): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (sizeDp * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val radius = size / 2f
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(radius, radius, radius, paint)
    paint.color = android.graphics.Color.parseColor(category.colorHex)
    canvas.drawCircle(radius, radius, radius - 2 * density, paint)
    val inset = (size * 0.22f).toInt()
    ContextCompat.getDrawable(context, category.iconRes)!!.mutate().apply {
        setTint(android.graphics.Color.WHITE)
        setBounds(inset, inset, size - inset, size - inset)
        draw(canvas)
    }
    return bitmap
}
