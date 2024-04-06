package club.hellin.forceblocks.forceblock.impl;

public enum ForceMode {
    FORCE_FIELD,
    MAGNET;

    public ForceMode next() {
        int nextIndex = (this.ordinal() + 1) % ForceMode.values().length;
        return ForceMode.values()[nextIndex];
    }
}