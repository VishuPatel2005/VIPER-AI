package com.rudra.myassistant;

/**
 * Generic callback interface for asynchronous data operations.
 * Resolves the "cannot find symbol" error and provides a standardized
 * way for repositories to return data to ViewModels or Activities.
 *
 * @param <T> The type of data to be returned upon success.
 */
public interface DataCallback<T> {
    /**
     * Called when the data operation completes successfully.
     * @param result The data returned from the operation.
     */
    void onSuccess(T result);

    /**
     * Called when the data operation fails or encounters an error.
     * @param error A description of the error encountered.
     */
    void onError(String error);
}
