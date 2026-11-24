package de.fokus.app;

import java.time.LocalDate;

/** Akzentfarben. */
final class Content {
    private Content() {}

    static final String[] COLOR_NAMES = {"Grün", "Minze", "Ozean", "Lila", "Gold", "Weiß"};
    static final int[] COLORS = {0xFF3DDC84, 0xFF4DD0A8, 0xFF4FC3F7, 0xFFB388FF, 0xFFE6C36A, 0xFFE0E0E0};

    static int autoColorIndex() {
        LocalDate d = LocalDate.now();
        return (d.getDayOfYear() + d.getYear() * 3) % COLORS.length;
    }
}
