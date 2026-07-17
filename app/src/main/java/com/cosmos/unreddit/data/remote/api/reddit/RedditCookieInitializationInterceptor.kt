package com.cosmos.unreddit.data.remote.api.reddit

import android.util.Log
import android.webkit.CookieManager
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup

// https://gitlab.com/cosmosapps/stealth/-/work_items/141#note_3430084325
class RedditCookieInitializationInterceptor() : Interceptor {

    private fun hasRequiredCookies(): Boolean {
        val cookies = CookieManager.getInstance()
            .getCookie("https://www.reddit.com/")
            ?: return false
        return cookies.contains("token_v2")
    }


    override fun intercept(chain: Interceptor.Chain): Response {
        if (!hasRequiredCookies()) {
            initializeCookies(chain)
        }

        return chain.proceed(chain.request())
    }

    private fun initializeCookies(chain: Interceptor.Chain) {
        try {
            val jsChallengeUrl = "https://www.reddit.com/"

            val initRequest = Request.Builder()
                .url(jsChallengeUrl)
                .build()

            var response = chain.proceed(initRequest)
            val htmlBody = response.body?.string() ?: ""
            response.close()

            val document = Jsoup.parse(htmlBody)
            val scriptTag = document.selectFirst("script[nonce]")
            val scriptContent = scriptTag?.html() ?: ""
            val solutionValue = extractAndExecuteJavaScriptLogic(scriptContent)
            val tokenInput = document.selectFirst("input[name=token]")
            val tokenValue = tokenInput?.attr("value") ?: ""
            val rdtCookieUrl = "https://www.reddit.com/?solution=${solutionValue}&js_challenge=1&token=${tokenValue}&jsc_orig_r="

            val rdtCookieRequest = Request.Builder()
                .url(rdtCookieUrl)
                .build()

            response = chain.proceed(rdtCookieRequest)
            response.close()

            Log.d("CookieGen", "Cookies Generated!")

        } catch (e: Exception) {
            Log.e("CookieInterceptor", "Failed to initialize cookies", e)
        }
    }

    private fun extractAndExecuteJavaScriptLogic(scriptContent: String): String {
        try {
            val asyncPattern = """await\(async\s+(\w+)=>(.+?)\)\("([^"]+)"\)""".toRegex()
            val matchResult = asyncPattern.find(scriptContent)

            if (matchResult != null) {
                val (paramName, operation, baseValue) = matchResult.destructured
                val result = executeOperation(paramName, operation, baseValue)
                return result
            } else {
                Log.w("JSExtraction", "Could not find async pattern in script")
                return ""
            }
        } catch (e: Exception) {
            Log.e("JSExtraction", "Failed to extract and execute JS logic", e)
            return ""
        }
    }

    private fun executeOperation(paramName: String, operation: String, baseValue: String): String {
        return when {
            operation.contains("+") && !operation.contains("\"") && !operation.contains("'") -> {
                val parts = operation.split("+").map { it.trim() }
                if (parts.all { it == paramName }) {
                    baseValue + baseValue
                } else {
                    baseValue
                }
            }
            else -> baseValue
        }
    }

}
