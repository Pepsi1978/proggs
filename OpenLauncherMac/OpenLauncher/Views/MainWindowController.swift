import AppKit

/// Hauptfenster. Baut die Oberflaeche aus MainWindow.xaml nach:
///   Titelleiste (58 hoch) | Hauptbereich (Modelle | Provider + Profil | Thinking) | Fußleiste.
///
/// **Eine bewusste Abweichung von Windows:** dort ersetzt die App die Fensterknöpfe durch eigene
/// (WindowStyle="None"), weil die Windows-Standardleiste nicht ins Design passt. Auf macOS sind die
/// Ampel-Knöpfe links der Systemstandard - sie bleiben erhalten und werden NICHT nachgebaut
/// (doppelte Knöpfe wären verwirrend und würden Fensterverwaltung, Vollbild und Mission Control
/// stören). Alle Funktionen bleiben: Minimieren, Zoomen, Schließen über die Ampel, der
/// Design-Umschalter an derselben Stelle rechts wie unter Windows.
@MainActor
final class MainWindowController: NSWindowController, MainViewModelDelegate, NSWindowDelegate {
    private let viewModel: MainViewModel
    private let layoutSettings: LayoutSettings

    private let rootView = GradientBackgroundView()
    private let titleBar = TitleBarView()
    private let versionChip = SurfaceView(cornerRadius: 8)
    private let versionLabel = UI.label("", size: 12, role: .dim)
    private let themeButton = StyledButton(style: .theme, title: "☀︎")

    private let splitView = TransparentSplitView()
    private let modelCard = CardView()
    private let providerCard = CardView()
    private let profileCard = CardView()
    private let thinkingCard = CardView()

    private var modelListView: ModelListView!
    private var providerTableView: ProviderTableView!
    private var profileSectionView: ProfileSectionView!
    private var thinkingListView: ThinkingListView!

    private let hiddenModelsButton = StyledButton(style: .ghost, title: "Ausgeblendet (0)")
    private let addModelButton = StyledButton(style: .ghost, title: "+ Hinzufügen")
    private let editModelButton = StyledButton(style: .ghost, title: "Bearbeiten")
    private let removeModelButton = StyledButton(style: .ghost, title: "Entfernen")
    private let providerTitleLabel = UI.label("PROVIDER ", size: 12, weight: .bold, role: .muted)
    private let providerModelLabel = UI.label("—", size: 15, weight: .bold, role: .accent)
    private let refreshButton = StyledButton(style: .ghost, title: "Aktualisieren")

    private let workDirField = RoundedTextField()
    private let browseButton = StyledButton(style: .ghost, title: "…")
    private let errorDetailsButton = StyledButton(style: .ghost, title: "Fehlerdetails")
    private let logsButton = StyledButton(style: .ghost, title: "Logs")
    private let statusLabel = UI.label("Bereit.", size: 13, role: .dim)
    private let startButton = StyledButton(style: .accent, title: "▶ Start")

    /// Breite der Effort-/Thinking-Spalte (Width="210" in XAML).
    static let thinkingColumnWidth: CGFloat = 210

    /// Layout-Speicherung wird gebuendelt: Bewegen/Groessenaenderung feuert sehr haeufig - ohne
    /// Verzoegerung loeste jedes Pixel eine JSON-Schreiboperation aus (wie der DispatcherTimer
    /// unter Windows).
    private var layoutSaveWorkItem: DispatchWorkItem?

    /// Erst wenn die gespeicherte Teilerposition wirklich gesetzt ist, darf sie wieder
    /// mitgeschrieben werden. Ohne diese Sperre ueberschreiben die Layout-Durchlaeufe waehrend des
    /// Fensteraufbaus den gemerkten Wert mit einem Zwischenstand - die Position "vergaesse" sich
    /// dann bei jedem Start.
    private var didRestoreSplitPosition = false

    init(viewModel: MainViewModel, layoutSettings: LayoutSettings) {
        self.viewModel = viewModel
        self.layoutSettings = layoutSettings

        let window = NSWindow(
            contentRect: NSRect(x: 0, y: 0, width: layoutSettings.windowWidth, height: layoutSettings.windowHeight),
            // StyleMask final im Init (Bug-Almanach swift-appkit §A2/§A5 - nie zur Laufzeit umschalten).
            styleMask: [.titled, .closable, .miniaturizable, .resizable, .fullSizeContentView],
            backing: .buffered,
            defer: false)
        window.title = "OpenLauncher"
        window.titleVisibility = .hidden
        window.titlebarAppearsTransparent = true
        window.minSize = NSSize(width: LayoutSettings.minWindowWidth, height: LayoutSettings.minWindowHeight)
        window.isReleasedWhenClosed = false

        super.init(window: window)
        window.delegate = self

        buildInterface()
        restoreWindowLayout()
        applyTheme()

        viewModel.delegate = self
        NotificationCenter.default.addObserver(self, selector: #selector(applyTheme),
                                               name: ThemeManager.themeChangedNotification, object: nil)

        reloadEverything()
        viewModel.activateInitialSelection()
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }
    deinit { NotificationCenter.default.removeObserver(self) }

    // MARK: - Aufbau

    private func buildInterface() {
        guard let window else { return }
        rootView.translatesAutoresizingMaskIntoConstraints = false
        window.contentView = rootView

        buildTitleBar()
        buildMainArea()
        let footer = buildFooter()

        NSLayoutConstraint.activate([
            titleBar.topAnchor.constraint(equalTo: rootView.topAnchor),
            titleBar.leadingAnchor.constraint(equalTo: rootView.leadingAnchor),
            titleBar.trailingAnchor.constraint(equalTo: rootView.trailingAnchor),
            // Flacher als unter Windows (dort 58): auf macOS liegen die Ampel-Knoepfe ohnehin in
            // dieser Leiste, und der freie Streifen darueber war ungenutzt.
            titleBar.heightAnchor.constraint(equalToConstant: 44),

            // Karten ruecken dicht an die Leiste heran - der Platz gehoert dem Inhalt.
            splitView.topAnchor.constraint(equalTo: titleBar.bottomAnchor, constant: 6),
            splitView.leadingAnchor.constraint(equalTo: rootView.leadingAnchor, constant: 16),
            splitView.trailingAnchor.constraint(equalTo: rootView.trailingAnchor, constant: -16),
            splitView.bottomAnchor.constraint(equalTo: footer.topAnchor, constant: -16),

            footer.leadingAnchor.constraint(equalTo: rootView.leadingAnchor),
            footer.trailingAnchor.constraint(equalTo: rootView.trailingAnchor),
            footer.bottomAnchor.constraint(equalTo: rootView.bottomAnchor),
            footer.heightAnchor.constraint(equalToConstant: 62)
        ])
    }

    private func buildTitleBar() {
        titleBar.translatesAutoresizingMaskIntoConstraints = false
        titleBar.wantsLayer = true
        // Doppelklick auf die Leiste maximiert - wie bei jeder macOS-App. Ohne das schluckt die
        // eigene Leiste die Klicks, weil sie ueber der (transparenten) Systemtitelleiste liegt.
        titleBar.onDoubleClick = { [weak self] in self?.zoomToScreen() }
        rootView.addSubview(titleBar)

        let badge = LogoBadgeView()
        let appLabel = UI.label("OpenLauncher", size: 15, weight: .semibold)
        versionChip.translatesAutoresizingMaskIntoConstraints = false
        versionChip.addSubview(versionLabel)

        themeButton.fontSize = 15
        themeButton.horizontalPadding = 12
        themeButton.verticalPadding = 8
        themeButton.toolTip = "Design wechseln"
        themeButton.target = self
        themeButton.action = #selector(toggleTheme)

        for view in [badge, appLabel, versionChip, themeButton] as [NSView] {
            view.translatesAutoresizingMaskIntoConstraints = false
            titleBar.addSubview(view)
        }

        NSLayoutConstraint.activate([
            // 78 statt 18: links liegen die macOS-Ampel-Knoepfe, die nicht ueberdeckt werden duerfen.
            badge.leadingAnchor.constraint(equalTo: titleBar.leadingAnchor, constant: 78),
            badge.centerYAnchor.constraint(equalTo: titleBar.centerYAnchor),
            badge.widthAnchor.constraint(equalToConstant: 32),
            badge.heightAnchor.constraint(equalToConstant: 32),

            appLabel.leadingAnchor.constraint(equalTo: badge.trailingAnchor, constant: 12),
            appLabel.centerYAnchor.constraint(equalTo: titleBar.centerYAnchor),

            versionChip.leadingAnchor.constraint(equalTo: appLabel.trailingAnchor, constant: 12),
            versionChip.centerYAnchor.constraint(equalTo: titleBar.centerYAnchor),
            versionLabel.leadingAnchor.constraint(equalTo: versionChip.leadingAnchor, constant: 9),
            versionLabel.trailingAnchor.constraint(equalTo: versionChip.trailingAnchor, constant: -9),
            versionLabel.topAnchor.constraint(equalTo: versionChip.topAnchor, constant: 3),
            versionLabel.bottomAnchor.constraint(equalTo: versionChip.bottomAnchor, constant: -3),

            themeButton.trailingAnchor.constraint(equalTo: titleBar.trailingAnchor, constant: -18),
            themeButton.centerYAnchor.constraint(equalTo: titleBar.centerYAnchor),
            themeButton.widthAnchor.constraint(equalToConstant: 42),
            themeButton.heightAnchor.constraint(equalToConstant: 33)
        ])
    }

    private func buildMainArea() {
        splitView.translatesAutoresizingMaskIntoConstraints = false
        splitView.isVertical = true
        splitView.dividerStyle = .thin
        splitView.delegate = self
        rootView.addSubview(splitView)

        // --- Linke Spalte: Modelle ---
        modelListView = ModelListView(viewModel: viewModel)

        let modelTitle = UI.label("MODELLE", size: 12, weight: .bold, role: .muted)
        for button in [hiddenModelsButton, addModelButton, editModelButton, removeModelButton] {
            button.fontSize = 12
            button.horizontalPadding = 11
            button.verticalPadding = 6
        }
        hiddenModelsButton.target = self
        hiddenModelsButton.action = #selector(showHiddenModels)
        addModelButton.target = self
        addModelButton.action = #selector(addModel)
        editModelButton.target = self
        editModelButton.action = #selector(editModel)
        removeModelButton.target = self
        removeModelButton.action = #selector(removeModel)

        // Ueberschrift vor den Schaltern schuetzen: ohne das staucht NSStackView bei schmaler
        // Modell-Spalte zuerst den Text ("MODELLE" wurde zu "MOD").
        modelTitle.setContentCompressionResistancePriority(.required, for: .horizontal)
        modelTitle.setContentHuggingPriority(.required, for: .horizontal)
        for button in [hiddenModelsButton, addModelButton] {
            button.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        }
        let modelHeader = NSStackView(views: [modelTitle, UI.spacer(), hiddenModelsButton, addModelButton])
        modelHeader.orientation = .horizontal
        modelHeader.spacing = 8
        modelHeader.translatesAutoresizingMaskIntoConstraints = false

        let modelFooter = NSStackView(views: [editModelButton, removeModelButton])
        modelFooter.orientation = .horizontal
        modelFooter.distribution = .fillEqually
        modelFooter.spacing = 8
        modelFooter.translatesAutoresizingMaskIntoConstraints = false

        modelCard.translatesAutoresizingMaskIntoConstraints = false
        modelCard.addSubview(modelHeader)
        modelCard.addSubview(modelListView)
        modelCard.addSubview(modelFooter)

        NSLayoutConstraint.activate([
            modelHeader.topAnchor.constraint(equalTo: modelCard.topAnchor, constant: 16),
            modelHeader.leadingAnchor.constraint(equalTo: modelCard.leadingAnchor, constant: 16),
            modelHeader.trailingAnchor.constraint(equalTo: modelCard.trailingAnchor, constant: -16),

            modelListView.topAnchor.constraint(equalTo: modelHeader.bottomAnchor, constant: 12),
            modelListView.leadingAnchor.constraint(equalTo: modelCard.leadingAnchor, constant: 12),
            modelListView.trailingAnchor.constraint(equalTo: modelCard.trailingAnchor, constant: -12),
            modelListView.bottomAnchor.constraint(equalTo: modelFooter.topAnchor, constant: -10),

            modelFooter.leadingAnchor.constraint(equalTo: modelCard.leadingAnchor, constant: 16),
            modelFooter.trailingAnchor.constraint(equalTo: modelCard.trailingAnchor, constant: -16),
            modelFooter.bottomAnchor.constraint(equalTo: modelCard.bottomAnchor, constant: -14)
        ])

        // --- Rechte Seite: Provider (oben), Profil (unten), Thinking (ganze Hoehe rechts) ---
        let rightContainer = NSView()
        rightContainer.translatesAutoresizingMaskIntoConstraints = false

        providerTableView = ProviderTableView(viewModel: viewModel)
        refreshButton.fontSize = 12
        refreshButton.horizontalPadding = 10
        refreshButton.verticalPadding = 6
        refreshButton.target = self
        refreshButton.action = #selector(refresh)

        let providerHeader = NSStackView(views: [providerTitleLabel, providerModelLabel, UI.spacer(), refreshButton])
        providerHeader.orientation = .horizontal
        providerHeader.spacing = 6
        providerHeader.translatesAutoresizingMaskIntoConstraints = false
        providerModelLabel.lineBreakMode = .byTruncatingTail

        providerCard.translatesAutoresizingMaskIntoConstraints = false
        providerCard.addSubview(providerHeader)
        providerCard.addSubview(providerTableView)

        profileSectionView = ProfileSectionView(viewModel: viewModel)
        profileCard.translatesAutoresizingMaskIntoConstraints = false
        profileCard.addSubview(profileSectionView)

        thinkingListView = ThinkingListView(viewModel: viewModel)
        thinkingCard.translatesAutoresizingMaskIntoConstraints = false
        thinkingCard.addSubview(thinkingListView)

        for card in [providerCard, profileCard, thinkingCard] { rightContainer.addSubview(card) }

        // Alle Karten sollen gleich eng aneinander liegen. Der Abstand zwischen Modell- und
        // Provider-Karte entsteht durch den Trenner des Splitters - genau dessen Breite wird hier
        // auch fuer die uebrigen Fugen verwendet, statt einen festen Wert zu raten. So bleiben die
        // Abstaende auch dann identisch, wenn macOS den Trenner anders zeichnet.
        let gap = splitView.dividerThickness

        NSLayoutConstraint.activate([
            providerHeader.topAnchor.constraint(equalTo: providerCard.topAnchor, constant: 15),
            providerHeader.leadingAnchor.constraint(equalTo: providerCard.leadingAnchor, constant: 18),
            providerHeader.trailingAnchor.constraint(equalTo: providerCard.trailingAnchor, constant: -18),
            providerTableView.topAnchor.constraint(equalTo: providerHeader.bottomAnchor, constant: 12),
            providerTableView.leadingAnchor.constraint(equalTo: providerCard.leadingAnchor, constant: 14),
            providerTableView.trailingAnchor.constraint(equalTo: providerCard.trailingAnchor, constant: -14),
            providerTableView.bottomAnchor.constraint(equalTo: providerCard.bottomAnchor, constant: -6),

            profileSectionView.topAnchor.constraint(equalTo: profileCard.topAnchor, constant: 15),
            profileSectionView.leadingAnchor.constraint(equalTo: profileCard.leadingAnchor, constant: 14),
            profileSectionView.trailingAnchor.constraint(equalTo: profileCard.trailingAnchor, constant: -14),
            profileSectionView.bottomAnchor.constraint(lessThanOrEqualTo: profileCard.bottomAnchor, constant: -12),
            // Karte klemmt ihren Inhalt: ohne das ragte die Kopfzeile bei knappem Platz darueber hinaus.

            thinkingListView.topAnchor.constraint(equalTo: thinkingCard.topAnchor, constant: 15),
            thinkingListView.leadingAnchor.constraint(equalTo: thinkingCard.leadingAnchor, constant: 12),
            thinkingListView.trailingAnchor.constraint(equalTo: thinkingCard.trailingAnchor, constant: -12),
            thinkingListView.bottomAnchor.constraint(equalTo: thinkingCard.bottomAnchor, constant: -12),

            // Spalten: Provider/Profil links (flexibel), Thinking rechts mit fester Breite 210
            // (MinWidth 190, MaxWidth 260 in XAML).
            providerCard.topAnchor.constraint(equalTo: rightContainer.topAnchor),
            providerCard.leadingAnchor.constraint(equalTo: rightContainer.leadingAnchor),
            providerCard.trailingAnchor.constraint(equalTo: thinkingCard.leadingAnchor, constant: -gap),

            profileCard.topAnchor.constraint(equalTo: providerCard.bottomAnchor, constant: gap),
            profileCard.leadingAnchor.constraint(equalTo: rightContainer.leadingAnchor),
            profileCard.trailingAnchor.constraint(equalTo: thinkingCard.leadingAnchor, constant: -gap),
            profileCard.bottomAnchor.constraint(equalTo: rightContainer.bottomAnchor),
            profileCard.heightAnchor.constraint(equalTo: providerCard.heightAnchor),

            thinkingCard.topAnchor.constraint(equalTo: rightContainer.topAnchor),
            thinkingCard.trailingAnchor.constraint(equalTo: rightContainer.trailingAnchor),
            thinkingCard.bottomAnchor.constraint(equalTo: rightContainer.bottomAnchor),
            thinkingCard.widthAnchor.constraint(equalToConstant: Self.thinkingColumnWidth)
        ])

        splitView.addArrangedSubview(modelCard)
        splitView.addArrangedSubview(rightContainer)
        // Die endgueltige Position wird erst in showWindow gesetzt: hier steht die Breite des
        // Splitters noch nicht fest, und AppKit wuerde den Wert beim ersten Layout verwerfen.
    }

    private func buildFooter() -> NSView {
        let footer = FooterView()
        footer.translatesAutoresizingMaskIntoConstraints = false
        rootView.addSubview(footer)

        let caption = UI.label("Arbeitsverzeichnis", size: 13, role: .muted)
        workDirField.translatesAutoresizingMaskIntoConstraints = false
        workDirField.target = self
        workDirField.action = #selector(workDirEdited)
        workDirField.delegate = self

        for button in [browseButton, errorDetailsButton, logsButton] {
            button.fontSize = 13
            button.horizontalPadding = 13
            button.verticalPadding = 7
        }
        browseButton.horizontalPadding = 14
        browseButton.target = self
        browseButton.action = #selector(browseWorkDir)
        errorDetailsButton.target = self
        errorDetailsButton.action = #selector(showLastError)
        errorDetailsButton.isEnabled = false
        logsButton.target = self
        logsButton.action = #selector(openLogFolder)

        startButton.fontSize = 14
        startButton.horizontalPadding = 26
        startButton.verticalPadding = 10
        startButton.target = self
        startButton.action = #selector(start)

        statusLabel.lineBreakMode = .byTruncatingTail

        for view in [caption, workDirField, browseButton, errorDetailsButton, logsButton, statusLabel, startButton] as [NSView] {
            view.translatesAutoresizingMaskIntoConstraints = false
            footer.addSubview(view)
        }

        let statusMaxWidth = statusLabel.widthAnchor.constraint(lessThanOrEqualToConstant: 360)
        statusMaxWidth.priority = .defaultHigh

        NSLayoutConstraint.activate([
            caption.leadingAnchor.constraint(equalTo: footer.leadingAnchor, constant: 16),
            caption.centerYAnchor.constraint(equalTo: footer.centerYAnchor),

            workDirField.leadingAnchor.constraint(equalTo: caption.trailingAnchor, constant: 10),
            workDirField.centerYAnchor.constraint(equalTo: footer.centerYAnchor),
            workDirField.trailingAnchor.constraint(equalTo: browseButton.leadingAnchor, constant: -8),

            browseButton.trailingAnchor.constraint(equalTo: errorDetailsButton.leadingAnchor, constant: -8),
            browseButton.centerYAnchor.constraint(equalTo: footer.centerYAnchor),

            errorDetailsButton.trailingAnchor.constraint(equalTo: logsButton.leadingAnchor, constant: -8),
            errorDetailsButton.centerYAnchor.constraint(equalTo: footer.centerYAnchor),

            logsButton.trailingAnchor.constraint(equalTo: statusLabel.leadingAnchor, constant: -8),
            logsButton.centerYAnchor.constraint(equalTo: footer.centerYAnchor),

            statusLabel.trailingAnchor.constraint(equalTo: startButton.leadingAnchor, constant: -16),
            statusLabel.centerYAnchor.constraint(equalTo: footer.centerYAnchor),
            statusMaxWidth,

            startButton.trailingAnchor.constraint(equalTo: footer.trailingAnchor, constant: -16),
            startButton.centerYAnchor.constraint(equalTo: footer.centerYAnchor)
        ])

        workDirField.setContentHuggingPriority(.defaultLow, for: .horizontal)
        workDirField.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        return footer
    }

    // MARK: - Aktualisieren

    private func reloadEverything() {
        modelListView.reload()
        providerTableView.reload()
        thinkingListView.reload()
        profileSectionView.refresh()
        statusChanged()
        workDirChanged()
        hiddenModelsChanged()
        selectedModelChanged()
        versionLabel.stringValue = viewModel.version
    }

    @objc private func applyTheme() {
        guard let window else { return }
        window.appearance = NSAppearance(named: ThemeManager.current == .dark ? .darkAqua : .aqua)
        rootView.applyTheme()
        titleBar.layer?.backgroundColor = ThemeManager.palette.titleBarBg.cgColor
        // Design-Knopf zeigt das ZIEL-Design: Sonne im Dunkelmodus, Mond im Tagmodus.
        themeButton.title = ThemeManager.current == .dark ? "☀︎" : "☾"
        themeButton.toolTip = ThemeManager.current == .dark ? "Zum Tagmodus wechseln" : "Zum Nachtmodus wechseln"
        splitView.needsDisplay = true
    }

    // MARK: - Aktionen

    @objc private func toggleTheme() {
        ThemeManager.toggle()
        layoutSettings.theme = ThemeManager.current.rawValue
        layoutSettings.save()
    }

    @objc private func addModel() { viewModel.addModel() }
    @objc private func editModel() { viewModel.editModel() }
    @objc private func removeModel() { viewModel.removeModel() }
    @objc private func showHiddenModels() { viewModel.showHiddenModels() }
    @objc private func refresh() { viewModel.refresh() }
    @objc private func browseWorkDir() { viewModel.browseWorkDir() }
    @objc private func showLastError() { viewModel.showLastError() }
    @objc private func openLogFolder() { viewModel.openLogFolder() }
    @objc private func start() { viewModel.start() }
    @objc private func workDirEdited() { viewModel.workDir = workDirField.stringValue }

    // MARK: - Fenster-Layout

    /// Zeigt das Fenster bildschirmfuellend an (wie ein Doppelklick auf die Titelleiste) und stellt
    /// danach die gemerkte Teilerposition wieder her.
    override func showWindow(_ sender: Any?) {
        super.showWindow(sender)
        guard let window else { return }

        // Bildschirmfuellend starten - exakt bis an Menueleiste und Dock heran.
        if !window.isZoomed { zoomToScreen() }

        // Erst nach dem Zoomen steht die Breite des Splitters fest; vorher gesetzte Positionen
        // verwirft AppKit beim naechsten Layout-Durchlauf.
        DispatchQueue.main.async { [weak self] in
            guard let self, let splitView = self.window != nil ? self.splitView : nil else { return }
            splitView.setPosition(self.layoutSettings.modelPaneWidth, ofDividerAt: 0)
            self.didRestoreSplitPosition = true
        }
    }

    /// Maximiert bzw. stellt wieder her (wie der gruene Knopf und der Doppelklick auf die Leiste).
    private func zoomToScreen() {
        window?.zoom(nil)
    }

    private func restoreWindowLayout() {
        guard let window else { return }
        // NaN-Sentinel = noch nie gespeichert. Echte negative Koordinaten (linker/oberer Monitor)
        // sind gueltig und werden unten geklemmt.
        guard !layoutSettings.windowLeft.isNaN, !layoutSettings.windowTop.isNaN else {
            window.center()
            if layoutSettings.windowState == "Maximized" { window.zoom(nil) }
            return
        }

        var frame = NSRect(x: layoutSettings.windowLeft, y: layoutSettings.windowTop,
                           width: Swift.max(layoutSettings.windowWidth, LayoutSettings.minWindowWidth),
                           height: Swift.max(layoutSettings.windowHeight, LayoutSettings.minWindowHeight))
        // Position immer auf einen sichtbaren Bildschirm klemmen (Bug-Almanach swift-appkit §B5:
        // eine gespeicherte Position darf nach Monitorwechsel nicht aus dem Bild ragen). NUR die
        // Position wird korrigiert, nie die Groesse.
        frame = Self.clampToVisibleScreen(frame)
        window.setFrame(frame, display: false)
        if layoutSettings.windowState == "Maximized" { window.zoom(nil) }
    }

    /// Waehlt den Bildschirm mit dem groessten Ueberlapp und schiebt das Fenster in dessen
    /// sichtbaren Bereich zurueck.
    static func clampToVisibleScreen(_ frame: NSRect) -> NSRect {
        let screens = NSScreen.screens
        guard !screens.isEmpty else { return frame }
        let best = screens.max { a, b in
            a.visibleFrame.intersection(frame).area < b.visibleFrame.intersection(frame).area
        } ?? NSScreen.main ?? screens[0]

        let visible = best.visibleFrame
        var result = frame
        result.origin.x = Swift.min(Swift.max(result.origin.x, visible.minX), Swift.max(visible.minX, visible.maxX - result.width))
        result.origin.y = Swift.min(Swift.max(result.origin.y, visible.minY), Swift.max(visible.minY, visible.maxY - result.height))
        return result
    }

    private func queueSaveWindowLayout() {
        layoutSaveWorkItem?.cancel()
        let item = DispatchWorkItem { [weak self] in self?.saveWindowLayout() }
        layoutSaveWorkItem = item
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4, execute: item)
    }

    private func saveWindowLayout() {
        guard let window, !window.isMiniaturized else { return }
        let frame = window.frame
        guard frame.width >= LayoutSettings.minWindowWidth, frame.height >= LayoutSettings.minWindowHeight else { return }

        layoutSettings.windowLeft = frame.origin.x
        layoutSettings.windowTop = frame.origin.y
        layoutSettings.windowWidth = frame.width
        layoutSettings.windowHeight = frame.height
        layoutSettings.windowState = window.isZoomed ? "Maximized" : "Normal"
        layoutSettings.save()
    }

    // MARK: - NSWindowDelegate

    /// Legt fest, welchen Rahmen "Maximieren" verwendet. Ohne diese Antwort waehlt AppKit einen
    /// eigenen "besten" Rahmen und laesst unten einen Streifen zum Dock frei. `visibleFrame` ist
    /// exakt der Bereich zwischen Menueleiste und Dock - damit klebt das Fenster fugenlos daran.
    func windowWillUseStandardFrame(_ window: NSWindow, defaultFrame newFrame: NSRect) -> NSRect {
        (window.screen ?? NSScreen.main)?.visibleFrame ?? newFrame
    }

    func windowDidResize(_ notification: Notification) { queueSaveWindowLayout() }
    func windowDidMove(_ notification: Notification) { queueSaveWindowLayout() }

    func windowWillClose(_ notification: Notification) {
        layoutSaveWorkItem?.cancel()
        saveWindowLayout()
        NSApp.terminate(nil)
    }

    // MARK: - MainViewModelDelegate

    func modelGroupsChanged() {
        modelListView.reload()
    }

    func selectedModelChanged() {
        modelListView.refreshSelection()
        providerModelLabel.stringValue = viewModel.selectedModel?.displayName ?? "—"
    }

    func providersChanged() {
        providerTableView.reload()
    }

    func thinkingOptionsChanged() {
        thinkingListView.reload()
        profileSectionView.refresh()
    }

    func profileStateChanged() {
        profileSectionView.refresh()
        thinkingListView.refreshSelection()
    }

    func statusChanged() {
        statusLabel.stringValue = viewModel.statusText
        errorDetailsButton.isEnabled = viewModel.hasLastError
    }

    func hiddenModelsChanged() {
        hiddenModelsButton.title = "Ausgeblendet (\(viewModel.hiddenModels.count))"
        hiddenModelsButton.isEnabled = viewModel.hasHiddenModels
    }

    func workDirChanged() {
        if workDirField.stringValue != viewModel.workDir { workDirField.stringValue = viewModel.workDir }
    }

    func askModelDialog(groups: [ModelGroupEntry], defaultGroup: ModelGroupEntry, title: String,
                        confirmText: String, initialSlug: String,
                        initialDisplayName: String) -> (group: ModelGroupEntry, slug: String, displayName: String)? {
        Dialogs.modelDialog(parent: window, groups: groups, defaultGroup: defaultGroup, title: title,
                            confirmText: confirmText, initialSlug: initialSlug,
                            initialDisplayName: initialDisplayName)
    }

    func confirmRemoveModel(displayName: String, slug: String) -> Bool {
        Dialogs.confirmRemoveModel(parent: window, displayName: displayName, slug: slug)
    }

    func showHiddenModelsWindow() {
        Dialogs.showHiddenModels(parent: window, viewModel: viewModel)
    }

    func showLastErrorWindow() {
        Dialogs.showErrorDetails(parent: window, details: viewModel.lastErrorDetails)
    }

    func editProfileDocument(documents: InstructionProfileDocuments, isClaudeCode: Bool,
                             profileName: String) -> String? {
        let fileName = InstructionProfileService.activeFileName(isClaudeCode: isClaudeCode)
        let intro = "Dieses Profil ist genau EINE Datei (\(fileName)). Ihr Inhalt wird beim Start des Profils "
                  + "in die aktive \(fileName) geschrieben (vorher geleert). Änderungen gelten für neu gestartete Sessions."
        return Dialogs.editText(parent: window,
                                windowTitle: "Profil bearbeiten",
                                headline: "Profil \(profileName) bearbeiten",
                                cliText: (isClaudeCode ? "Claude Code · " : "OpenCode · ") + fileName,
                                introText: intro,
                                filePath: documents.globalPath,
                                text: documents.globalText,
                                saveButtonTitle: "Profil speichern")
    }

    func editWorkModePrompt(workModeName: String, sourcePath: String, promptText: String) -> String? {
        let intro = "Dieser Text ist der komplette Modus-Prompt und wird genau so verwendet: OpenCode setzt ihn "
                  + "bei jedem Modellaufruf als Systemprompt (auch beim Umschalten in der TUI), Claude Code hängt "
                  + "ihn beim Start hinter das gewählte Profil. Leer lassen heißt: dieser Modus ergänzt nichts."
        return Dialogs.editText(parent: window,
                                windowTitle: "Modus bearbeiten",
                                headline: "Modus \(workModeName) bearbeiten",
                                cliText: "Claude Code + OpenCode · Modus-Prompt",
                                introText: intro,
                                filePath: sourcePath,
                                text: promptText,
                                saveButtonTitle: "Modus speichern")
    }

    func browseWorkDirectory(current: String) -> String? {
        let panel = NSOpenPanel()
        panel.title = "Arbeitsverzeichnis für OpenCode wählen"
        panel.canChooseDirectories = true
        panel.canChooseFiles = false
        panel.allowsMultipleSelection = false
        panel.directoryURL = URL(fileURLWithPath: Paths.directoryExists(current) ? current : Paths.home)
        guard panel.runModal() == .OK, let url = panel.url else { return nil }
        return url.path
    }
}

// MARK: - NSSplitViewDelegate

extension MainWindowController: NSSplitViewDelegate {
    func splitView(_ splitView: NSSplitView, constrainMinCoordinate proposedMinimumPosition: CGFloat,
                   ofSubviewAt dividerIndex: Int) -> CGFloat {
        240   // MinWidth der Modell-Spalte aus XAML
    }

    func splitView(_ splitView: NSSplitView, constrainMaxCoordinate proposedMaximumPosition: CGFloat,
                   ofSubviewAt dividerIndex: Int) -> CGFloat {
        // MaxWidth der Modell-Spalte aus XAML (760) UND die dort ebenfalls gesetzte MinWidth der
        // Provider-/Profil-Spalte (540). Ohne die zweite Grenze liesse sich die mittlere Spalte
        // beliebig schmal ziehen - dann brechen Schalterbeschriftungen und Kacheltexte um.
        let rightMinimum = 540 + splitView.dividerThickness + Self.thinkingColumnWidth
        let maximumForRight = splitView.bounds.width - splitView.dividerThickness - rightMinimum
        return Swift.min(760, maximumForRight, proposedMaximumPosition)
    }

    func splitViewDidResizeSubviews(_ notification: Notification) {
        // Waehrend des Fensteraufbaus feuert das mehrfach mit Zwischenwerten - die duerfen die
        // gemerkte Position nicht ueberschreiben (siehe didRestoreSplitPosition).
        guard didRestoreSplitPosition, modelCard.frame.width > 1 else { return }
        layoutSettings.modelPaneWidth = modelCard.frame.width
        queueSaveWindowLayout()
    }
}

// MARK: - NSTextFieldDelegate (Arbeitsverzeichnis live uebernehmen)

extension MainWindowController: NSTextFieldDelegate {
    func controlTextDidChange(_ obj: Notification) {
        // UpdateSourceTrigger=PropertyChanged in XAML: jede Eingabe wirkt sofort.
        guard let field = obj.object as? NSTextField, field === workDirField else { return }
        viewModel.workDir = field.stringValue
    }
}

// MARK: - Hintergruende

/// Fensterhintergrund mit dem Farbverlauf aus `DesktopBg`.
final class GradientBackgroundView: NSView {
    override var isFlipped: Bool { true }

    override func draw(_ dirtyRect: NSRect) {
        let palette = ThemeManager.palette
        let gradient = NSGradient(colors: [palette.desktopBgTop, palette.desktopBgMid, palette.desktopBgBottom],
                                  atLocations: [0.0, 0.6, 1.0], colorSpace: .sRGB)
        gradient?.draw(in: bounds, angle: -90)
    }

    @objc func applyTheme() { needsDisplay = true }
}

/// Splitter ohne sichtbaren Trenner: der graue Systemstrich zwischen den Karten passte nicht zum
/// Design (er lief als graue Flaeche in die Kartenrundungen hinein). Die Fuge bleibt gleich breit -
/// nur gezeichnet wird sie nicht mehr, dort steht jetzt der Fensterverlauf. Ziehen geht weiterhin.
final class TransparentSplitView: NSSplitView {
    override var dividerColor: NSColor { .clear }
    override func drawDivider(in rect: NSRect) { /* bewusst leer */ }
}

/// Fußleiste mit oberer Trennlinie (`BorderThickness="0,1,0,0"` in XAML).
final class FooterView: NSView {
    override var isFlipped: Bool { true }

    override func draw(_ dirtyRect: NSRect) {
        let palette = ThemeManager.palette
        palette.titleBarBg.setFill()
        bounds.fill()
        palette.borderSoft.setStroke()
        let line = NSBezierPath()
        line.move(to: NSPoint(x: 0, y: 0.5))
        line.line(to: NSPoint(x: bounds.width, y: 0.5))
        line.lineWidth = 1
        line.stroke()
    }
}

private extension NSRect {
    var area: CGFloat { width * height }
}
