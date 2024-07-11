package com.yervant.huntgames.ui.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun CustomTextInput(
    modifier: Modifier = Modifier,
    textValue: String,
    onTextChange: (String) -> Unit,
    label: String = "",
    placeholder: String = ""
) {
    val textFieldValue = remember { mutableStateOf(TextFieldValue(textValue)) }

    Box(modifier = modifier.padding(1.dp)) {
        BasicTextField(
            value = textFieldValue.value,
            onValueChange = { value ->
                textFieldValue.value = value.copy(text = value.text.replace("\n", ""))
                onTextChange(value.text)
            },
            modifier = Modifier.padding(0.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(top = 0.dp)) {
                    if (textFieldValue.value.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}



@Composable
fun TextInput(
    modifier: Modifier = Modifier,
    textValue: String,
    onTextChange: (String) -> Unit,
    label: String = "",
    placeholder: String = ""
) {
    Row(modifier = modifier) {
        CustomTextInput(
            modifier = Modifier.weight(1f),
            textValue = textValue,
            onTextChange = onTextChange,
            label = label,
            placeholder = placeholder
        )
    }
}

@Composable
fun TextInput2(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    value: String,
    label: String = "",
    placeholder: String = "",
    onValueChange: (String) -> Unit,
) {

    TextField(
        modifier = modifier,
        enabled = enabled,
        value = value,
        onValueChange = { value ->
            // make sure we don't pass in value with \n
            // because of enter, because that will cause the caller
            // parsing problem
            onValueChange(value.replace("\n", ""))
        },
        label = { Text(text = label) },
        placeholder = { Text(text = placeholder) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        singleLine = true,
    )
}
