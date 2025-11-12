package org.onion.diffusion.rule

import com.dokar.quickjs.binding.JsObject
import com.dokar.quickjs.binding.function
import com.dokar.quickjs.quickJs
import kotlinx.coroutines.runBlocking
import org.onion.diffusion.constant.JsPattern.JS_PATTERN
import org.onion.diffusion.constant.JsPattern.PAGE_PATTERN
import org.onion.diffusion.utils.HTTP_JSON
import org.onion.diffusion.utils.NetworkUtil
import org.onion.diffusion.utils.RequestMethod

class ExploreRuleParser(val page: Int? = null,private var baseUrl: String = "",private var exploreUrl: String = "") {

    var ruleUrl = ""
        private set
    var url: String = ""
        private set
    private var urlNoQuery: String = ""
    private var method = RequestMethod.GET
    private val headerMap = LinkedHashMap<String, String>()
    private var body: String? = null
    private var type: String? = null
        private set
    private var charset: String? = null
    private var retry: Int = 0
    private var useWebView: Boolean = false
    private var webJs: String? = null

    init {
        ruleUrl = exploreUrl
    }

    suspend fun startParseUrl(){
        //执行@js,<js></js>
        analyzeJs()
        //替换参数
        replaceKeyPageJs()
        //处理URL
        analyzeUrl()
    }

    private suspend fun analyzeJs() {
        // 1. 初始化累加器和游标
        var accumulatedResult = ruleUrl // 使用 Any 类型以匹配 evalJS 的返回类型
        var currentIndex = 0

        // 提取重复的逻辑到一个局部函数中，使代码更清晰
        fun processTextSegment(segment: String, currentResult: String): String {
            val trimmedSegment = segment.trim()
            return if (trimmedSegment.isNotEmpty()) {
                trimmedSegment.replace("@result", currentResult)
            } else {
                currentResult // 如果片段为空，则结果不变
            }
        }

        // 2. 查找所有匹配项并遍历
        JS_PATTERN.findAll(ruleUrl).forEach { matchResult ->
            // 2a. 处理上一个匹配结束到当前匹配开始之间的“间隔”文本
            val textBeforeMatch = ruleUrl.substring(currentIndex, matchResult.range.first)
            accumulatedResult = processTextSegment(textBeforeMatch, accumulatedResult)

            // 2b. 处理当前匹配到的JS部分
            val jsCode = matchResult.groupValues[2].ifEmpty { matchResult.groupValues[1] }
            quickJs {
                function("result"){
                    accumulatedResult
                }
                accumulatedResult = evaluate(jsCode)
            }
            // 2c. 更新游标到当前匹配的末尾
            currentIndex = matchResult.range.last + 1

        }

        // 3. 处理最后一个匹配到字符串末尾的“尾部”文本
        if (currentIndex < ruleUrl.length) {
            val tailingText = ruleUrl.substring(currentIndex)
            accumulatedResult = processTextSegment(tailingText, accumulatedResult)
        }

        ruleUrl = accumulatedResult.toString()
    }


    /**
     * 替换关键字,页数,JS
     */
    private suspend fun replaceKeyPageJs() { //先替换内嵌规则再替换页数规则，避免内嵌规则中存在大于小于号时，规则被切错
        //js
        if (ruleUrl.contains("{{") && ruleUrl.contains("}}")) {
            val analyze = CommonRuleParser(ruleUrl) //创建解析
            //替换所有内嵌{{js}}
            val url = analyze.innerRule("{{", "}}") {
                val jsEval = runBlocking {
                    quickJs {
                        function("page"){ args ->
                            page
                        }
                        evaluate<JsObject>(it)
                    }
                }
                jsEval.values.first().toString()
            }
            if (url.isNotEmpty()) ruleUrl = url
        }
        //page
        page?.let { currentPage ->
            // 使用 Regex.replace 替换所有页码块，这比手动循环更安全、更简洁
            ruleUrl = JS_PATTERN.replace(ruleUrl) { matchResult ->
                // matchResult.groupValues[1] 包含 < 和 > 之间的页码字符串，例如 "1,2,3"
                val pagesString = matchResult.groupValues[1]
                val pages = pagesString.split(',').map { it.trim() }

                if (pages.isEmpty()) {
                    "" // 如果 pages 列表为空，则用空字符串替换
                } else {
                    // 如果当前页码在范围内，则使用对应页码；否则使用最后一页。
                    if (currentPage > 0 && currentPage <= pages.size) {
                        pages[currentPage - 1]
                    } else {
                        pages.last()
                    }
                }
            }
        }
    }

    /**
     * 解析Url
     */
    private suspend fun analyzeUrl() {
        val urlMatcher = PAGE_PATTERN.find(ruleUrl)
        val urlNoOption = if (urlMatcher != null) ruleUrl.substring(0, urlMatcher.range.first)
        else ruleUrl
        url = NetworkUtil.getAbsoluteURL(baseUrl, urlNoOption)
        NetworkUtil.getBaseUrl(url)?.let {
            baseUrl = it
        }
        if (urlNoOption.length != ruleUrl.length) {
            val urlOptionStr = ruleUrl.substring(urlMatcher!!.range.last)
            val urlOption = HTTP_JSON.decodeFromString<UrlOption>(urlOptionStr)

            urlOption.let { option ->
                option.getMethod()?.let {
                    if (it.equals("POST", true)) method = RequestMethod.POST
                }
                option.getHeaderMap()?.forEach { entry ->
                    headerMap[entry.key.toString()] = entry.value.toString()
                }
                option.getBody()?.let {
                    body = it
                }
                type = option.type
                charset = option.charset
                retry = option.retry ?: 0
                useWebView = option.webView ?: false
                webJs = option.webJs ?: ""
                option.js?.let { jsStr ->
                    url = quickJs {
                        function("result"){
                            url
                        }
                        evaluate(jsStr)
                    }
                }
            }
        }
        println("explore url -> $url")
    }

    data class UrlOption(
        private var method: String? = null,
        var charset: String? = null,
        private var headers: Any? = null,
        private var body: Any? = null,
        /**
         * 源Url
         **/
        private var origin: String? = null,
        /**
         * 重试次数
         **/
        var retry: Int? = null,
        /**
         * 类型
         **/
        var type: String? = null,
        /**
         * 是否使用webView
         **/
        var webView: Boolean? = null,
        /**
         * webView中执行的js
         **/
        var webJs: String? = null,
        /**
         * 解析完url参数时执行的js
         * 执行结果会赋值给url
         */
        var js: String? = null,
        /**
         * 服务器id
         */
        private var serverID: Long? = null,
        /**
         * webview等待页面加载完毕的延迟时间（毫秒）
         */
        private var webViewDelayTime: Long? = null,
    ){

        fun getMethod(): String? {
            return method
        }

        fun getHeaderMap(): Map<*, *>? {
            return when (val value = headers) {
                is Map<*, *> -> value
                is String -> HTTP_JSON.decodeFromString<Map<String, Any>>(value)
                else -> null
            }
        }

        fun getBody(): String? {
            return body?.let {
                it as? String ?: HTTP_JSON.encodeToString(it)
            }
        }
    }
}