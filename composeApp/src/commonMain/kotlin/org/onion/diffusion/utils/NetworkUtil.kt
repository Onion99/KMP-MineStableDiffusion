package org.onion.diffusion.utils

import io.ktor.http.Url
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom
import kotlinx.serialization.json.Json


val HTTP_JSON = Json {
    // 忽略 JSON 中存在但数据类中没有的字段，避免崩溃
    ignoreUnknownKeys = true
    // 允许不规范的 JSON 格式（例如，属性名没有引号）
    isLenient = true
    // 如果一个字段在 JSON 中不存在，但类中有默认值，则使用默认值
    coerceInputValues = true
    // 将 null 值编码进去，而不是省略字段
    encodeDefaults = true
}

object NetworkUtil {



    /**
     * 检查字符串是否为绝对 URL
     */
    fun String.isAbsUrl(): Boolean =
        startsWith("http://", true) ||
                startsWith("https://", true) ||
                startsWith("ftp://", true)

    /**
     * 检查字符串是否为 Data URL
     */
    fun String.isDataUrl(): Boolean =
        startsWith("data:", true)



    /**
     * 获取绝对地址 (KMP 版本)
     *
     * @param baseURL 基础 URL 字符串，如果包含逗号，则只取第一个逗号前的内容。
     * @param relativePath 相对路径或绝对路径。
     * @return 拼接后的绝对地址。
     */
    fun getAbsoluteURL(baseURL: String?, relativePath: String): String {
        if (baseURL.isNullOrEmpty()) {
            return relativePath.trim()
        }
        val baseUrlObj: Url? = try {
            // 和原逻辑保持一致，只使用 baseURL 中逗号前的部分
            Url(baseURL.substringBefore(","))
        } catch (e: Exception) {
            // 使用 KMP 兼容的日志或打印方式
            println("KMP Log: BaseURL 解析失败 - ${e.message}")
            null
        }
        return getAbsoluteURL(baseUrlObj, relativePath)
    }

    /**
     * 获取绝对地址 (KMP 版本, 接受 Ktor Url 对象)
     *
     * @param baseURL 基础 URL 对象。
     * @param relativePath 相对路径或绝对路径。
     * @return 拼接后的绝对地址。
     */
    fun getAbsoluteURL(baseURL: Url?, relativePath: String): String {
        val relativePathTrim = relativePath.trim()
        if (baseURL == null) {
            return relativePathTrim
        }
        // 如果已经是绝对 URL 或 Data URL，直接返回
        if (relativePathTrim.isAbsUrl() || relativePathTrim.isDataUrl()) {
            return relativePathTrim
        }
        // 过滤 javascript 伪协议
        if (relativePathTrim.startsWith("javascript", ignoreCase = true)) {
            return ""
        }

        return try {
            // 使用 Ktor 的 URLBuilder 和 takeFrom 来正确处理相对路径
            URLBuilder(baseURL).takeFrom(relativePathTrim).build().toString()
        } catch (e: Exception) {
            // 替换 AppLog.put 为 KMP 兼容的日志
            println("KMP Log: 网址拼接出错 - ${e.message}")
            // 拼接失败时，返回原始相对路径作为备用
            relativePathTrim
        }
    }

    fun getBaseUrl(url: String?): String? {
        url ?: return null
        if (url.startsWith("http://", true)
            || url.startsWith("https://", true)
        ) {
            val index = url.indexOf("/", 9)
            return if (index == -1) {
                url
            } else url.substring(0, index)
        }
        return null
    }
}
enum class RequestMethod {
    GET, POST
}