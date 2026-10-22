package com.example.foecho.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.foecho.R
import java.util.Random

enum class ScatterIcon(@param:DrawableRes val drawable: Int) {
    ROSE(R.drawable.scatter_rose),
    BUNNY(R.drawable.scatter_bunny),
    DEER(R.drawable.scatter_deer)
}

private enum class ScatterCorner { TOP_START, TOP_END, BOTTOM_START, BOTTOM_END }

private class ScatterSpec(
    val show: Boolean,
    val corner: ScatterCorner,
    val rotation: Float,
    val size: Dp,
    val icon: ScatterIcon
)

private fun scatterSpec(seed: Int, force: Boolean): ScatterSpec {
    val r = Random(seed.toLong())
    val roll = r.nextInt(100)
    val corner = ScatterCorner.entries[r.nextInt(ScatterCorner.entries.size)]
    val rotation = (r.nextInt(61) - 30).toFloat()
    val size = (16 + r.nextInt(10)).dp
    val icon = ScatterIcon.entries[r.nextInt(ScatterIcon.entries.size)]
    return ScatterSpec(force || roll < 35, corner, rotation, size, icon)
}

@Composable
fun BoxScope.ScatterDecoration(seed: Int, force: Boolean) {
    val spec = remember(seed, force) { scatterSpec(seed, force) }
    if (!spec.show) return
    val alignment = when (spec.corner) {
        ScatterCorner.TOP_START -> Alignment.TopStart
        ScatterCorner.TOP_END -> Alignment.TopEnd
        ScatterCorner.BOTTOM_START -> Alignment.BottomStart
        ScatterCorner.BOTTOM_END -> Alignment.BottomEnd
    }
    val inset = 6.dp
    val dx = when (spec.corner) {
        ScatterCorner.TOP_START, ScatterCorner.BOTTOM_START -> inset
        else -> -inset
    }
    val dy = when (spec.corner) {
        ScatterCorner.TOP_START, ScatterCorner.TOP_END -> inset
        else -> -inset
    }
    Box(
        modifier = Modifier
            .align(alignment)
            .offset(x = dx, y = dy)
            .size(spec.size)
            .graphicsLayer { rotationZ = spec.rotation }
    ) {
        ScatterIconImage(icon = spec.icon, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun ScatterIconImage(icon: ScatterIcon, modifier: Modifier = Modifier) {
    val tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
    val filter = remember(tint) { ColorFilter.tint(tint) }
    Image(
        painter = painterResource(icon.drawable),
        contentDescription = null,
        colorFilter = filter,
        modifier = modifier
    )
}