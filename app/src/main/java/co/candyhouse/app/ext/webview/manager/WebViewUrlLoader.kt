package co.candyhouse.app.ext.webview.manager

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.CHAPIClientBiz.getWebUrlByScene
import co.candyhouse.sesame.server.OfflineConfig
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.utils.L

/**
 * WebView URL加载管理
 * 支持离线模式和本地缓存
 *
 * @author frey on 2025/11/12
 */
object WebViewUrlLoader {

    private const val PREF_NAME = "WebViewUrlCache"

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun saveUrlToCache(context: Context, scene: String, url: String) {
        getPrefs(context).edit().putString(scene, url).apply()
    }

    private fun getUrlFromCache(context: Context, scene: String): String? {
        return getPrefs(context).getString(scene, null)
    }

    /**
     * 通用的URL加载逻辑
     * @param context 上下文，用于本地存储
     * @param scene 场景名
     * @param extInfo 额外信息
     * @param onSuccess 成功回调
     * @param onError 失败回调
     */
    fun loadWebUrl(
        context: Context,
        scene: String,
        extInfo: Map<String, String>? = null,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val cachedUrl = getUrlFromCache(context, scene)

        // 如果是离线模式，优先使用缓存
        if (OfflineConfig.isOfflineMode()) {
            L.d("WebViewUrlLoader", "Offline mode detected for scene: $scene")
            if (cachedUrl != null) {
                onSuccess(cachedUrl)
            } else {
                // 如果没有缓存，可以尝试返回默认的本地地址，或者报错
                val defaultUrl = getDefaultOfflineUrl(scene)
                if (defaultUrl != null) {
                    onSuccess(defaultUrl)
                } else {
                    onError("Offline mode: No cached URL for $scene")
                }
            }
            return
        }

        // Check if CHAPIClient is initialized before using it
        if (!CHAPIClientBiz.isInitialized()) {
            // 如果未初始化但有缓存，仍然允许加载缓存内容以支持离线/预加载
            if (cachedUrl != null) {
                L.d("WebViewUrlLoader", "CHAPIClient not initialized, using cached URL for scene: $scene")
                onSuccess(cachedUrl)
            } else {
                onError("CHAPIClient is not initialized. Please wait for initialization.")
            }
            return
        }
        
        getWebUrlByScene(scene, extInfo) { result ->
            result.fold(
                onSuccess = { state ->
                    val url = ((state as? CHResultState.CHResultStateNetworks)?.data ?: "")
                    if (url.isNotEmpty()) {
                        saveUrlToCache(context, scene, url)
                        onSuccess(url)
                    } else if (cachedUrl != null) {
                        onSuccess(cachedUrl)
                    } else {
                        onError("Empty URL")
                    }
                },
                onFailure = { t ->
                    // 网络请求失败时，如果有缓存则返回缓存
                    if (cachedUrl != null) {
                        L.d("WebViewUrlLoader", "Network failed, using cached URL for scene: $scene")
                        onSuccess(cachedUrl)
                    } else {
                        onError(t.message ?: "Load url failed")
                    }
                }
            )
        }
    }

    /**
     * 获取场景对应的默认离线URL（如果有）
     */
    private fun getDefaultOfflineUrl(scene: String): String? {
        // 在这里可以定义一些硬编码的本地 asset 路径作为最后的兜底
        return when (scene) {
            "me-index" -> "file:///android_asset/offline/me.html"
            "contacts" -> "file:///android_asset/offline/contacts.html"
            else -> null
        }
    }
    
    /**
     * Compose中使用的remember版本
     */
    @Composable
    fun rememberWebUrl(
        initialUrl: String,
        scene: String,
        extInfo: Map<String, String> = emptyMap(),
        onError: (String) -> Unit = {}
    ): State<String> {
        val context = LocalContext.current
        val webUrl = remember { mutableStateOf(initialUrl) }
        
        LaunchedEffect(scene) {
            if (scene.isNotEmpty() && initialUrl.isEmpty()) {
                loadWebUrl(
                    context = context,
                    scene = scene,
                    extInfo = extInfo.takeIf { it.isNotEmpty() },
                    onSuccess = { url ->
                        webUrl.value = url
                    },
                    onError = onError
                )
            }
        }
        
        return webUrl
    }
}
