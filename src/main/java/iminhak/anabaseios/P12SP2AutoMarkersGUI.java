package iminhak.anabaseios;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.events.triggers.duties.ewult.omega.BooleanSettingHidingPanel;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.DutyPluginTab;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.gui.tabs.SmartTabbedPane;
import gg.xp.xivsupport.persistence.gui.BasicAutomarkSettingGroupGui;
import gg.xp.xivsupport.persistence.gui.JobSortGui;
import gg.xp.xivsupport.persistence.gui.JobSortOverrideGui;
import gg.xp.xivsupport.persistence.settings.JobSortOverrideSetting;
import gg.xp.xivsupport.persistence.settings.MultiSlotAutomarkSetting;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ScanMe
public class P12SP2AutoMarkersGUI implements DutyPluginTab {

    private final P12SP2AutoMarkers backend;
    private final List<Runnable> toRefresh = new CopyOnWriteArrayList<>();
    private JobSortGui jsg;

    public P12SP2AutoMarkersGUI(P12SP2AutoMarkers backend) {
        this.backend = backend;
    }

    @Override
    public KnownDuty getDuty() {
        return KnownDuty.P12S;
    }

    @Override
    public String getTabName() {
        return "Iminha's Automarks";
    }

    @Override
    public int getSortOrder() {
        return 101;
    }

    private void refresh() {
        toRefresh.forEach(Runnable::run);
    }

    @Override
    public Component getTabContents() {
        jsg = new JobSortGui(backend.getGroupPrioJobSort());
        toRefresh.add(jsg::externalRefresh);
        RefreshLoop<P12SP2AutoMarkersGUI> refresher = new RefreshLoop<>("P12SAmRefresh", this, P12SP2AutoMarkersGUI::refresh, unused -> 10_000L);
        TitleBorderFullsizePanel outer = new TitleBorderFullsizePanel("Group Prio") {
            @Override
            public void setVisible(boolean aFlag) {
                super.setVisible(aFlag);
                if (aFlag) {
                    refresh();
                    refresher.startIfNotStarted();
                }
            }
        };
        outer.setLayout(new BorderLayout());
        SmartTabbedPane tabs = new SmartTabbedPane();
        ReadOnlyText helpText = new ReadOnlyText("""
                Instructions:
                The first tab lets you pick your default priority. The tabs for individual markers allow you to further
                customize each mark. Some of them allow you to override the priority for that AM specifically.""");
        {
            JPanel combined = jsg.getCombined();
            tabs.add("Default Prio", combined);
        }
        {
            MultiSlotAutomarkSetting<PangenesisAssignment> markSettings = backend.getPangenesisAMSettings();
            BasicAutomarkSettingGroupGui<PangenesisAssignment> pangenesisSettings = new BasicAutomarkSettingGroupGui<>("Pangenesis", markSettings, 4, true);
            tabs.addTab("Pangenesis", makeAmPanel(new BooleanSettingHidingPanel(backend.getPangenesisAmEnable(), "Pangenesis Automark", pangenesisSettings, true), backend.getPangenesisPrio()));
        }
        outer.add(tabs, BorderLayout.CENTER);
        outer.add(helpText, BorderLayout.NORTH);

        return outer;
    }

    private JPanel makeAmPanel(Component top, JobSortOverrideSetting override) {
        JobSortOverrideGui overrideGui = new JobSortOverrideGui(override);
        JPanel panel = new JPanel(new BorderLayout());
        int HEIGHT = 250;
        top.setPreferredSize(new Dimension(32767, HEIGHT));
        top.setMinimumSize(new Dimension(1, HEIGHT));
        top.setMinimumSize(new Dimension(32767, HEIGHT));
        panel.add(top, BorderLayout.NORTH);
        toRefresh.add(overrideGui::externalRefresh);
        JPanel overrideGuiPanel = overrideGui.getCombined();
        panel.add(overrideGuiPanel, BorderLayout.CENTER);
        panel.revalidate();
        return panel;
    }
}
