package org.onion.diffusion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class BookSourceViewModel(): ViewModel() {

    private fun importBookSourceFromUrl(url: String){
        viewModelScope.launch {

        }
    }
}