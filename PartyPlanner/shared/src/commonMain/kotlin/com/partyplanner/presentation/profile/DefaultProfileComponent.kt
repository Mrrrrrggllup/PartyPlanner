package com.partyplanner.presentation.profile

import com.arkivanov.decompose.ComponentContext
import com.partyplanner.data.local.SessionStorage
import com.partyplanner.domain.usecase.auth.LogoutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DefaultProfileComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit,
    private val onLogout: () -> Unit,
) : ProfileComponent, ComponentContext by componentContext, KoinComponent {

    private val sessionStorage: SessionStorage by inject()
    private val logoutUseCase: LogoutUseCase by inject()

    private val _state = MutableStateFlow<ProfileState>(ProfileState.Loading)
    override val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        val session = sessionStorage.getSession()
        _state.value = ProfileState.Success(
            displayName  = session?.displayName ?: "Utilisateur",
            currentTheme = ThemeManager.themeMode.value,
        )
    }

    override fun onThemeChange(mode: ThemeMode) {
        ThemeManager.setTheme(mode)
        val current = _state.value as? ProfileState.Success ?: return
        _state.value = current.copy(currentTheme = mode)
    }

    override fun onLogout() {
        logoutUseCase()
        onLogout.invoke()
    }

    override fun onBack() = onBack.invoke()
}
