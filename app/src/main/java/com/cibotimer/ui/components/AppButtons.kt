package com.cibotimer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cibotimer.ui.theme.CiboStopRed
import com.cibotimer.ui.theme.FastingBlue
import com.cibotimer.ui.theme.FastingBlueDarkText
import com.cibotimer.ui.theme.SlateGray

/**
 * Pulsante primario standard con stile a pillola, sfondo FastingBlue e testo scuro ad alto contrasto.
 */
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .height(48.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = FastingBlue,
            contentColor = FastingBlueDarkText,
            disabledContainerColor = FastingBlue.copy(alpha = 0.25f),
            disabledContentColor = FastingBlueDarkText.copy(alpha = 0.50f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(color = Color.Unspecified),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Pulsante secondario standard con stile a pillola, sfondo trasparente e bordo FastingBlue.
 */
@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .height(48.dp),
        shape = RoundedCornerShape(50),
        border = BorderStroke(
            1.5.dp,
            if (enabled) FastingBlue else FastingBlue.copy(alpha = 0.35f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = FastingBlue,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = FastingBlue.copy(alpha = 0.50f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(color = Color.Unspecified),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Pulsante di avviso/pericolo con stile a pillola, sfondo tenue e contorno CiboStopRed
 * per azioni distruttive come il Reset dell'applicazione.
 */
@Composable
fun AppDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .height(48.dp),
        shape = RoundedCornerShape(50),
        border = BorderStroke(
            1.5.dp,
            if (enabled) CiboStopRed else CiboStopRed.copy(alpha = 0.35f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = CiboStopRed.copy(alpha = 0.12f),
            contentColor = CiboStopRed,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = CiboStopRed.copy(alpha = 0.50f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(color = Color.Unspecified),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Palette colori espliciti per gli Switch dell'applicazione.
 * Spento: contorno e pallino grigio-azzurro (#6B7A90), track trasparente.
 * Acceso: track FastingBlue, pallino blu notte scuro (#04202E).
 */
@Composable
fun appSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = FastingBlueDarkText,
    checkedTrackColor = FastingBlue,
    checkedBorderColor = FastingBlue,
    uncheckedThumbColor = SlateGray,
    uncheckedTrackColor = Color.Transparent,
    uncheckedBorderColor = SlateGray
)

/**
 * Palette colori espliciti per i campi di testo Outlined dell'applicazione.
 * Bordo a riposo grigio-azzurro, bordo e label in focus FastingBlue, testo bianco.
 */
@Composable
fun appTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = FastingBlue,
    unfocusedBorderColor = SlateGray,
    focusedLabelColor = FastingBlue,
    unfocusedLabelColor = SlateGray,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = FastingBlue,
    focusedPlaceholderColor = SlateGray.copy(alpha = 0.7f),
    unfocusedPlaceholderColor = SlateGray.copy(alpha = 0.7f)
)

/**
 * Campo di testo arrotondato a 12dp con colori conformi al tema.
 */
@Composable
fun AppOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        singleLine = singleLine,
        maxLines = maxLines,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(12.dp),
        colors = appTextFieldColors()
    )
}
