package org.onion.diffusion.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.onion.diffusion.viewmodel.BookSourceViewModel

val viewModelModule  = module {
    viewModelOf(::BookSourceViewModel)
}