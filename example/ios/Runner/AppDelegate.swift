import cactus_locator
import Flutter
import UIKit

func registerPlugins(registry: FlutterPluginRegistry) {
    GeneratedPluginRegistrant.register(with: registry)
}

@UIApplicationMain
@objc class AppDelegate: FlutterAppDelegate {
    override func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication
            .LaunchOptionsKey: Any]?
    ) -> Bool {
        GeneratedPluginRegistrant.register(with: self)
        BackgroundLocatorPlugin.setPluginRegistrantCallback(registerPlugins)
        return super
            .application(application,
                         didFinishLaunchingWithOptions: launchOptions)
    }

}
