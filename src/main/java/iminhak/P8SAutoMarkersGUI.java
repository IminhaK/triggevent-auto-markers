package iminhak;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.DutyPluginTab;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class P8SAutoMarkersGUI implements DutyPluginTab {

    private final P8SAutoMarkers p8s;
    private JPanel inner;
    private JPanel innerLD;
    private JPanel innerHC1;
    private JPanel innerHC2;

    public P8SAutoMarkersGUI(P8SAutoMarkers p8s) {
        this.p8s = p8s;
    }

    @Override
    public String getTabName() {
        return "Iminha's P8S Automarkers";
    }

    @Override
    public Component getTabContents() {
        TitleBorderFullsizePanel outer = new TitleBorderFullsizePanel("P8S Automarkers");
        outer.setLayout(new BorderLayout());
        JCheckBox p8markers = new BooleanSettingGui(p8s.getUseAutoMarks(), "P8S Automarkers").getComponent();
        outer.add(p8markers, BorderLayout.NORTH);
        GridBagConstraints c = new GridBagConstraints(0, 0, 2, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0);

        inner = new JPanel();
        inner.setLayout(new GridBagLayout());
        innerLD = new JPanel();
        innerLD.setLayout(new GridBagLayout());
        JCheckBox useLD = new BooleanSettingGui(p8s.getUseLimitlessDesolation(), "Use Limitless Desolation markers").getComponent();
        JCheckBox useLDSupAttack = new BooleanSettingGui(p8s.getLDSupportAttack(), "Inverse roles").getComponent();

        JCheckBox useHC1 = new BooleanSettingGui(p8s.getUseHC1(), "Use HC1 markers").getComponent();
        JCheckBox useHC2 = new BooleanSettingGui(p8s.getUseHC2(), "Use HC1 markers").getComponent();
        JCheckBox useDominion = new BooleanSettingGui(p8s.getUseDominion(), "Use Dominion markers").getComponent();
        ReadOnlyText text = new ReadOnlyText("""
                Limitless Desolation markers will mark DPS players with Attack 1-4 markers and support players with bind 1-3 for the first third baits, then a square for the fourth.
                Inverse roles will instead put the Attack markers on supports
                
                HC1 markers will -
                HC2 markers will -
                Dominion markers will -
                """);

        innerLD.add(useLDSupAttack, c);

        inner.add(useLD, c);
        c.gridy++;
        inner.add(innerLD, c);
        c.gridy++;
        inner.add(useHC1, c);
        c.gridy++;
        inner.add(useHC2, c);
        c.gridy++;
        inner.add(useDominion, c);
        c.gridy++;
        c.gridwidth = 1;
        c.weighty = 1;
        inner.add(text, c);

        p8s.getUseAutoMarks().addAndRunListener(this::checkVis);
        p8s.getUseLimitlessDesolation().addAndRunListener(this::checkLDVis);
        outer.add(inner, BorderLayout.CENTER);
        return outer;
    }

    @Override
    public KnownDuty getDuty() {
        return KnownDuty.P8S;
    }

    private void checkVis() {
        boolean enabled = p8s.getUseAutoMarks().get();
        inner.setVisible(enabled);
    }

    private void checkLDVis() {
        boolean enabled = p8s.getUseLimitlessDesolation().get();
        innerLD.setVisible(enabled);
    }
}
