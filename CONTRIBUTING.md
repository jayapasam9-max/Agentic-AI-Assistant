# Contributing

Thanks for your interest in contributing to the Agentic Code Review Assistant! This is a personal project, but contributions, suggestions, and bug reports are welcome.

## How to contribute

1. **Fork** this repository to your own GitHub account
2. **Clone** your fork locally:
```bash
   git clone https://github.com/YOUR-USERNAME/Agentic-AI-Assistant.git
```
3. **Create a branch** for your change:
```bash
   git checkout -b your-feature-name
```
4. **Make your changes** — keep them focused (one fix or feature per PR)
5. **Run the tests** before submitting:
```bash
   cd backend
   ./mvnw test
```
6. **Commit** with a clear message describing what changed and why
7. **Push** to your fork and open a **Pull Request** against `main`

## Commit message style

Use short, present-tense messages that describe what changed:

- ✅ `Add input validation to webhook controller`
- ✅ `Fix race condition in Kafka consumer`
- ❌ `updated stuff`
- ❌ `wip`

## Code style

- Follow standard Java conventions (camelCase, 4-space indent)
- Add Javadoc comments for public methods
- Keep methods short — if something feels too long, split it

## Reporting bugs

Open an issue with:
- What you expected to happen
- What actually happened
- Steps to reproduce
- Your environment (OS, Java version, Docker version)

## Questions

Feel free to open an issue with the `question` label, or reach out via the maintainer's GitHub profile.