/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.uitest.rendering

class LONG_CLASS_NAME_WITH_UNDERSCORES_EVEN_IF_IT_IS_NOT_BY_CONVENTION_BUT_IT_MIGHT_EXIST_WHO_KNOWS(
    paramWithLong_Name_with_Underscore_howWillItBe_rendered_By_DokkaIWonderOne: String,
    paramWithLong_Name_with_Underscore_howWillItBe_rendered_By_DokkaIWonderTwo: String,
) {
    fun function_with_long_name_with_underscores_same_story_as_this_class_name() {}

    fun `very-very-long-name-with-dashes-for-some-reason-even-if-not-popular-but-might-happen-maybe`() {}

    fun `mixed_underscore-with-dashes_same_thing-as_above-you-never_know-what-might_happen`(
        paramWithLong_Name_with_Underscore_howWillItBe_rendered_By_DokkaIWonderOne: String,
        paramWithLong_Name_with_Underscore_howWillItBe_rendered_By_DokkaIWonderTwo: LONG_CLASS_NAME_WITH_UNDERSCORES_EVEN_IF_IT_IS_NOT_BY_CONVENTION_BUT_IT_MIGHT_EXIST_WHO_KNOWS,
    ): LONG_CLASS_NAME_WITH_UNDERSCORES_EVEN_IF_IT_IS_NOT_BY_CONVENTION_BUT_IT_MIGHT_EXIST_WHO_KNOWS {
        TODO()
    }
}
