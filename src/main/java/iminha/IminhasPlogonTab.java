package iminha;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.extra.PluginTab;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class IminhasPlogonTab implements PluginTab {

    private JLabel initLabel;
    private volatile boolean initReceived;

    @Override
    public String getTabName() {
        return "Iminha's Plogon";
    }

    @Override
    public Component getTabContents() {
        TitleBorderFullsizePanel panel = new TitleBorderFullsizePanel("Iminha's Plogon");
        panel.add(new JLabel("It works!"));
        initLabel = new JLabel();
        panel.add(initLabel);
        recalc();
        return panel;
    }

    private void recalc() {
        if (initLabel != null) {
            initLabel.setText(initReceived ? "Init event received!" : "Not yet...");
        }
    }

    @HandleEvents
    public void handleEvents(EventContext context, InitEvent init) {
        initReceived = true;
        recalc();
    }
}
