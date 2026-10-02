// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.about

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.domain.engine.repository.AppIconRepository.Companion.SETTINGS_APP_LIST
import com.rosan.installer.domain.engine.usecase.GetAppIconUseCase
import com.rosan.installer.domain.settings.provider.SystemEnvProvider
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.usecase.settings.UpdateSettingUseCase
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AboutViewModel(
    appSettingsRepo: AppSettingsRepository,
    private val systemEnvProvider: SystemEnvProvider,
    private val updateSetting: UpdateSettingUseCase,
    private val getAppIcon: GetAppIconUseCase,
) : ViewModel() {

    private val _uiEvents = MutableSharedFlow<AboutEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEvents = _uiEvents.asSharedFlow()

    private val _appIcon = MutableStateFlow<ImageBitmap?>(null)

    val state: StateFlow<AboutState> = combine(
        appSettingsRepo.preferencesFlow,
        _appIcon,
    ) { prefs, appIcon ->
        AboutState(
            enableFileLogging = prefs.enableFileLogging,
            appIcon = appIcon,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AboutState(),
    )

    init {
        loadAppIcon()
    }

    fun dispatch(action: AboutAction) {
        when (action) {
            is AboutAction.SetEnableFileLogging -> setEnableFileLogging(action.enable)
            is AboutAction.ShareLog -> shareLog()
        }
    }

    private fun loadAppIcon() = viewModelScope.launch {
        val bitmap = getAppIcon(
            sessionId = SETTINGS_APP_LIST,
            packageName = systemEnvProvider.packageName,
            iconSizePx = 512,
            preferSystemIcon = true,
        )
        _appIcon.value = bitmap?.asImageBitmap()
    }

    private fun setEnableFileLogging(enable: Boolean) = viewModelScope.launch {
        updateSetting(BooleanSetting.EnableFileLogging, enable)
    }

    private fun shareLog() = viewModelScope.launch {
        try {
            val uriStr = systemEnvProvider.getLatestLogUri()
            if (uriStr == null) {
                _uiEvents.emit(AboutEvent.ShareLogFailed("Log file is missing or empty"))
            } else {
                _uiEvents.emit(AboutEvent.OpenLogShare(uriStr.toUri()))
            }
        } catch (e: Exception) {
            _uiEvents.emit(AboutEvent.ShareLogFailed(e.message ?: "Failed"))
        }
    }
}
