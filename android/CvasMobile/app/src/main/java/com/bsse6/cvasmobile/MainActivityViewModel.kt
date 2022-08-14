package com.bsse6.cvasmobile

import androidx.lifecycle.ViewModel
import com.bsse6.cvasmobile.services.DaemonService
import kotlinx.coroutines.flow.consumeAsFlow

class MainActivityViewModel : ViewModel() {
    fun getBitmap() = DaemonService.bitmapChannel.consumeAsFlow()
}