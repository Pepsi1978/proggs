#import "JarvisObjC.h"

NSError * _Nullable JarvisObjCFangen(NS_NOESCAPE void (^block)(void)) {
    @try {
        block();
        return nil;
    } @catch (NSException *ausnahme) {
        NSString *text = [NSString stringWithFormat:@"%@: %@", ausnahme.name, ausnahme.reason ?: @"ohne Grund"];
        return [NSError errorWithDomain:@"JarvisObjC" code:1 userInfo:@{NSLocalizedDescriptionKey: text}];
    }
}
