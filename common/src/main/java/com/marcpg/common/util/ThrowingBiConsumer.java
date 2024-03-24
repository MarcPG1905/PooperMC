package com.marcpg.common.util;

/**
 * Represents an operation that accepts two input arguments, returns no result
 * and throws a specified exception when {@link #accept(Object, Object) accepted}.
 * @param <T> the type of the first argument to the operation
 * @param <U> the type of the second argument to the operation
 * @param <E> the type of exception that is thrown when accepted.
 * @see java.util.function.BiConsumer
 */
@SuppressWarnings("unused")
@FunctionalInterface
public interface ThrowingBiConsumer<T, U, E extends Exception> {
    /**
     * Performs this operation on the given arguments.
     * @param t the first input argument
     * @param u the second input argument
     * @throws E the specified exception
     */
    void accept(T t, U u) throws E;
}
