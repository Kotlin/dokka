/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Creates an array of elements split into groups the length of size. If a collection can’t be split evenly, the
 * final chunk will be the remaining elements.
 *
 * @param array The array to process.
 * @param size The length of each chunk.
 * @return Returns the new array containing chunks.
 */
export function chunk<T>(array: Array<T> | null | undefined, size?: number): Array<Array<T>> {
  if (!array) {
    return [];
  }

  const result = [];
  const chunkSize = Math.max(size ?? 0, 1);
  let inputIndex = 0;
  let resultIndex = 0;

  while (inputIndex < array.length) {
    result[resultIndex] = array.slice(inputIndex, inputIndex + chunkSize);
    resultIndex++;
    inputIndex += chunkSize;
  }

  return result;
}
