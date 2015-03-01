// Metaio Cordova Plugin
// (c) Boston Dell-Vandenberg <boston@pomelodesign.com>
// MetaioPlugin.h may be freely distributed under the MIT license.

#import <Cordova/CDV.h>
#import "ARELConfigParser.h"

@interface MetaioPlugin : CDVPlugin<UIWebViewDelegate>

@property(nonatomic, strong) id CDVDelegate;
@property(nonatomic, strong) id ARELDelegate;
@property (nonatomic, readonly, strong) NSXMLParser* configParser;
@property (nonatomic, strong)  ARELConfigParser *_delegate;
@end
