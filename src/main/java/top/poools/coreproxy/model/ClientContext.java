package top.poools.coreproxy.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientContext {
    private final SyncWrapper<String> minerInfo = new SyncWrapper<>();
    private final SyncWrapper<String> inetAddress = new SyncWrapper<>();
    private final SyncWrapper<Long> difficulty = new SyncWrapper<>();
    private final SyncWrapper<Pool> pool = new SyncWrapper<>();
    private final SyncWrapper<User> user = new SyncWrapper<>();
    private final SyncWrapper<Miner> miner = new SyncWrapper<>();
    private final SyncWrapper<LocalDateTime> lastUpdated = new SyncWrapper<>();
    private final Map<Integer, Share> shareMap = new ConcurrentHashMap<>();

    public String getMinerInfo() {
        return minerInfo.get();
    }

    public void setMinerInfo(String minerInfo) {
        this.minerInfo.set(minerInfo);
    }

    public String getInetAddress() {
        return inetAddress.get();
    }

    public void setInetAddress(String inetAddress) {
        this.inetAddress.set(inetAddress);
    }

    public Long getDifficulty() {
        return difficulty.get();
    }

    public void setDifficulty(Long difficulty) {
        this.difficulty.set(difficulty);
    }

    public Pool getPool() {
        return pool.get();
    }

    public void setPool(Pool pool) {
        this.pool.set(pool);
    }

    public User getUser() {
        return user.get();
    }

    public void setUser(User user) {
        this.user.set(user);
    }

    public Miner getMiner() {
        return miner.get();
    }

    public void setMiner(Miner miner) {
        this.miner.set(miner);
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated.get();
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated.set(lastUpdated);
    }

    public void addShare(Share share) {
        shareMap.put(share.getMessageId(), share);
    }

    public void removeShare(Integer messageId) {
        shareMap.remove(messageId);
    }

    public Share getShare(Integer messageId) {
        return shareMap.get(messageId);
    }

    public Map<Integer, Share> getShares() {
        return shareMap;
    }

    public void clearShares() {
        shareMap.clear();
    }
}
