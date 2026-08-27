import AppKit

/// Provider-Tabelle. Entspricht dem `DataGrid` aus MainWindow.xaml mit denselben sieben Spalten,
/// derselben Sortierbarkeit, denselben Zahlenformaten und der Status-Pille.
@MainActor
final class ProviderTableView: NSView, NSTableViewDataSource, NSTableViewDelegate {
    private let tableView = NSTableView()
    private let scrollView = NSScrollView()
    private weak var viewModel: MainViewModel?

    /// Sortierte Ansicht auf viewModel.providers. Die Reihenfolge des ViewModels bleibt unangetastet
    /// (dort steht die Preis-Sortierung von OpenRouter); die Tabelle sortiert nur die Anzeige.
    private var rows: [ProviderEntry] = []
    private var sortColumn: String = ""
    private var sortAscending = true
    /// Zuletzt verteilte Breite - verhindert, dass jede Layout-Runde die Spalten neu setzt.
    private var lastFittedWidth: CGFloat = -1

    private struct Column {
        let identifier: String
        let title: String
        let width: CGFloat
        let minWidth: CGFloat
        let flexible: Bool
    }

    // Breiten wie in XAML. Anders als dort duerfen sie hier mitwachsen und -schrumpfen: WPF
    // verkleinert ein DataGrid mit "Auto"-Spalten von selbst, NSTableView nicht - mit festen
    // Breiten (Summe 1170) waere die letzte Spalte in einem 1050 breiten Fenster abgeschnitten.
    private static let columns: [Column] = [
        Column(identifier: "provider", title: "Provider", width: 280, minWidth: 70, flexible: true),
        Column(identifier: "input", title: "Input $/1M", width: 150, minWidth: 58, flexible: true),
        Column(identifier: "output", title: "Output $/1M", width: 150, minWidth: 58, flexible: true),
        Column(identifier: "cache", title: "Cache $/1M", width: 150, minWidth: 58, flexible: true),
        Column(identifier: "context", title: "Kontext", width: 140, minWidth: 58, flexible: true),
        Column(identifier: "throughput", title: "Throughput", width: 140, minWidth: 66, flexible: true),
        Column(identifier: "status", title: "Status", width: 160, minWidth: 76, flexible: true)
    ]

    init(viewModel: MainViewModel) {
        self.viewModel = viewModel
        super.init(frame: .zero)
        translatesAutoresizingMaskIntoConstraints = false

        // .fullWidth statt .plain: der .plain-Stil legt links und rechts einen breiten Rand an und
        // setzt zusaetzlich 17 Punkt Zwischenraum je Spalte - bei sieben Spalten gehen dadurch allein
        // 119 Punkt verloren, und die Status-Spalte passte nicht mehr ins Bild.
        tableView.style = .fullWidth
        // Der Zellen-Innenabstand wird in den Zell-Ansichten selbst gezeichnet (12 Punkt, wie in
        // XAML). Ein zusaetzlicher Zwischenraum von AppKit waere doppelt.
        tableView.intercellSpacing = NSSize(width: 0, height: 2)
        tableView.rowHeight = 44
        tableView.headerView = ThemedTableHeaderView()
        tableView.headerView?.frame.size.height = 38
        tableView.backgroundColor = .clear
        tableView.usesAlternatingRowBackgroundColors = false
        tableView.gridStyleMask = [.solidHorizontalGridLineMask]
        tableView.selectionHighlightStyle = .regular
        tableView.allowsMultipleSelection = false
        tableView.allowsColumnReordering = false
        // Der Nutzer soll nicht ziehen duerfen (CanUserResizeColumns="False" in XAML).
        tableView.allowsColumnResizing = false
        // AppKit darf die Breiten NICHT selbst nachziehen: es wuerde die in fitColumnsToWidth
        // gesetzten Werte beim naechsten Resize wieder ueberschreiben - dabei rutschte die letzte
        // Spalte (Status) regelmaessig aus dem sichtbaren Bereich.
        tableView.columnAutoresizingStyle = .noColumnAutoresizing
        tableView.dataSource = self
        tableView.delegate = self
        tableView.target = self
        tableView.action = #selector(rowClicked)

        for column in Self.columns {
            let tableColumn = NSTableColumn(identifier: NSUserInterfaceItemIdentifier(column.identifier))
            tableColumn.title = column.title
            tableColumn.width = column.width
            tableColumn.minWidth = column.minWidth
            tableColumn.maxWidth = 10_000
            tableColumn.resizingMask = .autoresizingMask
            tableColumn.sortDescriptorPrototype = NSSortDescriptor(key: column.identifier, ascending: true)
            tableView.addTableColumn(tableColumn)
        }

        scrollView.documentView = tableView
        ScrollStyling.apply(to: scrollView)
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(scrollView)

        NSLayoutConstraint.activate([
            scrollView.topAnchor.constraint(equalTo: topAnchor),
            scrollView.leadingAnchor.constraint(equalTo: leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: bottomAnchor)
        ])

        // Auf die tatsaechliche Groessenaenderung des sichtbaren Bereichs hoeren. Ein Neuberechnen
        // nur in layout() greift zu frueh: dort steht die endgueltige Breite des Scroll-Bereichs
        // noch nicht fest, und die letzte Spalte (Status) landete ausserhalb des Sichtfensters.
        scrollView.contentView.postsFrameChangedNotifications = true
        NotificationCenter.default.addObserver(self, selector: #selector(visibleWidthChanged),
                                               name: NSView.frameDidChangeNotification,
                                               object: scrollView.contentView)

        applyTheme()
        NotificationCenter.default.addObserver(self, selector: #selector(applyTheme),
                                               name: ThemeManager.themeChangedNotification, object: nil)
    }

    @objc private func visibleWidthChanged() { fitColumnsToWidth() }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }
    deinit { NotificationCenter.default.removeObserver(self) }

    @objc private func applyTheme() {
        tableView.gridColor = ThemeManager.palette.gridLine
        tableView.headerView?.needsDisplay = true
        tableView.reloadData()
        restoreSelection()
    }

    // MARK: - Daten

    func reload() {
        rows = viewModel?.providers ?? []
        applySort()
        tableView.reloadData()
        restoreSelection()
        fitColumnsToWidth()
    }

    override func layout() {
        super.layout()
        fitColumnsToWidth()
    }

    /// Verteilt die verfuegbare Breite anteilig auf alle Spalten. NSTableView schrumpft - anders als
    /// ein WPF-DataGrid - nicht von selbst: mit den Vorgabebreiten (Summe 1170) waere die
    /// Status-Spalte in einem schmaleren Fenster einfach abgeschnitten und unsichtbar.
    private func fitColumnsToWidth() {
        // Massgeblich ist die Breite des Sichtfensters (Clip-View) - contentSize kann waehrend des
        // Aufbaus noch veraltet sein.
        let available = scrollView.contentView.bounds.width
        guard available > 50, tableView.tableColumns.count == Self.columns.count else { return }
        guard available != lastFittedWidth else { return }
        lastFittedWidth = available

        // NSTableView zieht zwischen den Spalten je einen Zwischenraum (intercellSpacing) ein - der
        // muss vom verteilbaren Platz abgezogen werden, sonst ist die Summe genau um diese Punkte zu
        // breit und die letzte Spalte (Status) rutscht aus dem Bild.
        let spacing = tableView.intercellSpacing.width * CGFloat(Self.columns.count)
        let distributable = available - spacing
        let total = Self.columns.reduce(0) { $0 + $1.width }
        guard distributable > 0, total > 0 else { return }

        var used: CGFloat = 0
        for (index, column) in Self.columns.enumerated() {
            let tableColumn = tableView.tableColumns[index]
            let share = index == Self.columns.count - 1
                ? Swift.max(column.minWidth, distributable - used)
                : Swift.max(column.minWidth, ((column.width / total) * distributable).rounded(.down))
            tableColumn.width = share
            used += share
        }

        // Die Tabelle selbst auf die Sichtbreite setzen und die letzte Spalte von AppKit exakt
        // einpassen lassen. Ohne das bleibt die Tabelle breiter als ihr Sichtfenster und die
        // Status-Spalte steht rechts ausserhalb.
        tableView.setFrameSize(NSSize(width: available, height: tableView.frame.height))
        tableView.sizeLastColumnToFit()
    }

    private func restoreSelection() {
        guard let selected = viewModel?.selectedProvider,
              let index = rows.firstIndex(where: { $0 === selected }) else {
            tableView.deselectAll(nil)
            return
        }
        tableView.selectRowIndexes(IndexSet(integer: index), byExtendingSelection: false)
    }

    @objc private func rowClicked() {
        let row = tableView.selectedRow
        guard row >= 0, row < rows.count else { return }
        viewModel?.selectedProvider = rows[row]
    }

    private func applySort() {
        guard !sortColumn.isEmpty else { return }
        let ascending = sortAscending
        rows.sort { a, b in
            let result: Bool
            switch sortColumn {
            case "provider": result = a.providerName.localizedCaseInsensitiveCompare(b.providerName) == .orderedAscending
            case "input": result = a.inputPerMillion < b.inputPerMillion
            case "output": result = a.outputPerMillion < b.outputPerMillion
            // SortMemberPath="CacheReadPerMillion" bzw. "ThroughputLast30m" in XAML: sortiert wird
            // nach der Zahl, nicht nach dem angezeigten "--".
            case "cache": result = a.cacheReadPerMillion < b.cacheReadPerMillion
            case "context": result = a.contextLength < b.contextLength
            case "throughput": result = (a.throughputLast30m ?? -1) < (b.throughputLast30m ?? -1)
            case "status": result = a.status < b.status
            default: result = false
            }
            return ascending ? result : !result
        }
    }

    // MARK: - NSTableViewDataSource / Delegate

    func numberOfRows(in tableView: NSTableView) -> Int { rows.count }

    func tableView(_ tableView: NSTableView, sortDescriptorsDidChange oldDescriptors: [NSSortDescriptor]) {
        guard let descriptor = tableView.sortDescriptors.first, let key = descriptor.key else { return }
        sortColumn = key
        sortAscending = descriptor.ascending
        let selected = viewModel?.selectedProvider
        applySort()
        tableView.reloadData()
        if let selected, let index = rows.firstIndex(where: { $0 === selected }) {
            tableView.selectRowIndexes(IndexSet(integer: index), byExtendingSelection: false)
        }
    }

    func tableView(_ tableView: NSTableView, rowViewForRow row: Int) -> NSTableRowView? {
        ProviderRowView()
    }

    func tableView(_ tableView: NSTableView, viewFor tableColumn: NSTableColumn?, row: Int) -> NSView? {
        guard let tableColumn, row < rows.count else { return nil }
        let provider = rows[row]
        let identifier = tableColumn.identifier.rawValue

        if identifier == "status" {
            let container = NSView()
            let pill = StatusPillView(statusText: provider.statusText)
            pill.translatesAutoresizingMaskIntoConstraints = false
            container.addSubview(pill)
            NSLayoutConstraint.activate([
                pill.centerXAnchor.constraint(equalTo: container.centerXAnchor),
                pill.centerYAnchor.constraint(equalTo: container.centerYAnchor)
            ])
            return container
        }

        let text: String
        var weight: NSFont.Weight = .regular
        switch identifier {
        case "provider":
            text = provider.providerName
            weight = .semibold
        case "input": text = Format.price(provider.inputPerMillion)
        case "output": text = Format.price(provider.outputPerMillion)
        case "cache": text = provider.cacheReadText
        case "context": text = Format.thousands(provider.contextLength)
        case "throughput": text = provider.throughputText
        default: text = ""
        }

        let container = NSView()
        let label = UI.label(text, size: 13, weight: weight)
        label.alignment = .center
        label.lineBreakMode = .byTruncatingTail
        container.addSubview(label)
        NSLayoutConstraint.activate([
            label.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: 8),
            label.trailingAnchor.constraint(equalTo: container.trailingAnchor, constant: -8),
            label.centerYAnchor.constraint(equalTo: container.centerYAnchor)
        ])
        return container
    }
}

// MARK: - Zeile mit Hover/Auswahl

final class ProviderRowView: NSTableRowView {
    private var trackingAreaRef: NSTrackingArea?
    private var isHovered = false { didSet { needsDisplay = true } }

    override func updateTrackingAreas() {
        super.updateTrackingAreas()
        if let trackingAreaRef { removeTrackingArea(trackingAreaRef) }
        let area = NSTrackingArea(rect: bounds, options: [.mouseEnteredAndExited, .activeInActiveApp], owner: self)
        addTrackingArea(area)
        trackingAreaRef = area
    }

    override func mouseEntered(with event: NSEvent) { isHovered = true }
    override func mouseExited(with event: NSEvent) { isHovered = false }

    override func resetCursorRects() { addCursorRect(bounds, cursor: .pointingHand) }

    override func drawBackground(in dirtyRect: NSRect) {
        let palette = ThemeManager.palette
        if isHovered && !isSelected {
            palette.hoverBg.flattened(over: ThemeManager.flattenBase).setFill()
            dirtyRect.fill()
        }
    }

    override func drawSelection(in dirtyRect: NSRect) {
        guard selectionHighlightStyle != .none else { return }
        ThemeManager.palette.rowSelectedBg.flattened(over: ThemeManager.flattenBase).setFill()
        dirtyRect.fill()
    }
}

// MARK: - Status-Pille

/// `<Style x:Key="StatusPill">` + StatusDot + StatusText aus XAML in einer View.
final class StatusPillView: NSView {
    private let dot = NSView()
    private let label: ThemedLabel
    private let statusText: String

    init(statusText: String) {
        self.statusText = statusText
        label = UI.label(statusText, size: 12, weight: .semibold)
        super.init(frame: .zero)
        wantsLayer = true

        dot.wantsLayer = true
        dot.translatesAutoresizingMaskIntoConstraints = false
        addSubview(dot)
        addSubview(label)

        NSLayoutConstraint.activate([
            dot.widthAnchor.constraint(equalToConstant: 7),
            dot.heightAnchor.constraint(equalToConstant: 7),
            dot.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 9),
            dot.centerYAnchor.constraint(equalTo: centerYAnchor),
            label.leadingAnchor.constraint(equalTo: dot.trailingAnchor, constant: 6),
            label.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -9),
            label.topAnchor.constraint(equalTo: topAnchor, constant: 4),
            label.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -4)
        ])

        applyTheme()
        NotificationCenter.default.addObserver(self, selector: #selector(applyTheme),
                                               name: ThemeManager.themeChangedNotification, object: nil)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }
    deinit { NotificationCenter.default.removeObserver(self) }

    @objc private func applyTheme() {
        let palette = ThemeManager.palette
        let background: NSColor
        let foreground: NSColor
        switch statusText {
        case "eingeschränkt":
            background = palette.statusWarnBg
            foreground = palette.statusWarnFg
        case "gestört":
            background = palette.statusBadBg
            foreground = palette.statusBadFg
        default:
            background = palette.statusOkBg
            foreground = palette.statusOkFg
        }
        layer?.cornerRadius = 8
        layer?.borderWidth = 1
        layer?.backgroundColor = background.flattened(over: ThemeManager.flattenBase).cgColor
        layer?.borderColor = palette.statusOkFg.cgColor
        dot.layer?.cornerRadius = 3.5
        dot.layer?.backgroundColor = foreground.cgColor
        label.textColor = foreground
    }
}

// MARK: - Tabellenkopf

/// Kopfzeile im Theme-Look (`DataGridColumnHeader`-Style aus XAML).
final class ThemedTableHeaderView: NSTableHeaderView {
    override func draw(_ dirtyRect: NSRect) {
        let palette = ThemeManager.palette
        palette.tableHeaderBg.flattened(over: ThemeManager.flattenBase).setFill()
        dirtyRect.fill()
        super.draw(dirtyRect)
    }

}
