package iminhak.anabaseios;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public enum PangenesisAssignment implements HasFriendlyName {

    Debuff1("Unstable 1"),
    Debuff2("Unstable 2"),
    Nothing1("Nothing 1"),
    Nothing2("Nothing 2"),
    ShortDark("Short Dark (16s)"),
    LongDark("Long Dark (20s)"),
    ShortLight("Short Light (16s)"),
    LongLight("Long Light (20s)");

    private final String friendlyName;

    PangenesisAssignment(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    @Override
    public String getFriendlyName() {
        return friendlyName;
    }
}
