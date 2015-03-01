# MetaioGap

Metaio SDK plugin for Cordova (supports up to SDK 5.5)

## Installation

1. Install plugin `cordova plugin add com.pomelodesign.cordova.metaio`.
2. Add arelConfigPath to config.xml, default `www/metaio/arelConfig.xml`. eg. `<preference name="arelConfigPath" value="www/metaio/arelConfig.xml" />`
3. Follow additional instructions output by the plugin install.

## iOS configuration

You need to manually configure some values in the Xcode project.

- Remove `arm64` from `Valid Architectures`

- Set to `Default compiler` for `Compiler for C/C++/Objective-C`, `C Language Dialect`, and `C++ Language Dialect`

- `$(SRCROOT)/ScanIt/Frameworks` to `Framework search paths`

- Add `-DNS_BLOCK_ASSERTIONS=1` to Release in `Other C Flags`

- Add `METAIOSDK_COMPAT_NO_CPP11_STL_FEATURES` to Release and Debug in `Preprocessor Macros`

- Add `DEBUG=1` to Debug in `Preprocessor Macros`


## Notes

**Events**

The plugin dispatches two events that you can listen for in either web view.

metaioOpen - fired when metaio view is opened successfully.

metaioClose - fired when metaio view is closed successfully. If a url is provided in the close method the event 'detail' parameter will contain that url.
