---
paths:
  - "**/*.c"
  - "**/*.cpp"
  - "**/*.h"
  - "**/*.hpp"
  - "**/CMakeLists.txt"
  - "**/Makefile"
---

# C/C++ Development Rules

- Use modern C++ (C++20 or later) unless targeting older platforms
- Prefer smart pointers over raw pointers (`std::unique_ptr`, `std::shared_ptr`)
- Use `std::string_view` for non-owning string references
- Build with CMake: `cmake -B build -DCMAKE_BUILD_TYPE=Release && cmake --build build`
- Compile with warnings: `-Wall -Wextra -Werror` (or `/W4 /WX` on MSVC)
- Cross-compile for Windows: Use MinGW or set up CMake toolchain file
- Format: `clang-format -i <file>`
- Lint: `clang-tidy <file> -- -std=c++20` (or use clangd LSP diagnostics)
- Test: `cmake --build build --target test` (CTest) or `ctest --test-dir build`
- Audit: Check for buffer overflows, use-after-free, double-free with AddressSanitizer: `-fsanitize=address`
