package org.jsmpp.session;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jsmpp.bean.Bind;

/**
 * @author uudashr
 * 
 */
class BindRequestReceiver {
    private final Lock lock = new ReentrantLock();
    private final Condition requestCondition = lock.newCondition();
    private final ServerResponseHandler responseHandler;
    private BindRequest request;
    private boolean alreadyWaitForRequest;

    public BindRequestReceiver(ServerResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
    }

    /**
     * Wait until the bind request received for specified timeout.
     * 
     * @param timeout
     *            is the timeout.
     * @return the {@link BindRequest}.
     * @throws IllegalStateException
     *             if this method already called before.
     * @throws TimeoutException
     *             if the timeout has been reach.
     */
    BindRequest waitForRequest(long timeout) throws IllegalStateException, TimeoutException {
        lock.lock();
        try {
            if (alreadyWaitForRequest) {
                throw new IllegalStateException("waitForRequest(long) method already invoked");
            } else if (request == null) {
                try {
                    requestCondition.await(timeout, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                }
            }

            if (request != null) {
                return request;
            }
            throw new TimeoutException("Wating for bind request take time too long");
        } finally {
            alreadyWaitForRequest = true;
            lock.unlock();
        }

    }

    /**
     * Notify that the bind has accepted.
     * 
     * @param bindParameter
     *            is the {@link Bind} command.
     * @throws IllegalStateException
     *             if this method already called before.
     */
    void notifyAcceptBind(Bind bindParameter) throws IllegalStateException {
        lock.lock();
        try {
            if (request == null) {
                request = new BindRequest(bindParameter, responseHandler);
                requestCondition.signal();
            } else {
                throw new IllegalStateException("Already waiting for acceptance bind");
            }
        } finally {
            lock.unlock();
        }
    }
}