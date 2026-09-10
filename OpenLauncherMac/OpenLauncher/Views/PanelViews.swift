import AppKit

/// Profil- und Modus-Bereich (untere Karte der mittleren Spalte).
/// Entspricht dem zweiten `<Border Style="Card">` aus MainWindow.xaml: Kopfzeile mit den drei
/// Schaltern, drei Profil-Kacheln nebeneinander, darunter vier Modus-Kacheln.
@MainActor
final class ProfileSectionView: NSView {
    private weak var viewModel: MainViewModel?

    private let contextLabel = UI.label("", size: 11, role: .dim)
    private let defaultSummaryLabel = UI.label("", size: 11, weight: .semibold, role: .accent)
    private let editProfileButton = StyledButton(style: .ghost, title: "Profil bearbeiten")
    private let editWorkModeButton = StyledButton(style: .ghost, title: "Modus bearbeiten")
    private let defaultButton = StyledButton(style: .ghost, title: "☆ Standard speichern")
    private let profileRow = NSStackView()
    private let workModeRow = NSStackView()
    private let cliTitle = UI.label("CLI", size: 12, weight: .bold, role: .muted)
    private let cliRow = NSStackView()
    /// Ohne CLI-Wahl endet die Karte unter den Modus-Kacheln, mit CLI-Wahl unter der CLI-Zeile.
    private var workModeBottom: NSLayoutConstraint!
    private var cliBottom: NSLayoutConstraint!

    private var profileTiles: [(entry: InstructionProfileEntry, view: SelectableRowView)] = []
    private var workModeTiles: [(entry: WorkModeEntry, view: SelectableRowView)] = []
    private var cliTiles: [(entry: CliTargetEntry, view: SelectableRowView)] = []

    init(viewModel: MainViewModel) {
        self.viewModel = viewModel
        super.init(frame: .zero)
        translatesAutoresizingMaskIntoConstraints = false

        let title = UI.label("PROFIL", size: 12, weight: .bold, role: .muted)

        for button in [editProfileButton, editWorkModeButton, defaultButton] {
            button.fontSize = 12
            button.horizontalPadding = 11
            button.verticalPadding = 6
        }
        defaultButton.toolTip = "Profil, Modus und Effort als Standard dieses Modells merken - er wird bei jedem Wechsel auf das Modell und beim Start vorausgewählt."
        editProfileButton.target = self
        editProfileButton.action = #selector(editProfileClicked)
        editWorkModeButton.target = self
        editWorkModeButton.action = #selector(editWorkModeClicked)
        defaultButton.target = self
        defaultButton.action = #selector(toggleDefaultClicked)

        contextLabel.lineBreakMode = .byTruncatingTail
        defaultSummaryLabel.lineBreakMode = .byTruncatingTail

        // Nur die Ueberschrift steht neben den Schaltern. Die Info-Zeile darunter bekommt die GANZE
        // Kartenbreite: "★ Standard: Minimal · Schnellmodus · High" ist laenger als der Platz neben
        // drei Schaltern und wurde sonst mitten im Wort abgeschnitten. Den dafuer noetigen Platz
        // liefern die flacheren Kacheln weiter unten.
        title.translatesAutoresizingMaskIntoConstraints = false

        let headerRight = NSStackView(views: [editProfileButton, editWorkModeButton, defaultButton])
        headerRight.orientation = .horizontal
        headerRight.spacing = 8
        headerRight.translatesAutoresizingMaskIntoConstraints = false

        let modeTitle = UI.label("MODUS", size: 12, weight: .bold, role: .muted)

        profileRow.orientation = .horizontal
        profileRow.distribution = .fillEqually
        profileRow.spacing = 8
        profileRow.translatesAutoresizingMaskIntoConstraints = false

        workModeRow.orientation = .horizontal
        workModeRow.distribution = .fillEqually
        workModeRow.spacing = 8
        workModeRow.translatesAutoresizingMaskIntoConstraints = false

        cliRow.orientation = .horizontal
        cliRow.distribution = .fillEqually
        cliRow.spacing = 8
        cliRow.translatesAutoresizingMaskIntoConstraints = false
        cliTitle.translatesAutoresizingMaskIntoConstraints = false

        for view in [title, headerRight, contextLabel, defaultSummaryLabel,
                     profileRow, modeTitle, workModeRow, cliTitle, cliRow] as [NSView] {
            addSubview(view)
        }
        modeTitle.translatesAutoresizingMaskIntoConstraints = false
        workModeBottom = workModeRow.bottomAnchor.constraint(equalTo: bottomAnchor)
        cliBottom = cliRow.bottomAnchor.constraint(equalTo: bottomAnchor)

        NSLayoutConstraint.activate([
            title.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 4),
            title.centerYAnchor.constraint(equalTo: headerRight.centerYAnchor),

            headerRight.topAnchor.constraint(equalTo: topAnchor),
            headerRight.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -4),
            headerRight.leadingAnchor.constraint(greaterThanOrEqualTo: title.trailingAnchor, constant: 12),

            // Info-Zeile ueber die volle Kartenbreite - beide Beschriftungen teilen sich denselben
            // Platz (nur eine ist jeweils sichtbar), damit die Kacheln nicht verrutschen.
            contextLabel.topAnchor.constraint(equalTo: headerRight.bottomAnchor, constant: 6),
            contextLabel.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 4),
            contextLabel.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -4),

            defaultSummaryLabel.topAnchor.constraint(equalTo: contextLabel.topAnchor),
            defaultSummaryLabel.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 4),
            defaultSummaryLabel.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -4),

            profileRow.topAnchor.constraint(equalTo: contextLabel.bottomAnchor, constant: 10),
            profileRow.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 4),
            profileRow.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -4),
            // Deutlich flacher als unter Windows (dort 108). Moeglich wird das durch die jetzt
            // erzwungene Mindestbreite der mittleren Spalte: die Beschreibungen brauchen dadurch nur
            // noch zwei Zeilen statt drei.
            // Etwas flacher als frueher (76), damit bei GPT-Modellen die CLI-Zeile noch Platz hat.
            profileRow.heightAnchor.constraint(equalToConstant: 68),

            modeTitle.topAnchor.constraint(equalTo: profileRow.bottomAnchor, constant: 10),
            modeTitle.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 4),

            workModeRow.topAnchor.constraint(equalTo: modeTitle.bottomAnchor, constant: 4),
            workModeRow.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 4),
            workModeRow.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -4),
            workModeRow.heightAnchor.constraint(equalToConstant: 64),
            workModeBottom,

            // Dritte Zeile: Ziel-CLI, nur bei OpenAI-Modellen sichtbar.
            cliTitle.topAnchor.constraint(equalTo: workModeRow.bottomAnchor, constant: 10),
            cliTitle.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 4),
            cliRow.topAnchor.constraint(equalTo: cliTitle.bottomAnchor, constant: 4),
            cliRow.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 4),
            cliRow.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -4),
            cliRow.heightAnchor.constraint(equalToConstant: 52)
        ])

        buildTiles()
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }

    private func buildTiles() {
        guard let viewModel else { return }

        for entry in viewModel.profileList {
            let tile = SelectableRowView()
            tile.translatesAutoresizingMaskIntoConstraints = false
            tile.isDisabled = !entry.isEnabled
            tile.setAccessibilityLabel("Profil \(entry.displayName)")
            tile.onClick = { [weak self] in self?.viewModel?.selectedProfile = entry }

            let name = UI.label(entry.displayName, size: 15, weight: .semibold)
            let description = UI.label(entry.descriptionText, size: 11, role: .dim)
            description.lineBreakMode = .byWordWrapping
            description.maximumNumberOfLines = 2
            tile.addSubview(name)
            tile.addSubview(description)
            NSLayoutConstraint.activate([
                name.topAnchor.constraint(equalTo: tile.topAnchor, constant: 9),
                name.leadingAnchor.constraint(equalTo: tile.leadingAnchor, constant: 12),
                name.trailingAnchor.constraint(lessThanOrEqualTo: tile.trailingAnchor, constant: -12),
                description.topAnchor.constraint(equalTo: name.bottomAnchor, constant: 5),
                description.leadingAnchor.constraint(equalTo: tile.leadingAnchor, constant: 12),
                description.trailingAnchor.constraint(equalTo: tile.trailingAnchor, constant: -12),
                description.bottomAnchor.constraint(lessThanOrEqualTo: tile.bottomAnchor, constant: -9)
            ])
            profileRow.addArrangedSubview(tile)
            profileTiles.append((entry, tile))
        }

        for entry in viewModel.workModes {
            let tile = SelectableRowView()
            tile.translatesAutoresizingMaskIntoConstraints = false
            tile.setAccessibilityLabel("Modus \(entry.displayName)")
            tile.onClick = { [weak self] in self?.viewModel?.selectedWorkMode = entry }

            // Selbst-verkleinernd: "Gründlichkeitsmodus" ist ein einziges langes Wort und wuerde
            // sonst mitten im Wort umbrechen, sobald die mittlere Spalte schmal wird.
            let name = AutoShrinkLabel(labelWithString: entry.displayName)
            name.translatesAutoresizingMaskIntoConstraints = false
            name.maximumFontSize = 13
            name.font = .systemFont(ofSize: 13, weight: .semibold)
            name.applyTheme()
            name.lineBreakMode = .byWordWrapping
            name.maximumNumberOfLines = 2
            let description = UI.label(entry.descriptionText, size: 10, role: .dim)
            description.lineBreakMode = .byWordWrapping
            description.maximumNumberOfLines = 3
            tile.addSubview(name)
            tile.addSubview(description)
            NSLayoutConstraint.activate([
                name.topAnchor.constraint(equalTo: tile.topAnchor, constant: 7),
                name.leadingAnchor.constraint(equalTo: tile.leadingAnchor, constant: 8),
                name.trailingAnchor.constraint(equalTo: tile.trailingAnchor, constant: -8),
                description.topAnchor.constraint(equalTo: name.bottomAnchor, constant: 3),
                description.leadingAnchor.constraint(equalTo: tile.leadingAnchor, constant: 8),
                description.trailingAnchor.constraint(equalTo: tile.trailingAnchor, constant: -8),
                description.bottomAnchor.constraint(lessThanOrEqualTo: tile.bottomAnchor, constant: -7)
            ])
            workModeRow.addArrangedSubview(tile)
            workModeTiles.append((entry, tile))
        }

        for entry in viewModel.cliTargets {
            let tile = SelectableRowView()
            tile.translatesAutoresizingMaskIntoConstraints = false
            tile.setAccessibilityLabel("CLI \(entry.displayName)")
            tile.onClick = { [weak self] in self?.viewModel?.selectedCliTarget = entry }

            let name = UI.label(entry.displayName, size: 13, weight: .semibold)
            let description = UI.label(entry.descriptionText, size: 10, role: .dim)
            description.lineBreakMode = .byTruncatingTail
            tile.addSubview(name)
            tile.addSubview(description)
            NSLayoutConstraint.activate([
                name.topAnchor.constraint(equalTo: tile.topAnchor, constant: 7),
                name.leadingAnchor.constraint(equalTo: tile.leadingAnchor, constant: 10),
                name.trailingAnchor.constraint(lessThanOrEqualTo: tile.trailingAnchor, constant: -10),
                description.topAnchor.constraint(equalTo: name.bottomAnchor, constant: 3),
                description.leadingAnchor.constraint(equalTo: tile.leadingAnchor, constant: 10),
                description.trailingAnchor.constraint(equalTo: tile.trailingAnchor, constant: -10)
            ])
            cliRow.addArrangedSubview(tile)
            cliTiles.append((entry, tile))
        }
    }

    func refresh() {
        guard let viewModel else { return }
        for (entry, view) in profileTiles {
            view.isSelected = entry === viewModel.selectedProfile
        }
        for (entry, view) in workModeTiles {
            view.isSelected = entry === viewModel.selectedWorkMode
        }
        for (entry, view) in cliTiles {
            view.isSelected = entry === viewModel.selectedCliTarget
        }
        let showCli = viewModel.hasCliChoice
        cliTitle.isHidden = !showCli
        cliRow.isHidden = !showCli
        if showCli {
            workModeBottom.isActive = false
            cliBottom.isActive = true
        } else {
            cliBottom.isActive = false
            workModeBottom.isActive = true
        }
        contextLabel.stringValue = viewModel.profileContextText
        contextLabel.isHidden = viewModel.hasModelDefault
        defaultSummaryLabel.stringValue = viewModel.modelDefaultSummary
        defaultSummaryLabel.isHidden = !viewModel.hasModelDefault
        defaultButton.title = viewModel.modelDefaultButtonText
        defaultButton.isEnabled = viewModel.canSaveModelDefault
        editProfileButton.isEnabled = viewModel.canEditSelectedProfile
    }

    @objc private func editProfileClicked() { viewModel?.editProfile() }
    @objc private func editWorkModeClicked() { viewModel?.editWorkMode() }
    @objc private func toggleDefaultClicked() { viewModel?.toggleModelDefault() }
}

/// Rechte, schmale Spalte: THINKING bzw. EFFORT.
/// Entspricht dem dritten `<Border Style="Card">` aus MainWindow.xaml.
@MainActor
final class ThinkingListView: NSView {
    private weak var viewModel: MainViewModel?

    private let titleLabel = UI.label("THINKING", size: 12, weight: .bold, role: .muted)
    private let subtitleLabel = UI.label("Reasoning-Level", size: 11, role: .dim)
    private let optionsFrame = SurfaceView(cornerRadius: 13)
    private let stack = NSStackView()
    private let emptyStack = NSStackView()
    private let emptyDash = UI.label("—", size: 30, role: .dim)
    private let emptyLabel = UI.label("", size: 13, role: .dim)
    private let scrollView = NSScrollView()

    private var tiles: [(entry: ThinkingOptionEntry, view: SelectableRowView)] = []

    init(viewModel: MainViewModel) {
        self.viewModel = viewModel
        super.init(frame: .zero)
        translatesAutoresizingMaskIntoConstraints = false

        stack.orientation = .vertical
        stack.alignment = .leading
        stack.spacing = 4
        stack.translatesAutoresizingMaskIntoConstraints = false

        let documentView = FlippedView()
        documentView.translatesAutoresizingMaskIntoConstraints = false
        documentView.addSubview(stack)

        scrollView.documentView = documentView
        ScrollStyling.apply(to: scrollView)
        scrollView.translatesAutoresizingMaskIntoConstraints = false

        emptyDash.alignment = .center
        emptyLabel.alignment = .center
        emptyLabel.lineBreakMode = .byWordWrapping
        emptyLabel.maximumNumberOfLines = 4
        emptyLabel.preferredMaxLayoutWidth = 160
        emptyStack.orientation = .vertical
        emptyStack.alignment = .centerX
        emptyStack.spacing = 8
        emptyStack.translatesAutoresizingMaskIntoConstraints = false
        emptyStack.addArrangedSubview(emptyDash)
        emptyStack.addArrangedSubview(emptyLabel)

        optionsFrame.translatesAutoresizingMaskIntoConstraints = false
        optionsFrame.addSubview(scrollView)
        optionsFrame.addSubview(emptyStack)

        addSubview(titleLabel)
        addSubview(subtitleLabel)
        addSubview(optionsFrame)

        NSLayoutConstraint.activate([
            titleLabel.topAnchor.constraint(equalTo: topAnchor),
            titleLabel.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 4),
            subtitleLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 3),
            subtitleLabel.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 4),

            optionsFrame.topAnchor.constraint(equalTo: subtitleLabel.bottomAnchor, constant: 12),
            optionsFrame.leadingAnchor.constraint(equalTo: leadingAnchor),
            optionsFrame.trailingAnchor.constraint(equalTo: trailingAnchor),
            optionsFrame.bottomAnchor.constraint(equalTo: bottomAnchor),

            scrollView.topAnchor.constraint(equalTo: optionsFrame.topAnchor, constant: 8),
            scrollView.leadingAnchor.constraint(equalTo: optionsFrame.leadingAnchor, constant: 8),
            scrollView.trailingAnchor.constraint(equalTo: optionsFrame.trailingAnchor, constant: -8),
            scrollView.bottomAnchor.constraint(equalTo: optionsFrame.bottomAnchor, constant: -8),

            documentView.leadingAnchor.constraint(equalTo: scrollView.contentView.leadingAnchor),
            documentView.trailingAnchor.constraint(equalTo: scrollView.contentView.trailingAnchor),
            documentView.topAnchor.constraint(equalTo: scrollView.contentView.topAnchor),
            stack.topAnchor.constraint(equalTo: documentView.topAnchor),
            stack.leadingAnchor.constraint(equalTo: documentView.leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: documentView.trailingAnchor),
            stack.bottomAnchor.constraint(equalTo: documentView.bottomAnchor),

            emptyStack.centerYAnchor.constraint(equalTo: optionsFrame.centerYAnchor),
            emptyStack.leadingAnchor.constraint(equalTo: optionsFrame.leadingAnchor, constant: 10),
            emptyStack.trailingAnchor.constraint(equalTo: optionsFrame.trailingAnchor, constant: -10)
        ])
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }

    func reload() {
        guard let viewModel else { return }
        titleLabel.stringValue = viewModel.thinkingTitle
        subtitleLabel.stringValue = viewModel.thinkingSubtitle

        for view in stack.arrangedSubviews {
            stack.removeArrangedSubview(view)
            view.removeFromSuperview()
        }
        tiles.removeAll()

        for entry in viewModel.thinkingOptions {
            let tile = SelectableRowView()
            tile.translatesAutoresizingMaskIntoConstraints = false
            tile.setAccessibilityLabel("\(viewModel.thinkingTitle) \(entry.displayName)")
            tile.onClick = { [weak self] in self?.viewModel?.selectedThinkingOption = entry }

            let name = UI.label(entry.displayName, size: 13, weight: .semibold)
            let description = UI.label(entry.descriptionText, size: 11, role: .dim)
            tile.addSubview(name)
            tile.addSubview(description)
            NSLayoutConstraint.activate([
                name.topAnchor.constraint(equalTo: tile.topAnchor, constant: 9),
                name.leadingAnchor.constraint(equalTo: tile.leadingAnchor, constant: 10),
                name.trailingAnchor.constraint(lessThanOrEqualTo: tile.trailingAnchor, constant: -10),
                description.topAnchor.constraint(equalTo: name.bottomAnchor, constant: 2),
                description.leadingAnchor.constraint(equalTo: tile.leadingAnchor, constant: 10),
                description.trailingAnchor.constraint(lessThanOrEqualTo: tile.trailingAnchor, constant: -10),
                description.bottomAnchor.constraint(equalTo: tile.bottomAnchor, constant: -9)
            ])
            stack.addArrangedSubview(tile)
            tile.widthAnchor.constraint(equalTo: stack.widthAnchor).isActive = true
            tiles.append((entry, tile))
        }

        emptyLabel.stringValue = viewModel.thinkingEmptyText
        scrollView.isHidden = !viewModel.hasThinkingOptions
        emptyStack.isHidden = viewModel.hasThinkingOptions
        refreshSelection()
    }

    func refreshSelection() {
        guard let viewModel else { return }
        for (entry, view) in tiles {
            view.isSelected = entry === viewModel.selectedThinkingOption
        }
    }
}
