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
    private RoundAmount roundAmount = new RoundAmount();
    private TransactionVelocity transactionVelocity = new TransactionVelocity();
    private VendorConcentration vendorConcentration = new VendorConcentration();

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

    public RoundAmount getRoundAmount() {
        return roundAmount;
    }

    public void setRoundAmount(RoundAmount roundAmount) {
        this.roundAmount = roundAmount;
    }

    public TransactionVelocity getTransactionVelocity() {
        return transactionVelocity;
    }

    public void setTransactionVelocity(TransactionVelocity transactionVelocity) {
        this.transactionVelocity = transactionVelocity;
    }

    public VendorConcentration getVendorConcentration() {
        return vendorConcentration;
    }

    public void setVendorConcentration(VendorConcentration vendorConcentration) {
        this.vendorConcentration = vendorConcentration;
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

    public static class RoundAmount {
        private BigDecimal unit = new BigDecimal("10000");
        private int score = 10;

        public BigDecimal getUnit() {
            return unit;
        }

        public void setUnit(BigDecimal unit) {
            this.unit = unit;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }
    }

    public static class TransactionVelocity {
        private long windowMinutes = 30;
        private int threshold = 3;
        private int score = 20;

        public long getWindowMinutes() {
            return windowMinutes;
        }

        public void setWindowMinutes(long windowMinutes) {
            this.windowMinutes = windowMinutes;
        }

        public int getThreshold() {
            return threshold;
        }

        public void setThreshold(int threshold) {
            this.threshold = threshold;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }
    }

    public static class VendorConcentration {
        private double thresholdPercent = 70.0;
        private int minimumTransactions = 3;
        private int score = 15;

        public double getThresholdPercent() {
            return thresholdPercent;
        }

        public void setThresholdPercent(double thresholdPercent) {
            this.thresholdPercent = thresholdPercent;
        }

        public int getMinimumTransactions() {
            return minimumTransactions;
        }

        public void setMinimumTransactions(int minimumTransactions) {
            this.minimumTransactions = minimumTransactions;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }
    }
}
