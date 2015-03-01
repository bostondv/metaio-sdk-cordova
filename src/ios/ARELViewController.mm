//
//  ARELViewController.m
//
// Copyright 2007-2014 metaio GmbH. All rights reserved.
//

#import "ARELViewController.h"


#include "TargetConditionals.h"		// to know if we're building for SIMULATOR
#include <metaioSDK/IMetaioSDKIOS.h>
#include <metaioSDK/IARELInterpreterIOS.h>
#include <metaioSDK/GestureHandler.h>


#import "EAGLView.h"
#import <UIKit/UIGestureRecognizerSubclass.h>


@interface MetaioTouchesRecognizer : UIGestureRecognizer
{
	UIViewController* theLiveViewController;
}
- (void) setTheLiveViewController:(UIViewController*) controller;
@end


@implementation MetaioTouchesRecognizer

- (void) setTheLiveViewController:(UIViewController*) controller
{
	theLiveViewController = controller;
}

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
	if (theLiveViewController)
	{
		[theLiveViewController touchesBegan:touches withEvent:event];
	}
}

- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
	if (theLiveViewController && [self numberOfTouches] == 1)
	{
		[theLiveViewController touchesMoved:touches withEvent:event];
	}
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
	if (theLiveViewController)
	{
		[theLiveViewController touchesEnded:touches withEvent:event];
	}
}

@end


@implementation ARELViewController

@synthesize m_arelWebView;

- (BOOL)canBecomeFirstResponder
{
	return YES;
}

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil instructions:(NSString *)arelTutorialConfig
{
	self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
	if (self) {
		m_arelFile = [[NSString alloc] initWithString:arelTutorialConfig];
	}
	return self;
}

- (void) onSDKReady
{
	// Load the AREL file after the SDK is ready
	m_ArelInterpreter->loadARELFile([m_arelFile fileSystemRepresentation]);
}


// Implement viewDidLoad to do additional setup after loading the view, typically from a nib.
- (void)viewDidLoad
{
	[super viewDidLoad];

	m_arelWebView.scrollView.bounces = NO;


	MetaioTouchesRecognizer* recognizer = [[MetaioTouchesRecognizer alloc] init];
	[recognizer setTheLiveViewController:self];
	[recognizer setDelegate:self];
	[m_arelWebView addGestureRecognizer:recognizer];
   	[recognizer release];

	m_pGestureHandlerIOS = [[GestureHandlerIOS alloc] initWithSDK:m_metaioSDK
														 withView:m_arelWebView
													 withGestures:metaio::GestureHandler::GESTURE_ALL];

	m_ArelInterpreter = metaio::CreateARELInterpreterIOS(m_arelWebView, self);

	m_ArelInterpreter->initialize( m_metaioSDK, m_pGestureHandlerIOS->m_pGestureHandler );

	m_ArelInterpreter->setRadarProperties(metaio::IGeometry::ANCHOR_TL, metaio::Vector3d(1), metaio::Vector3d(1));

	m_ArelInterpreter->registerDelegate(self);
}


- (void) viewWillAppear:(BOOL)animated
{
	[super viewWillAppear:animated];
}


- (void) viewDidAppear:(BOOL)animated
{
	[super viewDidAppear:animated];
	NSLog(@"AREL file %@", m_arelFile);

	[self startAnimation];
}


- (BOOL) shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation
{
	return YES;
}


- (void) viewWillDisappear:(BOOL)animated
{
	[super viewWillDisappear:animated];
}


- (void)didReceiveMemoryWarning
{
	// Releases the view if it doesn't have a superview.
	[super didReceiveMemoryWarning];

	// Release any cached data, images, etc that aren't in use.
}


- (void)viewDidUnload
{
	[super viewDidUnload];
}


- (void) viewDidLayoutSubviews
{
	float scale = [UIScreen mainScreen].scale;
	m_metaioSDK->resizeRenderer(self.glView.bounds.size.width*scale, self.glView.bounds.size.height*scale);
}


- (void)dealloc
{
	// Tear down context.
	if ([EAGLContext currentContext] == context)
		[EAGLContext setCurrentContext:nil];

	delete m_ArelInterpreter;
	[m_arelFile release];

	[super dealloc];
}


- (void)willAnimateRotationToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation duration:(NSTimeInterval)duration
{
	m_metaioSDK->setScreenRotation( metaio::getScreenRotationForInterfaceOrientation(interfaceOrientation) );
}


- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
	[m_pGestureHandlerIOS touchesBegan:touches withEvent:event withView:glView];
}


- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
	[m_pGestureHandlerIOS touchesMoved:touches withEvent:event withView:glView];
}


- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
	[m_pGestureHandlerIOS touchesEnded:touches withEvent:event withView:glView];
}


#pragma mark - IARELInterpreterIOSDelegate


- (void)image:(UIImage*)image didFinishSavingWithError:(NSError*)error contextInfo:(void*)contextInfo
{
	// Override to handle "saved to gallery" event
}


/** Take a snapshot and open the share screen
 * \param image							The image to save
 * \param saveToGalleryWithoutDialog	True if we should just save the image to the gallery without
 *										displaying the sharing dialog
 */
- (void)shareScreenshot:(UIImage*)image options:(bool)saveToGalleryWithoutDialog
{
	if (saveToGalleryWithoutDialog)
	{
		dispatch_async(dispatch_get_global_queue(0, 0), ^(void) {
			dispatch_async(dispatch_get_main_queue(), ^(void) {
				UIImageWriteToSavedPhotosAlbum(image, self, @selector(image:didFinishSavingWithError:contextInfo:), nil);
			});
		});
	}
	else
	{
		// Open a share controller with the image
		NSLog(@"Implement your sharing controller here.");
	}
}


- (void)drawFrame
{
	[glView setFramebuffer];

	// tell sdk to render
	if( m_ArelInterpreter )
	{
		m_ArelInterpreter->update();
	}

	[glView presentFramebuffer];
}

@end
