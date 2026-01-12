package com.onion.theme.style

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import ui.theme.AppTheme

@Composable
fun Text(text: String,
               modifier: Modifier = Modifier,
               color: Color = Color.Unspecified,
               fontSize: TextUnit = TextUnit.Unspecified,
               fontStyle: FontStyle? = null,
               fontWeight: FontWeight? = null,
               fontFamily: FontFamily? = null,
               letterSpacing: TextUnit = TextUnit.Unspecified,
               textDecoration: TextDecoration? = null,
               textAlign: TextAlign? = null,
               lineHeight: TextUnit = TextUnit.Unspecified,
               overflow: TextOverflow = TextOverflow.Clip,
               softWrap: Boolean = true,
               maxLines: Int = Int.MAX_VALUE,
               minLines: Int = 1,
               onTextLayout: ((TextLayoutResult) -> Unit)? = null
){
    Text(text, modifier, color, fontSize, fontStyle, fontWeight, fontFamily, letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines, minLines, onTextLayout,
        AppTheme.typography.bodySmall)
}
@Composable
fun MediumText(text: String,
               modifier: Modifier = Modifier,
               color: Color = Color.Unspecified,
               fontSize: TextUnit = TextUnit.Unspecified,
               fontStyle: FontStyle? = null,
               fontWeight: FontWeight? = null,
               fontFamily: FontFamily? = null,
               letterSpacing: TextUnit = TextUnit.Unspecified,
               textDecoration: TextDecoration? = null,
               textAlign: TextAlign? = null,
               lineHeight: TextUnit = TextUnit.Unspecified,
               overflow: TextOverflow = TextOverflow.Clip,
               softWrap: Boolean = true,
               maxLines: Int = Int.MAX_VALUE,
               minLines: Int = 1,
               onTextLayout: ((TextLayoutResult) -> Unit)? = null
){
    Text(text, modifier, color, fontSize, fontStyle, fontWeight, fontFamily, letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines, minLines, onTextLayout,
        AppTheme.typography.bodyMedium)
}
@Composable
fun LargeText(text: String,
               modifier: Modifier = Modifier,
               color: Color = Color.Unspecified,
               fontSize: TextUnit = TextUnit.Unspecified,
               fontStyle: FontStyle? = null,
               fontWeight: FontWeight? = null,
               fontFamily: FontFamily? = null,
               letterSpacing: TextUnit = TextUnit.Unspecified,
               textDecoration: TextDecoration? = null,
               textAlign: TextAlign? = null,
               lineHeight: TextUnit = TextUnit.Unspecified,
               overflow: TextOverflow = TextOverflow.Clip,
               softWrap: Boolean = true,
               maxLines: Int = Int.MAX_VALUE,
               minLines: Int = 1,
               onTextLayout: ((TextLayoutResult) -> Unit)? = null
){
    Text(text, modifier, color, fontSize, fontStyle, fontWeight, fontFamily, letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines, minLines, onTextLayout,
        AppTheme.typography.bodyLarge)
}

@Composable
fun MediumOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
){
    OutlinedTextField(value, onValueChange, modifier, enabled, readOnly, AppTheme.typography.bodyMedium, label, placeholder, leadingIcon, trailingIcon, prefix, suffix, supportingText, isError, visualTransformation, keyboardOptions, keyboardActions, singleLine, maxLines, minLines, interactionSource, shape, colors)
}