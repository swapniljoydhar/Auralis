# Contributing to Auralis

Thank you for your interest in contributing to Auralis!

## Crashes & Bugs

Log them in the [Issues](https://github.com/swapniljoydhar/Auralis/issues) tab.

Before reporting:
- **Search existing issues** to avoid duplicates
- **Test with the latest version** to confirm the bug still exists
- **Check if a fix has already been merged**

When creating an issue, provide:
- A clear description of the bug/crash
- Steps to reproduce
- Expected vs actual behavior
- Stack trace or logcat output
- Device model and Android version

## Feature Requests

Log them in the [Issues](https://github.com/swapniljoydhar/Auralis/issues) tab.

Before requesting:
- **Search existing requests** to avoid duplicates
- **Check the latest release** to see if it's already implemented

When creating a request:
- Describe what you want
- Explain the problem it solves
- Describe why it benefits all users

## Code Contributions

### Getting Started
1. Fork the repository
2. Create a feature branch from `dev`
3. Make your changes
4. Run tests: `./gradlew app:testDebugUnitTest musikr:testDebugUnitTest`
5. Check formatting: `./gradlew spotlessCheck`
6. Auto-format if needed: `./gradlew spotlessApply`
7. Submit a pull request to `dev`

### Guidelines
- **Language**: Kotlin only (except for vendored UI components)
- **Testing**: Fully test changes before submitting a PR
- **Formatting**: Code is auto-formatted with Spotless on every build
- **Architecture**: Follow existing patterns (Hilt DI, ViewBinding, etc.)
- **No proprietary code**: Do not bring non-free software into the project
- **Keep PRs focused**: One feature/fix per PR

### Build Requirements
- JDK 17
- Android SDK 35
- Gradle 8.x

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable/function names
- Add KDoc comments for public APIs
- Keep functions small and focused

## Translations

Translation support is being reorganized. Please open an issue before adding or changing translation resources.

## License

By contributing, you agree that your contributions will be licensed under the GPL-3.0-or-later license.
