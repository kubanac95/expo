package host.exp.exponent.kernel

import expo.modules.updates.manifest.raw.RawManifest
import org.json.JSONException
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

data class ExperienceKey(
  val legacyId: String,
  val stableLegacyId: String,
  val scopeKey: String
) {
  @Throws(UnsupportedEncodingException::class)
  fun getUrlEncodedLegacyId() = URLEncoder.encode(legacyId, "UTF-8")

  @Throws(UnsupportedEncodingException::class)
  fun getUrlEncodedStableLegacyId() = URLEncoder.encode(stableLegacyId, "UTF-8")

  @Throws(UnsupportedEncodingException::class)
  fun getUrlEncodedScopeKey() = URLEncoder.encode(scopeKey, "UTF-8")

  companion object {
    @Throws(JSONException::class)
    fun fromRawManifest(rawManifest: RawManifest): ExperienceKey {
      return ExperienceKey(rawManifest.getLegacyID(), rawManifest.getStableLegacyID(), rawManifest.getScopeKey())
    }
  }
}
