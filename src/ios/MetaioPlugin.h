// MetaioGap - v1.0.0
// (c) 2013 Boston Dell-Vandenberg, boston@pomelodesign.com, MIT Licensed.
// MetaioPlugin.h may be freely distributed under the MIT license.

#import <Cordova/CDV.h>
#import "ARELConfigParser.h"

@interface MetaioPlugin : CDVPlugin<UIWebViewDelegate>

@property(nonatomic, strong) id CDVDelegate;
@property(nonatomic, strong) id ARELDelegate;
@property (nonatomic, readonly, strong) NSXMLParser* configParser;
@property (nonatomic, strong)  ARELConfigParser *_delegate;
@end
