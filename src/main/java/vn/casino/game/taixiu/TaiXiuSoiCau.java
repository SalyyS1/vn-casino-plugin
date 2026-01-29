package vn.casino.game.taixiu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tai Xiu Soi Cau (Pattern Analysis) tracker.
 * Tracks historical results for pattern display and statistical analysis.
 *
 * Features:
 * - Last N results tracking
 * - Streak detection (consecutive TAI/XIU wins)
 * - Win percentage calculation
 * - Triple occurrence tracking
 */
public class TaiXiuSoiCau {

    private final List<TaiXiuResult> history;
    private final int maxSize;

    /**
     * Create new Soi Cau tracker.
     *
     * @param maxSize Maximum number of results to track
     */
    public TaiXiuSoiCau(int maxSize) {
        this.maxSize = maxSize;
        this.history = new ArrayList<>(maxSize);
    }

    /**
     * Add a new result to history.
     * Automatically removes oldest result if at max capacity.
     *
     * @param result Result to add
     */
    public synchronized void addResult(TaiXiuResult result) {
        if (result == null) {
            throw new IllegalArgumentException("Result cannot be null");
        }

        // Add to beginning of list (most recent first)
        history.add(0, result);

        // Remove oldest if exceeded max size
        if (history.size() > maxSize) {
            history.remove(history.size() - 1);
        }
    }

    /**
     * Get last N results.
     *
     * @param n Number of results to retrieve
     * @return List of results (most recent first)
     */
    public synchronized List<TaiXiuResult> getLastN(int n) {
        if (n <= 0) {
            return Collections.emptyList();
        }

        int count = Math.min(n, history.size());
        return Collections.unmodifiableList(history.subList(0, count));
    }

    /**
     * Get all tracked results.
     *
     * @return List of all results (most recent first)
     */
    public synchronized List<TaiXiuResult> getAllResults() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    /**
     * Get current TAI streak (consecutive TAI wins).
     * Returns 0 if last result was XIU or triple.
     *
     * @return Current TAI streak count
     */
    public synchronized int getTaiStreak() {
        int streak = 0;
        for (TaiXiuResult result : history) {
            if (result.isTai()) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    /**
     * Get current XIU streak (consecutive XIU wins).
     * Returns 0 if last result was TAI or triple.
     *
     * @return Current XIU streak count
     */
    public synchronized int getXiuStreak() {
        int streak = 0;
        for (TaiXiuResult result : history) {
            if (result.isXiu()) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    /**
     * Calculate TAI win percentage.
     * Excludes triples from calculation.
     *
     * @return TAI win percentage (0.0 to 100.0)
     */
    public synchronized double getTaiPercentage() {
        if (history.isEmpty()) {
            return 0.0;
        }

        long taiCount = history.stream()
            .filter(r -> !r.isTriple())
            .filter(TaiXiuResult::isTai)
            .count();

        long nonTripleCount = history.stream()
            .filter(r -> !r.isTriple())
            .count();

        if (nonTripleCount == 0) {
            return 0.0;
        }

        return (taiCount * 100.0) / nonTripleCount;
    }

    /**
     * Calculate XIU win percentage.
     * Excludes triples from calculation.
     *
     * @return XIU win percentage (0.0 to 100.0)
     */
    public synchronized double getXiuPercentage() {
        if (history.isEmpty()) {
            return 0.0;
        }

        long xiuCount = history.stream()
            .filter(r -> !r.isTriple())
            .filter(TaiXiuResult::isXiu)
            .count();

        long nonTripleCount = history.stream()
            .filter(r -> !r.isTriple())
            .count();

        if (nonTripleCount == 0) {
            return 0.0;
        }

        return (xiuCount * 100.0) / nonTripleCount;
    }

    /**
     * Calculate triple occurrence percentage.
     *
     * @return Triple percentage (0.0 to 100.0)
     */
    public synchronized double getTriplePercentage() {
        if (history.isEmpty()) {
            return 0.0;
        }

        long tripleCount = history.stream()
            .filter(TaiXiuResult::isTriple)
            .count();

        return (tripleCount * 100.0) / history.size();
    }

    /**
     * Get total number of tracked results.
     *
     * @return Total result count
     */
    public synchronized int getResultCount() {
        return history.size();
    }

    /**
     * Clear all history.
     */
    public synchronized void clear() {
        history.clear();
    }

    /**
     * Check if history is empty.
     *
     * @return true if no results tracked
     */
    public synchronized boolean isEmpty() {
        return history.isEmpty();
    }

    /**
     * Get most recent result.
     *
     * @return Most recent result or null if empty
     */
    public synchronized TaiXiuResult getLastResult() {
        return history.isEmpty() ? null : history.get(0);
    }

    /**
     * Get formatted statistics summary.
     *
     * @return Statistics summary string
     */
    public synchronized String getStatsSummary() {
        if (history.isEmpty()) {
            return "No data";
        }

        return String.format(
            "Results: %d | Tài: %.1f%% | Xỉu: %.1f%% | Ba: %.1f%% | Streak: %s",
            getResultCount(),
            getTaiPercentage(),
            getXiuPercentage(),
            getTriplePercentage(),
            getCurrentStreak()
        );
    }

    /**
     * Get current streak description.
     *
     * @return Streak description (e.g., "Tài x5", "Xỉu x3", "None")
     */
    private String getCurrentStreak() {
        int taiStreak = getTaiStreak();
        int xiuStreak = getXiuStreak();

        if (taiStreak > 0) {
            return "Tài x" + taiStreak;
        } else if (xiuStreak > 0) {
            return "Xỉu x" + xiuStreak;
        } else {
            return "None";
        }
    }

    /**
     * Get full history.
     *
     * @return List of all results (most recent first)
     */
    public synchronized List<TaiXiuResult> getHistory() {
        return getAllResults();
    }

    /**
     * Get statistics data.
     *
     * @return Statistics object
     */
    public synchronized Statistics getStatistics() {
        // Calculate counts
        long taiCount = history.stream().filter(r -> !r.isTriple()).filter(TaiXiuResult::isTai).count();
        long xiuCount = history.stream().filter(r -> !r.isTriple()).filter(TaiXiuResult::isXiu).count();
        long tripleCount = history.stream().filter(TaiXiuResult::isTriple).count();

        // Calculate longest streaks
        int longestTaiStreak = calculateLongestStreak(true);
        int longestXiuStreak = calculateLongestStreak(false);

        return new Statistics(
            getTaiPercentage(),
            getXiuPercentage(),
            getTriplePercentage(),
            getTaiStreak(),
            getXiuStreak(),
            getResultCount(),
            (int) taiCount,
            (int) xiuCount,
            (int) tripleCount,
            longestTaiStreak,
            longestXiuStreak
        );
    }

    /**
     * Calculate longest streak for TAI or XIU.
     *
     * @param isTai true for TAI, false for XIU
     * @return Longest streak count
     */
    private int calculateLongestStreak(boolean isTai) {
        int maxStreak = 0;
        int currentStreak = 0;

        for (TaiXiuResult result : history) {
            if (isTai ? result.isTai() : result.isXiu()) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 0;
            }
        }

        return maxStreak;
    }

    /**
     * Statistics data class.
     */
    public static class Statistics {
        private final double taiPercentage;
        private final double xiuPercentage;
        private final double triplePercentage;
        private final int taiStreak;
        private final int xiuStreak;
        private final int totalCount;
        private final int taiCount;
        private final int xiuCount;
        private final int tripleCount;
        private final int longestTaiStreak;
        private final int longestXiuStreak;

        public Statistics(
            double taiPercentage,
            double xiuPercentage,
            double triplePercentage,
            int taiStreak,
            int xiuStreak,
            int totalCount,
            int taiCount,
            int xiuCount,
            int tripleCount,
            int longestTaiStreak,
            int longestXiuStreak
        ) {
            this.taiPercentage = taiPercentage;
            this.xiuPercentage = xiuPercentage;
            this.triplePercentage = triplePercentage;
            this.taiStreak = taiStreak;
            this.xiuStreak = xiuStreak;
            this.totalCount = totalCount;
            this.taiCount = taiCount;
            this.xiuCount = xiuCount;
            this.tripleCount = tripleCount;
            this.longestTaiStreak = longestTaiStreak;
            this.longestXiuStreak = longestXiuStreak;
        }

        public double getTaiPercentage() {
            return taiPercentage;
        }

        public double getXiuPercentage() {
            return xiuPercentage;
        }

        public double getTriplePercentage() {
            return triplePercentage;
        }

        public int getTaiStreak() {
            return taiStreak;
        }

        public int getXiuStreak() {
            return xiuStreak;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public int getTaiCount() {
            return taiCount;
        }

        public int getXiuCount() {
            return xiuCount;
        }

        public int getTripleCount() {
            return tripleCount;
        }

        public int getCurrentTaiStreak() {
            return taiStreak;
        }

        public int getCurrentXiuStreak() {
            return xiuStreak;
        }

        public int getLongestTaiStreak() {
            return longestTaiStreak;
        }

        public int getLongestXiuStreak() {
            return longestXiuStreak;
        }

        public int getTotalRounds() {
            return totalCount;
        }
    }
}
