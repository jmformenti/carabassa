package org.atypical.carabassa.engine.llm;

import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;
import org.springframework.data.domain.Page;

/**
 * LLM-assisted search over a {@link Dataset}: turns a natural-language question
 * into a structured search and returns the matching items along with a
 * one-shot summary.
 */
public interface LlmSearchService {

    /**
     * Ask a natural-language question scoped to {@code dataset}.
     *
     * @param dataset  dataset to search; must not be {@code null}
     * @param question user question; must not be {@code null} or blank
     * @return the LLM-generated summary, the structured query the LLM ran, and
     *         the first page of matching items
     * @throws IllegalStateException if the LLM client is unavailable or invocation fails
     */
    AskResult ask(Dataset dataset, String question);

    /**
     * @param summary plain-text summary the LLM produced (never {@code null})
     * @param search  the structured query the LLM settled on, or {@code null}
     *                if the LLM never called {@code search_items}
     * @param page    first page of matching items, or {@code null} if no
     *                search was issued
     */
    record AskResult(String summary, String search, Page<IndexedItem> page) {
    }
}
