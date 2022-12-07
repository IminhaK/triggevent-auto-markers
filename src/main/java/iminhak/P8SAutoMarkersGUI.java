package iminhak;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.events.triggers.duties.Pandamonium.P8S2DominionPrioGui;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.DutyPluginTab;
import gg.xp.xivsupport.gui.nav.GlobalUiRegistry;
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

    private final GlobalUiRegistry reg;

    public P8SAutoMarkersGUI(P8SAutoMarkers p8s, GlobalUiRegistry reg) {
        this.p8s = p8s;
        this.reg = reg;
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
        JCheckBox useHC2 = new BooleanSettingGui(p8s.getUseHC2(), "Use HC2 markers").getComponent();
        JCheckBox useDominion = new BooleanSettingGui(p8s.getUseDominion(), "(NYI) Use Dominion markers").getComponent();
        ReadOnlyText text = new ReadOnlyText("""
                Limitless Desolation markers will mark DPS players with Attack 1-4 markers and support players with Bind 1-3 for the first third baits, then a square for the fourth.
                - Inverse roles will instead put the Attack markers on supports
                
                HC1 markers will mark the players who need to mix Attack 1 and 2 while also marking the Supersplice and Multisplice players with Bind 1-3 to indicate which debuff they must soak (Alpha 1, Beta 2, Gamma 3).
                For the second set of towers two players will be marked as Attack 1 & 2 and the other two will be marked as Ignore 1 & 2
                
                HC2 markers will mark the no debuff players with Ignore 1 and 2, then the first mixing players at Attack 1 and 2. After second debuffs it will mark long Alpha and Beta with Bind 1 and 2, and Gamma with Square and the unused short with Triangle
                
                Dominion markers will mark players soaking the first set of towers as Attack 1-4, and then swap the markers to the second set of players after the first set of towers goes off.
                Dominion markers use the priority from base Triggevent's Dominion prio, click below to be taken there:
                """);
        JButton domPrio = new JButton("Dominion Priority");
        domPrio.addActionListener(l -> reg.activateItem(P8S2DominionPrioGui.class));

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
        inner.add(text, c);
        c.gridy++;
        c.gridwidth = 1;
        c.weighty = 1;
        inner.add(domPrio, c);

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
