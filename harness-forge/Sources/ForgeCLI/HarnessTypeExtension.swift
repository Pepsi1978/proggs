// HarnessTypeExtension.swift
// Laesst HarnessType direkt als @Option-Wert in ArgumentParser verwenden.

import ArgumentParser
import HarnessForgeBuilders

extension HarnessType: ExpressibleByArgument {}
