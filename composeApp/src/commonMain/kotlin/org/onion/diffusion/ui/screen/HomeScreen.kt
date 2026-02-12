package org.onion.diffusion.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.onion.theme.state.ContentType
import ui.theme.AppTheme
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import coil3.compose.AsyncImage
import com.onion.model.ChatMessage
import com.onion.theme.style.MediumOutlinedTextField
import com.onion.theme.style.MediumText
import com.onion.theme.style.Text
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.DotLottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import minediffusion.composeapp.generated.resources.Res
import minediffusion.composeapp.generated.resources.ai_avatar
import minediffusion.composeapp.generated.resources.ai_image
import minediffusion.composeapp.generated.resources.ask_anything_placeholder
import minediffusion.composeapp.generated.resources.attachment
import minediffusion.composeapp.generated.resources.clear
import minediffusion.composeapp.generated.resources.creating
import minediffusion.composeapp.generated.resources.error_no_interrupt_api
import minediffusion.composeapp.generated.resources.error_select_correct_llm_model
import minediffusion.composeapp.generated.resources.feature_not_available
import minediffusion.composeapp.generated.resources.ic_avatar_sytem
import minediffusion.composeapp.generated.resources.ic_avatar_user
import minediffusion.composeapp.generated.resources.loading
import minediffusion.composeapp.generated.resources.scroll_to_bottom
import minediffusion.composeapp.generated.resources.select
import minediffusion.composeapp.generated.resources.select_llm_model_title
import minediffusion.composeapp.generated.resources.send_message
import minediffusion.composeapp.generated.resources.stop_generation
import minediffusion.composeapp.generated.resources.user_avatar
import minediffusion.composeapp.generated.resources.user_image
import minediffusion.composeapp.generated.resources.save_image
import minediffusion.composeapp.generated.resources.image_saved
import minediffusion.composeapp.generated.resources.image_save_failed
import org.onion.diffusion.native.DiffusionLoader
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.onion.diffusion.ui.navigation.route.MainRoute
import org.onion.diffusion.utils.Animations
import org.onion.diffusion.viewmodel.ChatViewModel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


fun NavGraphBuilder.homeScreen(onSettingsClick: () -> Unit = {},onAdvancedSettingsClick: () -> Unit = {}){
    composable(/*DetailRoute.Home.name*/MainRoute.HomeRoute.name) {
        HomeScreen(onSettingsClick,onAdvancedSettingsClick)
    }
}

@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit = {},onAdvancedSettingsClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        // koinInject() 只是单纯地从 Koin 容器中取出这个 Singleton 实例，而不将其绑定到屏幕的生命周期。这样，即使你在屏幕间导航，ChatViewModel 的 Scope 也会一直保持活跃
        val chatViewModel = koinInject<ChatViewModel>()
        val chatMessages = chatViewModel.currentChatMessages
        var text by remember { mutableStateOf("") }
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        val snackbarHostState = remember { SnackbarHostState() }
        var showFileDialog by remember { mutableStateOf(chatViewModel.diffusionModelPath.value.isEmpty() || chatViewModel.loadingModelState.value == 0) }
        val coroutineScope = rememberCoroutineScope()
        coroutineScope.launch {
            chatViewModel.loadingModelState.collect { state ->
                if(state == 2) showFileDialog = false
            }
        }
        LLMFileSelectTipDialog(
            showDialog = showFileDialog,
            selectAction = {
                coroutineScope.launch(Dispatchers.Default) {
                    if(chatViewModel.loadingModelState.value == 1) return@launch
                    if(chatViewModel.diffusionModelPath.value.isBlank()){
                        snackbarHostState.showSnackbar(getString(Res.string.error_select_correct_llm_model))
                    }else {
                        chatViewModel.loadingModelState.emit(1)
                        chatViewModel.initLLM()
                    }
                }
            },
            settingClick = onAdvancedSettingsClick,
        )
        ChatMessagesList(chatMessages = chatMessages,snackbarHostState)
        
        // Settings Entry Button - Premium floating design
        SettingsEntryButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 16.dp)
        )
        
        AskAnythingField(
            modifier = Modifier.align(Alignment.BottomStart),
            onAttachClick = {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(	getString(Res.string.feature_not_available))
                }
            },
            onSendClick = {
                if (chatViewModel.isGenerating.value) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(getString(Res.string.error_no_interrupt_api))
                    }
                    //chatViewModel.stopGeneration()
                } else {
                    if (text.isNotEmpty()) {
                        chatViewModel.sendMessage(text)
                        text = ""
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                }
            },
            text = text,
            onTextChange = { text = it },
            isGenerating = chatViewModel.isGenerating.value
        )

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
            snackbar = { snackbarData ->
                Snackbar(
                    snackbarData,
                    modifier = Modifier
                        .widthIn(min = 100.dp, max = 300.dp)
                        .heightIn(min = 40.dp, max = 120.dp)
                        .padding(8.dp),
                    shape = RoundedCornerShape(26.dp),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        )
    }
}

/**
 * Premium styled settings entry button with glassmorphism effect and rotation animation.
 * Features a smooth rotation on press and elegant shadow effects.
 */
@Composable
private fun SettingsEntryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "settings_rotation")
    
    // Subtle continuous rotation animation for visual interest
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gear_rotation"
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                    },
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun ChatMessagesList(chatMessages: List<ChatMessage>,snackbarHostState: SnackbarHostState) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val chatViewModel = koinInject<ChatViewModel>()

    // Track scroll position to show/hide button
    val showScrollButton by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val totalItems = chatMessages.size
            if (totalItems == 0) false else {
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleItem < totalItems - 1
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 70.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chatMessages) { message ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = if (message.isUser) 64.dp else 16.dp,
                            end = if (message.isUser) 16.dp else 64.dp,
                            top = 4.dp,
                            bottom = 4.dp
                        )
                ) {
                    ChatBubble(
                        message = message.message,
                        image = message.image,
                        isUser = message.isUser,
                        onSaveImage = { imageData ->
                            coroutineScope.launch(Dispatchers.Default) {
                                val fileName = "diffusion_${Clock.System.now().toEpochMilliseconds()}.png"
                                val success = chatViewModel.diffusionLoader.saveImage(imageData, fileName)
                                val msg = if (success) getString(Res.string.image_saved) else getString(Res.string.image_save_failed)
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    )
                }
            }
        }

        ScrollToBottomButton(
            onClick = {
                coroutineScope.launch {
                    lazyListState.animateScrollToItem(chatMessages.lastIndex)
                }
            },
            visibility = showScrollButton,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 16.dp)
        )
    }

    // Existing auto-scroll logic
    val lastMessageLength by remember(chatMessages.size) {
        derivedStateOf { chatMessages.lastOrNull()?.message?.length ?: 0 }
    }

    LaunchedEffect(chatMessages.size, lastMessageLength) {
        if (chatMessages.isNotEmpty()) {
            val lastIndex = chatMessages.lastIndex
            val scrollThreshold = 3
            val layoutInfo = lazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if ((visibleItems.lastOrNull()?.index ?: 0) >= lastIndex - scrollThreshold) {
                lazyListState.scrollToItem(lastIndex)
            }
        }
    }
}

// Extracted scroll-to-bottom button component
@Composable
private fun ScrollToBottomButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    visibility: Boolean
) {
    AnimatedVisibility(
        visible = visibility,
        enter = Animations.slideFadeIn(),
        exit = Animations.slideFadeOut(),
        modifier = modifier
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                )
                .shadow(6.dp, CircleShape)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardDoubleArrowDown,
                contentDescription = stringResource(Res.string.scroll_to_bottom),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun AskAnythingField(
    text: String,
    isGenerating: Boolean,
    modifier: Modifier = Modifier,
    onAttachClick: () -> Unit,
    onSendClick: () -> Unit,
    onTextChange: (String) -> Unit
) {
    Box(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 8.dp,
                shape = MaterialTheme.shapes.extraLarge,
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.extraLarge
            )
    ) {
        MediumOutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 56.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            shape = MaterialTheme.shapes.extraLarge,
            placeholder = {
                MediumText(
                    text = stringResource(Res.string.ask_anything_placeholder),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            leadingIcon = {
                AttachIcon(onAttachClick = onAttachClick)
            },
            trailingIcon = {
                if (text.isNotEmpty()) {
                    ClearIcon(
                        show = text.isNotEmpty(),
                        onClick = { onTextChange("") }
                    )
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent
            )
        )

        SendStopButton(
            isGenerating = isGenerating,
            modifier = Modifier
                .padding(end = 12.dp)
                .size(40.dp)
                .align(Alignment.CenterEnd),
            onClick = onSendClick,
        )
    }
}

@Composable
private fun AttachIcon(
    onAttachClick: () -> Unit
) = IconButton(onAttachClick) {
    Icon(
        imageVector = Icons.Filled.AttachFile,
        contentDescription = stringResource(Res.string.attachment),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
private fun ClearIcon(
    show: Boolean,
    onClick: () -> Unit
) {
    if (show) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.clear),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SendStopButton(
    isGenerating: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = if (isGenerating) Icons.Filled.Stop
            else Icons.AutoMirrored.Filled.Send,
            contentDescription = if (isGenerating) 	stringResource(Res.string.stop_generation) else stringResource(Res.string.send_message),
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ChatBubble(
    message: String,
    image: ByteArray? = null,
    isUser: Boolean,
    onSaveImage: ((ByteArray) -> Unit)? = null
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (isUser)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(
                        topStart = if (isUser) 16.dp else 2.dp,
                        topEnd = if (isUser) 2.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ChatBubbleIcon(isUser = isUser)

            ChatBubbleMessage(
                message = message,
                image = image,
                isUser = isUser,
                onSaveImage = onSaveImage
            )
        }
    }
}

@Composable
private fun ChatBubbleIcon(isUser: Boolean) {
    if (isUser) {
        UserIcon()
    } else {
        AiProviderIcon()
    }
}

@Composable
private fun UserIcon() {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                shape = CircleShape
            )
            .padding(6.dp)
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_avatar_user),
            contentDescription = stringResource(Res.string.user_avatar),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun AiProviderIcon() {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                shape = CircleShape
            )
            .padding(6.dp)
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_avatar_sytem),
            contentDescription = stringResource(Res.string.ai_avatar),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ChatBubbleMessage(
    message: String,
    image: ByteArray? = null,
    isUser: Boolean,
    onSaveImage: ((ByteArray) -> Unit)? = null
) {
    if (isUser) {
        UserMessage(message = message, image = image)
    } else {
        AiMessage(message = message, image = image, onSaveImage = onSaveImage)
    }
}

@Composable
private fun UserMessage(message: String, image: ByteArray? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (image != null) {
            AsyncImage(
                model = image,
                contentDescription = stringResource(Res.string.user_image),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .padding(bottom = 4.dp)
            )
        }
        if (message.isNotEmpty()) {
            MediumText(
                text = message,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .padding(top = 4.dp, end = 8.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AiMessage(
    message: String,
    image: ByteArray? = null,
    onSaveImage: ((ByteArray) -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 当消息为空且无图片时，显示创意加载动画
        if (message.isEmpty() && image == null) {
            MagicLoadingAnimation()
        } else {
            // 正常显示逻辑
            if (image != null) {
                Box(modifier = Modifier.wrapContentSize()) {
                    AsyncImage(
                        model = image,
                        contentDescription = stringResource(Res.string.ai_image),
                        alignment = Alignment.Center,
                        contentScale = ContentScale.Inside,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .wrapContentSize()
                            .padding(bottom = 4.dp)
                    )
                    // Save button overlay
                    IconButton(
                        onClick = { onSaveImage?.invoke(image) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SaveAlt,
                            contentDescription = stringResource(Res.string.save_image),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            if (message.isNotEmpty()) {
                MediumText(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(top = 4.dp, end = 8.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun LLMFileSelectTipDialog(
    showDialog: Boolean,
    selectAction: () -> Unit,settingClick: () -> Unit
) {
    if (showDialog) {
        // koinInject唯一的“副作用”是 onCleared() 永远不会被调用
        val chatViewModel = koinInject<ChatViewModel>()
        val loadingState by chatViewModel.loadingModelState.collectAsState(0)
        val vaePath by chatViewModel.vaePath
        val llmPath by chatViewModel.llmPath
        val clipLPath by chatViewModel.clipLPath
        val clipGPath by chatViewModel.clipGPath
        val t5xxlPath by chatViewModel.t5xxlPath
        val diffusionPath by chatViewModel.diffusionModelPath
        val isDiffusionModelLoading by chatViewModel.isDiffusionModelLoading
        val isVaeModelLoading by chatViewModel.isVaeModelLoading
        val isLlmModelLoading by chatViewModel.isLlmModelLoading
        val isClipLModelLoading by chatViewModel.isClipLModelLoading
        val isClipGModelLoading by chatViewModel.isClipGModelLoading
        val isT5xxlModelLoading by chatViewModel.isT5xxlModelLoading
        val coroutineScope = rememberCoroutineScope()

        val infiniteTransition = rememberInfiniteTransition(label = "settings_rotation")

        // Subtle continuous rotation animation for visual interest
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 20000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "gear_rotation"
        )
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .fillMaxHeight(0.85f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Scrollable Content
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val composition by rememberLottieComposition {
                                    LottieCompositionSpec.DotLottie(
                                        Res.readBytes("files/anim_ai_file_.lottie")
                                    )
                                }
                                Image(
                                    painter = rememberLottiePainter(
                                        composition = composition,
                                        iterations = Compottie.IterateForever
                                    ),
                                    contentDescription = "File animation",
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(120.dp)
                                        .padding(bottom = 8.dp)
                                )

                                IconButton(
                                    onClick = settingClick,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = "Settings",
                                        modifier = Modifier
                                            .size(24.dp)
                                            .graphicsLayer {
                                                rotationZ = rotation
                                            }
                                    )
                                }
                            }
                        }

                        // Required Section
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                MediumText(
                                    text = "Core Model",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Required for image generation",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        item {
                            FileSelectionCard(
                                title = "Diffusion Model",
                                subtitle = "Required",
                                selectedPath = diffusionPath,
                                isRequired = true,
                                isLoading = isDiffusionModelLoading,
                                gradientColors = listOf(
                                    Color(0xFFFFA726),
                                    Color(0xFF81C784)
                                ),
                                onSelectClick = {
                                    coroutineScope.launch(Dispatchers.Default) {
                                        if(loadingState == 1) return@launch
                                        chatViewModel.selectDiffusionModelFile()
                                    }
                                }
                            )
                        }

                        // Optional Section
                        item {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                MediumText(
                                    text = "Optional Modules",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Enhance quality and capabilities",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        item {
                            FileSelectionCard(
                                title = "VAE Model",
                                subtitle = "Optional",
                                selectedPath = vaePath,
                                isRequired = false,
                                isLoading = isVaeModelLoading,
                                gradientColors = listOf(
                                    Color(0xFF9C27B0),
                                    Color(0xFF2196F3)
                                ),
                                onSelectClick = {
                                    coroutineScope.launch(Dispatchers.Default) {
                                        if(loadingState == 1) return@launch
                                        chatViewModel.selectVaeFile()
                                    }
                                }
                            )
                        }

                        item {
                           FileSelectionCard(
                                title = "LLM Model",
                                subtitle = "Optional",
                                selectedPath = llmPath,
                                isRequired = false,
                                isLoading = isLlmModelLoading,
                                gradientColors = listOf(
                                    Color(0xFF00BCD4),
                                    Color(0xFF00E5FF)
                                ),
                                onSelectClick = {
                                    coroutineScope.launch(Dispatchers.Default) {
                                        if(loadingState == 1) return@launch
                                        chatViewModel.selectLlmFile()
                                    }
                                }
                            )
                        }

                        item {
                            FileSelectionCard(
                                title = "CLIP-L Model",
                                subtitle = "Optional",
                                selectedPath = clipLPath,
                                isRequired = false,
                                isLoading = isClipLModelLoading,
                                gradientColors = listOf(
                                    Color(0xFFE91E63),
                                    Color(0xFFFF6090)
                                ),
                                onSelectClick = {
                                    coroutineScope.launch(Dispatchers.Default) {
                                        if(loadingState == 1) return@launch
                                        chatViewModel.selectClipLFile()
                                    }
                                }
                            )
                        }

                        item {
                            FileSelectionCard(
                                title = "CLIP-G Model",
                                subtitle = "Optional",
                                selectedPath = clipGPath,
                                isRequired = false,
                                isLoading = isClipGModelLoading,
                                gradientColors = listOf(
                                    Color(0xFF4CAF50),
                                    Color(0xFF8BC34A)
                                ),
                                onSelectClick = {
                                    coroutineScope.launch(Dispatchers.Default) {
                                        if(loadingState == 1) return@launch
                                        chatViewModel.selectClipGFile()
                                    }
                                }
                            )
                        }

                        item {
                            FileSelectionCard(
                                title = "T5XXL Model",
                                subtitle = "Optional",
                                selectedPath = t5xxlPath,
                                isRequired = false,
                                isLoading = isT5xxlModelLoading,
                                gradientColors = listOf(
                                    Color(0xFFFF9800),
                                    Color(0xFFFFB74D)
                                ),
                                onSelectClick = {
                                    coroutineScope.launch(Dispatchers.Default) {
                                        if(loadingState == 1) return@launch
                                        chatViewModel.selectT5xxlFile()
                                    }
                                }
                            )
                        }
                    }

                    // Footer Button (Fixed)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Button(
                            onClick = selectAction,
                            enabled = diffusionPath.isNotEmpty() && loadingState != 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(
                                    brush = if (diffusionPath.isNotEmpty() && loadingState != 1) {
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFFFFA726),
                                                Color(0xFF81C784)
                                            )
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Gray.copy(alpha = 0.3f),
                                                Color.Gray.copy(alpha = 0.3f)
                                            )
                                        )
                                    }
                                ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            )
                        ) {
                            MediumText(
                                text = if(loadingState == 1) stringResource(Res.string.loading) 
                                       else stringResource(Res.string.select),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (diffusionPath.isNotEmpty() && loadingState != 1) 
                                    Color.White else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileSelectionCard(
    title: String,
    subtitle: String,
    selectedPath: String,
    isRequired: Boolean,
    isLoading: Boolean,
    gradientColors: List<Color>,
    onSelectClick: () -> Unit
) {
    val isSelected = selectedPath.isNotEmpty()
    val contentType = AppTheme.contentType
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (isSelected) {
            BorderStroke(
                width = 2.dp,
                brush = Brush.horizontalGradient(gradientColors)
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side: Icon and Info
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon with gradient background
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = Brush.linearGradient(gradientColors),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.CheckCircle 
                                      else Icons.Filled.InsertDriveFile,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                // Title and path
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (contentType == ContentType.Single) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                             MediumText(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            // Badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isRequired) 
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                        else 
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = subtitle,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isRequired) 
                                        MaterialTheme.colorScheme.error
                                    else 
                                        MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MediumText(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            // Badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isRequired) 
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                        else 
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = subtitle,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isRequired) 
                                        MaterialTheme.colorScheme.error
                                    else 
                                        MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    // File path or placeholder
                    MediumText(
                        text = if (isSelected) {
                            selectedPath.split("/", "\\").lastOrNull() ?: selectedPath
                        } else {
                            "No file selected"
                        },
                        fontSize = 12.sp,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            
            // Select button
            Button(
                onClick = onSelectClick,
                enabled = !isLoading,
                modifier = Modifier
                    .height(36.dp)
                    .widthIn(min = 70.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    MediumText(
                        text = if (isSelected) "Change" else "Select",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}


@Composable
private fun MagicLoadingAnimation() {
    val infiniteTransition = rememberInfiniteTransition()

    // 主圆环呼吸动画 - 大小变化
    val primaryScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // 主圆环透明度 - 配合呼吸
    val primaryAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // 光点环绕角度
    val orbitRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(20.dp))
            //.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ,
        contentAlignment = Alignment.Center
    ) {
        // 主呼吸圆环 - 两层叠加
        Box(
            modifier = Modifier.size(80.dp).padding(bottom = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            // 外层光晕
            Box(
                modifier = Modifier
                    .size(80.dp * primaryScale)
                    .graphicsLayer {
                        alpha = primaryAlpha * 0.5f
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // 内层核心圆
            Box(
                modifier = Modifier
                    .size(48.dp * primaryScale)
                    .graphicsLayer {
                        alpha = primaryAlpha + 0.3f
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // 环绕的光点 (6个)
            repeat(6) { index ->
                val angle = orbitRotation + (index * 60f)
                val angleRad = angle * PI / 180
                val radius = 40f // 轨道半径

                val offsetX = (radius * cos(angleRad)).toFloat()
                val offsetY = (radius * sin(angleRad)).toFloat()

                // 光点透明度 - 创造深度感
                val dotAlpha = 0.4f + 0.3f * abs(sin((angle + index * 30) * PI / 180))

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .offset(x = offsetX.dp, y = offsetY.dp)
                        .graphicsLayer {
                            alpha = dotAlpha.toFloat()
                        }
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
            }
        }

        // 渐进文字 - "Creating···"
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            MediumText(
                text = stringResource(Res.string.creating),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Normal
            )

            // 三个点依次淡入淡出
            repeat(3) { index ->
                val dotAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 0.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 1200,
                            delayMillis = index * 300,
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    )
                )

                MediumText(
                    text = "·",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dotAlpha),
                    modifier = Modifier.padding(start = 1.dp)
                )
            }
        }
    }
}