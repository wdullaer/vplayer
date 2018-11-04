package com.wdullaer.vplayer

import com.google.android.exoplayer2.drm.*
import com.google.android.exoplayer2.upstream.HttpDataSource
import java.util.UUID

/**
 * A callback that formats the challenge produced by the Widevine content module into an HTTP request
 * that the vualto drm license server can process.
 *
 * Each DRM provider implements their own API to obtain a license (seems the widevine spec does not
 * specify what that REST API should look like).
 * The default implementation from the framework just produces the binary challenge and does an http
 * POST to the license URL and expects a binary blob with the license to be returned.
 *
 * This custom callback modifies executeKeyRequest to put the binary blob in a json structure that the
 * vualto license server can understand.
 *
 * We're lucky that the vualto server returns the license as a binary blob, so no modifications to the method
 * return value are needed. (Other servers return the license packaged in a json object, so this
 * would be the place where you put that logic).
 *
 * For some more background information see this link: https://bitmovin.com/player-configuration-v5/
 */
class VualtoHttpMediaDrmCallback(
        defaultLicenseUrl: String,
        dataSourceFactory: HttpDataSource.Factory,
        private val drmKey: String,
        private val kid: String
) : MediaDrmCallback {
    private val httpMediaDrmCallback: HttpMediaDrmCallback = HttpMediaDrmCallback(defaultLicenseUrl, dataSourceFactory)

    override fun executeProvisionRequest(uuid: UUID?, request: ExoMediaDrm.ProvisionRequest?): ByteArray {
        return httpMediaDrmCallback.executeProvisionRequest(uuid, request)
    }

    @ExperimentalUnsignedTypes
    override fun executeKeyRequest(uuid: UUID?, request: ExoMediaDrm.KeyRequest?): ByteArray {
        val challenge = request?.data?.asUByteArray()?.joinToString(",", "[", "]") ?: "[]"
        val newData = "{\"kid\": \"$kid\", \"token\": \"$drmKey\", \"drm_info\": $challenge}"
        return httpMediaDrmCallback.executeKeyRequest(
                uuid,
                ExoMediaDrm.KeyRequest(newData.toByteArray(), request?.licenseServerUrl)
        )
    }
}