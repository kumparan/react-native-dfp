require 'json'
pkg = JSON.parse(File.read("package.json"))

Pod::Spec.new do |s|
  s.name             = 'react-native-dfp'
  s.version          = pkg["version"]
  s.summary          = pkg["description"]
  s.requires_arc     = true
  s.license          = pkg["license"]
  s.homepage         = "https://github.com/kumparan/react-native-dfp"
  s.author           = pkg["author"]
  s.source           = { :git => "https://github.com/kumparan/react-native-dfp.git", :tag => "v#{s.version}" }
  s.source_files     = 'ios/**/*.{h,m}'
  s.platform         = :ios, "8.0"
  s.static_framework = true
  s.dependency 'React'
  s.dependency 'Google-Mobile-Ads-SDK'
end
