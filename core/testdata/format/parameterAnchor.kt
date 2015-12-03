/**
 * Runs [processor] for each file and collects its results into single list
 * @param processor function to receive context for symbol resolution and file for processing
 */
public fun processFiles<T>(processor: () -> T): List<T> {
}
