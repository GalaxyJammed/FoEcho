package com.example.foecho.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.foecho.data.Appearance
import java.util.Random

@Composable
fun ShieldCard(
    modifier: Modifier = Modifier,
    forceScatter: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val seed = remember { Random().nextInt() }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x66000000))
    ) {
        Box {
            Column(Modifier.fillMaxWidth(), content = content)
            if (Appearance.scatter) {
                ScatterDecoration(seed = seed, force = forceScatter)
            }
        }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    navIcon: ImageVector,
    navDescription: String,
    onNavClick: () -> Unit,
    rightIcon: ImageVector? = null,
    rightDescription: String = "",
    onRightClick: () -> Unit = {}
) {
    ShieldCard(forceScatter = true) {
        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp)) {
            IconButton(
                onClick = onNavClick,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(navIcon, contentDescription = navDescription)
            }
            if (rightIcon != null) {
                IconButton(
                    onClick = onRightClick,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(rightIcon, contentDescription = rightDescription)
                }
            }
            Text(
                text = title,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 48.dp),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}