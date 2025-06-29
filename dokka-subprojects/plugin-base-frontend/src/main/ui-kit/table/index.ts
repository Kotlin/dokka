/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import './styles.scss';
import { debounce } from '../utils';

function getActualScrollBarWidth(): number {
  const scrollDiv = document.createElement('div');
  scrollDiv.style.width = '100px';
  scrollDiv.style.height = '100px';
  scrollDiv.style.overflow = 'scroll';
  document.body.appendChild(scrollDiv);
  const width = scrollDiv.offsetWidth - scrollDiv.clientWidth;
  document.body.removeChild(scrollDiv);
  return width;
}

function initTables() {
  const container = document.getElementById('content');
  if (!container) {
    return;
  }

  const tables = document.querySelectorAll('.table--container');

  const prevMarginsRight: string[] = [];

  /**
   * Overflowing tables on desktop and tablet should be scrollable.
   * The left side of a table in such a case should align to the left,
   * when the right side of the table should exceed the content area as much as possible.
   * This is achieved by calculation a negative margin right for each table.
   * */
  function enlargeTableToTheRightIfNeeded() {
    tables.forEach((element, index) => {
      const table = element as HTMLTableElement;
      let nextMarginRight = '';
      if (table.scrollWidth > table.clientWidth) {
        const overflowingWidth = table.scrollWidth - table.clientWidth;
        const gapToViewportSide =
          document.documentElement.clientWidth - table.getBoundingClientRect().right - getActualScrollBarWidth();
        const negativeMargin = Math.min(overflowingWidth, gapToViewportSide);
        const needsToChangeMargin = gapToViewportSide !== 0;
        nextMarginRight = needsToChangeMargin ? `-${negativeMargin}px` : prevMarginsRight[index];
      }
      if (nextMarginRight !== prevMarginsRight[index]) {
        table.style.marginRight = nextMarginRight;
        prevMarginsRight[index] = nextMarginRight;
      }
    });
  }

  const debouncedEnlargeTablesIfNeeded = debounce(enlargeTableToTheRightIfNeeded, 100);

  const resizeObserver = new ResizeObserver(() => debouncedEnlargeTablesIfNeeded());
  resizeObserver.observe(container);
}

document.addEventListener('DOMContentLoaded', initTables);
