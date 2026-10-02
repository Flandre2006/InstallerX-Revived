// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.about

import androidx.compose.ui.graphics.ImageBitmap

data class AboutState(
    val enableFileLogging: Boolean = false,
    val appIcon: ImageBitmap? = null,
)
