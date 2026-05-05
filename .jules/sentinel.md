## 2025-12-28 - Path Traversal via Symbolic Links
**Vulnerability:** The `FileSystemNavigator` class was vulnerable to path traversal via symbolic links. While it correctly checked that `targetPath` started with `root` using simple path components, it used `toAbsolutePath().normalize()` which does not resolve symbolic links. This allowed a malicious symlink inside the root directory to point to an external directory (e.g., `/root/link` -> `/outside`), and `FileSystemNavigator` would treat it as valid because `/root/link` textually starts with `/root`.

**Learning:** `Path.toAbsolutePath().normalize()` in Java does *not* resolve symbolic links. It only handles `.` and `..` components syntactically. To ensure that a path is truly within a jailed directory, one must use `toRealPath()` (which resolves symlinks) on *both* the root and the target path, and then perform the `startsWith` check.

**Prevention:** When implementing file system jails or constrained navigation:
1. Resolve the `root` path to its real path using `toRealPath()`.
2. Resolve any user-supplied or derived `targetPath` using `toRealPath()`.
3. Check `targetPath.startsWith(root)`.
4. Be aware that `toRealPath()` throws `IOException` if the file does not exist, so handle that case (usually by treating it as "access denied" or "file not found").
