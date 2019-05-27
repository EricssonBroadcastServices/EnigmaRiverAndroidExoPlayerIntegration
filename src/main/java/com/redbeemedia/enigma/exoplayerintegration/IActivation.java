package com.redbeemedia.enigma.exoplayerintegration;

/**
 * Utility type for executing code dependent on the execution of an other event.
 */
/*package-protected*/ interface IActivation {
    /**
     * Executes <code>runnable</code> if already active, otherwise queues the
     * <code>runnable</code> and waits until <code>activate()</code> is called.
     *
     * @param runnable
     */
    void whenActive(Runnable runnable);
    void activate();
}
