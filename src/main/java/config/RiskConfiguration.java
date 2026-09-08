package config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalTime;

@Configuration
@ConfigurationProperties(prefix = "risk.rules")
public class RiskConfiguration {

    private HighAmount highAmount = new HighAmount();
    private UnusualTime unusualTime = new UnusualTime();
    private Duplicate duplicate = new Duplicate();

    public HighAmount getHighAmount() {
        return highAmount;
    }

    public void setHighAmount(HighAmount highAmount) {
        this.highAmount = highAmount;
    }

    public UnusualTime getUnusualTime() {
        return unusualTime;
    }

    public void setUnusualTime(UnusualTime unusualTime) {
        this.unusualTime = unusualTime;
    }

    public Duplicate getDuplicate() {
        return duplicate;
    }

    public void setDuplicate(Duplicate duplicate) {
        this.duplicate = duplicate;
    }

    public static class HighAmount {
        private BigDecimal threshold = new BigDecimal("100000");
        private int score = 30;

        public BigDecimal getThreshold() {
            return threshold;
        }

        public void setThreshold(BigDecimal threshold) {
            this.threshold = threshold;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }
    }

    public static class UnusualTime {
        private LocalTime businessStart = LocalTime.of(9, 0);
        private LocalTime businessEnd = LocalTime.of(18, 0);
        private int score = 20;

        public LocalTime getBusinessStart() {
            return businessStart;
        }

        public void setBusinessStart(LocalTime businessStart) {
            this.businessStart = businessStart;
        }

        public LocalTime getBusinessEnd() {
            return businessEnd;
        }

        public void setBusinessEnd(LocalTime businessEnd) {
            this.businessEnd = businessEnd;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }
    }

    public static class Duplicate {
        private long windowMinutes = 10;
        private int score = 25;

        public long getWindowMinutes() {
            return windowMinutes;
        }

        public void setWindowMinutes(long windowMinutes) {
            this.windowMinutes = windowMinutes;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }
    }
}
