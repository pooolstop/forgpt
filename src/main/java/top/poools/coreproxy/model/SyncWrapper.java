package top.poools.coreproxy.model;

public class SyncWrapper<T> {
    private T t;

    public synchronized T get() {
        return t;
    }

    public synchronized void set(T t) {
        this.t = t;
    }

    @Override
    public String toString() {
        return t.toString();
    }
}
