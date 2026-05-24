package com.yoyuzh.cliplink.ime

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.text.InputType

class InputConnectionInserter {
    /**
     * Inserts [text] into the active [inputConnection].
     *
     * Returns false if:
     * - inputConnection is null
     * - text is blank
     * - the target field is a password / sensitive input type
     */
    fun insertText(inputConnection: InputConnection?, text: String, editorInfo: EditorInfo? = null): Boolean {
        val value = text.trim()
        if (value.isBlank() || inputConnection == null) return false

        // Refuse insertion into password fields
        if (editorInfo != null && isPasswordField(editorInfo)) return false

        return inputConnection.commitText(value, 1)
    }

    /**
     * Checks if the given EditorInfo corresponds to a password or sensitive field.
     */
    fun isPasswordField(editorInfo: EditorInfo): Boolean {
        val inputType = editorInfo.inputType
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
    }
}
