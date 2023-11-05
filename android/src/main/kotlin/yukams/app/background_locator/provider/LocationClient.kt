package yukams.app.background_locator.provider

enum class LocationClient(val value: Int) {
    Android(0);

    companion object {
        fun fromInt(value: Int) = values().firstOrNull { it.value == value }
    }
}