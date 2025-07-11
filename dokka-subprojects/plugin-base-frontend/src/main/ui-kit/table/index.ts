/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import './styles.scss';
import { debounce } from '../utils';

const CONTAINER_PADDING = 48;
const BORDER_WIDTH = 1;

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

  /**
   * Overflowing tables on desktop and tablet should be scrollable.
   * The left side of a table in such a case should align to the left,
   * when the right side of the table should exceed the content area as much as possible.
   * This is achieved by calculation a negative margin right for each table.
   * */
  function enlargeTableToTheRightIfNeeded(container: HTMLElement) {
    tables.forEach((element) => {
      const table = element as HTMLTableElement;
      const containerWidth = container.clientWidth - CONTAINER_PADDING * 2;
      if (table.scrollWidth > containerWidth) {
        const tableOverflowOffset = table.scrollWidth - containerWidth + BORDER_WIDTH;
        const availableRightSpace = Math.floor(
          document.documentElement.clientWidth -
            container.getBoundingClientRect().right +
            CONTAINER_PADDING -
            getActualScrollBarWidth()
        );
        const negativeMargin = Math.min(tableOverflowOffset, availableRightSpace);
        table.style.marginRight = `-${negativeMargin}px`;
      }
    });
  }

  const debouncedEnlargeTablesIfNeeded = debounce(enlargeTableToTheRightIfNeeded, 200);

  const resizeObserver = new ResizeObserver(() => {
    debouncedEnlargeTablesIfNeeded(container);
  });
  resizeObserver.observe(container);
  window.addEventListener('resize', () => {
    debouncedEnlargeTablesIfNeeded(container);
  });
}

document.addEventListener('DOMContentLoaded', initTables);
