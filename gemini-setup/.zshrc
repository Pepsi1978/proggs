
# Added by LM Studio CLI (lms)
export PATH="$PATH:/Users/frank/.lmstudio/bin"
# End of LM Studio CLI section

export PATH="$HOME/.local/bin:$PATH"

# Rust/Cargo
export PATH="$HOME/.cargo/bin:$PATH"

# GitHub PAT for MCP servers (from macOS Keychain)
export GITHUB_PERSONAL_ACCESS_TOKEN="$(security find-generic-password -a "$USER" -s "github-pat" -w 2>/dev/null)"

# fzf shell integration (fuzzy finder)
source <(fzf --zsh)

# Better defaults for common tools
alias ls='eza --icons --group-directories-first'
alias ll='eza -la --icons --group-directories-first --git'
alias cat='bat --paging=never'
alias tree='eza --tree --icons'

# .NET telemetry opt-out
export DOTNET_CLI_TELEMETRY_OPTOUT=1

# Go
export GOPATH="$HOME/go"
export PATH="$GOPATH/bin:$PATH"

# Java (OpenJDK 17 for Android development)
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

# Android SDK (Homebrew command-line tools)
export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
export PATH="$ANDROID_HOME/emulator:$PATH"
export PATH="$ANDROID_HOME/build-tools/35.0.0:$PATH"
