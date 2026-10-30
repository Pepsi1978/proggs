// Bridging-Header von Jarvis: sherpa-onnx (Pfad setzt build.sh per -I) und die Fänger für Objective-C-Ausnahmen.
#import <AVFoundation/AVFoundation.h>
#include "c-api.h"

NS_ASSUME_NONNULL_BEGIN

/// AVFAudio-Aufrufe, die NSExceptions werfen können. Swift kann sie nicht fangen, und ungefangen beenden sie die App.
/// Die Aufrufe liegen bewusst ganz in Objective-C: Eine Ausnahme darf beim Abwickeln keinen Swift-Frame durchlaufen.
/// Rückgabe: die Ausnahme als NSError (Domäne JarvisObjC), sonst nil.
NSError * _Nullable JarvisSpielen(AVAudioPlayerNode *spieler);
NSError * _Nullable JarvisVerbinden(AVAudioEngine *engine, AVAudioNode *von, AVAudioNode *nach, AVAudioFormat *format);
NSError * _Nullable JarvisAbgriffSetzen(AVAudioNode *knoten, AVAudioFrameCount groesse, AVAudioFormat *format, AVAudioNodeTapBlock block);

NS_ASSUME_NONNULL_END
