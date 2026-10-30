#import "JarvisObjC.h"

static NSError *JarvisAlsFehler(NSException *ausnahme) {
    NSString *text = [NSString stringWithFormat:@"%@: %@", ausnahme.name, ausnahme.reason ?: @"ohne Grund"];
    return [NSError errorWithDomain:@"JarvisObjC" code:1 userInfo:@{NSLocalizedDescriptionKey: text}];
}

NSError * _Nullable JarvisSpielen(AVAudioPlayerNode *spieler) {
    @try {
        [spieler play];
        return nil;
    } @catch (NSException *ausnahme) {
        return JarvisAlsFehler(ausnahme);
    }
}

NSError * _Nullable JarvisVerbinden(AVAudioEngine *engine, AVAudioNode *von, AVAudioNode *nach, AVAudioFormat *format) {
    @try {
        [engine connect:von to:nach format:format];
        return nil;
    } @catch (NSException *ausnahme) {
        return JarvisAlsFehler(ausnahme);
    }
}

NSError * _Nullable JarvisAbgriffSetzen(AVAudioNode *knoten, AVAudioFrameCount groesse, AVAudioFormat *format, AVAudioNodeTapBlock block) {
    @try {
        [knoten installTapOnBus:0 bufferSize:groesse format:format block:block];
        return nil;
    } @catch (NSException *ausnahme) {
        return JarvisAlsFehler(ausnahme);
    }
}
