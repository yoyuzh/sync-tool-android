package com.yoyuzh.cliplink.ime

import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import com.yoyuzh.cliplink.domain.usecase.GetLocalHistoryUseCase
import com.yoyuzh.cliplink.ui.theme.ClipLinkTheme
import kotlinx.coroutines.flow.MutableStateFlow
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ClipLinkInputMethodService : android.inputmethodservice.InputMethodService() {
    @Inject lateinit var getLocalHistory: GetLocalHistoryUseCase
    private val inserter = InputConnectionInserter()
    private val editorInfoState = MutableStateFlow<EditorInfo?>(null)

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        editorInfoState.value = info
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        editorInfoState.value = null
    }

    override fun onCreateInputView(): View {
        return ComposeView(this).apply {
            setContent {
                val records by getLocalHistory(20).collectAsState(initial = emptyList())
                val currentEditorInfo by editorInfoState.collectAsState()
                
                // Do not show panel if current field is a password field
                val isPasswordField = currentEditorInfo?.let { inserter.isPasswordField(it) } ?: false
                ClipLinkTheme {
                    if (isPasswordField) {
                        // Show minimal/empty panel for password fields
                        ImeHistoryPanel(
                            records = emptyList(),
                            onInsert = { }
                        )
                    } else {
                        ImeHistoryPanel(
                            records = records,
                            onInsert = { text ->
                                inserter.insertText(currentInputConnection, text, currentEditorInfo)
                            }
                        )
                    }
                }
            }
        }
    }
}
