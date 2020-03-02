package org.jetbrains.dokka.model

import org.jetbrains.dokka.model.properties.ExtraProperty

class AdditionalModifiers(val content: List<ExtraModifiers>) : ExtraProperty<Documentable> {
    object AdditionalKey : ExtraProperty.Key<Documentable, AdditionalModifiers>

    override val key: ExtraProperty.Key<Documentable, *> = AdditionalKey
}