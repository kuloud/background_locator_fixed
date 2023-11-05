package yukams.app.background_locator.provider

import android.location.Location
import android.os.Build
import yukams.app.background_locator.Keys

class LocationParserUtil {
    companion object {
        fun getLocationMapFromLocation(location: Location): HashMap<String, Any> {
            val isMocked =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) location.isMock else location.isFromMockProvider
            val provider = if (location.provider != null) location.provider else ""
            return hashMapOf(
                Keys.ARG_IS_MOCKED to isMocked,
                Keys.ARG_LATITUDE to location.latitude,
                Keys.ARG_LONGITUDE to location.longitude,
                Keys.ARG_ACCURACY to location.accuracy,
                Keys.ARG_ALTITUDE to location.altitude,
                Keys.ARG_SPEED to location.speed,
                Keys.ARG_SPEED_ACCURACY to location.speedAccuracyMetersPerSecond,
                Keys.ARG_HEADING to location.bearing,
                Keys.ARG_TIME to location.time.toDouble(),
                Keys.ARG_PROVIDER to provider!!,
            )
        }

    }
}