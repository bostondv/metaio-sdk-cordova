//
//  ARELConfigParser.m
//  ScanIt
//
//  Created by Munir Ahmed on 16/12/2013.
//
//

#import "ARELConfigParser.h"

@implementation ARELConfigParser

-(id) init
{
    self = [super init];
    
    if (self)
    {
        self.configPath = nil;
    }
    
    return self;
}

- (void)parser:(NSXMLParser*)parser didStartElement:(NSString*)elementName namespaceURI:(NSString*)namespaceURI qualifiedName:(NSString*)qualifiedName attributes:(NSDictionary*)attributeDict
{
    if ([elementName isEqualToString:@"preference"]) {
        if ([[attributeDict[@"name"] lowercaseString] isEqualToString:@"arelconfigpath"])
        {
            self.configPath =  attributeDict[@"value"];
        }
    }
}

- (void)parser:(NSXMLParser*)parser didEndElement:(NSString*)elementName namespaceURI:(NSString*)namespaceURI qualifiedName:(NSString*)qualifiedName
{
  
}

- (void)parser:(NSXMLParser*)parser parseErrorOccurred:(NSError*)parseError
{
}


@end
