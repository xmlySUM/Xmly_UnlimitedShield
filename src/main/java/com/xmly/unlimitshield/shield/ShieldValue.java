package com.xmly.unlimitshield.shield;

import java.util.Locale;

public final class ShieldValue {

    private static final double MANTISSA_LIMIT = Double.MAX_VALUE;

    private static final long EXPONENT_GUARD = 1L << 32;

    public static final long ZERO_DAMAGE_DELTA = 256L;

    public static final long NAN_DAMAGE_DELTA = -512L;

    public static final long INFINITE_DAMAGE_DELTA = -384L;

    private static final double LOG10_OF_2 = Math.log10(2.0);

    public static final byte HITS_Y_DISABLED = -128;

    private static final long HIT_SCALE = 2048L;

    private static final long[] M_TABLE = new long[128];

    private static final double[] B_TABLE = new double[128];

    private static final double[] INVERSE_POW_TABLE = new double[128];

    static {
        for (int n = 1; n <= 127; n++) {
            long m = HIT_SCALE * (128L - n);
            double b = Math.pow(Math.pow(2.0, 52.0) / (129.0 - n), 1.0 / (n - 0.9)) - 1.0;
            M_TABLE[n] = m;
            B_TABLE[n] = Math.min(b, (double) m);
            INVERSE_POW_TABLE[n] = Math.pow(1.0 + (double) m, 1.0 - n);
        }
    }

    private static final long EXPONENT_SUM_LIMIT = Long.MAX_VALUE / 4096L;

    private double x;
    private long y;

    public ShieldValue(double x, long y) {
        this.x = x;
        this.y = y;
    }

    public double x() {
        return this.x;
    }

    public long y() {
        return this.y;
    }

    public boolean isHolding() {
        return this.y >= 0L || this.x >= 1.0;
    }

    public boolean isDepleted() {
        return !isHolding();
    }

    public Settlement applyDamage(double[] damages) {
        return applyDamage(damages, (byte) 0);
    }

    public Settlement applyDamage(double[] damages, byte hitsY) {
        return applyDamage(damages, hitsY, damages == null ? 0L : damages.length);
    }

    public Settlement applyDamage(double[] damages, byte hitsY, long hits) {
        if (damages == null || damages.length == 0 || isDepleted()) {

            return null;
        }

        if (hitsY == HITS_Y_DISABLED || this.y < 0L) {
            Settlement settlement = applyToMantissa(damages);
            if (settlement == null) {
                return null;
            }
            canonicaliseMantissa();
            return settlement;
        }

        DamageTerms terms = damageTerms(damages);
        boolean anyCount = hitsY != 0;
        if (terms == null && !anyCount) {
            return null;
        }

        long drop = terms == null ? 0L : terms.exponentLoss();
        long countLoss = 0L;
        if (anyCount) {
            countLoss = hitCountLoss(hitsY, hits);
            drop = hitsY < 0 ? countLoss : drop + countLoss;
        }

        double xBefore = this.x;
        long yBefore = this.y;
        this.y = saturatingAdd(this.y, -drop);
        canonicaliseMantissa();
        return new Settlement(Settlement.Mode.EXPONENT,
                terms == null ? 0L : terms.log2Sum(),
                terms == null ? 0L : terms.exponent(),
                terms == null ? 0L : terms.specialDelta(),
                countLoss, xBefore, yBefore, this.x, this.y);
    }

    private DamageTerms damageTerms(double[] damages) {
        long log2Sum = 0L;
        long specialDelta = 0L;
        boolean anyDamage = false;

        for (double damage : damages) {
            long bits = Double.doubleToRawLongBits(damage);
            if ((bits & 0x7FFFFFFFFFFFFFFFL) == 0L) {

                specialDelta += ZERO_DAMAGE_DELTA;
            } else if (Double.isNaN(damage)) {
                specialDelta += NAN_DAMAGE_DELTA;
            } else if (Double.isInfinite(damage)) {
                specialDelta += INFINITE_DAMAGE_DELTA;
            } else if (damage > 0.0) {
                if (log2Sum < EXPONENT_SUM_LIMIT) {
                    log2Sum += Math.getExponent(damage);
                }
                anyDamage = true;
            }
        }
        if (!anyDamage && specialDelta == 0L) {
            return null;
        }

        long exponent = saturatingCast(Math.floor(log2Sum * LOG10_OF_2));
        return new DamageTerms(log2Sum, exponent, specialDelta);
    }

    private void canonicaliseMantissa() {
        if (this.x <= 0.0) {
            this.x = -0.0;
        }
    }

    private record DamageTerms(long log2Sum, long exponent, long specialDelta) {
        long exponentLoss() {
            return exponent - specialDelta;
        }
    }

    private Settlement applyToMantissa(double[] damages) {
        double sum = 0.0;
        for (double damage : damages) {

            if (damage > 0.0) {
                sum += damage;
            }
        }
        if (!(sum > 0.0)) {
            return null;
        }

        double xBefore = this.x;
        long yBefore = this.y;
        this.x -= sum;
        return new Settlement(Settlement.Mode.MANTISSA, 0L, 0L, 0L, sum,
                xBefore, yBefore, this.x, this.y);
    }

    private long hitCountLoss(byte hitsY, long hits) {
        int n = Math.min(Math.abs(hitsY), 127);
        long h = Math.min(M_TABLE[n], Math.max(hits, 0L));
        if (h <= 0L) {
            return 0L;
        }
        if (n == 1) {

            return h;
        }
        double raw = (double) h
                * Math.pow(1.0 + (double) M_TABLE[n] + B_TABLE[n] * (double) h, n - 1)
                * INVERSE_POW_TABLE[n];
        return saturatingCast(Math.floor(raw));
    }

    private static long saturatingCast(double value) {
        if (value >= (double) Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        if (value <= (double) Long.MIN_VALUE) {
            return Long.MIN_VALUE;
        }
        return (long) value;
    }

    public void clampExponent(long cap) {
        long limit = Math.max(0L, cap);
        if (this.y > limit) {
            this.y = limit;
        }
    }

    public void clampMantissa(double cap) {
        if (this.x > cap) {
            this.x = Math.max(cap, 0.0);
        }
    }

    public void doubleExponent() {
        this.y = saturatingAdd(this.y, this.y);
    }

    public String describeMantissaOnly() {
        return isDepleted() ? "0" : formatMantissa(this.x);
    }

    public void subtractExponent(long amount) {
        this.y = saturatingAdd(this.y, -Math.max(amount, 0L));
    }

    public void subtractMantissa(double amount) {
        if (amount > 0.0) {
            this.x -= amount;
        }
    }

    public void addToMantissa(double amount) {
        if (!isUsable(amount)) {
            return;
        }
        double updated = this.x + amount;
        this.x = Double.isFinite(updated) ? updated : MANTISSA_LIMIT;
    }

    public String describe() {
        return describe(this.y >= 0);
    }

    public String describe(boolean exponentVisible) {
        if (isDepleted()) {
            return "0";
        }
        return (exponentVisible && this.y >= 0) ? formatMantissa(this.x) + " × 10^" + this.y : formatMantissa(this.x);
    }

    private static String formatMantissa(double value) {
        if (value == Math.floor(value) && Math.abs(value) < 1.0e7) {
            return Long.toString((long) value);
        }
        String text = String.format(Locale.ROOT, "%.4g", value);
        int dot = text.indexOf('.');
        if (dot < 0 || text.indexOf('e') >= 0) {

            return text;
        }

        int end = text.length();
        while (end > dot + 1 && text.charAt(end - 1) == '0') {
            end--;
        }
        if (end == dot + 1) {
            end = dot;
        }
        return text.substring(0, end);
    }

    @Override
    public String toString() {
        return describe();
    }

    private static boolean isUsable(double value) {
        return value > 0.0 && Double.isFinite(value);
    }

    private static long saturatingAdd(long value, long delta) {
        long updated = value + delta;
        if (((value ^ updated) & (delta ^ updated)) < 0L) {
            return delta > 0L ? Long.MAX_VALUE - EXPONENT_GUARD : Long.MIN_VALUE + EXPONENT_GUARD;
        }
        return updated;
    }

    public static final class Settlement {

        public enum Mode {

            EXPONENT,

            MANTISSA,

            HITS
        }

        private final Mode mode;
        private final long log2Sum;
        private final long exponent;
        private final long specialDelta;

        private final double sum;
        private final double xBefore;
        private final long yBefore;
        private final double xAfter;
        private final long yAfter;

        Settlement(Mode mode, long log2Sum, long exponent, long specialDelta, double sum,
                   double xBefore, long yBefore, double xAfter, long yAfter) {
            this.mode = mode;
            this.log2Sum = log2Sum;
            this.exponent = exponent;
            this.specialDelta = specialDelta;
            this.sum = sum;
            this.xBefore = xBefore;
            this.yBefore = yBefore;
            this.xAfter = xAfter;
            this.yAfter = yAfter;
        }

        public Mode mode() {
            return this.mode;
        }

        public String describe(double[] damages) {
            StringBuilder batch = new StringBuilder("[");
            for (int i = 0; i < damages.length; i++) {
                if (i > 0) {
                    batch.append(", ");
                }
                batch.append(String.format(Locale.ROOT, "%.4g", damages[i]));
            }
            batch.append(']');

            String detail = switch (this.mode) {
                case EXPONENT -> this.sum > 0.0
                        ? String.format(Locale.ROOT,
                        "log2Sum=%d exponent=%d special=%+d hits=%d",
                        this.log2Sum, this.exponent, this.specialDelta, (long) this.sum)
                        : String.format(Locale.ROOT, "log2Sum=%d exponent=%d special=%+d",
                        this.log2Sum, this.exponent, this.specialDelta);
                case HITS -> String.format(Locale.ROOT, "hitsY=%d H=%d delta=%d",
                        this.log2Sum, this.specialDelta, this.exponent);
                case MANTISSA -> String.format(Locale.ROOT,
                        "sum=%.6g (y negative or off, no multiplication)", this.sum);
            };

            return String.format(Locale.ROOT,
                    "%d damage(s) %s -> %s | x %.6g -> %.6g, y %d -> %d",
                    damages.length, batch, detail, this.xBefore, this.xAfter, this.yBefore, this.yAfter);
        }
    }
}
