package com.yervant.huntgames.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.yervant.huntgames.ui.util.NumberInputField
import com.kuhakupixel.libuberalles.overlay.OverlayContext
import com.kuhakupixel.libuberalles.overlay.service.dialog.OverlayDialog

class OverlayInputDialog(
    overlayContext: OverlayContext, alpha: Float = 1.0f
) : OverlayDialog(
    overlayContext, alpha = alpha
) {
    private val valueInput: MutableState<String> = mutableStateOf("")

    @Composable
    override fun DialogBody() {
        NumberInputField(
            value = valueInput.value,
            onValueChange = { value ->
                valueInput.value = value
            },
            label = "Value Input ...",
            placeholder = "value ...",
        )
    }

    fun show(title: String, defaultValue: String = "", onConfirm: (input: String) -> Unit) {
        this.valueInput.value = defaultValue
        super.show(
            title = title,
            onConfirm = {
                onConfirm(valueInput.value)
            },
            onClose = {}
        )
    }


}