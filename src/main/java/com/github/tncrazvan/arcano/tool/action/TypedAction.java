package com.github.tncrazvan.arcano.tool.action;

/**
 * Make a callback and define its parameter type (P).
 * @author Razvan Tanase
 * @param <P> type of the parameter object.
 */
public interface TypedAction<P>{
    public abstract void callback(P e);
}