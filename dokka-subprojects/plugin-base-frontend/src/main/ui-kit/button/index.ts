/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import './styles.scss';

function onToggleDropdown(event: PointerEvent): void {
  (event.target as HTMLButtonElement).classList.toggle('button_dropdown_active');
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
(window as any).onToggleDropdown = onToggleDropdown;
