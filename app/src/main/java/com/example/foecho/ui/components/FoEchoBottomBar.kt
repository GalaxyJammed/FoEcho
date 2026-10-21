package com.example.foecho.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val BarHeight = 80.dp
private val FlyoutArea = 200.dp
private val FabSize = 60.dp

data class BarAction(
    val icon: ImageVector,
    val label: String,
    val dx: Dp,
    val dy: Dp,
    val color: Color,
    val contentColor: Color,
    val onClick: () -> Unit
)

@Composable
fun FoEchoBottomBar(
    modifier: Modifier = Modifier,
    centerIcon: ImageVector,
    centerLabel: String,
    centerColor: Color,
    centerContent: Color,
    rotateCenter: Boolean,
    menuVisible: Boolean,
    menuProgress: Float,
    actions: List<BarAction>,
    onCenterClick: () -> Unit,
    sideIcon: ImageVector? = null,
    sideLabel: String = "",
    onSideClick: () -> Unit = {},
    sideColor: Color? = null,
    sideContent: Color? = null,
    leftIcon: ImageVector? = null,
    leftLabel: String = "",
    onLeftClick: () -> Unit = {}
) {
    val navBottom = with(LocalDensity.current) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    val barTotal = BarHeight + navBottom

    Box(modifier.fillMaxWidth().height(FlyoutArea + barTotal)) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(barTotal),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            tonalElevation = 3.dp,
            shadowElevation = 8.dp
        ) {}

        if (centerLabel.isNotEmpty()) {
            Text(
                text = centerLabel,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = navBottom + 10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (menuVisible) {
            actions.forEach { action ->
                FlyoutButton(
                    action = action,
                    progress = menuProgress,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = -(barTotal - 58.dp))
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(barTotal),
            contentAlignment = Alignment.TopCenter
        ) {
            val centerInteraction = remember { MutableInteractionSource() }
            Surface(
                onClick = onCenterClick,
                interactionSource = centerInteraction,
                shape = CircleShape,
                color = centerColor,
                contentColor = centerContent,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .offset(y = (-18).dp)
                    .size(FabSize)
                    .pressScale(centerInteraction, 0.9f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = centerIcon,
                        contentDescription = centerLabel.ifEmpty { "Add" },
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer { rotationZ = if (rotateCenter) 45f * menuProgress else 0f }
                    )
                }
            }
        }

        if (leftIcon != null) {
            val leftInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(barTotal),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    onClick = onLeftClick,
                    interactionSource = leftInteraction,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .offset(x = (-96).dp, y = (-14).dp)
                        .size(52.dp)
                        .pressScale(leftInteraction, 0.9f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = leftIcon,
                            contentDescription = leftLabel
                        )
                    }
                }
            }
        }

        if (sideIcon != null) {
            val sideInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(barTotal),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    onClick = onSideClick,
                    interactionSource = sideInteraction,
                    shape = CircleShape,
                    color = sideColor ?: MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = sideContent ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .offset(x = 96.dp, y = (-14).dp)
                        .size(52.dp)
                        .pressScale(sideInteraction, 0.9f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = sideIcon,
                            contentDescription = sideLabel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FlyoutButton(
    action: BarAction,
    progress: Float,
    modifier: Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier.graphicsLayer {
            translationX = action.dx.toPx() * progress
            translationY = action.dy.toPx() * progress
            scaleX = 0.3f + 0.7f * progress
            scaleY = 0.3f + 0.7f * progress
            alpha = progress
            rotationZ = (if (action.dx < 0.dp) -20f else 20f) * (1f - progress)
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            onClick = action.onClick,
            interactionSource = interaction,
            shape = CircleShape,
            color = action.color,
            contentColor = action.contentColor,
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(56.dp)
                .pressScale(interaction, 0.88f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(action.icon, contentDescription = action.label)
            }
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Text(
                text = action.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.92f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 300f),
        label = "pressScale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}