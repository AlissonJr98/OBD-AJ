package com.aj.obd.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aj.obd.ui.theme.AmberGlow
import com.aj.obd.ui.theme.RacingRed
import com.aj.obd.ui.theme.SurfaceVariantDark
import com.aj.obd.ui.theme.TelemetryGreen

@Composable
fun ShiftLightBar(
    rpm: Float,
    shiftRpm: Float = 5300f,
    modifier: Modifier = Modifier
) {
    val numLeds = 8
    val startRpm = shiftRpm * 0.70f // Começa a acender a 70% do corte (~3.700 RPM)
    val step = (shiftRpm - startRpm) / numLeds

    val isShiftNow = rpm >= shiftRpm

    // Efeito de piscar contínuo no momento da troca de marcha (corte)
    val infiniteTransition = rememberInfiniteTransition(label = "shift_flash")
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(120),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flash_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariantDark.copy(alpha = 0.6f))
            .then(
                if (isShiftNow) Modifier.border(2.dp, RacingRed.copy(alpha = flashAlpha), RoundedCornerShape(8.dp))
                else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isShiftNow) {
            Text(
                text = "⚡ TROQUE A MARCHA! ⚡",
                color = RacingRed.copy(alpha = flashAlpha),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (i in 0 until numLeds) {
                    val ledTrigger = startRpm + (i * step)
                    val isLit = rpm >= ledTrigger

                    val ledColor = when (i) {
                        0, 1, 2 -> TelemetryGreen
                        3, 4, 5 -> AmberGlow
                        else -> RacingRed
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isLit) ledColor else Color.Gray.copy(alpha = 0.2f)
                            )
                    )
                }
            }
        }
    }
}
