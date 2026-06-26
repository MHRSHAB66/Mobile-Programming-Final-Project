package com.example.project.domain.usecase

import com.example.project.domain.repository.SettingsRepository
import kotlinx.coroutines.delay

/**
 * Simulated purchase/upgrade flow. A real implementation would call a billing/payment
 * backend; here we emulate a short processing delay and then persist premium = true.
 */
class UpgradeToPremiumUseCase(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke() {
        delay(1500) // simulate payment processing
        settingsRepository.setPremium(true)
    }
}
