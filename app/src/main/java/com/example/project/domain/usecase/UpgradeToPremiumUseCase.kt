package com.example.project.domain.usecase

import com.example.project.domain.repository.ProfileRepository

/**
 * Premium upgrade flow. Calls `POST /me/premium/upgrade` when a backend session exists,
 * otherwise flips premium locally (demo user).
 */
class UpgradeToPremiumUseCase(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(): Result<Unit> = profileRepository.upgradePremium()
}
