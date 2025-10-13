package org.onion.read

import com.dokar.quickjs.alias.func
import com.dokar.quickjs.binding.define
import com.dokar.quickjs.binding.function
import com.dokar.quickjs.quickJs
import com.onion.model.BookKind
import com.onion.model.BookSource
import com.onion.model.rule.BookListRule
import com.onion.network.constant.UA_NAME
import com.onion.network.di.getHttpClient
import com.skydoves.sandwich.getOrElse
import com.skydoves.sandwich.ktor.getApiResponse
import com.skydoves.sandwich.message
import com.skydoves.sandwich.onFailure
import com.skydoves.sandwich.onSuccess
import io.ktor.client.request.header
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import ktsoup.KtSoupDocument
import ktsoup.KtSoupParser
import org.onion.read.rule.ExploreRuleParser
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeAppCommonTest {

    @Test
    fun example() {
        assertEquals(3, 1 + 2)
    }


    private val httpClient = getHttpClient()
    private val httpJson = Json {
        // 忽略 JSON 中存在但数据类中没有的字段，避免崩溃
        ignoreUnknownKeys = true
        // 允许不规范的 JSON 格式（例如，属性名没有引号）
        isLenient = true
        // 如果一个字段在 JSON 中不存在，但类中有默认值，则使用默认值
        coerceInputValues = true
        // 将 null 值编码进去，而不是省略字段
        encodeDefaults = true
    }


    // ---- 在JS代码中获取Jsoup对应的select内容 ------
    val jsoupSelectRegex = Regex("""\.select\s*\(['"](.*?)['"]\)""")


    @Test
    fun requestBookSource() = runTest{
        val url = "https://www.yckceo.sbs/yuedu/shuyuans/json/id/811.json"
        val requestUrl: String
        val noUaRequest = url.endsWith("#requestWithoutUA")
        requestUrl = if (noUaRequest) {
            url.substringBeforeLast("#requestWithoutUA")
        } else {
            url
        }
        // 发起请求并获取响应
        println("url-> $requestUrl")
        httpClient.getApiResponse<List<BookSource>>(requestUrl) {
            if (noUaRequest) {
                header(UA_NAME, "null")
            }
        }.onSuccess {
            data.filter { bookSource -> bookSource.exploreUrl.isNullOrEmpty().not() }[3].run {
                val ruler = exploreUrl ?: ""
                var jsStr = ruler
                if (ruler.startsWith("<js>", true) || ruler.startsWith("@js:", true)){
                    jsStr = if (ruler.startsWith("@")) {
                        ruler.substring(4)
                    } else {
                        ruler.substring(4, ruler.lastIndexOf("<"))
                    }
                }
                println("sourceUrl-> $bookSourceUrl")
                launch {
                    quickJs {
                        define("source"){
                            function("getVariable"){
                                ""
                            }
                            function("key"){
                                bookSourceUrl
                            }
                            function("getLoginInfoMap"){
                                mapOf<String, String>()
                            }
                        }
                        define("cookie"){
                            function("getCookie"){
                                ""
                            }
                        }
                        define("java"){
                            function("getCookie"){
                                ""
                            }
                            function("androidId"){
                                ""
                            }
                            function("ajax"){
                                val result = runBlocking {
                                    httpClient.getApiResponse<String>(it.first().toString()).onSuccess {
                                        //println("ajax onSuccess -> $data")
                                    }.onFailure {
                                        println("ajax onFailure -> ${message()}")
                                    }
                                }
                                result.getOrElse { "" }
                            }
                            function("longToast"){
                                println("js toast -> ${it.first()}")
                            }
                            function("toast"){
                                println("js toast -> ${it.first()}")
                            }
                        }
                        function("getArguments"){ args ->
                            ""
                        }
                        function("ck"){ args ->
                            ""
                        }
                        function("gets_key"){ args ->
                            ""
                        }
                        // ---- 在KMP中,针对原Java的org.jsoup.Jsoup进行替换成这边quickJs定义的 ------
                        var formatJsStr = jsStr.replace("org.jsoup.Jsoup","jsoup")
                        // ---- 正则获取Jsoup select 要查询的内容,以便后面KMP的Ksoup库进行查询 ------
                        val jsoupSelectContent = jsoupSelectRegex.find(jsStr)?.groupValues
                        println("jsoup select-> $jsoupSelectContent")
                        // ---- 因为quickJs 中 无法处理 jsoup.parse(tag).select('xxxx")的语法,需要进行空格替换,自己处理 ------
                        if(jsoupSelectContent.isNullOrEmpty().not()){
                            formatJsStr = formatJsStr.replace(jsoupSelectContent.first(),"")
                        }
                        println("jsStr-> $formatJsStr")
                        define("jsoup"){
                            function("parse"){ args ->
                                KtSoupParser.parse(args.first().toString()).run {
                                    val query = querySelectorAll(jsoupSelectContent!![1])
                                    val mapContent = query.map { it.textContent() }
                                    println("jsoup query-> $mapContent")
                                    mapContent
                                }
                            }
                        }
                        var jsonParseResult = runCatching {
                            httpJson.decodeFromString<List<BookKind>>(formatJsStr.trim())
                        }.getOrNull()
                        if(jsonParseResult.isNullOrEmpty().not()){
                            println("json kind-> ${jsonParseResult}")
                        }else {
                            val evaluateResult = evaluate<List<BookKind>>(formatJsStr.trim())
                            jsonParseResult = evaluateResult
                            println("evaluate kind-> ${evaluateResult}")
                        }
                        val exploreRuleParser = ExploreRuleParser(1,bookSourceUrl,jsonParseResult.filter { it.url.isNullOrEmpty().not() }[0].url ?: "")
                        exploreRuleParser.startParseUrl()
                        /*val result = evaluate<List<JsObject>>(jsStr.trim())
                        result.forEach {
                            println("kind JsObject-> ${it.entries}")
                        }*/
                        /*val kindList = httpJson.decodeFromString<List<BookKind>>(result.toString())
                        kindList.get(0).let { kind ->
                            println("kind-> ${kind}")
                        }*/
                    }
                }
            }
        }
    }

    private fun extractString(json: JsonElement?, path: String): String? {
        if (json !is JsonObject) return null
        val current: JsonElement? = json[path]
        return (current as? JsonPrimitive)?.content
    }


    fun getExploreData(data: JsonObject, ruleExplore: BookListRule) {
        val bookListRule = ruleExplore.bookList ?: return
        // 规则示例: "$.data.books[*]"
        val pathSegments = bookListRule.removePrefix("$.").split('.')

        var currentElement: JsonElement = data
        for (segment in pathSegments) {
            if (currentElement !is JsonObject) {
                println("Error: Not a JsonObject, cannot find key '$segment'")
                return
            }
            if (segment.endsWith("[*]")) {
                val arrayKey = segment.removeSuffix("[*]")
                val arrayElement = currentElement[arrayKey]
                if (arrayElement is JsonArray) {
                    currentElement = arrayElement
                    break // Assume [*] is the last part of the path
                } else {
                    println("Error: Key '$arrayKey' does not point to a JsonArray")
                    return
                }
            } else {
                currentElement = currentElement[segment] ?: run {
                    println("Error: Key '$segment' not found")
                    return
                }
            }
        }

        if (currentElement is JsonArray) {
            val bookList = currentElement.map { jsonObject ->
                BookListRule(
                    name = extractString(jsonObject, ruleExplore.name!!),
                    author = extractString(jsonObject, ruleExplore.author!!),
                    intro = extractString(jsonObject, ruleExplore.intro!!),
                    kind = extractString(jsonObject, ruleExplore.kind!!),
                    lastChapter = extractString(jsonObject, ruleExplore.lastChapter!!),
                    updateTime = extractString(jsonObject, ruleExplore.updateTime!!),
                    bookUrl = extractString(jsonObject, ruleExplore.bookUrl!!),
                    coverUrl = extractString(jsonObject, ruleExplore.coverUrl!!),
                    wordCount = extractString(jsonObject, ruleExplore.wordCount!!)
                )
            }
            println("Successfully extracted book list: $bookList")
        } else {
            println("Warning: The final element is not a JsonArray.")
        }
    }

    val bookSourceUrlRegex = Regex("""(https?://[^/\s]+)""")
    // ------------------------------------------------------------------------
    // \{\{: 匹配字面量的 {{。花括号在正则中有特殊含义，所以需要用反斜杠 \ 来转义。
    // ( 和 ): 创建一个捕获组。这是关键，因为我们只想要括号里面的内容。
    // .+?: 这是捕获组的内容。
    // .: 匹配除换行符外的任何字符。
    // +: 匹配前面的元素一次或多次。
    // ?: 使 + 变为非贪婪（non-greedy）模式。这意味着它会匹配尽可能少的字符，直到遇到后面的 }} 为止。如果没有 ?，对于输入 {{a}} and {{b}}，.+ 会贪婪地匹配到 a}} and {{b。
    // \}\}: 匹配字面量的 }}
    // ------------------------------------------------------------------------
    val bookKindRegex = Regex("""\{\{(.+?)\}\}""")


    fun isHttpUrlWithKtor(urlString: String?): Boolean {
        if (urlString.isNullOrBlank()) {
            return false
        }
        return urlString.startsWith("http://", ignoreCase = true) || urlString.startsWith("https://", ignoreCase = true)
    }

    @Test
    fun testQuickJs() = runTest {
        quickJs {
            define("console") {
                function("log") { args ->
                    println(args.joinToString(" "))
                }
            }


            function<String, String>("greet") { "Hello, $it!" }

            evaluate<Any?>(
                """
        console.log("Hello from JavaScript!")
        """.trimIndent()
            )
        }
    }
}
