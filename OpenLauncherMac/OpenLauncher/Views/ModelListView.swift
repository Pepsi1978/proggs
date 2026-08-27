import AppKit

/// Linke Spalte: Modellgruppen mit ein-/ausklappbaren Kopfzeilen, Modell-Zeilen und Drag&Drop.
/// Entspricht dem `ItemsControl` + verschachtelten `ListBox` aus MainWindow.xaml samt der
/// Drag&Drop-Handler aus MainWindow.xaml.cs.
@MainActor
final class ModelListView: NSView {
    /// Zwischenablage-Typen fuer das Ziehen. Windows nutzt die .NET-Typen selbst als Datenformat;
    /// AppKit braucht benannte Pasteboard-Typen.
    static let modelDragType = NSPasteboard.PasteboardType("de.frank.openlauncher.model")
    static let groupDragType = NSPasteboard.PasteboardType("de.frank.openlauncher.group")

    private weak var viewModel: MainViewModel?
    private let scrollView = NSScrollView()
    private let stack = NSStackView()
    private let hintLabel = UI.label("Ziehen zum Sortieren", size: 11, role: .dim)

    /// Merker fuer den laufenden Zieh-Vorgang (Gegenstueck zu _dragSourceGroup/_dragSourceIndex).
    private var dragSourceGroup: ModelGroupEntry?
    private var dragSourceIndex = -1
    private var dragSourceGroupIndex = -1

    init(viewModel: MainViewModel) {
        self.viewModel = viewModel
        super.init(frame: .zero)
        translatesAutoresizingMaskIntoConstraints = false

        stack.orientation = .vertical
        stack.alignment = .leading
        stack.spacing = 9
        stack.translatesAutoresizingMaskIntoConstraints = false

        let documentView = FlippedView()
        documentView.translatesAutoresizingMaskIntoConstraints = false
        documentView.addSubview(stack)

        scrollView.translatesAutoresizingMaskIntoConstraints = false
        ScrollStyling.apply(to: scrollView)
        scrollView.documentView = documentView
        addSubview(scrollView)

        NSLayoutConstraint.activate([
            scrollView.topAnchor.constraint(equalTo: topAnchor),
            scrollView.leadingAnchor.constraint(equalTo: leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: bottomAnchor),

            documentView.leadingAnchor.constraint(equalTo: scrollView.contentView.leadingAnchor),
            documentView.trailingAnchor.constraint(equalTo: scrollView.contentView.trailingAnchor),
            documentView.topAnchor.constraint(equalTo: scrollView.contentView.topAnchor),

            stack.topAnchor.constraint(equalTo: documentView.topAnchor),
            stack.leadingAnchor.constraint(equalTo: documentView.leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: documentView.trailingAnchor),
            stack.bottomAnchor.constraint(equalTo: documentView.bottomAnchor, constant: -4)
        ])
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }

    // MARK: - Aufbau

    func reload() {
        guard let viewModel else { return }
        for view in stack.arrangedSubviews {
            stack.removeArrangedSubview(view)
            view.removeFromSuperview()
        }

        for (groupIndex, group) in viewModel.modelGroups.enumerated() {
            let groupView = makeGroupView(group: group, groupIndex: groupIndex)
            stack.addArrangedSubview(groupView)
            groupView.widthAnchor.constraint(equalTo: stack.widthAnchor).isActive = true
        }

        // Hinweis unter der letzten Gruppe statt neben den Buttons: dort ueberlappte er bei schmaler
        // Modell-Spalte mit "Bearbeiten"/"Entfernen".
        hintLabel.removeFromSuperview()
        stack.addArrangedSubview(hintLabel)
    }

    /// Aktualisiert nur die Auswahl-Markierung, ohne die Liste neu aufzubauen.
    func refreshSelection() {
        guard let viewModel else { return }
        for case let row as ModelRowView in allRows() {
            row.isSelected = row.model === viewModel.selectedModel
            row.refreshTexts()
        }
    }

    private func allRows() -> [NSView] {
        var result: [NSView] = []
        func walk(_ view: NSView) {
            for subview in view.subviews {
                result.append(subview)
                walk(subview)
            }
        }
        walk(stack)
        return result
    }

    private func makeGroupView(group: ModelGroupEntry, groupIndex: Int) -> NSView {
        let container = GroupContainerView(group: group)
        container.translatesAutoresizingMaskIntoConstraints = false
        container.onModelDrop = { [weak self] sourceGroup, sourceIndex, targetGroup in
            self?.viewModel?.moveModel(sourceGroup, from: sourceIndex, to: targetGroup,
                                       index: targetGroup.models.count)
        }
        container.onGroupDrop = { [weak self] sourceIndex, targetGroup in
            guard let self, let viewModel = self.viewModel,
                  let targetIndex = viewModel.modelGroups.firstIndex(where: { $0 === targetGroup }) else { return }
            viewModel.moveGroup(from: sourceIndex, to: targetIndex)
        }
        container.dragState = { [weak self] in
            (self?.dragSourceGroup, self?.dragSourceIndex ?? -1, self?.dragSourceGroupIndex ?? -1)
        }

        let inner = NSStackView()
        inner.orientation = .vertical
        inner.alignment = .leading
        inner.spacing = 6
        inner.edgeInsets = NSEdgeInsets(top: 8, left: 8, bottom: 8, right: 8)
        inner.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(inner)
        NSLayoutConstraint.activate([
            inner.topAnchor.constraint(equalTo: container.topAnchor),
            inner.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            inner.trailingAnchor.constraint(equalTo: container.trailingAnchor),
            inner.bottomAnchor.constraint(equalTo: container.bottomAnchor)
        ])

        let header = makeGroupHeader(group: group, groupIndex: groupIndex)
        inner.addArrangedSubview(header)
        header.widthAnchor.constraint(equalTo: inner.widthAnchor, constant: -16).isActive = true

        if group.isExpanded {
            for (modelIndex, model) in group.models.enumerated() where !model.isHidden {
                let row = makeModelRow(model: model, group: group, index: modelIndex)
                inner.addArrangedSubview(row)
                row.widthAnchor.constraint(equalTo: inner.widthAnchor, constant: -16).isActive = true
            }
        }
        return container
    }

    private func makeGroupHeader(group: ModelGroupEntry, groupIndex: Int) -> NSView {
        let header = GroupHeaderView(group: group, groupIndex: groupIndex)
        header.translatesAutoresizingMaskIntoConstraints = false
        header.onToggle = { [weak self] in self?.viewModel?.toggleGroup(group) }
        header.onDragStart = { [weak self] index in
            self?.dragSourceGroupIndex = index
            self?.dragSourceGroup = group
            self?.dragSourceIndex = -1
        }

        let grip = UI.label("⠿", size: 13, role: .dim)
        let title = UI.label(group.title, size: 13, weight: .semibold)
        let countChip = ChipView(text: "\(group.visibleModelCount)")
        let arrow = UI.label(group.isExpanded ? "▾" : "▸", size: 11, role: .accent)

        for view in [grip, title, countChip, arrow] as [NSView] {
            view.translatesAutoresizingMaskIntoConstraints = false
            header.addSubview(view)
        }

        NSLayoutConstraint.activate([
            header.heightAnchor.constraint(equalToConstant: 26),
            grip.leadingAnchor.constraint(equalTo: header.leadingAnchor, constant: 2),
            grip.centerYAnchor.constraint(equalTo: header.centerYAnchor),
            title.leadingAnchor.constraint(equalTo: grip.trailingAnchor, constant: 8),
            title.centerYAnchor.constraint(equalTo: header.centerYAnchor),
            title.trailingAnchor.constraint(lessThanOrEqualTo: countChip.leadingAnchor, constant: -8),
            countChip.trailingAnchor.constraint(equalTo: arrow.leadingAnchor, constant: -8),
            countChip.centerYAnchor.constraint(equalTo: header.centerYAnchor),
            arrow.trailingAnchor.constraint(equalTo: header.trailingAnchor, constant: -4),
            arrow.centerYAnchor.constraint(equalTo: header.centerYAnchor)
        ])
        return header
    }

    private func makeModelRow(model: ModelEntry, group: ModelGroupEntry, index: Int) -> ModelRowView {
        let row = ModelRowView(model: model, group: group, index: index)
        row.translatesAutoresizingMaskIntoConstraints = false
        row.isSelected = model === viewModel?.selectedModel
        row.onClick = { [weak self] in self?.viewModel?.selectedModel = model }
        row.onHide = { [weak self] in self?.viewModel?.hideModel(model) }
        row.onDragStart = { [weak self] in
            self?.dragSourceGroup = group
            self?.dragSourceIndex = index
            self?.dragSourceGroupIndex = -1
            self?.viewModel?.selectedModel = model
        }
        row.onDrop = { [weak self] targetIndex in
            guard let self, let sourceGroup = self.dragSourceGroup, self.dragSourceIndex >= 0 else { return }
            self.viewModel?.moveModel(sourceGroup, from: self.dragSourceIndex, to: group, index: targetIndex)
            self.dragSourceGroup = nil
            self.dragSourceIndex = -1
        }
        return row
    }
}

// MARK: - Gruppen-Container (Drop-Ziel)

/// `<Border AllowDrop="True" DragEnter/DragOver/Drop>` aus XAML.
@MainActor
final class GroupContainerView: SurfaceView {
    let group: ModelGroupEntry
    var onModelDrop: ((ModelGroupEntry, Int, ModelGroupEntry) -> Void)?
    var onGroupDrop: ((Int, ModelGroupEntry) -> Void)?
    var dragState: (() -> (ModelGroupEntry?, Int, Int))?

    init(group: ModelGroupEntry) {
        self.group = group
        super.init(cornerRadius: 13)
        registerForDraggedTypes([ModelListView.modelDragType, ModelListView.groupDragType])
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }

    override func draggingEntered(_ sender: NSDraggingInfo) -> NSDragOperation { .move }
    override func draggingUpdated(_ sender: NSDraggingInfo) -> NSDragOperation { .move }

    override func performDragOperation(_ sender: NSDraggingInfo) -> Bool {
        let types = sender.draggingPasteboard.types ?? []
        let (sourceGroup, sourceIndex, sourceGroupIndex) = dragState?() ?? (nil, -1, -1)

        if types.contains(ModelListView.groupDragType), sourceGroupIndex >= 0 {
            onGroupDrop?(sourceGroupIndex, group)
            return true
        }
        if types.contains(ModelListView.modelDragType), let sourceGroup, sourceIndex >= 0 {
            onModelDrop?(sourceGroup, sourceIndex, group)
            return true
        }
        return false
    }
}

// MARK: - Gruppen-Kopfzeile (Drag-Quelle + Klapp-Schalter)

@MainActor
final class GroupHeaderView: NSView, NSDraggingSource {
    private let group: ModelGroupEntry
    private let groupIndex: Int
    private var mouseDownPoint: NSPoint = .zero
    private var dragStarted = false

    var onToggle: (() -> Void)?
    var onDragStart: ((Int) -> Void)?

    init(group: ModelGroupEntry, groupIndex: Int) {
        self.group = group
        self.groupIndex = groupIndex
        super.init(frame: .zero)
        setAccessibilityRole(.button)
        setAccessibilityLabel("Gruppe \(group.title) auf-/zuklappen")
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }

    override func resetCursorRects() { addCursorRect(bounds, cursor: .pointingHand) }

    override func mouseDown(with event: NSEvent) {
        mouseDownPoint = event.locationInWindow
        dragStarted = false
    }

    override func mouseDragged(with event: NSEvent) {
        guard !dragStarted else { return }
        let current = event.locationInWindow
        // Gleiche Schwelle wie SystemParameters.MinimumHorizontal/VerticalDragDistance in WPF.
        if abs(current.x - mouseDownPoint.x) < 4 && abs(current.y - mouseDownPoint.y) < 4 { return }

        dragStarted = true
        onDragStart?(groupIndex)

        let item = NSPasteboardItem()
        item.setString(group.id, forType: ModelListView.groupDragType)
        let dragItem = NSDraggingItem(pasteboardWriter: item)
        dragItem.setDraggingFrame(bounds, contents: snapshot())
        beginDraggingSession(with: [dragItem], event: event, source: self)
    }

    override func mouseUp(with event: NSEvent) {
        if !dragStarted { onToggle?() }
        dragStarted = false
    }

    override func accessibilityPerformPress() -> Bool {
        onToggle?()
        return true
    }

    func draggingSession(_ session: NSDraggingSession,
                         sourceOperationMaskFor context: NSDraggingContext) -> NSDragOperation { .move }

    private func snapshot() -> NSImage {
        let image = NSImage(size: bounds.size)
        image.lockFocus()
        if let context = NSGraphicsContext.current?.cgContext {
            layer?.render(in: context)
        }
        image.unlockFocus()
        return image
    }
}

// MARK: - Modell-Zeile

@MainActor
final class ModelRowView: SelectableRowView, NSDraggingSource {
    let model: ModelEntry
    let group: ModelGroupEntry
    let index: Int

    private let nameLabel: ThemedLabel
    private let slugLabel: ThemedLabel
    private var mouseDownPoint: NSPoint = .zero
    private var dragStarted = false

    var onHide: (() -> Void)?
    var onDragStart: (() -> Void)?
    var onDrop: ((Int) -> Void)?

    init(model: ModelEntry, group: ModelGroupEntry, index: Int) {
        self.model = model
        self.group = group
        self.index = index
        nameLabel = UI.label(model.displayName, size: 13, weight: .semibold)
        slugLabel = UI.label(ModelRowView.subtitle(for: model), size: 11, role: .dim, monospaced: true)
        super.init()
        setAccessibilityLabel(model.displayName)

        let grip = UI.label("⠿", size: 13, role: .dim)
        // Windows nutzt das Segoe-Icon E890 ("ausblenden"); auf macOS ist das Gegenstueck das
        // SF-Symbol "eye.slash" - als Text-Glyphe gibt es dafuer kein passendes Unicode-Zeichen.
        let hideButton = StyledButton(style: .window, title: "\u{f8ff}")
        hideButton.symbolName = "eye.slash"
        hideButton.fontSize = 13
        hideButton.horizontalPadding = 4
        hideButton.verticalPadding = 4
        hideButton.toolTip = "Modell ausblenden"
        hideButton.setAccessibilityLabel("Modell \(model.displayName) ausblenden")
        hideButton.target = self
        hideButton.action = #selector(hideClicked)

        nameLabel.lineBreakMode = .byTruncatingTail
        slugLabel.lineBreakMode = .byTruncatingMiddle

        for view in [grip, nameLabel, slugLabel, hideButton] as [NSView] {
            view.translatesAutoresizingMaskIntoConstraints = false
            addSubview(view)
        }

        NSLayoutConstraint.activate([
            grip.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 10),
            grip.topAnchor.constraint(equalTo: topAnchor, constant: 9),

            nameLabel.leadingAnchor.constraint(equalTo: grip.trailingAnchor, constant: 9),
            nameLabel.topAnchor.constraint(equalTo: topAnchor, constant: 9),
            nameLabel.trailingAnchor.constraint(lessThanOrEqualTo: hideButton.leadingAnchor, constant: -8),

            slugLabel.leadingAnchor.constraint(equalTo: nameLabel.leadingAnchor),
            slugLabel.topAnchor.constraint(equalTo: nameLabel.bottomAnchor, constant: 2),
            slugLabel.trailingAnchor.constraint(lessThanOrEqualTo: hideButton.leadingAnchor, constant: -8),
            slugLabel.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -9),

            hideButton.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -10),
            hideButton.centerYAnchor.constraint(equalTo: centerYAnchor),
            hideButton.widthAnchor.constraint(equalToConstant: 30),
            hideButton.heightAnchor.constraint(equalToConstant: 30)
        ])

        registerForDraggedTypes([ModelListView.modelDragType])
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }

    /// Zweite Zeile eines Modelleintrags: die Modell-ID, bei lokalen Modellen ergaenzt um den
    /// Ladezustand. "geladen, 79k Kontext" ist die Antwort auf die Frage, die man vor jedem Start
    /// eines lokalen Modells hat - laeuft es schon, oder kostet der Start erst Minuten?
    private static func subtitle(for model: ModelEntry) -> String {
        guard model.lmStudioLoadedContext > 0 else { return model.modelString }
        let thousands = model.lmStudioLoadedContext / 1000
        return "\(model.modelString)  ·  geladen, \(thousands)k Kontext"
    }

    func refreshTexts() {
        nameLabel.stringValue = model.displayName
        slugLabel.stringValue = ModelRowView.subtitle(for: model)
    }

    @objc private func hideClicked() { onHide?() }

    override func mouseDown(with event: NSEvent) {
        mouseDownPoint = event.locationInWindow
        dragStarted = false
        super.mouseDown(with: event)
    }

    override func mouseDragged(with event: NSEvent) {
        guard !dragStarted else { return }
        let current = event.locationInWindow
        if abs(current.x - mouseDownPoint.x) < 4 && abs(current.y - mouseDownPoint.y) < 4 { return }

        dragStarted = true
        onDragStart?()

        let item = NSPasteboardItem()
        item.setString(model.modelString, forType: ModelListView.modelDragType)
        let dragItem = NSDraggingItem(pasteboardWriter: item)
        dragItem.setDraggingFrame(bounds, contents: snapshot())
        beginDraggingSession(with: [dragItem], event: event, source: self)
    }

    func draggingSession(_ session: NSDraggingSession,
                         sourceOperationMaskFor context: NSDraggingContext) -> NSDragOperation { .move }

    override func draggingEntered(_ sender: NSDraggingInfo) -> NSDragOperation { .move }
    override func draggingUpdated(_ sender: NSDraggingInfo) -> NSDragOperation { .move }

    override func performDragOperation(_ sender: NSDraggingInfo) -> Bool {
        // Nur echte Modell-Drags verarbeiten. Ohne diese Typpruefung wuerde ein Gruppen-Drag, das
        // ueber einer Modell-Zeile losgelassen wird, den noch vom letzten Modell-Klick stammenden
        // Index verwenden und faelschlich ein Modell der falschen Gruppe verschieben.
        guard sender.draggingPasteboard.types?.contains(ModelListView.modelDragType) == true else { return false }
        onDrop?(index)
        return true
    }

    private func snapshot() -> NSImage {
        let image = NSImage(size: bounds.size)
        image.lockFocus()
        if let context = NSGraphicsContext.current?.cgContext {
            layer?.render(in: context)
        }
        image.unlockFocus()
        return image
    }
}

// MARK: - Zaehler-Chip

/// `<Border CornerRadius="20" Background="{DynamicResource ChipBg}">` aus der Gruppen-Kopfzeile.
final class ChipView: NSView {
    private let label: ThemedLabel

    init(text: String) {
        label = UI.label(text, size: 11, weight: .semibold, role: .muted)
        super.init(frame: .zero)
        wantsLayer = true
        addSubview(label)
        NSLayoutConstraint.activate([
            label.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 7),
            label.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -7),
            label.topAnchor.constraint(equalTo: topAnchor, constant: 1),
            label.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -1)
        ])
        applyTheme()
        NotificationCenter.default.addObserver(self, selector: #selector(applyTheme),
                                               name: ThemeManager.themeChangedNotification, object: nil)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }
    deinit { NotificationCenter.default.removeObserver(self) }

    @objc private func applyTheme() {
        layer?.cornerRadius = bounds.height / 2 > 0 ? Swift.min(10, bounds.height / 2) : 10
        layer?.backgroundColor = ThemeManager.palette.chipBg.flattened(over: ThemeManager.flattenBase).cgColor
    }

    override func layout() {
        super.layout()
        layer?.cornerRadius = Swift.min(10, bounds.height / 2)
    }
}
