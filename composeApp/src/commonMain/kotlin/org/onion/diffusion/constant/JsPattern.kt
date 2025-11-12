package org.onion.diffusion.constant

import kotlin.text.Regex
import kotlin.text.RegexOption

object JsPattern {
    val JS_PATTERN = Regex("<js>([\\w\\W]*?)</js>|@js:([\\w\\W]*)", RegexOption.IGNORE_CASE)
    val PAGE_PATTERN = Regex("<(.*?)>")
    val PARAM_PATTERN = Regex("\\s*,\\s*(?=\\{)")
}
