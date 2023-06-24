package iminhak.anabaseios;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.telestosupport.TelestoGameCommand;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.misc.EchoEvent;
import gg.xp.xivsupport.events.misc.EchoMessage;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.marks.ClearAutoMarkRequest;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ScanMe
public class P12SP1Stuff extends AutoChildEventHandler implements FilteredEventHandler {

    private static final Logger log = LoggerFactory.getLogger(P12SP1Stuff.class);

    public P12SP1Stuff(XivState state, StatusEffectRepository buffs, PersistenceProvider pers) {
        this.state = state;
        this.buffs = buffs;
        String settingKeyBase = "triggers.iminha.p12sp1.";
        lcEchoEnable = new BooleanSetting(pers, settingKeyBase + "lcEcho.enabled", false);
    }

    private final XivState state;
    private final StatusEffectRepository buffs;
    private final BooleanSetting lcEchoEnable;

    private XivState getState() {
        return state;
    }

    private StatusEffectRepository getBuffs() {
        return buffs;
    }

    public BooleanSetting getLcEchoEnable() {
        return lcEchoEnable;
    }

    @Override
    public boolean enabled(EventContext context) {
        return state.dutyIs(KnownDuty.P12S);
    }

    @AutoFeed
    private final SequentialTrigger<BaseEvent> limitCutEcho = SqtTemplates.sq(60_000,
            AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x82F3),
            (e1, s) -> {
                List<HeadMarkerEvent> headmarkers = s.waitEventsQuickSuccession(8, HeadMarkerEvent.class, hme -> true);
                int offset = (int)headmarkers.get(0).getMarkerId() - 1;
                s.accept(new TelestoGameCommand("/e Limit Cut:"));
                for (int i = 0; i < headmarkers.size(); i++) {
                    s.accept(new TelestoGameCommand(String.format(
                            "/e %s: %s",
                            i + 1,
                            headmarkers.get(i).getTarget().getName()
                    )));
                }
            });
}
