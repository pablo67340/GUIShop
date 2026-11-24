package com.pablo67340.guishop.neoforge.client;

/**
 * Shared colours and metrics for the shop screens, so the menu and the shop
 * grid stay visually consistent.
 * <p>
 * Everything is drawn with solid fills rather than a nine-slice texture, which
 * keeps the UI free of texture assets and lets it scale to any panel size.
 */
final class UiTheme {

    // Panel
    static final int PANEL_BG = 0xF0121216;
    static final int PANEL_BORDER = 0xFF2E2E3A;
    static final int HEADER_BG = 0xFF1B1B22;
    static final int DIVIDER = 0xFF2E2E3A;

    // Cells
    static final int CELL_BG = 0xFF1A1A21;
    static final int CELL_BG_HOVER = 0xFF262633;
    static final int CELL_BORDER_HOVER = 0xFF4A4A5E;

    // Text
    static final int TEXT = 0xFFECECF1;
    static final int TEXT_MUTED = 0xFF8B90A0;
    static final int BUY = 0xFF5FD98A;
    static final int SELL = 0xFFE9B155;
    static final int WARN = 0xFFE86A6A;

    // Scrollbar
    static final int SCROLL_TRACK = 0xFF16161C;
    static final int SCROLL_THUMB = 0xFF3A3A4A;

    // Metrics
    static final int PANEL_MAX_WIDTH = 480;
    static final int PANEL_MARGIN = 20;
    static final int HEADER_HEIGHT = 34;
    static final int FOOTER_HEIGHT = 26;
    static final int PADDING = 8;

    static final int CELL_WIDTH = 148;
    static final int CELL_HEIGHT = 32;
    static final int CELL_GAP = 4;
    static final int SCROLLBAR_WIDTH = 4;

    private UiTheme() {
    }
}
