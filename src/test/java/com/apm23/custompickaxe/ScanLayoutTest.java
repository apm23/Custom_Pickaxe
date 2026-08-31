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

            assertTrue(x >= -32 && x <= 31);
            assertTrue(y >= -32 && y <= 31);
            assertTrue(z >= -32 && z <= 31);

            int normalized = (x + 32)
                    | ((z + 32) << 6)
                    | ((y + 32) << 12);
            assertTrue(!seen.get(normalized), "duplicate scan position at index " + index);
            seen.set(normalized);
        }

        assertEquals(64 * 64 * 64, seen.cardinality());
        assertEquals(-32, ScanLayout.offsetX(0));
        assertEquals(-32, ScanLayout.offsetY(0));
        assertEquals(-32, ScanLayout.offsetZ(0));
        assertEquals(31, ScanLayout.offsetX(ScanLayout.TOTAL_POSITIONS - 1));
        assertEquals(31, ScanLayout.offsetY(ScanLayout.TOTAL_POSITIONS - 1));
        assertEquals(31, ScanLayout.offsetZ(ScanLayout.TOTAL_POSITIONS - 1));
    }
}
