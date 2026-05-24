package com.yoyuzh.cliplink.ime

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InputConnectionInserterTest {

    private lateinit var inserter: InputConnectionInserter

    @Before
    fun setUp() {
        inserter = InputConnectionInserter()
    }

    @Test
    fun `isPasswordField returns true for TYPE_TEXT_VARIATION_PASSWORD`() {
        val editorInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        assertTrue(inserter.isPasswordField(editorInfo))
    }

    @Test
    fun `isPasswordField returns true for TYPE_TEXT_VARIATION_VISIBLE_PASSWORD`() {
        val editorInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        assertTrue(inserter.isPasswordField(editorInfo))
    }

    @Test
    fun `isPasswordField returns true for TYPE_NUMBER_VARIATION_PASSWORD`() {
        val editorInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        assertTrue(inserter.isPasswordField(editorInfo))
    }

    @Test
    fun `isPasswordField returns false for normal text field`() {
        val editorInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
        }
        assertFalse(inserter.isPasswordField(editorInfo))
    }

    @Test
    fun `isPasswordField returns false for email field`() {
        val editorInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        assertFalse(inserter.isPasswordField(editorInfo))
    }

    @Test
    fun `insertText with null InputConnection returns false`() {
        val result = inserter.insertText(null, "hello")
        assertFalse(result)
    }

    @Test
    fun `insertText with blank text returns false`() {
        val result = inserter.insertText(null, "   ")
        assertFalse(result)
    }
}
