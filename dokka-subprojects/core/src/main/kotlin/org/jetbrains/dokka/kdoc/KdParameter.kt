/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

// parameter = receiver, context, value
public interface KdParameter : KdDocumented {
    public val type: KdType
}

public interface KdNamedParameter : KdParameter {
    public val name: String
}

public interface KdReceiverParameter : KdParameter
public interface KdContextParameter : KdNamedParameter
public interface KdValueParameter : KdNamedParameter {
    public val defaultValue: String?
}
