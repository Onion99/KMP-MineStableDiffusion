package org.onion.diffusion.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import minediffusion.composeapp.generated.resources.Res
import minediffusion.composeapp.generated.resources.settings_advanced_subtitle
import minediffusion.composeapp.generated.resources.settings_advanced_title
import minediffusion.composeapp.generated.resources.settings_back
import minediffusion.composeapp.generated.resources.settings_flash_attn
import minediffusion.composeapp.generated.resources.settings_flash_attn_desc
import minediffusion.composeapp.generated.resources.settings_flash_attn_info
import minediffusion.composeapp.generated.resources.settings_offload_to_cpu
import minediffusion.composeapp.generated.resources.settings_offload_to_cpu_desc
import minediffusion.composeapp.generated.resources.settings_keep_clip_on_cpu
import minediffusion.composeapp.generated.resources.settings_keep_clip_on_cpu_desc
import minediffusion.composeapp.generated.resources.settings_keep_vae_on_cpu
import minediffusion.composeapp.generated.resources.settings_keep_vae_on_cpu_desc
import minediffusion.composeapp.generated.resources.settings_quant_auto
import minediffusion.composeapp.generated.resources.settings_quant_default
import minediffusion.composeapp.generated.resources.settings_quant_info
import minediffusion.composeapp.generated.resources.settings_quantization
import minediffusion.composeapp.generated.resources.settings_quantization_desc
import minediffusion.composeapp.generated.resources.settings_quantization_warning
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.onion.diffusion.ui.navigation.route.RootRoute
import org.onion.diffusion.viewmodel.ChatViewModel
import ui.theme.AppTheme
import kotlin.math.cos
import kotlin.math.sin

fun NavGraphBuilder.advancedSettingScreen(
    onBackClick: () -> Unit = {}
) {
    composable(RootRoute.AdvancedSettingRoute.name) {
        AdvancedSettingScreen(onBackClick = onBackClick)
    }
}

@Composable
fun AdvancedSettingScreen(
    onBackClick: () -> Unit = {}
) {
    val chatViewModel = koinInject<ChatViewModel>()
    
    // Ethereal Light Background with animated nebulas
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FF)) // Soft airy white-blue
    ) {
        EtherealBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Header
            AdvancedSettingsHeader(onBackClick)
            
            Spacer(modifier = Modifier.height(40.dp))
            
            NeonSectionHeader("PERFORMANCE CORE")
            
            Spacer(modifier = Modifier.height(16.dp))

            // Neon Glass Card 1
            NeonGlassCard {
                Column {
                    SettingsRow(
                        title = stringResource(Res.string.settings_flash_attn),
                        subtitle = stringResource(Res.string.settings_flash_attn_desc),
                    ) {
                        HolographicSwitch(
                            checked = chatViewModel.diffusionFlashAttn.value,
                            onCheckedChange = { chatViewModel.diffusionFlashAttn.value = it }
                        )
                    }
                    
                    // Flash Attention Info Box
                    if (chatViewModel.diffusionFlashAttn.value) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE8F5E9)) // Light green success background
                                .border(1.dp, Color(0xFF66BB6A), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.settings_flash_attn_info),
                                style = AppTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32), // Dark green text
                                lineHeight = 18.sp
                            )
                        }
                    }
                    
                    SettingsRow(
                        title = stringResource(Res.string.settings_offload_to_cpu),
                        subtitle = stringResource(Res.string.settings_offload_to_cpu_desc),
                    ) {
                        HolographicSwitch(
                            checked = chatViewModel.offloadToCpu.value,
                            onCheckedChange = { chatViewModel.offloadToCpu.value = it }
                        )
                    }
                    
                    SettingsRow(
                        title = stringResource(Res.string.settings_keep_clip_on_cpu),
                        subtitle = stringResource(Res.string.settings_keep_clip_on_cpu_desc),
                    ) {
                        HolographicSwitch(
                            checked = chatViewModel.keepClipOnCpu.value,
                            onCheckedChange = { chatViewModel.keepClipOnCpu.value = it }
                        )
                    }
                    
                    SettingsRow(
                        title = stringResource(Res.string.settings_keep_vae_on_cpu),
                        subtitle = stringResource(Res.string.settings_keep_vae_on_cpu_desc),
                    ) {
                        HolographicSwitch(
                            checked = chatViewModel.keepVaeOnCpu.value,
                            onCheckedChange = { chatViewModel.keepVaeOnCpu.value = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            NeonSectionHeader("QUANTUM MATRIX")
            
            Spacer(modifier = Modifier.height(16.dp))

            // Neon Glass Card 2
            NeonGlassCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(Res.string.settings_quantization),
                        style = AppTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFF1D1B20) // Dark text
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(Res.string.settings_quantization_desc),
                        style = AppTheme.typography.bodySmall,
                        color = Color(0xFF666666) // Soft grey text
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Warning Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFF3E0)) // Warm warning background
                            .border(1.dp, Color(0xFFFFB74D), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.settings_quantization_warning),
                            style = AppTheme.typography.bodySmall,
                            color = Color(0xFFE65100), // Dark orange text
                            lineHeight = 18.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Info Box - Detailed explanation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE3F2FD)) // Light blue info background
                            .border(1.dp, Color(0xFF64B5F6), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.settings_quant_info),
                            style = AppTheme.typography.bodySmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            color = Color(0xFF1565C0), // Dark blue text
                            lineHeight = 20.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    val wtypes = listOf(
                        -1 to stringResource(Res.string.settings_quant_auto),
                        0 to "F32",
                        1 to "F16",
                        30 to "BF16",
                        8 to "Q8_0",
                        14 to "Q6_K",
                        13 to "Q5_K",
                        6 to "Q5_0",
                        12 to "Q4_K",
                        2 to "Q4_0",
                        11 to "Q3_K",
                        10 to "Q2_K"
                    )
                    
                     CyberGridSelection(
                        items = wtypes,
                        selectedIndex = wtypes.indexOfFirst { it.first == chatViewModel.wtype.value }.coerceAtLeast(0),
                        onItemSelected = { chatViewModel.wtype.value = wtypes[it].first }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun EtherealBackground() {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Animate drift for 3 distinct nebulas
    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * 3.14159f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart)
    )
    val offset2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * 3.14159f,
        animationSpec = infiniteRepeatable(tween(27000, easing = LinearEasing), RepeatMode.Restart)
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Nebula 1: Soft Cyan
        val x1 = center.x + cos(offset1) * size.width * 0.3f
        val y1 = center.y + sin(offset1) * size.height * 0.2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF00C2FF).copy(0.08f), Color.Transparent),
                center = Offset(x1, y1),
                radius = size.minDimension * 0.8f
            )
        )
        
        // Nebula 2: Soft Pink/Lavender
        val x2 = center.x + cos(offset2 + 2f) * size.width * 0.4f
        val y2 = center.y + sin(offset2 + 2f) * size.height * 0.3f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFD600FF).copy(0.06f), Color.Transparent),
                center = Offset(x2, y2),
                radius = size.minDimension * 0.9f
            )
        )
        
        // Static Sunny Top Glow
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFFFFF), Color.Transparent),
                startY = 0f,
                endY = size.height * 0.5f
            )
        )
    }
}

@Composable
private fun AdvancedSettingsHeader(onBackClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(1.dp, Brush.linearGradient(listOf(Color(0xFF00C2FF).copy(0.5f), Color(0xFFD600FF).copy(0.5f))), CircleShape)
                .background(Color.White)
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Color(0xFF1D1B20),
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = stringResource(Res.string.settings_advanced_title).uppercase(),
                style = AppTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = Color(0xFF1D1B20)
            )
            Text(
                text = stringResource(Res.string.settings_advanced_subtitle),
                style = AppTheme.typography.bodyMedium,
                color = Color(0xFF007A99) // Darker Cyan for readability
            )
        }
    }
}

@Composable
fun NeonSectionHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(4.dp, 16.dp)
                .background(Color(0xFF00C2FF), RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = AppTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            ),
            color = Color(0xFF666666)
        )
    }
}

@Composable
fun NeonGlassCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF00C2FF).copy(alpha = 0.2f),
                        Color(0xFFD600FF).copy(alpha = 0.05f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                RoundedCornerShape(24.dp)
            )
            .background(Color(0xFFFFFFFF).copy(alpha = 0.8f)) // High opacity white glass
    ) {
        content()
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    control: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                style = AppTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFF1D1B20)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = AppTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
        }
        control()
    }
}

@Composable
fun HolographicSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 4.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )
    val glowAlpha by animateFloatAsState(if (checked) 0.6f else 0f)

    Box(
        modifier = Modifier
            .size(52.dp, 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF0F0F0)) // Light grey track
            .border(1.5.dp, if(checked) Color(0xFF00C2FF).copy(0.5f) else Color(0xFFDDDDDD), RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) }
    ) {
        // Active Glow
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF00C2FF).copy(0.2f), Color.Transparent)
                    )
                )
                .graphicsLayer { alpha = glowAlpha }
        )
        
        // Thumb
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbOffset)
                .size(24.dp)
                .shadow(4.dp, CircleShape, spotColor = Color(0xFF00C2FF).copy(0.5f))
                .background(if(checked) Color(0xFF00C2FF) else Color.White, CircleShape)
        ) {
             // Inner reflection
             Box(modifier = Modifier.fillMaxSize().background(
                 Brush.radialGradient(listOf(Color.White.copy(0.9f), Color.Transparent)),
                 CircleShape
             ))
        }
    }
}

@Composable
fun CyberGridSelection(
    items: List<Pair<Int, String>>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    // Grid layout implementation
    val chunked = items.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        chunked.forEachIndexed { r, rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEachIndexed { c, item ->
                    val isSelected = items.indexOf(item) == selectedIndex
                    
                    val borderBrush = if (isSelected) {
                        Brush.linearGradient(listOf(Color(0xFF00C2FF), Color(0xFFD600FF)))
                    } else {
                        SolidColor(Color(0xFFDDDDDD))
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if(isSelected) Color(0xFF00C2FF).copy(0.1f) else Color.White.copy(0.5f))
                            .border(1.dp, borderBrush, RoundedCornerShape(12.dp))
                            .clickable { onItemSelected(items.indexOf(item)) },
                        contentAlignment = Alignment.Center
                    ) {
                         Text(
                            text = item.second,
                            style = AppTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if(isSelected) Color(0xFF007A99) else Color(0xFF666666) // Colored text when selected
                        )
                    }
                }
                if (rowItems.size < 3) {
                     repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
