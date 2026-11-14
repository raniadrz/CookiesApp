package com.crumbcodex.crk.ui.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.crumbcodex.crk.CrkApplication
import com.crumbcodex.crk.di.AppContainer

/** Builds a ViewModel factory that receives the app's [AppContainer]. */
inline fun <reified VM : ViewModel> containerFactory(
    crossinline create: (AppContainer) -> VM,
): ViewModelProvider.Factory = viewModelFactory {
    initializer { create((this[APPLICATION_KEY] as CrkApplication).container) }
}

fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item
