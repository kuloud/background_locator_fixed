#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html
#
Pod::Spec.new do |s|
  s.name             = 'cactus_locator'
  s.version          = '0.0.1'
  s.summary          = 'A Flutter plugin for getting location updates even when the app is killed.'
  s.description      = <<-DESC
A new Flutter plugin.
                       DESC
  s.homepage         = 'https://github.com/kuloud/background_locator_fixed'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'REKAB' => 'xkuloud@gmail.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.dependency 'Flutter'

  s.ios.deployment_target = '8.0'
end
