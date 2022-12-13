package iminhak.ultimate;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.DutyPluginTab;
import gg.xp.xivsupport.gui.nav.GlobalUiRegistry;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class TBDUltimateGUI implements DutyPluginTab {

    private final TBDUltimate tbdUltimate;
    private JPanel inner;
    private JPanel innerDummyMechanic;

    private final GlobalUiRegistry reg;

    public TBDUltimateGUI(TBDUltimate tbdUltimate, GlobalUiRegistry reg) {
        this.tbdUltimate = tbdUltimate;
        this.reg = reg;
    }

    @Override
    public String getTabName() {
        return "Iminha's TBD Ultimate Automarkers";
    }

    @Override
    public Component getTabContents() {
        TitleBorderFullsizePanel outer = new TitleBorderFullsizePanel("TBD Ultimate Automarkers");
        outer.setLayout(new BorderLayout());
        JCheckBox TBDMarkers = new BooleanSettingGui(tbdUltimate.getUseAutomarks(), "TBD Ultimate Automarkers").getComponent();
        outer.add(TBDMarkers, BorderLayout.NORTH);
        GridBagConstraints c = new GridBagConstraints(0, 0, 2, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0);

        inner = new JPanel();
        inner.setLayout(new GridBagLayout());
        innerDummyMechanic = new JPanel();
        innerDummyMechanic.setLayout(new GridBagLayout());
        JCheckBox useSomeMechanic = new BooleanSettingGui(tbdUltimate.getUseSomeMechanic(), "Use 'Some mechanic' markers").getComponent();
        ReadOnlyText text = new ReadOnlyText("""
                Here I would explain what each checkbox does, but the fight isnt out yet so I don't even know
                """);

        inner.add(useSomeMechanic, c);

        tbdUltimate.getUseAutomarks().addAndRunListener(this::checkVis);
        tbdUltimate.getUseSomeMechanic().addAndRunListener(this::checkSMVis);
        outer.add(inner, BorderLayout.CENTER);
        return outer;
    }

    @Override
    public KnownDuty getDuty() {
        return KnownDuty.None;
    }

    private void checkVis() {
        boolean enabled = tbdUltimate.getUseAutomarks().get();
        inner.setVisible(enabled);
    }

    private void checkSMVis() {
        boolean enabled = tbdUltimate.getUseSomeMechanic().get();
        innerDummyMechanic.setVisible(enabled);
    }
}
