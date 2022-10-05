require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "ble-react-native"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = "https://github.com/sicpa-dlab/ble-react-native.git"
  s.license      = "MIT"
  s.authors      = "SICPA"

  s.platforms    = { :ios => "10.0" }
  s.source       = { :git => "https://github.com/sicpa-dlab/ble-react-native.git", :tag => "#{s.version}" }

  s.source_files = ["ios/**/*.{h,swift}", "ios/Interfaces/*.m"]

  s.dependency "React-Core"

end
