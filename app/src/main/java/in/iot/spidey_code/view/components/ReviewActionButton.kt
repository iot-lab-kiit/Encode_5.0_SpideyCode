package `in`.iot.spidey_code.view.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.iot.spidey_code.ui.theme.SpiderBorderBlack
import `in`.iot.spidey_code.ui.theme.SpiderOnPrimaryContainer
import `in`.iot.spidey_code.ui.theme.SpiderOnSurface
import `in`.iot.spidey_code.ui.theme.SpiderPrimaryContainer
import `in`.iot.spidey_code.ui.theme.SpiderSurface

/**
 * Single review action (icon + label) rendered as a brutalist circular button.
 * All three review actions share this exact shape/sizing so the row reads as
 * one deliberate set rather than mismatched controls; [isPrimary] highlights
 * the emphasized action (Share) via color only, keeping the footprint equal.
 */
@Composable
fun ReviewActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false
) {
    val bg = if (isPrimary) SpiderPrimaryContainer else SpiderSurface
    val onBg = if (isPrimary) SpiderOnPrimaryContainer else SpiderOnSurface

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BrutalistBox(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            bg = bg,
            borderColor = SpiderBorderBlack,
            borderWidth = 4.dp,
            shadowOffset = 4.dp,
            contentAlignment = Alignment.Center,
            onClick = onClick
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = onBg,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = SpiderOnSurface,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )
    }
}
