package com.apm23.custompickaxe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;
import org.junit.jupiter.api.Test;

final class ScanLayoutTest {
    @Test
    void coversEveryPositionExactlyOnce() {
        BitSet seen = new BitSet(ScanLayout.TOTAL_POSITIONS);

        for (int index = 0; index < ScanLayout.TOTAL_POSITIONS; index++) {
            int x = ScanLayout.offsetX(index);
            int y = ScanLayout.offsetY(index);
            int z = ScanLayout.offsetZ(index);

            assertTrue(x >= -8 && x <= 7);
            assertTrue(y >= -8 && y <= 7);
            assertTrue(z >= -8 && z <= 7);

            int normalized = (x + 8)
                    | ((z + 8) << 4)
                    | ((y + 8) << 8);
            assertTrue(!seen.get(normalized), "duplicate scan position at index " + index);
            seen.set(normalized);
        }

        assertEquals(16 * 16 * 16, seen.cardinality());
        assertEquals(-8, ScanLayout.offsetX(0));
        assertEquals(-8, ScanLayout.offsetY(0));
        assertEquals(-8, ScanLayout.offsetZ(0));
        assertEquals(7, ScanLayout.offsetX(ScanLayout.TOTAL_POSITIONS - 1));
        assertEquals(7, ScanLayout.offsetY(ScanLayout.TOTAL_POSITIONS - 1));
        assertEquals(7, ScanLayout.offsetZ(ScanLayout.TOTAL_POSITIONS - 1));
    }
}
