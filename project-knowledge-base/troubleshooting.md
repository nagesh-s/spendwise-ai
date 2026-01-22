# Troubleshooting Guide

> **Purpose**: Quick reference for common issues and their solutions. Updated as issues are discovered and resolved.

---

## 🚨 Known Issues

*No issues discovered yet - project is in initial development phase.*

---

## 🔧 Common Problems & Solutions

### Build Issues

#### Problem: Gradle sync fails
**Symptoms**: Android Studio shows "Gradle sync failed" error
**Cause**: Usually network issues or dependency resolution problems
**Solution**:
1. Check internet connection
2. Try File → Invalidate Caches and Restart
3. Delete `.gradle` folder and re-sync

#### Problem: JitPack dependencies fail to resolve
**Symptoms**: Cannot find `com.github.PhilJay:MPAndroidChart` or `com.github.prolificinteractive:material-calendarview`
**Cause**: JitPack repository not configured
**Solution**: Ensure `settings.gradle` includes:
```groovy
maven { url 'https://jitpack.io' }
```

---

### Runtime Issues

*Will be populated as development progresses*

---

### Database Issues

*Will be populated when data layer is implemented*

---

## 📋 How to Report New Issues

When you encounter a new issue:
1. Add it to the "Known Issues" section above
2. Include: Symptoms, Cause (if known), Solution (if found)
3. If unresolved, mark as "UNRESOLVED" with any debugging notes

---

## 📅 Last Updated
2026-01-22
