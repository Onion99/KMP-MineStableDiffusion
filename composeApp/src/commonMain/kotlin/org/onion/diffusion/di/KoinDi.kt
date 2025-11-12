package org.onion.diffusion.di

import com.onion.network.di.networkModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin{
        config?.invoke(this)
        modules(networkModule)
    }
}