package co.edu.uco.crosscutting.helpers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UtilNumericTest {

    @Test
    void getDefault_returnsDefault_whenValueIsNull() {
        assertThat(UtilNumeric.getDefault(null, 5)).isEqualTo(5);
    }

    @Test
    void getDefault_returnsValue_whenNotNull() {
        assertThat(UtilNumeric.getDefault(7, 5)).isEqualTo(7);
    }

    @Test
    void getDefault_returnsZero_whenValueIsNull() {
        assertThat(UtilNumeric.getDefault(null)).isEqualTo(UtilNumeric.ZERO);
    }

    @Test
    void isGreaterThan_returnsTrue_whenFirstIsGreater() {
        assertThat(UtilNumeric.isGreaterThan(10, 5)).isTrue();
    }

    @Test
    void isGreaterThan_returnsFalse_whenFirstIsLower() {
        assertThat(UtilNumeric.isGreaterThan(5, 10)).isFalse();
    }

    @Test
    void isGreaterThan_returnsFalse_whenEqual() {
        assertThat(UtilNumeric.isGreaterThan(5, 5)).isFalse();
    }

    @Test
    void isGreaterThan_treatsNullAsZero() {
        assertThat(UtilNumeric.isGreaterThan(null, -1)).isTrue();
        assertThat(UtilNumeric.isGreaterThan(null, 1)).isFalse();
    }

    @Test
    void isLessThan_returnsTrue_whenFirstIsLower() {
        assertThat(UtilNumeric.isLessThan(5, 10)).isTrue();
    }

    @Test
    void isLessThan_returnsFalse_whenFirstIsGreater() {
        assertThat(UtilNumeric.isLessThan(10, 5)).isFalse();
    }

    @Test
    void isEqualThan_returnsTrue_whenEqual() {
        assertThat(UtilNumeric.isEqualThan(5, 5)).isTrue();
    }

    @Test
    void isEqualThan_returnsFalse_whenDifferent() {
        assertThat(UtilNumeric.isEqualThan(5, 6)).isFalse();
    }

    @Test
    void isGreaterOrEqualThan_returnsTrue_whenEqual() {
        assertThat(UtilNumeric.isGreaterOrEqualThan(5, 5)).isTrue();
    }

    @Test
    void isGreaterOrEqualThan_returnsTrue_whenGreater() {
        assertThat(UtilNumeric.isGreaterOrEqualThan(6, 5)).isTrue();
    }

    @Test
    void isGreaterOrEqualThan_returnsFalse_whenLower() {
        assertThat(UtilNumeric.isGreaterOrEqualThan(4, 5)).isFalse();
    }

    @Test
    void isLessOrEqualThan_returnsTrue_whenEqual() {
        assertThat(UtilNumeric.isLessOrEqualThan(5, 5)).isTrue();
    }

    @Test
    void isLessOrEqualThan_returnsFalse_whenGreater() {
        assertThat(UtilNumeric.isLessOrEqualThan(6, 5)).isFalse();
    }

    @Test
    void isDifferent_returnsTrue_whenDifferent() {
        assertThat(UtilNumeric.isDifferent(5, 6)).isTrue();
    }

    @Test
    void isDifferent_returnsFalse_whenEqual() {
        assertThat(UtilNumeric.isDifferent(5, 5)).isFalse();
    }

    @Test
    void isBetween_withInclusiveRanges_returnsTrue_onBoundaries() {
        assertThat(UtilNumeric.isBetween(1, 1, 3, true, true)).isTrue();
        assertThat(UtilNumeric.isBetween(3, 1, 3, true, true)).isTrue();
    }

    @Test
    void isBetween_withInclusiveRanges_returnsTrue_inMiddle() {
        assertThat(UtilNumeric.isBetween(2, 1, 3, true, true)).isTrue();
    }

    @Test
    void isBetween_withInclusiveRanges_returnsFalse_outside() {
        assertThat(UtilNumeric.isBetween(0, 1, 3, true, true)).isFalse();
        assertThat(UtilNumeric.isBetween(4, 1, 3, true, true)).isFalse();
    }

    @Test
    void isBetween_withExclusiveRanges_rejectsBoundaries() {
        assertThat(UtilNumeric.isBetween(1, 1, 3, false, false)).isFalse();
        assertThat(UtilNumeric.isBetween(3, 1, 3, false, false)).isFalse();
    }

    @Test
    void isBetween_withExclusiveRanges_acceptsMiddle() {
        assertThat(UtilNumeric.isBetween(2, 1, 3, false, false)).isTrue();
    }

    @Test
    void isBetween_withInclusiveInitialOnly_acceptsInitialBoundary() {
        assertThat(UtilNumeric.isBetween(1, 1, 3, true, false)).isTrue();
        assertThat(UtilNumeric.isBetween(3, 1, 3, true, false)).isFalse();
    }

    @Test
    void isBetween_withInclusiveFinalOnly_acceptsFinalBoundary() {
        assertThat(UtilNumeric.isBetween(3, 1, 3, false, true)).isTrue();
        assertThat(UtilNumeric.isBetween(1, 1, 3, false, true)).isFalse();
    }

    @Test
    void isBetweenIncludingRanges_delegatesWithBothInclusive() {
        assertThat(UtilNumeric.isBetweenIncludingRanges(1, 1, 3)).isTrue();
        assertThat(UtilNumeric.isBetweenIncludingRanges(3, 1, 3)).isTrue();
        assertThat(UtilNumeric.isBetweenIncludingRanges(2, 1, 3)).isTrue();
        assertThat(UtilNumeric.isBetweenIncludingRanges(4, 1, 3)).isFalse();
    }

    @Test
    void isPositive_returnsTrue_forZero() {
        assertThat(UtilNumeric.isPositive(0)).isTrue();
    }

    @Test
    void isPositive_returnsTrue_forPositive() {
        assertThat(UtilNumeric.isPositive(1)).isTrue();
    }

    @Test
    void isPositive_returnsFalse_forNegative() {
        assertThat(UtilNumeric.isPositive(-1)).isFalse();
    }
}