package yukams.app.background_locator.provider

interface BLLocationProvider {
    var listener: LocationUpdateListener?

    fun removeLocationUpdates()

    fun requestLocationUpdates(request: LocationRequestOptions)
}
