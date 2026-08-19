package `in`.iot.spidey_code.view.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.iot.spidey_code.data.model.FilterType
import `in`.iot.spidey_code.data.model.frameAssetPath
import `in`.iot.spidey_code.data.model.thumbnailAssetPath
import `in`.iot.spidey_code.ui.theme.SpiderSurfaceContainer
import `in`.iot.spidey_code.ui.theme.SpideyRed

import `in`.iot.spidey_code.view.components.SpiderWalkAnimation

@Composable
fun FilterCard(
    filterType: FilterType,
    title: String,
    description: String? = null,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    isClickable: Boolean = true,
    previewBackgroundColor: Color? = null
) {
    val context = LocalContext.current
    val frameBitmap = remember(context, filterType) {
        filterType.thumbnailAssetPath?.let { path ->
            runCatching {
                context.assets.open(path).use { stream ->
                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                        inPremultiplied = true
                    }
                    BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    val icon: ImageVector = when (filterType) {
        FilterType.CLASSIC_MASK -> Icons.Default.Face
        FilterType.WEB_SHOOTER -> Icons.Default.Visibility
        FilterType.SPIDEY_SENSE -> Icons.Default.FlashOn
        FilterType.SPIDER_VERSE -> Icons.Default.AutoAwesome
        FilterType.EVENT_SQUAD -> Icons.Default.Groups
        FilterType.SPIDEY_PARTY -> Icons.Default.Celebration
        FilterType.GHOST_SPIDER -> Icons.Default.Nightlight
    }

    val innerBgColor = previewBackgroundColor ?: if (isSelected) {
        SpideyRed.copy(alpha = 0.15f)
    } else {
        Color.Black.copy(alpha = 0.4f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isClickable || isSelected) 1.0f else 0.55f)
            .then(
                if (isClickable) Modifier.clickable { onSelect() } else Modifier
            ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) SpideyRed else Color.White.copy(alpha = 0.15f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                SpiderSurfaceContainer.copy(alpha = 0.95f)
            } else {
                Color.Black.copy(alpha = 0.65f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Preview Image / Icon Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(innerBgColor),
                contentAlignment = Alignment.Center
            ) {
                if (frameBitmap != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // 1. Spider Walk Lottie Animation (slightly tiny & shifted towards the left area)
                        SpiderWalkAnimation(
                            modifier = Modifier
                                .fillMaxSize(0.68f)
                                .align(Alignment.CenterStart)
                                .offset(x = 12.dp)
                        )

                        // 2. Overlaid Frame Poster taking full max size of the card container
                        Image(
                            bitmap = frameBitmap,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) SpideyRed
                                else Color.White.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
