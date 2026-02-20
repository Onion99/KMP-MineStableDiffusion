package org.onion.diffusion.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import minediffusion.composeapp.generated.resources.Res
import minediffusion.composeapp.generated.resources.settings_advanced_subtitle
import minediffusion.composeapp.generated.resources.settings_advanced_title
import minediffusion.composeapp.generated.resources.settings_back
import minediffusion.composeapp.generated.resources.settings_cfg_scale
import minediffusion.composeapp.generated.resources.settings_cfg_scale_description
import minediffusion.composeapp.generated.resources.settings_current_configuration
import minediffusion.composeapp.generated.resources.settings_flash_attn
import minediffusion.composeapp.generated.resources.settings_flash_attn_desc
import minediffusion.composeapp.generated.resources.settings_generation_quality
import minediffusion.composeapp.generated.resources.settings_generation_quality_subtitle
import minediffusion.composeapp.generated.resources.settings_height
import minediffusion.composeapp.generated.resources.settings_image_dimensions
import minediffusion.composeapp.generated.resources.settings_image_dimensions_subtitle
import minediffusion.composeapp.generated.resources.settings_preset_landscape
import minediffusion.composeapp.generated.resources.settings_preset_portrait
import minediffusion.composeapp.generated.resources.settings_preset_square
import minediffusion.composeapp.generated.resources.settings_quant_default
import minediffusion.composeapp.generated.resources.settings_quantization
import minediffusion.composeapp.generated.resources.settings_quantization_desc
import minediffusion.composeapp.generated.resources.settings_quick_presets
import minediffusion.composeapp.generated.resources.settings_steps
import minediffusion.composeapp.generated.resources.settings_steps_description
import minediffusion.composeapp.generated.resources.settings_subtitle
import minediffusion.composeapp.generated.resources.settings_title
import minediffusion.composeapp.generated.resources.settings_width
import minediffusion.composeapp.generated.resources.settings_lora_title
import minediffusion.composeapp.generated.resources.settings_lora_subtitle
import minediffusion.composeapp.generated.resources.settings_lora_add
import minediffusion.composeapp.generated.resources.settings_lora_strength
import minediffusion.composeapp.generated.resources.settings_lora_remove
import minediffusion.composeapp.generated.resources.settings_lora_files
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.launch
import com.onion.model.LoraConfig
import org.koin.compose.koinInject
import org.onion.diffusion.ui.navigation.route.MainRoute
import org.onion.diffusion.ui.navigation.route.RootRoute
import org.onion.diffusion.viewmodel.ChatViewModel
import kotlin.math.roundToInt

fun NavGraphBuilder.settingScreen(
    onBackClick: () -> Unit = {},
) {
    composable(RootRoute.SettingRoute.name) {
        SettingScreen(
            onBackClick = onBackClick,
        )
    }
}

@Composable
fun SettingScreen(
    onBackClick: () -> Unit = {},
) {
    val chatViewModel = koinInject<ChatViewModel>()
    
    // Direct access to mutableStateOf properties (singleton ViewModel)
    val currentWidth by chatViewModel.imageWidth
    val currentHeight by chatViewModel.imageHeight
    val currentSteps by chatViewModel.generationSteps
    val currentCfg by chatViewModel.cfgScale

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Header with back button
            SettingsHeader(onBackClick = onBackClick)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Image Dimensions Section
            SettingsSectionCard(
                title = stringResource(Res.string.settings_image_dimensions),
                subtitle = stringResource(Res.string.settings_image_dimensions_subtitle),
                icon = Icons.Default.AspectRatio
            ) {
                // Width Selection
                DimensionSelector(
                    label = stringResource(Res.string.settings_width),
                    currentValue = currentWidth,
                    options = listOf(128, 256, 512, 768, 1024),
                    onValueSelected = { value ->
                        chatViewModel.imageWidth.value = value
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Height Selection
                DimensionSelector(
                    label = stringResource(Res.string.settings_height),
                    currentValue = currentHeight,
                    options = listOf(128, 256, 512, 768, 1024),
                    onValueSelected = { value ->
                        chatViewModel.imageHeight.value = value
                    }
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Quick Presets
                QuickPresetsRow(
                    onPresetSelected = { width, height ->
                        chatViewModel.imageWidth.value = width
                        chatViewModel.imageHeight.value = height
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Generation Settings Section
            SettingsSectionCard(
                title = stringResource(Res.string.settings_generation_quality),
                subtitle = stringResource(Res.string.settings_generation_quality_subtitle),
                icon = Icons.Default.Tune
            ) {
                // Steps Slider
                SliderSetting(
                    label = stringResource(Res.string.settings_steps),
                    value = currentSteps.toFloat(),
                    valueRange = 1f..50f,
                    steps = 49,
                    valueDisplay = currentSteps.toString(),
                    description = stringResource(Res.string.settings_steps_description),
                    onValueChange = { value ->
                        chatViewModel.generationSteps.value = value.toInt()
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // CFG Scale Slider
                SliderSetting(
                    label = stringResource(Res.string.settings_cfg_scale),
                    value = currentCfg,
                    valueRange = 1f..15f,
                    steps = 0,
                    valueDisplay = ((currentCfg * 10).roundToInt() / 10.0).toString(),
                    description = stringResource(Res.string.settings_cfg_scale_description),
                    onValueChange = { value ->
                        chatViewModel.cfgScale.value = value
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // LoRA Management Section
            LoraManagementSection(chatViewModel = chatViewModel)
            
            Spacer(modifier = Modifier.height(20.dp))

            
            // Current Settings Preview Card
            CurrentSettingsPreview(
                width = currentWidth,
                height = currentHeight,
                steps = currentSteps,
                cfg = currentCfg
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Glassmorphism back button
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(8.dp, CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.settings_back),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = stringResource(Res.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(Res.string.settings_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ... SettingsSectionCard ... 
// (The rest of the file needs minimal changes, mostly just function calls inside SettingsSectionCard being updated above)
// Wait, I need to make sure I don't delete helper functions if I'm replacing a huge block.
// The instruction above replaces from line 49 to 242.
// I will also provide the Updates for QuickPresetsRow and CurrentSettingsPreview separately or in one go if I include them.
// Let's do a multi_replace for safer editing.


@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DimensionSelector(
    label: String,
    currentValue: Int,
    options: List<Int>,
    onValueSelected: (Int) -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { value ->
                DimensionChip(
                    value = value,
                    isSelected = value == currentValue,
                    onClick = { onValueSelected(value) }
                )
            }
        }
    }
}

@Composable
private fun DimensionChip(
    value: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "chip_bg"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) 
            MaterialTheme.colorScheme.primary 
        else 
            Color.Transparent,
        animationSpec = tween(200),
        label = "chip_border"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(150),
        label = "chip_scale"
    )
    
    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) 
                MaterialTheme.colorScheme.onPrimaryContainer 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickPresetsRow(
    onPresetSelected: (width: Int, height: Int) -> Unit
) {
    Column {
        Text(
            text = stringResource(Res.string.settings_quick_presets),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetButton(
                label = stringResource(Res.string.settings_preset_portrait),
                ratio = "768×1024",
                modifier = Modifier.weight(1f),
                onClick = { onPresetSelected(768, 1024) }
            )
            PresetButton(
                label = stringResource(Res.string.settings_preset_landscape),
                ratio = "1024×768",
                modifier = Modifier.weight(1f),
                onClick = { onPresetSelected(1024, 768) }
            )
            PresetButton(
                label = stringResource(Res.string.settings_preset_square),
                ratio = "512×512",
                modifier = Modifier.weight(1f),
                onClick = { onPresetSelected(512, 512) }
            )
        }
    }
}

@Composable
private fun PresetButton(
    label: String,
    ratio: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = ratio,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueDisplay: String,
    description: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Value badge
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = valueDisplay,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun CurrentSettingsPreview(
    width: Int,
    height: Int,
    steps: Int,
    cfg: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(28.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.settings_current_configuration),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${width}×${height} • $steps steps • CFG ${((cfg * 10).roundToInt() / 10.0)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LoraManagementSection(
    chatViewModel: ChatViewModel
) {
    val scope = rememberCoroutineScope()

    SettingsSectionCard(
        title = stringResource(Res.string.settings_lora_title),
        subtitle = stringResource(Res.string.settings_lora_subtitle),
        icon = Icons.Default.Extension
    ) {
        // List of added LoRAs
        if (chatViewModel.loraList.isEmpty()) {
            Text(
                text = "No LoRA models added",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            chatViewModel.loraList.forEach { lora ->
                LoraItemRow(
                    loraConfig = lora,
                    onRemove = { chatViewModel.removeLora(lora) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Add LoRA Button
        Button(
            onClick = {
                scope.launch {
                    val path = chatViewModel.selectLoraFile()
                    if (path.isNotBlank()) {
                        chatViewModel.addLora(path)
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(Res.string.settings_lora_add))
        }
    }
}

@Composable
private fun LoraItemRow(
    loraConfig: LoraConfig,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = loraConfig.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = loraConfig.path,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                
                Switch(
                    checked = loraConfig.isEnabled,
                    onCheckedChange = { loraConfig.isEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.scale(0.8f)
                )
                
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Delete, 
                        contentDescription = stringResource(Res.string.settings_lora_remove),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            if (loraConfig.isEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                SliderSetting(
                    label = stringResource(Res.string.settings_lora_strength).replace("%s", ""),
                    value = loraConfig.strength,
                    valueRange = 0f..2f,
                    steps = 0,
                    valueDisplay = ((loraConfig.strength * 10).roundToInt() / 10.0).toString(),
                    description = "",
                    onValueChange = { loraConfig.strength = it }
                )
            }
        }
    }
}