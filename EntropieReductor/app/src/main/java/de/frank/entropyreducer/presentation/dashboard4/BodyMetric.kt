package de.frank.entropyreducer.presentation.dashboard4

import de.frank.entropyreducer.data.repository.ZeppBodyRepository

/** Shared units and stable navigation/card IDs for the direct Zepp body measurements. */
enum class BodyMetric(
    val repositoryKey: String,
    val routeKey: String,
    val cardId: String,
    val title: String,
    val unit: String,
    val lowerIsBetter: Boolean = false,
) {
    WEIGHT(ZeppBodyRepository.METRIC_WEIGHT, HealthConnectMetricKey.WEIGHT,
        BiomarkerCardId.MINI_WEIGHT, "Gewicht", "kg", true),
    BODY_FAT(ZeppBodyRepository.METRIC_BODY_FAT, HealthConnectMetricKey.BODY_FAT,
        BiomarkerCardId.MINI_BODY_FAT, "Körperfett", "%", true),
    LEAN(ZeppBodyRepository.METRIC_LEAN, HealthConnectMetricKey.LEAN_BODY_MASS,
        BiomarkerCardId.MINI_LEAN_BODY_MASS, "Magermasse", "kg"),
    MUSCLE(ZeppBodyRepository.METRIC_MUSCLE, HealthConnectMetricKey.MUSCLE_MASS,
        BiomarkerCardId.MINI_MUSCLE_MASS, "Muskelmasse", "kg"),
    BONE(ZeppBodyRepository.METRIC_BONE, HealthConnectMetricKey.BONE_MASS,
        BiomarkerCardId.MINI_BONE_MASS, "Knochenmasse", "kg"),
    SKELETAL_MUSCLE(ZeppBodyRepository.METRIC_SKELETAL_MUSCLE, "zepp_skeletal_muscle",
        BiomarkerCardId.MINI_SKELETAL_MUSCLE, "Skelettmuskeln", "kg"),
    VISCERAL_FAT(ZeppBodyRepository.METRIC_VISCERAL_FAT, "zepp_visceral_fat",
        BiomarkerCardId.MINI_VISCERAL_FAT, "Viszerales Fett", "Index", true),
    BMI(ZeppBodyRepository.METRIC_BMI, "zepp_bmi",
        BiomarkerCardId.MINI_BMI, "BMI", "", true),
    WATER(ZeppBodyRepository.METRIC_WATER, HealthConnectMetricKey.BODY_WATER,
        BiomarkerCardId.MINI_BODY_WATER, "Körperwasser", "kg"),
    WATER_PERCENT(ZeppBodyRepository.METRIC_WATER_PERCENT, "zepp_water_percent",
        BiomarkerCardId.MINI_BODY_WATER_PERCENT, "Wasseranteil", "%"),
    PROTEIN(ZeppBodyRepository.METRIC_PROTEIN, "zepp_protein",
        BiomarkerCardId.MINI_PROTEIN, "Eiweiß", "%"),
}
