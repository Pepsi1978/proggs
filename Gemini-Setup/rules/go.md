---
paths:
  - "**/*.go"
  - "**/go.mod"
  - "**/go.sum"
---

# Go Development Rules

- Use Go 1.26+ features (range over int, enhanced generics)
- Prefer standard library over third-party packages
- Use `cobra` for CLI apps, `net/http` for HTTP servers
- Error handling: Always check errors, use `fmt.Errorf("context: %w", err)` for wrapping
- Build release: `go build -ldflags="-s -w"` (strips debug info, smaller binary)
- Cross-compile for Windows: `GOOS=windows GOARCH=amd64 go build -ldflags="-s -w"`
- Cross-compile for macOS: `GOOS=darwin GOARCH=arm64 go build -ldflags="-s -w"`
- Run tests: `go test ./...`
- Run linter: `golangci-lint run` (preferred, installed) or `go vet ./...`
- Format code: `gofmt -w .`
