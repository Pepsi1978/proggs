// Bridging-Header von Jarvis: sherpa-onnx (Pfad setzt build.sh per -I) und der Fänger für Objective-C-Ausnahmen.
#import <Foundation/Foundation.h>
#include "c-api.h"

NS_ASSUME_NONNULL_BEGIN

/// Führt den Block aus und gibt eine geworfene NSException als NSError zurück, sonst nil.
/// Swift kann NSExceptions nicht fangen: AVFAudio wirft sie etwa bei play() ohne Tonlauf, und ungefangen beenden sie die App.
NSError * _Nullable JarvisObjCFangen(NS_NOESCAPE void (^block)(void));

NS_ASSUME_NONNULL_END
