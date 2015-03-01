// Metaio Cordova Plugin
// (c) Boston Dell-Vandenberg <boston@pomelodesign.com>
// MetaioPlugin.h may be freely distributed under the MIT license.

#import "MetaioPlugin.h"
#import <Cordova/CDVPluginResult.h>
#import <Cordova/CDVViewController.h>
#import "ARELViewController.h"
#import "ARELConfigParser.h"
#import "MainViewController.h"
#import <Cordova/CDVWebViewDelegate.h>

@interface MetaioPlugin ()

@property (nonatomic, strong) ARELViewController* metaio;

@end

@implementation MetaioPlugin

@synthesize configParser;
@synthesize metaio;
@synthesize CDVDelegate;
@synthesize ARELDelegate;
@synthesize _delegate;

static BOOL viewOpened = NO;
static  ARELViewController* _metaio = nil;
static  UIViewController *mainView = nil;
static  UIWebView *mainWebView = nil;


-(void) parseConfigFile
{
    // read from config.xml in the app bundle
    NSString* path = [[NSBundle mainBundle] pathForResource:@"config" ofType:@"xml"];
    
    if (![[NSFileManager defaultManager] fileExistsAtPath:path]) {
        NSAssert(NO, @"ERROR: config.xml does not exist. Please run cordova-ios/bin/cordova_plist_to_config_xml path/to/project.");
        return;
    }
    
    NSURL* url = [NSURL fileURLWithPath:path];
    
    configParser = [[NSXMLParser alloc] initWithContentsOfURL:url];
    if (configParser == nil) {
        NSLog(@"Failed to initialize XML parser.");
        return;
    }
    self._delegate = [[ARELConfigParser alloc] init];
    [configParser setDelegate:((id < NSXMLParserDelegate >)self._delegate)];
    [configParser parse];
}

MainViewController *controller;

- (void) open:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;
    NSLog(@"Metaio opening...");
    
 
    if (viewOpened)
    {
        viewOpened = YES;
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:nil];
        return;
    }
    
    @try
    {
        viewOpened = YES;
        
        if (!self.metaio)
        {
            NSLog(@"Metaio loading for first time...");
            [self parseConfigFile];
            
            if (!self._delegate.configPath)
                self._delegate.configPath = @"www/metaio/arelConfig.xml";
            
            NSString *xmlFileName = [[self._delegate.configPath lastPathComponent] stringByDeletingPathExtension];
            NSString *xmlPath = [self._delegate.configPath stringByDeletingLastPathComponent];
            NSString* arelConfigFile = [NSString stringWithFormat:@"%@", xmlFileName];
            NSString* arelDir = [NSString stringWithFormat:@"%@", xmlPath];
            NSString* arelConfigFilePath = [[NSBundle mainBundle] pathForResource:arelConfigFile ofType:@"xml" inDirectory:arelDir];
            
            if (arelConfigFilePath)
            {
                
                NSLog(@"Will load AREL config from: %@", arelConfigFilePath);
                
                self.metaio = [[ARELViewController alloc] initWithNibName:@"ARELViewController" bundle:nil instructions:arelConfigFilePath];
              
                [self.viewController presentViewController:self.metaio animated:YES completion:^{
                  
                  self.CDVDelegate = self.webView.delegate;
                  self.ARELDelegate = ((ARELViewController *)self.metaio).m_arelWebView.delegate;
                  
                  // //make adjustments, so areview could be work with cordova JSQueue
                  ((ARELViewController *)self.metaio).m_arelWebView.delegate = self;
                  ((CDVViewController *)self.viewController).webView = ((ARELViewController *)self.metaio).m_arelWebView;
                    
                }];
                
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:nil];
                
            }
            else
            {
                
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
                
            }
        }
        else
        {
            
            NSLog(@"Metaio being brought to foreground...");
            
            [self.viewController presentViewController:self.metaio animated:YES completion:^{
                
                //make adjustments, so areview could be work with cordova JSQueue
                ((ARELViewController *)self.metaio).m_arelWebView.delegate = self;
                ((CDVViewController *)self.viewController).webView = ((ARELViewController *)self.metaio).m_arelWebView;
                
                [((ARELViewController *)self.metaio).m_arelWebView stringByEvaluatingJavaScriptFromString:[NSString stringWithFormat:@"cordova.fireDocumentEvent('metaioopen')"]];
                
            }];
            
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:nil];
            
        }
        
    }
    @catch (NSException* exception)
    {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_JSON_EXCEPTION messageAsString:[exception reason]];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    
}

-(void) close:(CDVInvokedUrlCommand*)command
{
    
    CDVPluginResult* pluginResult = nil;
    
    NSLog(@"Metaio closing...");
    if (!viewOpened) return;
    viewOpened = NO;

    @try
    {
        
        if (self.metaio)
        {
            
            NSString *url = nil;
            
            if ([command.arguments count]> 0)
                url = [command.arguments objectAtIndex:0];
            
            ((CDVViewController *)self.viewController).webView = self.webView;
            
            [self.viewController dismissViewControllerAnimated:YES completion:nil];

            if (url && [url isKindOfClass:[NSString class]] && [url length] > 0)
            {
                [self.webView stringByEvaluatingJavaScriptFromString:[NSString stringWithFormat:@"cordova.fireDocumentEvent('metaioclose', { 'detail': '%@' })", url]];
            }
            else
            {
                [self.webView stringByEvaluatingJavaScriptFromString:[NSString stringWithFormat:@"cordova.fireDocumentEvent('metaioclose')"]];
            }
            
            NSLog(@"Metaio closed successfully...");
            
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:nil];
            
        }
        else
        {
            NSLog(@"Metaio close error...");
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
            
        }
        
    }
    @catch (NSException* exception)
    {
        NSLog(@"Metaio close exception...");
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_JSON_EXCEPTION messageAsString:[exception reason]];
        
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    
}


-(void) destroy:(CDVInvokedUrlCommand*)command
{
    
    // TODO
    
    CDVPluginResult* pluginResult = nil;

    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:nil];

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

}


- (BOOL)webView:(UIWebView*)webView shouldStartLoadWithRequest:(NSURLRequest*)request navigationType:(UIWebViewNavigationType)navigationType
{

    if ([CDVDelegate respondsToSelector:@selector(webView:shouldStartLoadWithRequest:navigationType:)])
    {
        [self.CDVDelegate webView:webView shouldStartLoadWithRequest:request navigationType:navigationType];
    }
    
    if ([ARELDelegate respondsToSelector:@selector(webView:shouldStartLoadWithRequest:navigationType:)])
    {
        [self.ARELDelegate webView:webView shouldStartLoadWithRequest:request navigationType:navigationType];
    }
    
    return YES;
}


/*
 
 webView:shouldStartLoadWithRequest:navigationType:
 – webViewDidStartLoad:
 – webViewDidFinishLoad:
 – webView:didFailLoadWithError:
 
 */

- (void)webViewDidStartLoad:(UIWebView*)webView
{
    if ([self.CDVDelegate respondsToSelector:@selector(webViewDidStartLoad:)])
    {
        [self.CDVDelegate webViewDidStartLoad:webView];
    }
    
    if ([self.ARELDelegate respondsToSelector:@selector(webViewDidStartLoad:)])
    {
        [self.ARELDelegate webViewDidStartLoad:webView];
    }
}


- (void)webViewDidFinishLoad:(UIWebView*)webView
{
    if ([self.CDVDelegate respondsToSelector:@selector(webViewDidFinishLoad:)])
    {
        [self.CDVDelegate webViewDidFinishLoad:webView];
    }
    
    if ([self.ARELDelegate respondsToSelector:@selector(webViewDidFinishLoad:)])
    {
        [self.ARELDelegate webViewDidFinishLoad:webView];
    }
}


- (void)webView:(UIWebView*)webView didFailLoadWithError:(NSError*)error
{
    if ([self.CDVDelegate respondsToSelector:@selector(webView:didFailLoadWithError:)])
    {
        [self.CDVDelegate  webView:webView didFailLoadWithError:error];
    }
}

@end
