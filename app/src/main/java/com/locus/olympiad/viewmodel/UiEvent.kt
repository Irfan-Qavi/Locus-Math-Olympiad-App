package com.locus.olympiad.viewmodel

sealed class UiEvent {
    data class ShowCompletionDialog(
        val message: String = "You have conquered this tier! No more questions available right now."
    ) : UiEvent()
}
