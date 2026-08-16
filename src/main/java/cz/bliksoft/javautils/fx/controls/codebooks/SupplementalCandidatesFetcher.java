package cz.bliksoft.javautils.fx.controls.codebooks;

import java.util.List;
import java.util.function.Consumer;

/**
 * Optional hook a {@link BasicCodebookProvider} may set to fetch extra
 * candidates (e.g. from a network call) when a selector opens with filter text
 * that could not be resolved directly via {@link ICodebookProvider#identify}.
 *
 * <p>
 * Implementations do their own asynchronous work and must invoke
 * {@code onResult} on the JavaFX application thread when done — or never, if
 * there is nothing to add. Never called on every keystroke: only once per
 * selector open, with the text that failed to resolve directly.
 *
 * @param <T> the type of codebook item
 */
@FunctionalInterface
public interface SupplementalCandidatesFetcher<T> {

	void fetch(BasicCodebookProvider<T> provider, String filterText, Consumer<List<T>> onResult);
}
