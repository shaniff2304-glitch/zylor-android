
package top.opubhai.zylor

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var pendingRequest: PermissionRequest? = null
    private var pendingResources: Array<String> = emptyArray()

    companion object {
        private const val PERMISSION_CODE = 101
        private const val SITE = "opubhai.top"
    }

    @Suppress("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadsImagesAutomatically = true
        webView.settings.mediaPlaybackRequiresUserGesture = true
        webView.settings.mixedContentMode =
            WebSettings.MIXED_CONTENT_NEVER_ALLOW

        webView.webViewClient = WebViewClient()

        webView.webChromeClient = object : WebChromeClient() {

            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    val origin = request.origin

                    if (origin.scheme != "https" ||
                        origin.host != SITE
                    ) {
                        request.deny()
                        return@runOnUiThread
                    }

                    val resources = request.resources
                    val needed = mutableListOf<String>()

                    if (resources.contains(
                            PermissionRequest.RESOURCE_AUDIO_CAPTURE
                        )
                    ) {
                        needed.add(Manifest.permission.RECORD_AUDIO)
                    }

                    if (resources.contains(
                            PermissionRequest.RESOURCE_VIDEO_CAPTURE
                        )
                    ) {
                        needed.add(Manifest.permission.CAMERA)
                    }

                    if (needed.isEmpty()) {
                        request.deny()
                        return@runOnUiThread
                    }

                    val missing = needed.filter {
                        checkSelfPermission(it) !=
                            PackageManager.PERMISSION_GRANTED
                    }

                    if (missing.isEmpty()) {
                        request.grant(resources)
                    } else {
                        pendingRequest?.deny()
                        pendingRequest = request
                        pendingResources = resources
                        requestPermissions(
                            missing.toTypedArray(),
                            PERMISSION_CODE
                        )
                    }
                }
            }

            override fun onPermissionRequestCanceled(
                request: PermissionRequest
            ) {
                if (pendingRequest === request) {
                    pendingRequest = null
                    pendingResources = emptyArray()
                }
            }
        }

        setContentView(webView)

        if (savedInstanceState == null) {
            webView.loadUrl("https://opubhai.top")
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        )

        if (requestCode == PERMISSION_CODE) {
            val request = pendingRequest
            val resources = pendingResources

            pendingRequest = null
            pendingResources = emptyArray()

            if (request != null) {
                val granted = resources.all { resource ->
                    when (resource) {
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE ->
                            checkSelfPermission(
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                        PermissionRequest.RESOURCE_VIDEO_CAPTURE ->
                            checkSelfPermission(
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                        else -> false
                    }
                }

                if (granted) {
                    request.grant(resources)
                } else {
                    request.deny()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (this::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        pendingRequest?.deny()
        pendingRequest = null
        webView.destroy()
        super.onDestroy()
    }
}
